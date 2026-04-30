package com.letsvpn.pay.client;

import com.letsvpn.common.core.dto.MerchantChannelConfigDTO;
import com.letsvpn.common.core.response.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "admin-service", path = "/admin/merchant/channel-config",
        fallbackFactory = AdminServiceClientFallbackFactory.class)
public interface AdminServiceClient {

    @GetMapping("/internal/list")
    R<List<MerchantChannelConfigDTO>> getMerchantChannelConfigs(
            @RequestParam("platformId") Integer platformId,
            @RequestParam(value = "channelType", required = false) String channelType);

    @PostMapping("/internal/disable")
    R<Void> disableMerchantChannelConfig(@RequestParam("id") Long id);
}
