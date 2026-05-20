package com.letsvpn.common.core.dto;

import lombok.Data;

@Data
public class BillOrderIdsDTO {
    private Long   billId;
    /** comma-separated order_ids for this bill */
    private String orderIds;
}
