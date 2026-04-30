# FinanceSystemTopupView 后端接口需求

## 页面定位
- `FinanceSystemTopupView.vue` 展示“财务/系统付费充值”页面，记录平台给系统/外部账户的充值订单、状态、渠道、负责人。
- 接口前缀 `/api/admin/v1/finance/system-topup/**`。

## 数据来源
- pay.sql 无充值记录表，需新增。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `finance_system_topup` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `topup_no` varchar(64) NOT NULL UNIQUE,
  `platform_id` int DEFAULT NULL,
  `channel_id` bigint DEFAULT NULL,
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVING|SUCCESS|FAILED',
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统付费充值订单';

CREATE TABLE IF NOT EXISTS `finance_system_topup_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `topup_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`topup_id`) REFERENCES `finance_system_topup`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统充值审批';
```

## 接口清单
| # | Method | Path | 功能 |
|---|---|---|---|
| 1 | GET | `/summary` | 充值 KPI |
| 2 | GET | `/orders` | 充值订单列表 |
| 3 | POST | `/orders` | 创建充值订单 |
| 4 | GET | `/orders/{id}` | 详情+审批 |
| 5 | POST | `/orders/{id}/approvals` | 审批动作 |

## 接口详情

### 1. `GET /api/admin/v1/finance/system-topup/summary`
- 返回 `{ stats:[...], approvals:[...] }`，数据来自 `finance_system_topup`/`finance_system_topup_approval`。

### 2. `GET /api/admin/v1/finance/system-topup/orders`
- **参数**：`status`,`platformId`,`channelId`,`dateFrom`,`dateTo`,`keyword`,`pageNo`,`pageSize`。
- **响应**： `{ list:[{id,topupNo,platform,channel,amount,status,owner,updatedAt}], total }`。

### 3. `POST /api/admin/v1/finance/system-topup/orders`
- **Body**：`{ "platformId":1, "channelId":25, "amount":500000, "currency":"CNY", "ownerUserId":101, "remark":"系统余额补充" }`.
- **逻辑**：创建 `finance_system_topup`，生成审批节点，写通知。

### 4. `GET /api/admin/v1/finance/system-topup/orders/{id}`
- 返回 `{ order, approvals, logs }`；`logs` 可基于 `order_notify_record`。

### 5. `POST /api/admin/v1/finance/system-topup/orders/{id}/approvals`
- **Body**：`{ "stage":"FINANCE", "decision":"APPROVED|REJECTED", "comment":"..." }`.
- **逻辑**：更新审批；通过全部阶段后 `status='SUCCESS'`，如失败 `status='FAILED'` 并记录原因。*** End Patch
