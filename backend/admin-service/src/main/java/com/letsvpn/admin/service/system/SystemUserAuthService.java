package com.letsvpn.admin.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.letsvpn.admin.entity.SystemUserAuth;
import com.letsvpn.common.core.dto.AdminLoginRequest;
import com.letsvpn.common.core.dto.AdminLoginResponse;
import com.letsvpn.common.core.dto.AdminRegisterRequest;
import com.letsvpn.common.core.dto.AdminRegisterResponse;
import java.util.List;

public interface SystemUserAuthService extends IService<SystemUserAuth> {

    List<SystemUserAuth> listCached();

    AdminRegisterResponse register(AdminRegisterRequest request);

    AdminLoginResponse verifyLogin(AdminLoginRequest request);

    /**
     * Returns the OTP auth URL for the given account, if a Google secret exists.
     */
    String getOtpAuthUrl(String account);

    void logout(String token);

    void evictUserCache();
}
