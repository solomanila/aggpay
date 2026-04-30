package com.letsvpn.admin.controller;

import com.letsvpn.admin.service.system.SystemUserAuthService;
import com.letsvpn.common.core.dto.AdminLoginRequest;
import com.letsvpn.common.core.dto.AdminLoginResponse;
import com.letsvpn.common.core.response.R;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/system/internal/auth")
@RequiredArgsConstructor
public class SystemAuthInternalController {

    private final SystemUserAuthService systemUserAuthService;

    @PostMapping("/login")
    public R<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request,
                                       HttpServletRequest servletRequest) {
        request.setClientIp(servletRequest.getRemoteAddr());
        return R.success(systemUserAuthService.verifyLogin(request));
    }
}
