package com.letsvpn.pay.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MerchantRateDTO {
    private Integer    platformId;
    private Long       orderNum;
    private Long       successCount;
    private BigDecimal successRate;
}
