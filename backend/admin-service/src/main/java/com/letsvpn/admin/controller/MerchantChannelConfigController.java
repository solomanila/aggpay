package com.letsvpn.admin.controller;

import com.letsvpn.admin.dto.MerchantChannelConfigRequest;
import com.letsvpn.admin.entity.MerchantChannelConfig;
import com.letsvpn.admin.service.MerchantChannelConfigService;
import com.letsvpn.common.core.dto.MerchantChannelConfigDTO;
import com.letsvpn.common.core.response.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商户通道配置（代收 & 代付路由权重 / 日限额 / 费率）
 */
@RestController
@RequestMapping("/admin/merchant/channel-config")
@RequiredArgsConstructor
public class MerchantChannelConfigController {

    private final MerchantChannelConfigService service;

    /**
     * 查询商户通道列表
     * @param channelType PAYMENT / PAYOUT / null(全部)
     */
    @GetMapping("/list/{platformId}")
    public R<List<MerchantChannelConfig>> list(
            @PathVariable Integer platformId,
            @RequestParam(required = false) String channelType) {
        return R.success(service.listByPlatformId(platformId, channelType));
    }

    /** 查询单条 */
    @GetMapping("/{id}")
    public R<MerchantChannelConfig> get(@PathVariable Long id) {
        MerchantChannelConfig config = service.getById(id);
        return config != null ? R.success(config) : R.fail("记录不存在");
    }

    /** 新建商户通道配置 */
    @PostMapping("/create")
    public R<Void> create(@RequestBody MerchantChannelConfigRequest req) {
        service.create(req);
        return R.success(null);
    }

    /** 更新商户通道配置 */
    @PutMapping("/update")
    public R<Void> update(@RequestBody MerchantChannelConfigRequest req) {
        service.update(req);
        return R.success(null);
    }

    /** 启用 / 禁用 */
    @PatchMapping("/enabled")
    public R<Void> toggleEnabled(@RequestParam Long id, @RequestParam Integer enabled) {
        service.toggleEnabled(id, enabled);
        return R.success(null);
    }

    /** 删除 */
    @DeleteMapping("/delete")
    public R<Void> delete(@RequestParam Long id) {
        service.delete(id);
        return R.success(null);
    }

    // ── Internal endpoints for pay-service ───────────────────────────

    /** pay-service 调用：获取某商户所有通道配置（启用/禁用均返回） */
    @GetMapping("/internal/list")
    public R<List<MerchantChannelConfigDTO>> internalList(
            @RequestParam Integer platformId,
            @RequestParam(required = false) String channelType) {
        List<MerchantChannelConfigDTO> dtos = service.listByPlatformId(platformId, channelType)
                .stream().map(this::toDTO).collect(Collectors.toList());
        return R.success(dtos);
    }

    /** pay-service 调用：日限额触发后将指定配置行 enabled=0 */
    @PostMapping("/internal/disable")
    public R<Void> internalDisable(@RequestParam Long id) {
        service.toggleEnabled(id, 0);
        return R.success(null);
    }

    private MerchantChannelConfigDTO toDTO(MerchantChannelConfig e) {
        MerchantChannelConfigDTO d = new MerchantChannelConfigDTO();
        d.setId(e.getId());
        d.setPlatformId(e.getPlatformId());
        d.setPayConfigChannelId(e.getPayConfigChannelId());
        d.setEnabled(e.getEnabled());
        d.setWeight(e.getWeight());
        d.setDailyLimit(e.getDailyLimit());
        d.setMinAmount(e.getMinAmount());
        d.setMaxAmount(e.getMaxAmount());
        d.setStartTime(e.getStartTime());
        d.setEndTime(e.getEndTime());
        return d;
    }
}
