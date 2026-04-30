package com.letsvpn.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.letsvpn.admin.dto.MerchantBalanceOpRequest;
import com.letsvpn.admin.entity.MerchantBalance;
import com.letsvpn.admin.entity.MerchantBalanceLog;
import com.letsvpn.admin.mapper.MerchantBalanceLogMapper;
import com.letsvpn.admin.mapper.MerchantBalanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantBalanceService {

    private final MerchantBalanceMapper balanceMapper;
    private final MerchantBalanceLogMapper logMapper;

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
                prev, b.getAvailable(), b.getFrozen(), b.getFrozen(), req.getRemark(), operatorId);
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
                prev, b.getAvailable(), b.getFrozen(), b.getFrozen(), req.getRemark(), operatorId);
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
                prevAvail, b.getAvailable(), prevFrozen, b.getFrozen(), req.getRemark(), operatorId);
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
                prevAvail, b.getAvailable(), prevFrozen, b.getFrozen(), req.getRemark(), operatorId);
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
                prev, b.getAvailable(), b.getFrozen(), b.getFrozen(), req.getRemark(), operatorId);
    }

    public List<MerchantBalanceLog> listLogs(Integer platformId, String currency, int page, int size) {
        return logMapper.selectList(new LambdaQueryWrapper<MerchantBalanceLog>()
                .eq(MerchantBalanceLog::getPlatformId, platformId)
                .eq(currency != null, MerchantBalanceLog::getCurrency, currency)
                .orderByDesc(MerchantBalanceLog::getId)
                .last("LIMIT " + Math.max(1, size) + " OFFSET " + (Math.max(0, page - 1) * size)));
    }

    private void writeLog(Integer platformId, String currency, String opType, BigDecimal amount,
                          BigDecimal beforeAvail, BigDecimal afterAvail,
                          BigDecimal beforeFrozen, BigDecimal afterFrozen,
                          String remark, Long operatorId) {
        MerchantBalanceLog log = new MerchantBalanceLog();
        log.setPlatformId(platformId);
        log.setCurrency(currency);
        log.setOpType(opType);
        log.setAmount(amount);
        log.setBeforeAvailable(beforeAvail);
        log.setAfterAvailable(afterAvail);
        log.setBeforeFrozen(beforeFrozen);
        log.setAfterFrozen(afterFrozen);
        log.setRemark(remark);
        log.setOperatorId(operatorId);
        logMapper.insert(log);
    }
}
