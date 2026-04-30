package com.letsvpn.common.core.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class DashboardCountMetric implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long todayCount;
    private Long yesterdayCount;
    private BigDecimal changePercent;
}
