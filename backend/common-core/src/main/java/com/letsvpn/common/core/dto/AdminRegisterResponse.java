package com.letsvpn.common.core.dto;

import java.io.Serializable;
import lombok.Data;

@Data
public class AdminRegisterResponse implements Serializable {
    private Long userId;
    private String account;
    private String googleSecret;
    private String otpAuthUrl;
}
