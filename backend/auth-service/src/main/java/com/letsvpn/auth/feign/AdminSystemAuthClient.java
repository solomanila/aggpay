package com.letsvpn.auth.feign;

import com.letsvpn.common.core.dto.AdminLoginRequest;
import com.letsvpn.common.core.dto.AdminLoginResponse;
import com.letsvpn.common.core.response.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "admin-service", path = "/admin/system/internal/auth", contextId = "adminSystemAuthClient")
public interface AdminSystemAuthClient {

    @PostMapping("/login")
    R<AdminLoginResponse> verifyLogin(@RequestBody AdminLoginRequest request);
}
