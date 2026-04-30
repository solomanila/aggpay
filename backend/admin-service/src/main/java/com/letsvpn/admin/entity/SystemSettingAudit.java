package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("system_setting_audit")
public class SystemSettingAudit {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long settingId;
    private Integer version;
    private String valueSnapshot;
    private String changeType;
    private Long operatorUserId;
    private String remark;
    private LocalDateTime createdAt;
}
