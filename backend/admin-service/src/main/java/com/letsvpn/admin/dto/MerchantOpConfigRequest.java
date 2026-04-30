package com.letsvpn.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchantOpConfigRequest {
    private Integer platformId;
    private String email;
    private Long agentId;
    private String remark;
    private Integer dailyPayOrderLimit;
    private Integer dailyWithdrawCountLimit;
    private BigDecimal dailyWithdrawAmountLimit;
    private BigDecimal dailyPayLimit;
    private BigDecimal dailyPayoutLimit;
    private Integer largePayoutRiskEnabled;
    private BigDecimal largePayoutRiskAmount;
    private String telegramGroupId;
    private Integer settlementNotify;
}
