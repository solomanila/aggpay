package com.letsvpn.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.letsvpn.admin.entity.SystemMenu;
import com.letsvpn.admin.entity.SystemRoleMenu;
import com.letsvpn.admin.entity.SystemUserRole;
import com.letsvpn.admin.mapper.SystemMenuMapper;
import com.letsvpn.admin.mapper.SystemRoleMenuMapper;
import com.letsvpn.admin.mapper.SystemUserRoleMapper;
import com.letsvpn.common.core.response.R;
import com.letsvpn.common.core.util.AuthContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/system/menu")
@RequiredArgsConstructor
public class SystemMenuController {

    private final SystemUserRoleMapper userRoleMapper;
    private final SystemRoleMenuMapper roleMenuMapper;
    private final SystemMenuMapper menuMapper;

    /**
     * 查询当前登录用户可见的一级菜单 route_path 列表
     */
    @GetMapping("/visible")
    public R<List<String>> visible() {
        Long userId = AuthContextHolder.getUserId();
        if (userId == null) {
            return R.success(Collections.emptyList());
        }

        // 查当前用户绑定的 role_id 列表
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SystemUserRole>()
                                .eq(SystemUserRole::getUserId, userId))
                .stream().map(SystemUserRole::getRoleId)
                .collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            return R.success(Collections.emptyList());
        }

        // 查这些角色绑定的 menu_id 列表
        List<Long> menuIds = roleMenuMapper.selectList(
                        new LambdaQueryWrapper<SystemRoleMenu>()
                                .in(SystemRoleMenu::getRoleId, roleIds))
                .stream().map(SystemRoleMenu::getMenuId)
                .collect(Collectors.toList());

        if (menuIds.isEmpty()) {
            return R.success(Collections.emptyList());
        }

        // 只取一级菜单（parent_id IS NULL）的 route_path，按 sort_order 排序
        List<String> routePaths = menuMapper.selectList(
                        new LambdaQueryWrapper<SystemMenu>()
                                .in(SystemMenu::getId, menuIds)
                                .isNull(SystemMenu::getParentId)
                                .eq(SystemMenu::getVisible, 1)
                                .orderByAsc(SystemMenu::getSortOrder))
                .stream().map(SystemMenu::getRoutePath)
                .collect(Collectors.toList());

        return R.success(routePaths);
    }
}
