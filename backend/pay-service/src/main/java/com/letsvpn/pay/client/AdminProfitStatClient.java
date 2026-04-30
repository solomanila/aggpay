package com.letsvpn.pay.client;

import com.letsvpn.common.core.dto.ChannelProfitStatDTO;
import com.letsvpn.common.core.response.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "admin-service", path = "/admin/channel-profit-stat",
        contextId = "adminProfitStatClient",
        fallbackFactory = AdminProfitStatClientFallbackFactory.class)
public interface AdminProfitStatClient {

    @PostMapping("/batch")
    R<Void> batchSave(@RequestBody List<ChannelProfitStatDTO> stats);
}
