package com.letsvpn.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.letsvpn.admin.dto.MerchantChannelConfigRequest;
import com.letsvpn.admin.entity.MerchantChannelConfig;
import com.letsvpn.admin.mapper.MerchantChannelConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantChannelConfigService {

    private final MerchantChannelConfigMapper mapper;

    /** 查询商户所有通道配置（可按类型筛选） */
    public List<MerchantChannelConfig> listByPlatformId(Integer platformId, String channelType) {
        LambdaQueryWrapper<MerchantChannelConfig> q = new LambdaQueryWrapper<MerchantChannelConfig>()
                .eq(MerchantChannelConfig::getPlatformId, platformId)
                .eq(channelType != null, MerchantChannelConfig::getChannelType, channelType)
                .orderByAsc(MerchantChannelConfig::getId);
        return mapper.selectList(q);
    }

    /** 查询单条 */
    public MerchantChannelConfig getById(Long id) {
        return mapper.selectById(id);
    }

    /** 新建 */
    public void create(MerchantChannelConfigRequest req) {
        MerchantChannelConfig config = toEntity(req, new MerchantChannelConfig());
        mapper.insert(config);
    }

    /** 更新 */
    public void update(MerchantChannelConfigRequest req) {
        if (req.getId() == null) throw new IllegalArgumentException("id 不能为空");
        MerchantChannelConfig config = mapper.selectById(req.getId());
        if (config == null) throw new IllegalArgumentException("记录不存在: " + req.getId());
        toEntity(req, config);
        mapper.updateById(config);
    }

    /** 切换启用/禁用 */
    public void toggleEnabled(Long id, Integer enabled) {
        mapper.update(null, new LambdaUpdateWrapper<MerchantChannelConfig>()
                .set(MerchantChannelConfig::getEnabled, enabled)
                .eq(MerchantChannelConfig::getId, id));
    }

    /** 删除 */
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private MerchantChannelConfig toEntity(MerchantChannelConfigRequest req, MerchantChannelConfig e) {
        if (req.getPlatformId() != null)         e.setPlatformId(req.getPlatformId());
        if (req.getPayConfigChannelId() != null) e.setPayConfigChannelId(req.getPayConfigChannelId());
        if (req.getChannelType() != null)        e.setChannelType(req.getChannelType());
        if (req.getDailyLimit() != null)         e.setDailyLimit(req.getDailyLimit());
        if (req.getWeight() != null)             e.setWeight(req.getWeight());
        if (req.getEnabled() != null)            e.setEnabled(req.getEnabled());
        if (req.getStartTime() != null)          e.setStartTime(req.getStartTime());
        if (req.getEndTime() != null)            e.setEndTime(req.getEndTime());
        if (req.getSettlementCycle() != null)    e.setSettlementCycle(req.getSettlementCycle());
        if (req.getAutoSettle() != null)         e.setAutoSettle(req.getAutoSettle());
        if (req.getMinAmount() != null)          e.setMinAmount(req.getMinAmount());
        if (req.getMaxAmount() != null)          e.setMaxAmount(req.getMaxAmount());
        if (req.getCurrency() != null)           e.setCurrency(req.getCurrency());
        if (req.getPayMode() != null)            e.setPayMode(req.getPayMode());
        if (req.getPayPage() != null)            e.setPayPage(req.getPayPage());
        if (req.getFeeRate() != null)            e.setFeeRate(req.getFeeRate());
        if (req.getFeeFixed() != null)           e.setFeeFixed(req.getFeeFixed());
        if (req.getTieredRateEnabled() != null)  e.setTieredRateEnabled(req.getTieredRateEnabled());
        if (req.getTieredRateConfig() != null)   e.setTieredRateConfig(req.getTieredRateConfig());
        return e;
    }
}
