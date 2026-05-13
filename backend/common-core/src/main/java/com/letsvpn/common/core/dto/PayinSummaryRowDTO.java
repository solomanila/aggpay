package com.letsvpn.common.core.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PayinSummaryRowDTO {
    private String     localTimeDay;
    private Integer    platformId;
    private String     merchant;
    private String     channel;
    private BigDecimal successAmount;
    private Long       successCount;
    private BigDecimal successRate;
    private Long       orderNum;
}
