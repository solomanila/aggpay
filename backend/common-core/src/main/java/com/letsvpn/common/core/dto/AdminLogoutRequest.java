package com.letsvpn.common.core.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminLogoutRequest {

    @NotBlank
    private String token;
}
