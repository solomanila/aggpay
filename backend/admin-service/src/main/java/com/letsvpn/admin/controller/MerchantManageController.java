package com.letsvpn.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.admin.dto.MerchantCreateResponse;
import com.letsvpn.admin.dto.MerchantListItemDTO;
import com.letsvpn.admin.dto.MerchantPageRequest;
import com.letsvpn.admin.dto.MerchantSaveRequest;
import com.letsvpn.admin.service.MerchantManageService;
import com.letsvpn.common.core.response.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商户管理（列表/详情/创建/更新/删除/重置密钥）
 * 数据来源：pay-service（Feign） + admin.merchant_op_config
 */
@RestController
@RequestMapping("/admin/merchant")
@RequiredArgsConstructor
public class MerchantManageController {

    private final MerchantManageService service;

    /** 分页查询商户列表 */
    @GetMapping("/page")
    public R<Page<MerchantListItemDTO>> page(MerchantPageRequest req) {
        return R.success(service.page(req));
    }

    /** 查询单商户详情（用于编辑回填） */
    @GetMapping("/detail/{platformId}")
    public R<Map<String, Object>> detail(@PathVariable Integer platformId) {
        return R.success(service.detail(platformId));
    }

    /** 创建商户 */
    @PostMapping("/create")
    public R<MerchantCreateResponse> create(@RequestBody MerchantSaveRequest req) {
        return R.success(service.create(req));
    }

    /** 更新商户 */
    @PutMapping("/update")
    public R<Void> update(@RequestBody MerchantSaveRequest req) {
        service.update(req);
        return R.success(null);
    }

    /** 启用 / 禁用 */
    @PatchMapping("/toggle-status")
    public R<Void> toggleStatus(@RequestParam Integer platformId, @RequestParam Integer status) {
        service.toggleStatus(platformId, status);
        return R.success(null);
    }

    /** 删除商户 */
    @DeleteMapping("/{platformId}")
    public R<Void> delete(@PathVariable Integer platformId) {
        service.delete(platformId);
        return R.success(null);
    }

    /** 重置 appKey */
    @PostMapping("/reset-key/{platformId}")
    public R<String> resetKey(@PathVariable Integer platformId) {
        return R.success(service.resetAppKey(platformId));
    }
}
