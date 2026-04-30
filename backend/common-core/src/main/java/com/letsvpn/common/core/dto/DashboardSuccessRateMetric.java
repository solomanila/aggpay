package com.letsvpn.common.core.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class DashboardSuccessRateMetric implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal currentRate;
    private BigDecimal previousRate;
    private BigDecimal changePercent;
}
