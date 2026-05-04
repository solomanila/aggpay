package com.letsvpn.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchantWithdrawRequest {
    private BigDecimal amount;
    private String usdtAddress;
    private String currency = "INR";
}
