package com.letsvpn.pay.shopline.dto;

import lombok.Data;

@Data
public class ShoplinePayRequest {

    private String orderTransactionId;
    private String referenceOrderId;
    private String kind;
    private Long amount;
    private String currency;
    private String redirectUrl;
    private String cancelUrl;
    private String notifyUrl;
}
