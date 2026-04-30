package com.letsvpn.common.core.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ChannelOpenRateRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private String merchant;
    private Long orderNum;
    private Long openNum;
    private BigDecimal openRate;
    private BigDecimal successRate;
    private BigDecimal openSuccessRate;
}
