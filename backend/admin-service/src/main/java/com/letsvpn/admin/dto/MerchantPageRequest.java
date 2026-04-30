package com.letsvpn.admin.dto;

import lombok.Data;

@Data
public class MerchantPageRequest {
    private String keyword;
    private Integer status;
    private Long agentId;
    private int pageNum = 1;
    private int pageSize = 20;
}
