package com.letsvpn.common.core.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ChannelSuccessRatePoint implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long payConfigId;
    private String channelName;
    private String windowTime;
    private BigDecimal successRate;
}
