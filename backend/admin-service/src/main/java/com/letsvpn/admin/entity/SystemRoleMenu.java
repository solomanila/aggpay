package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("system_role_menu")
public class SystemRoleMenu {

    private Long roleId;
    private Long menuId;
}
