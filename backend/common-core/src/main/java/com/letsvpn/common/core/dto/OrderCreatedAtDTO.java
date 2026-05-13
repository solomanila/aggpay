package com.letsvpn.common.core.dto;

import lombok.Data;
import java.util.Date;

@Data
public class OrderCreatedAtDTO {
    private String orderId;
    private Date createdAt;
}
