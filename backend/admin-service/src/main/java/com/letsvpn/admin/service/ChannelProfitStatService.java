package com.letsvpn.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.admin.entity.ChannelProfitStat;
import com.letsvpn.admin.entity.MerchantChannelConfig;
import com.letsvpn.admin.mapper.ChannelProfitStatMapper;
import com.letsvpn.admin.mapper.MerchantChannelConfigMapper;
import com.letsvpn.common.core.dto.ChannelPlatformStatDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelProfitStatService {

    private final ChannelProfitStatMapper statMapper;
    private final MerchantChannelConfigMapper channelConfigMapper;
    @Lazy private final PayServiceFacade payServiceFacade;

    public Page<ChannelProfitStat> list(String channelName, String startDate, String endDate,
                                        long pageNum, long pageSize) {
        long idx  = Math.max(pageNum, 1L);
        long size = Math.max(1L, Math.min(pageSize, 200L));

        LambdaQueryWrapper<ChannelProfitStat> wrapper = Wrappers.<ChannelProfitStat>lambdaQuery()
                .orderByDesc(ChannelProfitStat::getStatDate)
                .orderByAsc(ChannelProfitStat::getChannelId);

        if (StringUtils.hasText(channelName)) {
            wrapper.like(ChannelProfitStat::getChannelName, channelName.trim());
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(ChannelProfitStat::getStatDate, startDate.trim());
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(ChannelProfitStat::getStatDate, endDate.trim());
        }
        return statMapper.selectPage(new Page<>(idx, size), wrapper);
    }

    /**
     * 将 pay-service 返回的 (channel_id, platform_id, amount) 明细聚合后 upsert 到统计表。
     * system_amount      = Σ(所有商户的 platformAmount)
     * channel_fee_income = Σ(platformAmount × merchant_channel_config.fee_rate)
     */
    public void upsertDailyStat(String statDate, List<ChannelPlatformStatDTO> rawStats) {
        if (rawStats == null || rawStats.isEmpty()) {
            log.info("No channel platform stats to upsert for date={}", statDate);
            return;
        }

        Map<Long, List<ChannelPlatformStatDTO>> byChannel = rawStats.stream()
                .collect(Collectors.groupingBy(ChannelPlatformStatDTO::getChannelId));

        for (Map.Entry<Long, List<ChannelPlatformStatDTO>> entry : byChannel.entrySet()) {
            Long channelId = entry.getKey();
            List<ChannelPlatformStatDTO> platformStats = entry.getValue();

            String channelName = platformStats.get(0).getChannelName();
            BigDecimal systemAmount = platformStats.stream()
                    .map(ChannelPlatformStatDTO::getSystemAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal channelFeeIncome = platformStats.stream()
                    .map(dto -> {
                        if (dto.getPlatformId() == null) return BigDecimal.ZERO;
                        MerchantChannelConfig cfg = channelConfigMapper.selectOne(
                                Wrappers.<MerchantChannelConfig>lambdaQuery()
                                        .eq(MerchantChannelConfig::getPayConfigChannelId, channelId)
                                        .eq(MerchantChannelConfig::getPlatformId, dto.getPlatformId())
                                        .last("LIMIT 1"));
                        if (cfg == null || cfg.getFeeRate() == null) return BigDecimal.ZERO;
                        return dto.getSystemAmount().multiply(cfg.getFeeRate());
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            ChannelProfitStat existing = statMapper.selectOne(
                    Wrappers.<ChannelProfitStat>lambdaQuery()
                            .eq(ChannelProfitStat::getStatDate, statDate)
                            .eq(ChannelProfitStat::getChannelId, channelId));

            if (existing != null) {
                existing.setChannelName(channelName);
                existing.setSystemAmount(systemAmount);
                existing.setChannelFeeIncome(channelFeeIncome);
                statMapper.updateById(existing);
            } else {
                ChannelProfitStat stat = new ChannelProfitStat();
                stat.setStatDate(statDate);
                stat.setChannelId(channelId);
                stat.setChannelName(channelName);
                stat.setSystemAmount(systemAmount);
                stat.setChannelFeeIncome(channelFeeIncome);
                statMapper.insert(stat);
            }
        }
        log.info("Upserted channel profit stats for date={}, channels={}", statDate, byChannel.size());
    }

    /** 手动补跑指定日期的统计（从 pay-service 拉取后 upsert） */
    public void triggerForDate(String date) {
        log.info("Manual trigger channel profit stat, date={}", date);
        List<ChannelPlatformStatDTO> stats = payServiceFacade.fetchDailyChannelPlatformStats(date);
        upsertDailyStat(date, stats);
    }
}
