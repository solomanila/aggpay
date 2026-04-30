package com.letsvpn.common.core.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminLoginRequest {

    @NotBlank
    private String account;

    @NotBlank
    private String password;

    @NotBlank
    private String otpCode;

    /**
     * 客户端 IP，便于审计
     */
    private String clientIp;
}
