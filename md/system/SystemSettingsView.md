# SystemSettingsView 后端接口需求

## 页面定位
- `SystemSettingsView.vue` 是“系统/配置”页面，集中管理平台的全局参数、灰度开关、环境信息及通知配置。
- 页面包含 hero 区、统计卡、筛选表单、配置列表与 detail 面板，需要支持查询、编辑、灰度发布、审计。接口前缀 `/api/admin/v1/system/settings/**`。

## 现有数据来源
- pay.sql 中 `pay_config_parameter` 支持支付配置参数，但不适用于全局系统设置。
- 需新增通用配置表及审计记录。

## 新增数据结构
```sql
CREATE TABLE IF NOT EXISTS `system_setting` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `setting_key` varchar(128) NOT NULL UNIQUE,
  `setting_name` varchar(128) NOT NULL,
  `value` text NOT NULL,
  `value_type` varchar(16) DEFAULT 'STRING',
  `category` varchar(64) DEFAULT 'GENERAL',
  `description` varchar(255) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE|DISABLED|GRAY',
  `gray_scope` varchar(128) DEFAULT NULL COMMENT '灰度命中规则，如平台ID列表',
  `owner_user_id` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

CREATE TABLE IF NOT EXISTS `system_setting_audit` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `setting_id` bigint NOT NULL,
  `version` int NOT NULL,
  `value_snapshot` text NOT NULL,
  `change_type` varchar(32) NOT NULL COMMENT 'CREATE|UPDATE|DELETE|GRAY',
  `operator_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`setting_id`) REFERENCES `system_setting`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置审计';

CREATE TABLE IF NOT EXISTS `system_feature_flag` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `flag_key` varchar(128) NOT NULL UNIQUE,
  `description` varchar(255) DEFAULT NULL,
  `enabled` tinyint(1) DEFAULT 0,
  `scope` varchar(128) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灰度开关';
```

## 接口清单
| # | Method | Path | 功能 |
|---|---|---|---|
| 1 | GET | `/summary` | KPI：配置数、灰度项、待审核、最近同步 |
| 2 | GET | `/settings` | 配置列表，支持类别/状态/关键字过滤 |
| 3 | GET | `/settings/{id}` | 配置详情、历史版本、灰度范围 |
| 4 | POST | `/settings` | 创建/更新配置（含灰度发布） |
| 5 | POST | `/settings/{id}/rollback` | 版本回滚 |
| 6 | GET | `/feature-flags` | 灰度开关列表、状态 |
| 7 | POST | `/feature-flags` | 创建/更新开关 |

## 接口详情

### 1. `GET /api/admin/v1/system/settings/summary`
- **返回**：`{ stats:[...], hero:{sync,...} }`；统计 `system_setting` 中 ACTIVE/GRAY 数，以及 `system_setting_audit` 最新时间。

### 2. `GET /api/admin/v1/system/settings`
- **参数**：`category`,`status`,`owner`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：查询 `system_setting`，`keyword` 支持 key/name 模糊；可同时返回最近版本号。
- **响应**：`{ list:[ { id,settingKey,settingName,valuePreview,status,owner,updatedAt } ], total }`。

### 3. `GET /api/admin/v1/system/settings/{id}`
- **返回**：`{ setting, history }`。
  - `setting`: `system_setting` 全量字段。
  - `history`: 最近 N 条 `system_setting_audit`（包含 `version`、`valueSnapshot`、`operator`）。

### 4. `POST /api/admin/v1/system/settings`
- **Body**：`{ "id":null, "settingKey":"finance.settle.delay", "settingName":"结算延迟", "value":"15", "valueType":"NUMBER", "category":"FINANCE", "status":"ACTIVE", "grayScope":"platform:IN", "remark":"财务调优" }`.
- **逻辑**：新增或更新 `system_setting`；每次更新写 `system_setting_audit`（version 自增）；若 `status='GRAY'` 需记录 `gray_scope` 并通知相关模块。

### 5. `POST /api/admin/v1/system/settings/{id}/rollback`
- **Body**：`{ "version":12, "reason":"回退到稳定版本" }`.
- **逻辑**：从 `system_setting_audit` 读取指定版本 `value_snapshot` 并还原；新增一条 audit（change_type=ROLLBACK）。

### 6. `GET /api/admin/v1/system/settings/feature-flags`
- **返回**：`{ list:[ {flagKey,description,enabled,scope,updatedAt} ] }`，数据来源 `system_feature_flag`。

### 7. `POST /api/admin/v1/system/settings/feature-flags`
- **Body**：`{ "flagKey":"new_risk_rule", "enabled":true, "scope":"platform:SEA", "description":"新风控规则灰度" }`.
- **逻辑**：创建或更新开关；操作写入 `system_setting_audit`（change_type=FLAG）。
