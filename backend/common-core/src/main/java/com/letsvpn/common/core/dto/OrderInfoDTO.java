package com.letsvpn.common.core.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

@Data
public class OrderInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderId;
    private Integer platformId;
    private String frontId;
    private Integer payConfigId;
    private Integer userId;
    private Integer gameId;
    private Integer status;
    private BigDecimal reqAmount;
    private BigDecimal realAmount;
    private Date payTime;
    private Date createTime;
    private Integer createStatus;
    private String createIp;
    private Integer noticeStatus;
    private Date noticeTime;
    private Long payConfigChannelId;
    private String otherOrderId;
    private Long onLineId;
    private String remark;
    private String extend1;
    private String extend2;
    private String extend3;
    private Integer syncStatus;
    private BigDecimal settleAmount;
    private String upi;
}
