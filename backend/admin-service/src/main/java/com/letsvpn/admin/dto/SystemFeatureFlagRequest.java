package com.letsvpn.admin.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SystemFeatureFlagRequest {
    private Long id;
    @NotBlank
    private String flagKey;
    private String description;
    private Integer enabled;
    private String scope;
    private Long ownerUserId;
}
