package com.letsvpn.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.letsvpn.admin.client.PayServiceClient;
import com.letsvpn.admin.entity.MerchantBalance;
import com.letsvpn.admin.entity.SystemUserAuth;
import com.letsvpn.admin.mapper.MerchantBalanceMapper;
import com.letsvpn.admin.mapper.SystemUserAuthMapper;
import com.letsvpn.common.core.dto.MerchantDashboardResponse;
import com.letsvpn.common.core.dto.MerchantPlatformInfoDTO;
import com.letsvpn.common.core.response.R;
import com.letsvpn.common.core.util.AuthContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/merchant/dashboard")
@RequiredArgsConstructor
public class MerchantDashboardController {

    private final SystemUserAuthMapper userAuthMapper;
    private final MerchantBalanceMapper balanceMapper;
    private final PayServiceClient payServiceClient;

    @GetMapping
    public R<MerchantDashboardResponse> dashboard() {
        Long userId = AuthContextHolder.getRequiredUserId();

        SystemUserAuth user = userAuthMapper.selectById(userId);
        if (user == null || user.getPlatformId() == null) {
            return R.fail("未绑定商户平台");
        }
        Integer platformId = user.getPlatformId();

        // 查余额（admin DB）
        List<MerchantBalance> balances = balanceMapper.selectList(
                new LambdaQueryWrapper<MerchantBalance>()
                        .eq(MerchantBalance::getPlatformId, platformId)
                        .orderByAsc(MerchantBalance::getCurrency));

        // Feign 获取 platformNo / secretKey（pay-service → aggpay DB）
        R<MerchantPlatformInfoDTO> infoResp = payServiceClient.getMerchantPlatformInfo(platformId);
        MerchantPlatformInfoDTO info = (infoResp != null && infoResp.getData() != null)
                ? infoResp.getData() : new MerchantPlatformInfoDTO();

        // 待结算 = INR 可用余额（merchant_balance.available）
        BigDecimal pendingSettlement = balances.stream()
                .filter(b -> "INR".equals(b.getCurrency()))
                .map(MerchantBalance::getAvailable)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        MerchantDashboardResponse resp = new MerchantDashboardResponse();
        resp.setKeyId(info.getPlatformNo());
        resp.setAppKey(info.getSecretKey());
        resp.setPendingSettlement(pendingSettlement);
        resp.setBalances(balances.stream().map(b -> {
            MerchantDashboardResponse.BalanceItem item = new MerchantDashboardResponse.BalanceItem();
            item.setCurrency(b.getCurrency());
            item.setAvailable(b.getAvailable());
            item.setFrozen(b.getFrozen());
            return item;
        }).collect(Collectors.toList()));

        return R.success(resp);
    }
}
