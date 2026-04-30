package com.letsvpn.auth.controller;

import com.letsvpn.auth.service.AdminGatewayAuthService;
import com.letsvpn.common.core.dto.AdminLoginRequest;
import com.letsvpn.common.core.dto.AdminLoginResponse;
import com.letsvpn.common.core.dto.AdminLogoutRequest;
import com.letsvpn.common.core.response.R;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminGatewayAuthService adminGatewayAuthService;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody AdminLoginRequest request,
                                        HttpServletRequest servletRequest) {
        request.setClientIp(servletRequest.getRemoteAddr());
        return R.success(adminGatewayAuthService.login(request));
    }

    @PostMapping("/logout")
    public R<Void> logout(@Valid @RequestBody AdminLogoutRequest request) {
        adminGatewayAuthService.logout(request.getToken());
        return R.success();
    }
}
