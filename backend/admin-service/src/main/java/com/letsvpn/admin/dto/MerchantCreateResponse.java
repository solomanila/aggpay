package com.letsvpn.admin.dto;

import lombok.Data;

@Data
public class MerchantCreateResponse {
    private Integer platformId;
    private Long    userId;
    private String  account;
    private String  googleSecret;
    private String  otpAuthUrl;
}
