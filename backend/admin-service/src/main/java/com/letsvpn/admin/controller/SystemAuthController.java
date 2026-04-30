package com.letsvpn.admin.controller;

import com.letsvpn.admin.service.system.SystemUserAuthService;
import com.letsvpn.common.core.dto.AdminLoginRequest;
import com.letsvpn.common.core.dto.AdminLoginResponse;
import com.letsvpn.common.core.dto.AdminLogoutRequest;
import com.letsvpn.common.core.dto.AdminRegisterRequest;
import com.letsvpn.common.core.dto.AdminRegisterResponse;
import com.letsvpn.common.core.response.R;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/system/auth")
@RequiredArgsConstructor
public class SystemAuthController {

    private final SystemUserAuthService systemUserAuthService;

    @PostMapping("/register")
    public R<AdminRegisterResponse> register(@Valid @RequestBody AdminRegisterRequest request) {
        return R.success(systemUserAuthService.register(request));
    }

    @GetMapping("/otp-auth-url")
    public R<String> getOtpAuthUrl(@RequestParam String account) {
        return R.success(systemUserAuthService.getOtpAuthUrl(account));
    }

    @PostMapping("/logout")
    public R<Void> logout(@Valid @RequestBody AdminLogoutRequest request) {
        systemUserAuthService.logout(request.getToken());
        return R.success();
    }
}
