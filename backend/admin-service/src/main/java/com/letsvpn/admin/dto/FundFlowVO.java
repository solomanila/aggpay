package com.letsvpn.admin.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class FundFlowVO {
    private Long id;
    private String merchant;
    private BigDecimal amount;
    private BigDecimal remain;
    private String currency;
    private String reason;
    private String refId;
    private Date localTime;
    private BigDecimal orderAmount;
    private BigDecimal orderFee;
    /** INCREASE / DECREASE */
    private String type;
    private String outTradeNo;
}
