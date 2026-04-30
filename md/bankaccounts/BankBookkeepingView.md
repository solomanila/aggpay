# BankBookkeepingView 后端接口需求

## 页面定位
- `BankBookkeepingView.vue` 位于“银行户管理/记账流水”，展示内部记账、调账、冲补操作，提供审批与复核功能。
- 页面包含指标卡、筛选器、记账列表、审批列表，需要与银行流水、财务审批联动。

## 现有数据来源（pay.sql & pay-service）
- 现有表中缺乏内部记账记录，只有 `order_info`/`order_req_record` 可辅助校验。
- 需新增记账表及审批表，同时与 `bank_account_ledger`、`bank_account` 建立关联。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `bank_bookkeeping_entry` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `entry_type` varchar(16) NOT NULL COMMENT 'BOOK|ADJUST|REVERSAL',
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) NOT NULL,
  `tag` varchar(32) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'POSTED' COMMENT 'POSTED|PENDING_REVIEW|REJECTED',
  `owner_user_id` bigint DEFAULT NULL,
  `related_ledger_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`account_id`) REFERENCES `bank_account`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`related_ledger_id`) REFERENCES `bank_account_ledger`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行记账流水';

CREATE TABLE IF NOT EXISTS `bank_bookkeeping_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `entry_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL COMMENT 'FINANCE|RISK|OPS',
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVED|REJECTED',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`entry_id`) REFERENCES `bank_bookkeeping_entry`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记账审批流';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/bank/bookkeeping/summary` | 指标卡 | 统计今日记账金额/笔数、调账数量、待复核 |
| 2 | GET | `/api/admin/v1/bank/bookkeeping/entries` | 记账列表 | 按类型/状态/账户过滤，分页 |
| 3 | POST | `/api/admin/v1/bank/bookkeeping/entries` | 新建记账/调账 | 提交记账单并绑定相关流水 |
| 4 | GET | `/api/admin/v1/bank/bookkeeping/entries/{entryId}` | 详情 | 返回记账信息、审批流、关联流水 |
| 5 | POST | `/api/admin/v1/bank/bookkeeping/entries/{entryId}/approvals` | 审批动作 | 审批、驳回、补充说明 |

## 接口详情

### 1. `GET /api/admin/v1/bank/bookkeeping/summary`
- **查询参数**：`date`（默认当天）、`accountId`（可选）。
- **返回**：`{ stats:[ {id:'bk-total',value,meta}, ... ], approvals:[...], notices:[...] }`。
  - `stats`: `bank_bookkeeping_entry` 按 `created_at` 聚合今日记账金额、笔数；`bk-adjust` 统计 `entry_type='ADJUST'`; `bk-pending` 统计 `status='PENDING_REVIEW'`。
  - `approvals`: 最近 3 条 `bank_bookkeeping_approval` 状态为 `PENDING` 的条目。
  - `notices`: 来自 `order_notify_record`（type=`BANK_BOOKKEEPING_NOTICE`）。

### 2. `GET /api/admin/v1/bank/bookkeeping/entries`
- **查询参数**：`entryType`,`status`,`accountId`,`owner`,`dateFrom`,`dateTo`,`keyword`,`pageNo`,`pageSize`.
- **逻辑**：基于条件查询 `bank_bookkeeping_entry`；`keyword` 可匹配 `remark`、`related_ledger_id`、`ledger_no`（通过关联 `bank_account_ledger`）。
- **响应**：`{ "total": 380, "list": [ { id,accountName,type,amount,status,tag,owner,updatedAt } ] }`。
- **扩展**：`?withApprovals=true` 时附带每条最新审批状态。

### 3. `POST /api/admin/v1/bank/bookkeeping/entries`
- **Body**：`{ "accountId":1, "entryType":"ADJUST", "amount":150000, "currency":"INR", "tag":"调账", "relatedLedgerId":892, "remark":"BCA 调账", "ownerUserId":1002 }`.
- **处理**：校验账户存在、金额大于 0；若附 `relatedLedgerId` 则读取该流水同步 `direction`。写入 `bank_bookkeeping_entry`，并按预设阶段（FINANCE -> RISK -> OPS）生成 `bank_bookkeeping_approval` 记录；写 `order_notify_record`。
- **响应**：新建记账条目。

### 4. `GET /api/admin/v1/bank/bookkeeping/entries/{entryId}`
- **返回**：`{ entry, approvals, ledger }`。
  - `entry`: `bank_bookkeeping_entry`。
  - `approvals`: 全部 `bank_bookkeeping_approval` 阶段。
  - `ledger`: 若 `related_ledger_id` 存在，返回对应 `bank_account_ledger` 信息及状态。
- **权限**：`BANK_BOOKKEEPING_VIEW`；敏感备注需脱敏。

### 5. `POST /api/admin/v1/bank/bookkeeping/entries/{entryId}/approvals`
- **Body**：`{ "stage":"FINANCE", "decision":"APPROVED|REJECTED", "comment":"..." }`.
- **逻辑**：更新 `bank_bookkeeping_approval` 状态；若所有阶段通过则将 `bank_bookkeeping_entry.status='POSTED'` 并写时间线至 `bank_account_ledger_timeline`；若任一阶段拒绝则状态 `REJECTED` 并发送 notice。
- **审计**：必要时写 `order_notify_record` 并记录操作者。
