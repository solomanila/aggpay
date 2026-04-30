package com.letsvpn.common.core.dto;

import java.util.Date;
import lombok.Data;

@Data
public class MerchantProfileDTO {

    private Integer platformId;
    private String platformNo;
    private String title;
    private String secretKey;
    private Integer nullify;
    private Date createTime;
}
