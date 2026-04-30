package com.letsvpn.auth.service;

import com.letsvpn.auth.feign.AdminSystemAuthClient;
import com.letsvpn.common.core.dto.AdminLoginRequest;
import com.letsvpn.common.core.dto.AdminLoginResponse;
import com.letsvpn.common.core.response.R;
import com.letsvpn.common.core.util.JwtUtils;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminGatewayAuthService {

    private final AdminSystemAuthClient adminSystemAuthClient;
    private final TokenService tokenService;

    public Map<String, Object> login(AdminLoginRequest request) {
        R<AdminLoginResponse> response = adminSystemAuthClient.verifyLogin(request);
        if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
            throw new IllegalArgumentException(response != null ? response.getMsg() : "管理员认证失败");
        }
        AdminLoginResponse profile = response.getData();
        String token = JwtUtils.generateToken(profile.getAccount(), profile.getUserId(), "ADMIN");
        tokenService.cacheAdminSession(token, profile);
        Map<String, Object> payload = new HashMap<>();
        payload.put("token", token);
        payload.put("expiresIn", JwtUtils.getExpiration(token));
        payload.put("profile", profile);
        return payload;
    }

    public void logout(String token) {
        tokenService.blacklistAdminToken(token);
    }
}
