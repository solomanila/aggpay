# SystemUsersView 后端接口需求

## 页面定位
- `SystemUsersView.vue` 是“系统/用户”页面，负责管理后台用户、角色、权限、状态与操作追踪。
- 需要支持统计、组合过滤、分页列表、焦点用户详情、批量操作和审计日志。接口前缀 `/api/admin/v1/system/users/**`。

## 现有数据来源
- pay-service 未包含系统用户表，实际登录体系在 `vpn/auth-service` 中，但为了文档统一，这里定义 pay-service 侧需要的用户、角色、权限表。

## 新增数据结构
```sql
CREATE TABLE IF NOT EXISTS `system_user` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account` varchar(64) NOT NULL UNIQUE,
  `name` varchar(64) NOT NULL,
  `email` varchar(128) DEFAULT NULL,
  `mobile` varchar(32) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE|FROZEN|DISABLED|RISK|REVIEW',
  `risk_level` varchar(16) DEFAULT 'LOW',
  `last_login_at` datetime DEFAULT NULL,
  `last_login_ip` varchar(64) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL COMMENT '负责人/直属上级',
  `tags` varchar(128) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台系统用户';

CREATE TABLE IF NOT EXISTS `system_role` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `role_code` varchar(64) NOT NULL UNIQUE,
  `role_name` varchar(64) NOT NULL,
  `level` varchar(32) DEFAULT NULL,
  `permissions` json DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';

CREATE TABLE IF NOT EXISTS `system_user_role` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  FOREIGN KEY (`user_id`) REFERENCES `system_user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`role_id`) REFERENCES `system_role`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联';

CREATE TABLE IF NOT EXISTS `system_user_audit` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `event_time` datetime NOT NULL,
  `event` varchar(128) NOT NULL,
  `detail` varchar(255) DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL,
  FOREIGN KEY (`user_id`) REFERENCES `system_user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户操作审计';
```

## 接口清单
| # | Method | Path | 功能 |
|---|---|---|---|
| 1 | GET | `/summary` | KPI：用户总数、活跃、冻结、风险、最后同步 |
| 2 | GET | `/list` | 用户列表，支持多条件组合过滤 |
| 3 | GET | `/{userId}` | 用户详情、角色、权限、审计 |
| 4 | POST | `/` | 新建/更新用户档案（含角色、标签、负责人） |
| 5 | POST | `/{userId}/status` | 操作用户状态（冻结、解禁、密码重置等） |
| 6 | GET | `/timeline` | 全局操作追踪（近 24h） |

## 接口详情

### 1. `GET /api/admin/v1/system/users/summary`
- **参数**：`role`,`status`（可选）。
- **返回**：`{ hero:{sync,time,...}, stats:[ {id:'users-total',value,meta}, ... ] }`。数据来自 `system_user` 聚合，以及 `system_user_audit` 获取最近操作。

### 2. `GET /api/admin/v1/system/users/list`
- **参数**：由过滤器字段组成（如 `account`,`name`,`role`,`status`,`owner`,`tag`,`createdFrom`,`createdTo`,`pageNo`,`pageSize`）。
- **逻辑**：多条件组合查询 `system_user`，join `system_user_role` + `system_role` 获取角色信息。
- **响应**：`{ "total": 520, "list": [ { id,name,account,roles,status,risk,lastLogin,owner,tags } ] }`。
- **批量操作**：返回 `batchActions` 枚举（重置密码、冻结、多选导出）。

### 3. `GET /api/admin/v1/system/users/{userId}`
- **返回**：`{ user, roles, permissions, stats, timeline }`。
  - `user`: 基础信息。
  - `roles`: 来自 `system_role`。
  - `permissions`: 从角色 permissions JSON 聚合。
  - `stats`: 登录次数、活跃度（可从 `system_user_audit` 统计）。
  - `timeline`: 最近 10 条 `system_user_audit`。

### 4. `POST /api/admin/v1/system/users`
- **Body**：`{ "id":null, "account":"ops.yan", "name":"彦", "email":"ops@...", "mobile":"", "status":"ACTIVE", "roles":[1,2], "ownerUserId":9001, "tags":["OPS","Admin"] }`.
- **逻辑**：新增或更新 `system_user`，同步 `system_user_role`；密码初始化通过 auth-service 处理但结果记录在 `system_user_audit`。

### 5. `POST /api/admin/v1/system/users/{userId}/status`
- **Body**：`{ "action":"FREEZE|UNFREEZE|RESET_PASSWORD|FORCE_LOGOUT", "reason":"..." }`.
- **处理**：更新 `system_user.status` 或发起调用 auth-service；写 `system_user_audit`。

### 6. `GET /api/admin/v1/system/users/timeline`
- **参数**：`rangeMinutes=1440`,`userId`（可选）。
- **返回**：`system_user_audit` 近期事件列表 `{id,time,event,detail,operator}`，用于“操作追踪”模块。
