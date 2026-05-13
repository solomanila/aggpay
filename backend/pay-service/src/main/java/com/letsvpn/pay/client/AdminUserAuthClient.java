package com.letsvpn.pay.client;

import com.letsvpn.common.core.dto.PlatformAccountDTO;
import com.letsvpn.common.core.response.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "admin-service",
        path = "/admin/internal/user-auth",
        contextId = "adminUserAuthClient",
        fallbackFactory = AdminUserAuthClientFallbackFactory.class)
public interface AdminUserAuthClient {

    @GetMapping("/accounts-by-platform-ids")
    R<List<PlatformAccountDTO>> getAccountsByPlatformIds(
            @RequestParam("platformIds") List<Integer> platformIds);

    @GetMapping("/platform-id-by-account")
    R<Integer> getPlatformIdByAccount(@RequestParam("account") String account);
}
