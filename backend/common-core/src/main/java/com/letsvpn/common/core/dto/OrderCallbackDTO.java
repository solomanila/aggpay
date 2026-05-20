package com.letsvpn.common.core.dto;

import java.util.Date;
import lombok.Data;

@Data
public class OrderCallbackDTO {

    private String  orderId;
    private String  platformNo;
    private String  reqUrl;
    private String  param;
    private Integer respCode;
    private Date    createTime;
}
