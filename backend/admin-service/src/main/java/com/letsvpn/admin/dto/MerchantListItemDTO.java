package com.letsvpn.admin.dto;

import lombok.Data;

@Data
public class MerchantListItemDTO {
    private Integer platformId;
    private String title;
    private String keyId;
    private String appKey;
    private Integer status;
    private Long agentId;
    private String email;
    private String remark;
    private String createdAt;
    private String balanceSummary;
}
