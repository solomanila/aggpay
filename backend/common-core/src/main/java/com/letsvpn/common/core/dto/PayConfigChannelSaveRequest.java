package com.letsvpn.common.core.dto;

import lombok.Data;

@Data
public class PayConfigChannelSaveRequest {

    /** null for create, channel id for update */
    private Long id;

    /** pay_config_info.id */
    private Long payConfigId;

    /** channel title */
    private String title;

    /** JSON configuration string */
    private String jsonParam;

    /** 0=开, 1=关 */
    private Integer status;

    /** 1=收款, 2=出款 */
    private Integer shareId;
}
