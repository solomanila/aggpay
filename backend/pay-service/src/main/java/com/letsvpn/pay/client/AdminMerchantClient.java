package com.letsvpn.pay.client;

import com.letsvpn.common.core.response.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "admin-service", contextId = "adminMerchantClient", path = "/admin/merchant")
public interface AdminMerchantClient {

    @PostMapping("/create")
    R<Map<String, Object>> createMerchant(@RequestBody Map<String, Object> req);
}
