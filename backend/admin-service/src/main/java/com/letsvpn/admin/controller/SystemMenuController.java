package com.letsvpn.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.letsvpn.admin.dto.MenuTreeNode;
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

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/system/menu")
@RequiredArgsConstructor
public class SystemMenuController {

    private final SystemUserRoleMapper userRoleMapper;
    private final SystemRoleMenuMapper roleMenuMapper;
    private final SystemMenuMapper menuMapper;

    /**
     * 返回当前登录用户可见的菜单树（一级 + 二级）
     */
    @GetMapping("/visible")
    public R<List<MenuTreeNode>> visible() {
        Long userId = AuthContextHolder.getUserId();
        if (userId == null) {
            return R.success(Collections.emptyList());
        }

        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SystemUserRole>()
                                .eq(SystemUserRole::getUserId, userId))
                .stream().map(SystemUserRole::getRoleId)
                .collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            return R.success(Collections.emptyList());
        }

        List<Long> menuIds = roleMenuMapper.selectList(
                        new LambdaQueryWrapper<SystemRoleMenu>()
                                .in(SystemRoleMenu::getRoleId, roleIds))
                .stream().map(SystemRoleMenu::getMenuId)
                .collect(Collectors.toList());

        if (menuIds.isEmpty()) {
            return R.success(Collections.emptyList());
        }

        List<SystemMenu> allMenus = menuMapper.selectList(
                new LambdaQueryWrapper<SystemMenu>()
                        .in(SystemMenu::getId, menuIds)
                        .eq(SystemMenu::getVisible, 1)
                        .orderByAsc(SystemMenu::getSortOrder));

        // 先建一级节点（有序）
        Map<Long, MenuTreeNode> parentMap = new LinkedHashMap<>();
        for (SystemMenu m : allMenus) {
            if (m.getParentId() == null) {
                parentMap.put(m.getId(), toNode(m, new ArrayList<>()));
            }
        }
        // 挂二级节点
        for (SystemMenu m : allMenus) {
            if (m.getParentId() != null) {
                MenuTreeNode parent = parentMap.get(m.getParentId());
                if (parent != null) {
                    parent.getChildren().add(toNode(m, Collections.emptyList()));
                }
            }
        }

        return R.success(new ArrayList<>(parentMap.values()));
    }

    private MenuTreeNode toNode(SystemMenu m, List<MenuTreeNode> children) {
        MenuTreeNode node = new MenuTreeNode();
        node.setId(m.getId());
        node.setName(m.getName());
        node.setRoutePath(m.getRoutePath());
        node.setSortOrder(m.getSortOrder());
        node.setChildren(children);
        return node;
    }
}
