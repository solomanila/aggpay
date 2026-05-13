package com.letsvpn.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.letsvpn.admin.dto.MerchantBalanceOpRequest;
import com.letsvpn.admin.entity.MerchantBalance;
import com.letsvpn.admin.entity.MerchantBalanceLog;
import com.letsvpn.admin.entity.MerchantChannelConfig;
import com.letsvpn.admin.mapper.MerchantBalanceLogMapper;
import com.letsvpn.admin.mapper.MerchantBalanceMapper;
import com.letsvpn.admin.mapper.MerchantChannelConfigMapper;
import com.letsvpn.common.core.dto.OrderInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantBalanceService {

    private final MerchantBalanceMapper balanceMapper;
    private final MerchantBalanceLogMapper logMapper;
    private final MerchantChannelConfigMapper channelConfigMapper;

    public List<MerchantBalance> listByPlatformId(Integer platformId) {
        return balanceMapper.selectList(new LambdaQueryWrapper<MerchantBalance>()
                .eq(MerchantBalance::getPlatformId, platformId));
    }

    public MerchantBalance getOrCreate(Integer platformId, String currency) {
        MerchantBalance balance = balanceMapper.selectOne(new LambdaQueryWrapper<MerchantBalance>()
                .eq(MerchantBalance::getPlatformId, platformId)
                .eq(MerchantBalance::getCurrency, currency));
        if (balance == null) {
            balance = new MerchantBalance();
            balance.setPlatformId(platformId);
            balance.setCurrency(currency);
            balance.setAvailable(BigDecimal.ZERO);
            balance.setFrozen(BigDecimal.ZERO);
            balanceMapper.insert(balance);
        }
        return balance;
    }

    @Transactional
    public void recharge(MerchantBalanceOpRequest req, Long operatorId) {
        MerchantBalance b = getOrCreate(req.getPlatformId(), req.getCurrency());
        BigDecimal prev = b.getAvailable();
        b.setAvailable(prev.add(req.getAmount()));
        balanceMapper.updateById(b);
        writeLog(b.getPlatformId(), b.getCurrency(), "RECHARGE", req.getAmount(),
                prev, b.getAvailable(), b.getFrozen(), b.getFrozen(), req.getRemark(), operatorId, null, null);
    }

    @Transactional
    public void deduct(MerchantBalanceOpRequest req, Long operatorId) {
        MerchantBalance b = getOrCreate(req.getPlatformId(), req.getCurrency());
        if (b.getAvailable().compareTo(req.getAmount()) < 0)
            throw new IllegalStateException("可用余额不足");
        BigDecimal prev = b.getAvailable();
        b.setAvailable(prev.subtract(req.getAmount()));
        balanceMapper.updateById(b);
        writeLog(b.getPlatformId(), b.getCurrency(), "DEDUCT", req.getAmount(),
                prev, b.getAvailable(), b.getFrozen(), b.getFrozen(), req.getRemark(), operatorId, null, null);
    }

    @Transactional
    public void freeze(MerchantBalanceOpRequest req, Long operatorId) {
        MerchantBalance b = getOrCreate(req.getPlatformId(), req.getCurrency());
        if (b.getAvailable().compareTo(req.getAmount()) < 0)
            throw new IllegalStateException("可用余额不足，无法冻结");
        BigDecimal prevAvail = b.getAvailable();
        BigDecimal prevFrozen = b.getFrozen();
        b.setAvailable(prevAvail.subtract(req.getAmount()));
        b.setFrozen(prevFrozen.add(req.getAmount()));
        balanceMapper.updateById(b);
        writeLog(b.getPlatformId(), b.getCurrency(), "FREEZE", req.getAmount(),
                prevAvail, b.getAvailable(), prevFrozen, b.getFrozen(), req.getRemark(), operatorId, null, null);
    }

    @Transactional
    public void unfreeze(MerchantBalanceOpRequest req, Long operatorId) {
        MerchantBalance b = getOrCreate(req.getPlatformId(), req.getCurrency());
        if (b.getFrozen().compareTo(req.getAmount()) < 0)
            throw new IllegalStateException("冻结余额不足，无法解冻");
        BigDecimal prevAvail = b.getAvailable();
        BigDecimal prevFrozen = b.getFrozen();
        b.setFrozen(prevFrozen.subtract(req.getAmount()));
        b.setAvailable(prevAvail.add(req.getAmount()));
        balanceMapper.updateById(b);
        writeLog(b.getPlatformId(), b.getCurrency(), "UNFREEZE", req.getAmount(),
                prevAvail, b.getAvailable(), prevFrozen, b.getFrozen(), req.getRemark(), operatorId, null, null);
    }

    @Transactional
    public void withdraw(MerchantBalanceOpRequest req, Long operatorId) {
        MerchantBalance b = getOrCreate(req.getPlatformId(), req.getCurrency());
        if (b.getAvailable().compareTo(req.getAmount()) < 0)
            throw new IllegalStateException("可用余额不足");
        BigDecimal prev = b.getAvailable();
        b.setAvailable(prev.subtract(req.getAmount()));
        balanceMapper.updateById(b);
        writeLog(b.getPlatformId(), b.getCurrency(), "WITHDRAW", req.getAmount(),
                prev, b.getAvailable(), b.getFrozen(), b.getFrozen(), req.getRemark(), operatorId, null, null);
    }

    public List<MerchantBalanceLog> listLogs(Integer platformId, String currency, int page, int size) {
        return logMapper.selectList(new LambdaQueryWrapper<MerchantBalanceLog>()
                .eq(MerchantBalanceLog::getPlatformId, platformId)
                .eq(currency != null, MerchantBalanceLog::getCurrency, currency)
                .orderByDesc(MerchantBalanceLog::getId)
                .last("LIMIT " + Math.max(1, size) + " OFFSET " + (Math.max(0, page - 1) * size)));
    }

    @Transactional
    public void creditOrderIncome(OrderInfoDTO orderInfo, String currencyCode) {
        long alreadySettled = logMapper.selectCount(new LambdaQueryWrapper<MerchantBalanceLog>()
                .eq(MerchantBalanceLog::getOpType, "SETTLEMENT")
                .eq(MerchantBalanceLog::getOrderId, orderInfo.getOrderId()));
        if (alreadySettled > 0) {
            log.warn("creditOrderIncome: orderId={} already settled, skip", orderInfo.getOrderId());
            return;
        }

        BigDecimal feeRate = BigDecimal.ZERO;
        if (orderInfo.getPayConfigChannelId() != null) {
            MerchantChannelConfig config = channelConfigMapper.selectOne(
                    new LambdaQueryWrapper<MerchantChannelConfig>()
                            .eq(MerchantChannelConfig::getPlatformId, orderInfo.getPlatformId())
                            .eq(MerchantChannelConfig::getPayConfigChannelId, orderInfo.getPayConfigChannelId())
                            .last("LIMIT 1"));
            if (config != null && config.getFeeRate() != null) {
                feeRate = config.getFeeRate();
            }
        }
        BigDecimal income = orderInfo.getRealAmount()
                .multiply(BigDecimal.ONE.subtract(feeRate))
                .setScale(2, RoundingMode.HALF_DOWN);

        MerchantBalance b = getOrCreate(orderInfo.getPlatformId(), currencyCode);
        BigDecimal prev = b.getAvailable();
        b.setAvailable(prev.add(income));
        balanceMapper.updateById(b);
        writeLog(b.getPlatformId(), b.getCurrency(), "SETTLEMENT", income,
                prev, b.getAvailable(), b.getFrozen(), b.getFrozen(),
                "订单收款", null,
                orderInfo.getOrderId(), orderInfo.getOtherOrderId());
    }

    private void writeLog(Integer platformId, String currency, String opType, BigDecimal amount,
                          BigDecimal beforeAvail, BigDecimal afterAvail,
                          BigDecimal beforeFrozen, BigDecimal afterFrozen,
                          String remark, Long operatorId,
                          String orderId, String otherOrderId) {
        MerchantBalanceLog entry = new MerchantBalanceLog();
        entry.setPlatformId(platformId);
        entry.setCurrency(currency);
        entry.setOpType(opType);
        entry.setAmount(amount);
        entry.setBeforeAvailable(beforeAvail);
        entry.setAfterAvailable(afterAvail);
        entry.setBeforeFrozen(beforeFrozen);
        entry.setAfterFrozen(afterFrozen);
        entry.setRemark(remark);
        entry.setOperatorId(operatorId);
        entry.setOrderId(orderId);
        entry.setOtherOrderId(otherOrderId);
        logMapper.insert(entry);
    }
}
