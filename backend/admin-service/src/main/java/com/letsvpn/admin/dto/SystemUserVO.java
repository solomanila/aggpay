package com.letsvpn.admin.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SystemUserVO {
    private Long id;
    private String account;
    private String name;
    private String email;
    private String mobile;
    private String status;
    private String riskLevel;
    private Long ownerUserId;
    private List<String> tags;
    private LocalDateTime lastLoginAt;
}
