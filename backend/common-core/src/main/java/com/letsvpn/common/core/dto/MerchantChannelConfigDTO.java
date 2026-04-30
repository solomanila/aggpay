package com.letsvpn.common.core.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MerchantChannelConfigDTO {
    private Long id;
    private Integer platformId;
    private Long payConfigChannelId;
    private Integer enabled;
    private Integer weight;
    private BigDecimal dailyLimit;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String startTime;
    private String endTime;
}
