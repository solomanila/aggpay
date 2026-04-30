package com.letsvpn.admin.controller;

import com.letsvpn.admin.dto.MerchantOpConfigRequest;
import com.letsvpn.admin.entity.MerchantOpConfig;
import com.letsvpn.admin.service.MerchantOpConfigService;
import com.letsvpn.common.core.response.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商户运营配置（限额/风控/通知）
 * platform_id 与 pay-service.pay_platform_info.platform_id 对应
 */
@RestController
@RequestMapping("/admin/merchant/op-config")
@RequiredArgsConstructor
public class MerchantOpConfigController {

    private final MerchantOpConfigService service;

    /** 查询某商户配置 */
    @GetMapping("/{platformId}")
    public R<MerchantOpConfig> get(@PathVariable Integer platformId) {
        MerchantOpConfig config = service.getByPlatformId(platformId);
        return config != null ? R.success(config) : R.fail("配置不存在");
    }

    /** 新建或更新（upsert） */
    @PostMapping("/upsert")
    public R<Void> upsert(@RequestBody MerchantOpConfigRequest req) {
        service.upsert(req);
        return R.success(null);
    }

    /** 更新（必须已存在） */
    @PutMapping("/update")
    public R<Void> update(@RequestBody MerchantOpConfigRequest req) {
        service.update(req);
        return R.success(null);
    }
}
