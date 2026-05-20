package com.letsvpn.common.core.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class BillPageDTO {
    private Long       id;
    private String     account;
    private String     channelTitle;
    private BigDecimal amount;
    private Integer    status;
    private Date       settleAt;
    private Date       createdAt;
    /** GROUP_CONCAT(order_id) from aggpay.order_info — populated via Feign */
    private String     orderIds;
}
