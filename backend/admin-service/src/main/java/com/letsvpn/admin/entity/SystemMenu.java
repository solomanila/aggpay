package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("system_menu")
public class SystemMenu {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String name;
    private String routePath;
    private String type;
    private Integer sortOrder;
    private Integer visible;
    private LocalDateTime createdAt;
}
