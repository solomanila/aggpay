package com.letsvpn.admin.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SystemSettingUpsertRequest {

    private Long id;

    @NotBlank
    private String settingKey;

    @NotBlank
    private String settingName;

    @NotBlank
    private String value;

    private String valueType;
    private String category;
    private String description;
    private String status;
    private String grayScope;
    private Long ownerUserId;
}
