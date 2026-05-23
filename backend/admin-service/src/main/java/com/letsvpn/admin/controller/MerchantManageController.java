package com.letsvpn.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.admin.client.PayServiceClient;
import com.letsvpn.admin.dto.MerchantCreateResponse;
import com.letsvpn.admin.dto.MerchantListItemDTO;
import com.letsvpn.admin.dto.MerchantPageRequest;
import com.letsvpn.admin.dto.MerchantSaveRequest;
import com.letsvpn.admin.entity.SystemUserAuth;
import com.letsvpn.admin.mapper.SystemUserAuthMapper;
import com.letsvpn.admin.service.MerchantManageService;
import com.letsvpn.common.core.dto.MerchantPlatformInfoDTO;
import com.letsvpn.common.core.response.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 商户管理（列表/详情/创建/更新/删除/重置密钥）
 * 数据来源：pay-service（Feign） + admin.merchant_op_config
 */
@RestController
@RequestMapping("/admin/merchant")
@RequiredArgsConstructor
public class MerchantManageController {

    private final MerchantManageService service;
    private final SystemUserAuthMapper systemUserAuthMapper;
    private final PayServiceClient payServiceClient;

    private static final Pattern REPLACE_URL = Pattern.compile("window\\.location\\.replace\\('(.+?)'\\)");
    private static final Pattern SDK_DATA    = Pattern.compile("\"data\"\\s*:\\s*\"(.+?)\"");

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

    /** 收款测试：构造测试订单并返回支付跳转 URL */
    @PostMapping("/pay-test")
    public R<String> payTest(@RequestBody Map<String, Object> req) {
        Integer platformId = ((Number) req.get("platformId")).intValue();
        Long payConfigChannelId = ((Number) req.get("payConfigChannelId")).longValue();
        BigDecimal amount = new BigDecimal(req.get("amount").toString());

        SystemUserAuth userAuth = systemUserAuthMapper.selectOne(
                Wrappers.<SystemUserAuth>lambdaQuery()
                        .eq(SystemUserAuth::getPlatformId, platformId)
                        .last("LIMIT 1"));
        if (userAuth == null) {
            return R.fail("找不到该商户的用户信息");
        }

        R<MerchantPlatformInfoDTO> infoResp = payServiceClient.getMerchantPlatformInfo(platformId);
        if (infoResp == null || infoResp.getData() == null
                || infoResp.getData().getPlatformNo() == null) {
            return R.fail("获取商户平台信息失败");
        }
        String platformNo = infoResp.getData().getPlatformNo();

        long timestamp = System.currentTimeMillis();
        String fid = "FT" + timestamp;

        try {
            String body = payServiceClient.payTestReq(
                    fid, userAuth.getId(), amount.toPlainString(),
                    platformNo, "test", timestamp, payConfigChannelId);

            if (body == null) {
                return R.fail("收款测试请求失败，pay-service 无响应");
            }

            Matcher m = REPLACE_URL.matcher(body);
            if (m.find()) {
                return R.success(m.group(1));
            }

            m = SDK_DATA.matcher(body);
            if (m.find()) {
                return R.success(m.group(1));
            }

            return R.success(body);
        } catch (Exception e) {
            return R.fail("收款测试失败: " + e.getMessage());
        }
    }
}
