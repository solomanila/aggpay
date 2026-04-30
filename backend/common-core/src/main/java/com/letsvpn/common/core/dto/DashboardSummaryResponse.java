package com.letsvpn.common.core.dto;

import java.io.Serializable;
import lombok.Data;

@Data
public class DashboardSummaryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private DashboardAmountMetric transactionAmount;
    private DashboardCountMetric transactionCount;
    private DashboardSuccessRateMetric successRate;
}
