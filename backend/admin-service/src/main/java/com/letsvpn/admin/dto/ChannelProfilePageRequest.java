package com.letsvpn.admin.dto;

import lombok.Data;

@Data
public class ChannelProfilePageRequest {
    private String areaType;
    private String businessType;
    private int pageNum = 1;
    private int pageSize = 10;
}
