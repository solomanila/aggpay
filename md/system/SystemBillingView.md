# SystemBillingView 后端接口需求

## 页面定位
- `SystemBillingView.vue` 位于“系统/系统账单”，展示平台内部账单（例如云服务、监控、三方费用）的周期、金额、状态与负责人。
- 数据模块包括：指标卡、状态筛选、账单列表以及明细审批，需要 REST 接口 `/api/admin/v1/system/billing/**`。

## 现有数据来源
- `pay.sql` 尚无系统账单表，需新增。
- 可复用 `pay_platform_info`/`pay_config_info` 提供主体信息；审批结构可沿用财务模块中的审批模型。

## 新增数据结构
```sql
CREATE TABLE IF NOT EXISTS `system_billing_order` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `billing_no` varchar(64) NOT NULL UNIQUE,
  `project` varchar(64) NOT NULL COMMENT '费用类型，如 CLOUD|MONITOR|LICENSE',
  `cycle_start` date NOT NULL,
  `cycle_end` date NOT NULL,
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVING|PAID|CLOSED',
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统内部账单';

CREATE TABLE IF NOT EXISTS `system_billing_detail` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `billing_id` bigint NOT NULL,
  `item_name` varchar(128) NOT NULL,
  `item_amount` decimal(18,2) NOT NULL,
  `vendor` varchar(128) DEFAULT NULL,
  `note` varchar(255) DEFAULT NULL,
  FOREIGN KEY (`billing_id`) REFERENCES `system_billing_order`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单明细';

CREATE TABLE IF NOT EXISTS `system_billing_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `billing_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`billing_id`) REFERENCES `system_billing_order`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统账单审批';
```

## 接口清单
| # | Method | Path | 功能 |
|---|---|---|---|
| 1 | GET | `/summary` | 指标卡：账单数量、金额、待审批、逾期 |
| 2 | GET | `/orders` | 账单列表，可筛选状态/项目/周期 |
| 3 | GET | `/orders/{billingId}` | 账单详情：明细、审批、日志 |
| 4 | POST | `/orders` | 创建/更新系统账单 |
| 5 | POST | `/orders/{billingId}/approvals` | 审批动作 |

## 接口详情

### 1. `GET /api/admin/v1/system/billing/summary`
- **参数**：`project`、`dateRange`（可选）。
- **返回**：`{ stats:[{id:'sysbill-total',value,meta},...], approvals:[...] }`，数据来自 `system_billing_order` 及 `system_billing_approval`。

### 2. `GET /api/admin/v1/system/billing/orders`
- **参数**：`status`,`project`,`dateFrom`,`dateTo`,`owner`,`keyword`,`pageNo`,`pageSize`。
- **响应**：`{ "total": 80, "list": [ { id,billingNo,project,cycle,amount,status,owner,updatedAt } ] }`。
- **说明**：`keyword` 匹配 `billing_no` 或 `project`。

### 3. `GET /api/admin/v1/system/billing/orders/{billingId}`
- **返回**：`{ order, details, approvals }`。
  - `order`: `system_billing_order`。
  - `details`: `system_billing_detail`。
  - `approvals`: `system_billing_approval`。
- **扩展**：可附 `files`（附件），存于对象存储，路径记录在 `detail`.

### 4. `POST /api/admin/v1/system/billing/orders`
- **Body**：`{ "billingId":null, "billingNo":"SYS202405", "project":"CLOUD", "cycleStart":"2024-05-01", "cycleEnd":"2024-05-31", "amount":320000, "currency":"CNY", "ownerUserId":1001, "details":[...]}`
- **逻辑**：创建或更新账单，并同步写 `system_billing_detail`，默认状态 `PENDING`，创建审批链。

### 5. `POST /api/admin/v1/system/billing/orders/{billingId}/approvals`
- **Body**：`{ "stage":"FINANCE", "decision":"APPROVED|REJECTED", "comment":"..." }`.
- **逻辑**：更新 `system_billing_approval`；全部通过后将 `status='PAID'` 并记录付款凭证；拒绝则 `status='FAILED'`。
