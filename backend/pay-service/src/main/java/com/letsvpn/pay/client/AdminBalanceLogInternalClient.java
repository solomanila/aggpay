package com.letsvpn.pay.client;

import com.letsvpn.common.core.dto.OrderCreatedAtDTO;
import com.letsvpn.common.core.response.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "admin-service",
        path = "/admin/internal/balance-log",
        contextId = "adminBalanceLogInternalClient",
        fallbackFactory = AdminBalanceLogInternalClientFallbackFactory.class)
public interface AdminBalanceLogInternalClient {

    @PostMapping("/created-at-by-order-ids")
    R<List<OrderCreatedAtDTO>> getCreatedAtByOrderIds(@RequestBody List<String> orderIds);
}
