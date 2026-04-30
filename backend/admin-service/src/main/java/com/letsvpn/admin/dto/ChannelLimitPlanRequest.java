package com.letsvpn.admin.dto;

import lombok.Data;

@Data
public class ChannelLimitPlanRequest {
    private Long id;
    private String channelId;
    private String planType;
    private Integer windowMinutes;
    private Integer limitCount;
    private String status;
}
