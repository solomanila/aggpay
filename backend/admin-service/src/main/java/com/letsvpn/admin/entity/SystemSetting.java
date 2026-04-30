package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("system_setting")
public class SystemSetting {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String settingKey;
    private String settingName;
    private String value;
    private String valueType;
    private String category;
    private String description;
    private String status;
    private String grayScope;
    private Long ownerUserId;
    private LocalDateTime updatedAt;
}
