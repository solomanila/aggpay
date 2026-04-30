package com.letsvpn.common.core.dto;

import java.util.Date;
import lombok.Data;

@Data
public class PayConfigInfoDTO {

    private Integer id;

    private String shortCode;

    private String url;

    private String reqDomain;

    private String remark;

    private Date createTime;
}
