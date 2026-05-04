package com.letsvpn.common.core.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class MerchantPlatformInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** aggpay.pay_platform_info.platform_no — 商户 keyId */
    private String platformNo;

    /** aggpay.pay_platform_info.secret_key — 商户 appKey */
    private String secretKey;
}
