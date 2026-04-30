package com.letsvpn.common.core.dto;

import java.util.Date;
import lombok.Data;

@Data
public class PayConfigChannelDTO {

    private Integer id;

    private Integer areaType;

    private String title;

    private String url;

    private String remark;

    private Integer nullify;

    private Date createTime;

    /** pay_config_info.third_service — 通道名称 */
    private String thirdService;

    /** pay_config_info.short_code — 主体标识 */
    private String shortCode;

    /** pay_config_info.req_param — 通道参数 JSON */
    private String reqParam;

    /** merchant_info.app_id */
    private String appId;

    /** merchant_info.private_key */
    private String privateKey;
}
