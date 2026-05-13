package com.letsvpn.common.core.dto;

import java.util.Date;
import lombok.Data;

@Data
public class PayChannelPageRowDTO {
    private Long    id;
    private Integer areaType;
    private String  currency;
    private String  title;
    private String  url;
    private String  description;
    private String  configTitle;
    private Integer status;
    private Date    createTime;
}
