package com.letsvpn.admin.dto;

import java.util.List;
import lombok.Data;

@Data
public class SystemUserUpdateRequest {
    private String name;
    private String email;
    private String mobile;
    private String status;
    private String riskLevel;
    private Long ownerUserId;
    private List<String> tags;
    private String newPassword;
}
