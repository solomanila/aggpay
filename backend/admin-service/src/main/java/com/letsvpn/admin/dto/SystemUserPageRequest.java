package com.letsvpn.admin.dto;

import lombok.Data;

@Data
public class SystemUserPageRequest {
    private String nameOrEmail;
    private String createStartTime;
    private String createEndTime;
    private long pageNum = 1;
    private long pageSize = 20;
}
