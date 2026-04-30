package com.letsvpn.common.core.dto;

import java.util.Date;
import lombok.Data;

@Data
public class OrderBuildErrorDTO {
    private Long id;
    private String appId;
    private String title;
    private String mdcId;
    private String errorText;
    private Date createTime;
}
