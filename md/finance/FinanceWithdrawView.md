# FinanceWithdrawView 后端接口需求

## 页面定位
- `FinanceWithdrawView.vue` 负责“财务/提现”页面，展示商户提现申请的指标、列表、审批队列和状态。
- 需要提现申请管理接口，路径 `/api/admin/v1/finance/withdraw/**`。

## 现有数据来源
- pay.sql 未包含提现申请表，需新增。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `finance_withdraw_request` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` int NOT NULL,
  `channel_id` bigint DEFAULT NULL,
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVING|PAYING|PAID|FAILED',
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`merchant_id`) REFERENCES `pay_platform_info`(`platform_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请';

CREATE TABLE IF NOT EXISTS `finance_withdraw_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `withdraw_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`withdraw_id`) REFERENCES `finance_withdraw_request`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现审批';
```

## 接口清单
| # | Method | Path | 功能 |
|---|---|---|---|
| 1 | GET | `/summary` | 提现 KPI |
| 2 | GET | `/requests` | 列表筛选 |
| 3 | POST | `/requests` | 新建提现申请 |
| 4 | GET | `/requests/{id}` | 详情 + 审批 |
| 5 | POST | `/requests/{id}/approvals` | 审批动作 |

## 接口详情

### 1. `GET /api/admin/v1/finance/withdraw/summary`
- 返回 `{ stats:[{id:'withdraw-total',...},...], approvals:[...] }`。
- 数据来自 `finance_withdraw_request` 聚合；审批为 `finance_withdraw_approval` 状态 `PENDING`。

### 2. `GET /api/admin/v1/finance/withdraw/requests`
- **参数**：`status`,`merchantId`,`channelId`,`dateFrom`,`dateTo`,`keyword`,`pageNo`,`pageSize`。
- **响应**：列表 `{ id,merchant,amount,channel,status,owner,updatedAt }`。

### 3. `POST /api/admin/v1/finance/withdraw/requests`
- **Body**：`{ "merchantId":101, "channelId":22, "amount":180000, "currency":"CNY", "remark":"周结提现" }`.
- **逻辑**：写 `finance_withdraw_request`，创建审批链并发送通知。

### 4. `GET /api/admin/v1/finance/withdraw/requests/{id}`
- 返回 `{ request, approvals, logs }`；`logs` 可使用 `order_notify_record`。

### 5. `POST /api/admin/v1/finance/withdraw/requests/{id}/approvals`
- **Body**：`{ "stage":"FINANCE", "decision":"APPROVED|REJECTED", "comment":"..." }`.
- **逻辑**：更新 `finance_withdraw_approval`；通过后 status→`PAYING/PAID`；拒绝则标记 `FAILED` 并推送商户。*** End Patch
