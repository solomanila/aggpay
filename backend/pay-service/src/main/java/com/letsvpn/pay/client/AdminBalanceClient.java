package com.letsvpn.pay.client;

import com.letsvpn.common.core.dto.MerchantBalanceDTO;
import com.letsvpn.common.core.response.R;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "admin-service",
        path = "/admin/merchant/balance",
        contextId = "adminBalanceClient",
        fallbackFactory = AdminBalanceClientFallbackFactory.class)
public interface AdminBalanceClient {

    @PostMapping("/internal/order-credit")
    R<Void> creditOrderIncome(
            @RequestParam("platformId") Integer platformId,
            @RequestParam("amount") BigDecimal amount);

    @GetMapping("/internal/byPlatformIds")
    R<List<MerchantBalanceDTO>> getBalancesByPlatformIds(
            @RequestParam("platformIds") List<Integer> platformIds);
}
