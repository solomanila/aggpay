package com.letsvpn.common.core.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class PayinOrderVO {
    private Long id;
    private String orderId;
    private String otherOrderId;
    private BigDecimal reqAmount;
    private BigDecimal realAmount;
    private String title;
    private String account;
    private Integer status;
    private String refundStatus;
    private String utr;
    private String bank;
    private String payerVPA;
    private String phone;
    private String name;
    private Date createTime;
    private Date payTime;
    private Date createdAt;
}
