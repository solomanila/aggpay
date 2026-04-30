# BankMappingView 后端接口需求

## 页面定位
- `BankMappingView.vue` 位于“银行户管理/银行字段映射”，维护每家银行/业务类型的字段映射模板、版本、状态、审批流程与公告。
- 页面包括指标卡、模板列表、筛选、审批列表、公告。

## 现有数据来源（pay.sql & pay-service）
- 现有 DB 无字段映射相关表，仅能借助 `pay_config_parameter` 保存通道参数，不适合存储模板版本。
- 需要独立的银行映射模板与字段表，支持版本管理与审批。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `bank_mapping_template` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `bank_name` varchar(64) NOT NULL,
  `business_type` varchar(32) NOT NULL COMMENT 'UPI/WALLET/PIX 等',
  `version` varchar(16) NOT NULL,
  `status` varchar(16) DEFAULT 'ENABLED' COMMENT 'ENABLED|PENDING|DISABLED',
  `owner_user_id` bigint DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `file_url` varchar(512) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_bank_type_version` (`bank_name`,`business_type`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行字段映射模板';

CREATE TABLE IF NOT EXISTS `bank_mapping_field` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `template_id` bigint NOT NULL,
  `field_name` varchar(64) NOT NULL,
  `source_field` varchar(64) NOT NULL,
  `transform_rule` varchar(255) DEFAULT NULL COMMENT 'JSONPath/表达式',
  `required` tinyint DEFAULT 1,
  `data_type` varchar(32) DEFAULT 'STRING',
  `remark` varchar(255) DEFAULT NULL,
  FOREIGN KEY (`template_id`) REFERENCES `bank_mapping_template`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行字段映射详情';

CREATE TABLE IF NOT EXISTS `bank_mapping_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `template_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL COMMENT 'PLATFORM|OPS|RISK',
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`template_id`) REFERENCES `bank_mapping_template`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='映射模板审批';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/bank/mapping/summary` | 指标卡 | 统计银行数量、模板版本、字段数、待上线 |
| 2 | GET | `/api/admin/v1/bank/mapping/templates` | 模板列表 | 按银行/类型/状态过滤，分页 |
| 3 | GET | `/api/admin/v1/bank/mapping/templates/{templateId}` | 模板详情 | 返回字段列表、审批、历史版本 |
| 4 | POST | `/api/admin/v1/bank/mapping/templates` | 新增/更新模板版本 | 维护字段映射并生成审批 |
| 5 | POST | `/api/admin/v1/bank/mapping/templates/{templateId}/approvals` | 审批动作 | 审批、上线、驳回 |

## 接口详情

### 1. `GET /api/admin/v1/bank/mapping/summary`
- **查询参数**：`businessType`（可选）。
- **返回**：`{ stats:[ {id:'map-banks',...}, ... ], approvals:[...], notices:[...] }`。
  - `stats`: 从 `bank_mapping_template` 聚合银行数量、模板版本数、字段总数 (`SUM(bank_mapping_field)`)、待上线模板数量 (`status='PENDING'`)。
  - `approvals`: 最近 5 条 `bank_mapping_approval` 状态 `PENDING` 的记录。
  - `notices`: `order_notify_record` 中 type=`BANK_MAPPING_NOTICE`。

### 2. `GET /api/admin/v1/bank/mapping/templates`
- **查询参数**：`bankName`,`businessType`,`status`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：从 `bank_mapping_template` 模糊匹配并返回 `{id,bankName,version,businessType,status,owner,updatedAt}`，附带字段数量（count from `bank_mapping_field`）。
- **响应**：`{ "total": 32, "list": [...] }`。
- **扩展**：`?withHistory=true` 时附加相同 bank/type 的最近两个版本信息。

### 3. `GET /api/admin/v1/bank/mapping/templates/{templateId}`
- **返回**：
  - `template`: `bank_mapping_template` 基本信息。
  - `fields`: 关联 `bank_mapping_field` 支持分页/搜索。
  - `approvals`: `bank_mapping_approval` 列表。
  - `history`: 同银行/业务类型的历史版本列表（按时间倒序）。
- **权限**：`BANK_MAPPING_VIEW`；如需下载文件 `file_url`，需具备 `CONFIG_DOWNLOAD`。

### 4. `POST /api/admin/v1/bank/mapping/templates`
- **Body**：`{ "id":null, "bankName":"HDFC", "businessType":"UPI", "version":"v2.1", "status":"PENDING", "description":"支持多字段", "fields":[ { "fieldName":"beneficiaryName","sourceField":"customer.name","transformRule":"titleCase()" }, ... ] }`.
- **逻辑**：新增或更新模板版本：
  - 校验同 `bank+type+version` 不重复。
  - 写 `bank_mapping_template` 与 `bank_mapping_field`（先删后增或批量 upsert）。
  - 自动创建 `bank_mapping_approval` 阶段（Platform -> Ops -> Risk）。
  - 生成 `order_notify_record` 供“公告”使用。
- **响应**：模板对象，含 `pendingApprovals`。

### 5. `POST /api/admin/v1/bank/mapping/templates/{templateId}/approvals`
- **Body**：`{ "stage":"OPS", "decision":"APPROVED|REJECTED", "comment":"..." }`.
- **逻辑**：更新对应 `bank_mapping_approval`，当所有阶段通过时将 `bank_mapping_template.status='ENABLED'` 并记录上线时间；若驳回则状态=`DISABLED`/`PENDING`，并通知相关负责人。
- **扩展**：`decision='APPROVED'` 且 stage 为最终阶段时，可同步将映射推送到配置中心或 OSS；同时写 `order_notify_record` type=`BANK_MAPPING_RELEASE`。
