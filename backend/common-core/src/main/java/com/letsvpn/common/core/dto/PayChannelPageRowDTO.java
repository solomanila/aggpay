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
    /** pay_config_info.id — 关联主体ID */
    private Integer configId;
    /** pay_config_channel.share_id — 1=收款 2=出款 */
    private Integer shareId;
}
