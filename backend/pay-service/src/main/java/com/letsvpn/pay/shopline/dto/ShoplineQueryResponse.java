package com.letsvpn.pay.shopline.dto;

import lombok.Data;

@Data
public class ShoplineQueryResponse {

    private String returnCode;
    private String returnMessage;
    private String returnMessageId;
    private String orderTransactionId;
    private String channelOrderTransactionId;
    private String paymentStatus;
    private Long amount;
    private String currency;
    private String failCode;
    private String failMessage;
    private PaymentInstrumentInfo paymentInstrumentInfo;

    @Data
    public static class PaymentInstrumentInfo {
        private String paymentInstrumentId;
    }
}
