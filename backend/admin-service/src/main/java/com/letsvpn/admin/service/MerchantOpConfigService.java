package com.letsvpn.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.letsvpn.admin.dto.MerchantOpConfigRequest;
import com.letsvpn.admin.entity.MerchantOpConfig;
import com.letsvpn.admin.mapper.MerchantOpConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantOpConfigService {

    private final MerchantOpConfigMapper mapper;

    public MerchantOpConfig getByPlatformId(Integer platformId) {
        return mapper.selectOne(new LambdaQueryWrapper<MerchantOpConfig>()
                .eq(MerchantOpConfig::getPlatformId, platformId));
    }

    /** 新增或更新（upsert by platform_id） */
    public void upsert(MerchantOpConfigRequest req) {
        MerchantOpConfig existing = getByPlatformId(req.getPlatformId());
        if (existing == null) {
            MerchantOpConfig config = toEntity(req, new MerchantOpConfig());
            mapper.insert(config);
        } else {
            toEntity(req, existing);
            mapper.updateById(existing);
        }
    }

    public void update(MerchantOpConfigRequest req) {
        MerchantOpConfig existing = getByPlatformId(req.getPlatformId());
        if (existing == null) throw new IllegalArgumentException("商户配置不存在: " + req.getPlatformId());
        toEntity(req, existing);
        mapper.updateById(existing);
    }

    private MerchantOpConfig toEntity(MerchantOpConfigRequest req, MerchantOpConfig config) {
        config.setPlatformId(req.getPlatformId());
        if (req.getEmail() != null)                    config.setEmail(req.getEmail());
        if (req.getAgentId() != null)                  config.setAgentId(req.getAgentId());
        if (req.getRemark() != null)                   config.setRemark(req.getRemark());
        if (req.getDailyPayOrderLimit() != null)       config.setDailyPayOrderLimit(req.getDailyPayOrderLimit());
        if (req.getDailyWithdrawCountLimit() != null)  config.setDailyWithdrawCountLimit(req.getDailyWithdrawCountLimit());
        if (req.getDailyWithdrawAmountLimit() != null) config.setDailyWithdrawAmountLimit(req.getDailyWithdrawAmountLimit());
        if (req.getDailyPayLimit() != null)            config.setDailyPayLimit(req.getDailyPayLimit());
        if (req.getDailyPayoutLimit() != null)         config.setDailyPayoutLimit(req.getDailyPayoutLimit());
        if (req.getLargePayoutRiskEnabled() != null)   config.setLargePayoutRiskEnabled(req.getLargePayoutRiskEnabled());
        if (req.getLargePayoutRiskAmount() != null)    config.setLargePayoutRiskAmount(req.getLargePayoutRiskAmount());
        if (req.getTelegramGroupId() != null)          config.setTelegramGroupId(req.getTelegramGroupId());
        if (req.getSettlementNotify() != null)         config.setSettlementNotify(req.getSettlementNotify());
        return config;
    }
}
