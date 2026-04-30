package com.letsvpn.common.core.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminRegisterRequest {

    @NotBlank
    private String account;

    @NotBlank
    @Size(min = 8, message = "密码至少 8 位")
    private String password;

    @NotBlank
    private String name;

    @Email
    private String email;

    private String mobile;

    /**
     * 可自带 GA 秘钥，不传自动生成
     */
    private String googleSecret;
}
