package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("system_feature_flag")
public class SystemFeatureFlag {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String flagKey;
    private String description;
    private Integer enabled;
    private String scope;
    private Long ownerUserId;
    private LocalDateTime updatedAt;
}
