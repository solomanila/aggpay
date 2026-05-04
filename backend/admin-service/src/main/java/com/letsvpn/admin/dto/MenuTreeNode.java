package com.letsvpn.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class MenuTreeNode {
    private Long id;
    private String name;
    private String routePath;
    private Integer sortOrder;
    private List<MenuTreeNode> children;
}
