-- 1. 角色基表（若已存在可跳过）
  CREATE TABLE IF NOT EXISTS system_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(64) NOT NULL UNIQUE COMMENT '唯一编码，如 OPS_ADMIN',
    role_name VARCHAR(64) NOT NULL COMMENT '展示名称',
    level VARCHAR(32) DEFAULT NULL COMMENT '可选：权限层级',
    permissions JSON DEFAULT NULL COMMENT '缓存的权限集合，方便快速加载',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';

  -- 随后按照依赖顺序执行其余 DDL（确保每条语句成功后再执行下一条）：

  -- 2. 权限点
  CREATE TABLE IF NOT EXISTS system_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(128) NOT NULL UNIQUE COMMENT '唯一编码，如 ORDER_VIEW',
    permission_name VARCHAR(128) NOT NULL COMMENT '展示名称',
    category VARCHAR(64) NOT NULL COMMENT 'ORDER|MERCHANT|FINANCE|SYSTEM 等',
    http_method VARCHAR(16) DEFAULT NULL COMMENT '可选：绑定的 HTTP 动作',
    api_path VARCHAR(255) DEFAULT NULL COMMENT '可选：接口路径模式',
    ui_action VARCHAR(128) DEFAULT NULL COMMENT '可选：前端按钮或操作',
    data_scope_flag TINYINT DEFAULT 0 COMMENT '1=需要额外数据域控制',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台权限点';

  -- 3. 角色-权限关联
  CREATE TABLE IF NOT EXISTS system_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role
      FOREIGN KEY (role_id) REFERENCES system_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_perm
      FOREIGN KEY (permission_id) REFERENCES system_permission(id) ON DELETE CASCADE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联';

  -- 4. 菜单/按钮
  CREATE TABLE IF NOT EXISTS system_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT DEFAULT NULL COMMENT '父级菜单ID',
    name VARCHAR(64) NOT NULL COMMENT '菜单/按钮名称',
    route_path VARCHAR(255) DEFAULT NULL COMMENT '前端路由路径',
    component VARCHAR(255) DEFAULT NULL COMMENT '对应组件/页面',
    type VARCHAR(16) NOT NULL COMMENT 'MENU|BUTTON|LINK',
    icon VARCHAR(64) DEFAULT NULL COMMENT '前端图标',
    sort_order INT DEFAULT 0 COMMENT '排序值，越小越靠前',
    visible TINYINT DEFAULT 1 COMMENT '0=隐藏，1=可见',
    permission_code VARCHAR(128) DEFAULT NULL COMMENT '绑定权限编码',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台菜单与操作点';

  -- 5. 角色-菜单关联
  CREATE TABLE IF NOT EXISTS system_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id),
    CONSTRAINT fk_role_menu_role
      FOREIGN KEY (role_id) REFERENCES system_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_menu_menu
      FOREIGN KEY (menu_id) REFERENCES system_menu(id) ON DELETE CASCADE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单关联';

  -- 6. 角色数据域
  CREATE TABLE IF NOT EXISTS system_role_data_scope (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    scope_type VARCHAR(32) NOT NULL COMMENT 'MERCHANT|CHANNEL|AREA|OWNER 等',
    scope_value VARCHAR(64) NOT NULL COMMENT '具体取值，如商户ID/区域代码',
    CONSTRAINT fk_role_datascope_role
      FOREIGN KEY (role_id) REFERENCES system_role(id) ON DELETE CASCADE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色数据域配置';