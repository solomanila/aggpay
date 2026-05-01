package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("system_user_role")
public class SystemUserRole {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long roleId;
}
