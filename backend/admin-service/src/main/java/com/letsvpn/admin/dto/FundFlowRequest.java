package com.letsvpn.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class FundFlowRequest {
    /** today / week / month */
    private String dateType;
    private List<Integer> platformIds;
    private List<Integer> areaTypes;
    /** 模糊匹配 order_id 或 other_order_id */
    private String refId;
    private long pageNum = 1;
    private long pageSize = 20;
}
