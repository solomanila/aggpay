package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("system_user_auth")
public class SystemUserAuth {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String account;
    private String name;
    private String email;
    private String mobile;
    private String status;
    private String riskLevel;
    private Long ownerUserId;
    /** 商户 platform_id（仅商户角色用户，对应 aggpay.pay_platform_info.platform_id） */
    private Integer platformId;
    private String tags;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private String passwordHash;
    private String passwordSalt;
    private String passwordAlgo;
    private LocalDateTime passwordUpdatedAt;
    private Integer forceReset;
    private String googleSecret;
    private Integer googleEnabled;
    private LocalDateTime googleLastVerifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
