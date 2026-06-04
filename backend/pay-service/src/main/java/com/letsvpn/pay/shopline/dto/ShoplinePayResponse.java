package com.letsvpn.pay.shopline.dto;

import lombok.Data;

@Data
public class ShoplinePayResponse {

    private String returnCode;
    private String returnMessage;
    private String returnMessageId;
    private String orderTransactionId;
    private String channelOrderTransactionId;
    private String paymentUrl;
}
