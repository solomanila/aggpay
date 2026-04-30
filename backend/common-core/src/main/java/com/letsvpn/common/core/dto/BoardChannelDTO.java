package com.letsvpn.common.core.dto;

import lombok.Data;

@Data
public class BoardChannelDTO {

    private Long id;
    private String title;
    private Integer payConfigId;
    private String payConfigTitle;
}
