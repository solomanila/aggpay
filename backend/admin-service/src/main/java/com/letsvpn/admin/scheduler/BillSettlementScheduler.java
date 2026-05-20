package com.letsvpn.admin.scheduler;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.letsvpn.admin.entity.Bill;
import com.letsvpn.admin.entity.MerchantChannelConfig;
import com.letsvpn.admin.entity.SystemUserAuth;
import com.letsvpn.admin.mapper.BillMapper;
import com.letsvpn.admin.mapper.MerchantChannelConfigMapper;
import com.letsvpn.admin.mapper.SystemUserAuthMapper;
import com.letsvpn.admin.service.PayServiceFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 账单结算定时任务：每30分钟执行一次，汇总各通道成功订单生成账单。
 *
 * 分布式锁：同一时刻只允许一个实例运行，通过 Redis SETNX 实现。
 * 锁的 TTL 设为 28 分钟，短于调度周期，保证下次触发时锁必然已释放。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillSettlementScheduler {

    private static final String LOCK_KEY     = "lock:bill:settlement";
    private static final long   LOCK_TTL_MIN = 28;

    private final MerchantChannelConfigMapper merchantChannelConfigMapper;
    private final SystemUserAuthMapper         systemUserAuthMapper;
    private final BillMapper                   billMapper;
    private final PayServiceFacade             payServiceFacade;
    private final StringRedisTemplate          stringRedisTemplate;

    @Scheduled(cron = "0 0/30 * * * ?")
    public void settle() {
        // 分布式锁：竞争失败则直接跳过，由获锁实例独立执行
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY, "1", LOCK_TTL_MIN, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(locked)) {
            log.info("BillSettlementScheduler: another instance is running, skip");
            return;
        }

        try {
            doSettle();
        } finally {
            stringRedisTemplate.delete(LOCK_KEY);
        }
    }

    private void doSettle() {
        log.info("BillSettlementScheduler started");

        // 1. 查询所有开启的商户通道配置（admin schema）
        List<MerchantChannelConfig> configs = merchantChannelConfigMapper.selectList(
                Wrappers.<MerchantChannelConfig>lambdaQuery()
                        .eq(MerchantChannelConfig::getEnabled, 1)
                        .select(MerchantChannelConfig::getPlatformId,
                                MerchantChannelConfig::getPayConfigChannelId));

        if (configs == null || configs.isEmpty()) {
            log.info("BillSettlementScheduler: no enabled channels, skip");
            return;
        }

        // 时间窗口：过去 30 分钟
        Date startTime = new Date(System.currentTimeMillis() - 30L * 60 * 1000);

        int success = 0, skip = 0, fail = 0;

        for (MerchantChannelConfig config : configs) {
            Long    channelId  = config.getPayConfigChannelId();
            Integer platformId = config.getPlatformId();

            if (channelId == null || platformId == null) {
                skip++;
                continue;
            }

            try {
                // 2a. 过去30分钟该通道成功且未结算订单的总金额（跨 schema via Feign）
                BigDecimal amount = payServiceFacade.fetchOrderSumByChannel(channelId, startTime);
                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    log.debug("No unsettled orders for channelId={} platformId={}", channelId, platformId);
                    skip++;
                    continue;
                }

                // 2b. 通道名称（跨 schema via Feign）
                String channelTitle = payServiceFacade.fetchChannelTitleById(channelId);

                // 2c. 商户账号（admin.system_user_auth）
                String account = resolveAccount(platformId);

                // 2d. 插入 admin.bill
                Date now  = new Date();
                Bill bill = new Bill();
                bill.setAccount(account);
                bill.setPlatformId(platformId.longValue());
                bill.setChannelTitle(channelTitle);
                bill.setPayConfigChannelId(channelId);
                bill.setAmount(amount);
                bill.setStatus(1);
                bill.setSettleAt(now);
                bill.setCreatedAt(now);
                bill.setUpdatedAt(now);
                billMapper.insert(bill);

                Long billId = bill.getId();
                log.info("Bill created: id={} channelId={} platformId={} amount={}", billId, channelId, platformId, amount);

                // 2e. 回写 on_line_id（跨 schema via Feign，幂等：仅更新 on_line_id IS NULL 的行）
                payServiceFacade.updateOrderOnlineId(channelId, startTime, billId);

                success++;

            } catch (Exception e) {
                log.error("BillSettlementScheduler error for channelId={} platformId={}", channelId, platformId, e);
                fail++;
            }
        }

        log.info("BillSettlementScheduler finished: success={} skip={} fail={}", success, skip, fail);
    }

    private String resolveAccount(Integer platformId) {
        SystemUserAuth auth = systemUserAuthMapper.selectOne(
                Wrappers.<SystemUserAuth>lambdaQuery()
                        .eq(SystemUserAuth::getPlatformId, platformId)
                        .last("LIMIT 1"));
        return (auth != null && auth.getAccount() != null) ? auth.getAccount() : "";
    }
}
