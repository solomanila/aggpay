package com.letsvpn.common.core.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class AdminLoginResponse implements Serializable {

    private Long userId;
    private String account;
    private String name;
    private String status;
    private String riskLevel;
    private List<String> tags;
}
