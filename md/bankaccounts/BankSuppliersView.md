# BankSuppliersView 后端接口需求

## 页面定位
- `BankSuppliersView.vue` 位于“银行户管理/供户商”，用于维护跨国银行/放款供应商的资质、覆盖范围、联系人以及维护计划。
- 页面提供指标卡、筛选器、供应商卡片列表、维护计划、公告，需要后端给出增删改查接口和统计。

## 现有数据来源（pay.sql & pay-service）
- 现有库仅包含 `pay_config_info`,`pay_platform_info` 等支付配置相关表，无法直接存储银行供应商档案。
- 可复用 `order_info` 统计与某供应商关联账户产生的资金动向（通过 `bank_account` 关联后获得）。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `bank_supplier` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `supplier_code` varchar(64) NOT NULL UNIQUE,
  `name` varchar(128) NOT NULL,
  `country` varchar(32) NOT NULL,
  `region` varchar(32) DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/MAINTENANCE/INACTIVE',
  `coverage_services` varchar(128) DEFAULT NULL COMMENT 'UPI/NEFT/TED 等',
  `license_no` varchar(64) DEFAULT NULL,
  `kyc_status` varchar(16) DEFAULT 'PENDING',
  `account_count` int DEFAULT 0,
  `owner_user_id` bigint DEFAULT NULL,
  `contact_name` varchar(64) DEFAULT NULL,
  `contact_email` varchar(128) DEFAULT NULL,
  `contact_phone` varchar(32) DEFAULT NULL,
  `notes` varchar(512) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行供应商档案';

CREATE TABLE IF NOT EXISTS `bank_supplier_maintenance` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `supplier_id` bigint NOT NULL,
  `window_start` datetime NOT NULL,
  `window_end` datetime NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'PLANNED',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_supplier_status` (`supplier_id`,`status`),
  CONSTRAINT `fk_supplier_maintenance` FOREIGN KEY (`supplier_id`) REFERENCES `bank_supplier`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商维护计划';

CREATE TABLE IF NOT EXISTS `bank_supplier_document` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `supplier_id` bigint NOT NULL,
  `doc_type` varchar(32) NOT NULL COMMENT 'LICENSE|CONTRACT|AML',
  `title` varchar(128) NOT NULL,
  `file_url` varchar(512) NOT NULL,
  `status` varchar(16) DEFAULT 'VALID',
  `uploaded_by` bigint DEFAULT NULL,
  `uploaded_at` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_supplier_doc` (`supplier_id`,`doc_type`),
  CONSTRAINT `fk_supplier_document` FOREIGN KEY (`supplier_id`) REFERENCES `bank_supplier`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商证照记录';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/bank/suppliers/summary` | 指标卡/公告 | 统计供应商数量、启用数、账户数、覆盖国家 |
| 2 | GET | `/api/admin/v1/bank/suppliers` | 供应商列表 | 支持状态/国家/关键字过滤，返回分页 |
| 3 | GET | `/api/admin/v1/bank/suppliers/{supplierId}` | 供应商详情 | 返回档案、证照、维护计划、关联账户 |
| 4 | POST | `/api/admin/v1/bank/suppliers` | 新建/更新供应商 | 按请求体 `id` 是否存在决定新增或更新 |
| 5 | POST | `/api/admin/v1/bank/suppliers/{supplierId}/maintenance` | 创建/更新维护计划 | 支持修改状态（计划/进行/完成/取消） |

## 接口详情

### 1. `GET /api/admin/v1/bank/suppliers/summary`
- **查询参数**：`region`（可选）、`timezone`（默认 UTC+0，对齐页面同步时间）。
- **返回**：`{ stats: [{id,label,value,meta}], maintenance: [...], notices: [...] }`。
  - `stats` 通过 `bank_supplier` 聚合：总量、启用量、`SUM(account_count)` 与 `COUNT(DISTINCT country)`。
  - `maintenance`：最近 3 条 `bank_supplier_maintenance` 仍未完成的计划。
  - `notices`：来自 `order_notify_record`（type=`BANK_SUPPLIER_NOTICE`）或最新证照即将到期记录。
- **权限**：`BANK_SUPPLIER_VIEW`。

### 2. `GET /api/admin/v1/bank/suppliers`
- **查询参数**：`status`,`country`,`keyword`,`pageNo=1`,`pageSize=20`。
- **逻辑**：在 `bank_supplier` 中模糊匹配 `name/supplier_code/contact_name`，附加字段 `accounts` 通过左联 `bank_account`（定义于 `BankAccountsView`）统计启用账户数。
- **响应**：`{ "total": 24, "list": [ { id,name,country,status,accountCount,contact,owner,notes,updatedAt } ] }`。
- **排序**：默认按 `status`（启用优先）+ 最近更新时间。

### 3. `GET /api/admin/v1/bank/suppliers/{supplierId}`
- **返回**：
  - `profile`: `bank_supplier`.
  - `documents`: `bank_supplier_document`.
  - `maintenance`: `bank_supplier_maintenance` 未完成项。
  - `accounts`: 引用 `bank_account`（可分页）展示账号、币种、余额。
- **扩展**：支持 `?includeLogs=true` 载入 `order_req_record` 中该供应商相关失败日志（通过账户->通道映射）。

### 4. `POST /api/admin/v1/bank/suppliers`
- **Body**：`{ "id":null, "name":"HDFC Corporate", "country":"IN", "region":"印度", "status":"ENABLED", "contact":{"name":"Anuj","email":"..."}, "coverageServices":["UPI","NEFT"], "ownerUserId":1021, "notes":"..." }`.
- **校验**：`supplier_code` 若未填自动生成；`country` 必填；`status` 只允许 `ENABLED|MAINTENANCE|INACTIVE`。
- **逻辑**：新增或更新 `bank_supplier`，对应 `account_count` 从 `bank_account` 重新计算；写 `order_notify_record` 记录流水。
- **响应**：最新 `supplier` 对象。

### 5. `POST /api/admin/v1/bank/suppliers/{supplierId}/maintenance`
- **Body**：`{ "maintenanceId":null, "windowStart":"2024-05-22T14:00:00Z", "windowEnd":"2024-05-22T15:00:00Z", "reason":"系统升级", "status":"PLANNED" }`.
- **逻辑**：创建或更新 `bank_supplier_maintenance`；若 `status='PLANNED'` 且窗口即将开始，推送到告警系统；若 `status='DONE'` 将信息写入 `order_notify_record` 供页面“维护”及“公告”展示。
- **响应**：供应商维护列表。
