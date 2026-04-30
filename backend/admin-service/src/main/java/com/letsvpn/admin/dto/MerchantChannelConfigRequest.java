package com.letsvpn.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchantChannelConfigRequest {
    private Long id;
    private Integer platformId;
    private Long payConfigChannelId;
    /** PAYMENT / PAYOUT */
    private String channelType;
    private BigDecimal dailyLimit;
    private Integer weight;
    private Integer enabled;
    private String startTime;
    private String endTime;
    private Integer settlementCycle;
    private Integer autoSettle;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String currency;
    private String payMode;
    private String payPage;
    private BigDecimal feeRate;
    private BigDecimal feeFixed;
    private Integer tieredRateEnabled;
    private String tieredRateConfig;
}
