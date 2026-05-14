package com.letsvpn.admin.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SystemUserPageItemVO {
    private Long id;
    private String account;
    private String name;
    private String email;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
