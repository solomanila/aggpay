package com.letsvpn.common.core.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class BillOrderDetailDTO {
    private String     orderId;
    private String     otherOrderId;
    private BigDecimal realAmount;
    private Integer    status;
    private Date       createTime;
    private Date       payTime;
}
