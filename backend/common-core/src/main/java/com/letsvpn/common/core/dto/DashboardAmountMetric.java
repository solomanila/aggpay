package com.letsvpn.common.core.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class DashboardAmountMetric implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal todayAmount;
    private BigDecimal yesterdayAmount;
    private BigDecimal changePercent;
}
