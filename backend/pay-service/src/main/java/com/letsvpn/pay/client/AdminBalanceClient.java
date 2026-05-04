package com.letsvpn.pay.client;

import com.letsvpn.common.core.response.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

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
}
