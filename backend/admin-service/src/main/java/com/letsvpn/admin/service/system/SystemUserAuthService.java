package com.letsvpn.admin.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.letsvpn.admin.dto.ResetGoogleAuthResponse;
import com.letsvpn.admin.dto.ResetPasswordResponse;
import com.letsvpn.admin.dto.SystemUserPageItemVO;
import com.letsvpn.admin.dto.SystemUserPageRequest;
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

    String getOtpAuthUrl(String account);

    void logout(String token);

    void evictUserCache();

    IPage<SystemUserPageItemVO> pageUsers(SystemUserPageRequest request);

    ResetPasswordResponse resetPassword(Long id);

    ResetGoogleAuthResponse resetGoogleAuth(Long id);
}
