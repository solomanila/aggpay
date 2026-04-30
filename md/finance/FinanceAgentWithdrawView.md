# FinanceAgentWithdrawView 后端接口需求

## 页面定位
- `FinanceAgentWithdrawView.vue` 展示“财务/代理商提现申请”，管理代理商提交的提现、审批与执行状态。
- 接口前缀 `/api/admin/v1/finance/agent-withdraw/**`。

## 数据来源
- 可复用代理商表 `agent_profile`，需新增代理商提现申请表。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `finance_agent_withdraw` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_id` bigint NOT NULL,
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `bank_account` varchar(128) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVING|PAYING|PAID|FAILED',
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`agent_id`) REFERENCES `agent_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商提现申请';

CREATE TABLE IF NOT EXISTS `finance_agent_withdraw_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `withdraw_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`withdraw_id`) REFERENCES `finance_agent_withdraw`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商提现审批';
```

## 接口清单
| # | Method | Path | 功能 |
|---|---|---|---|
| 1 | GET | `/summary` | KPI：申请数、金额、进行中、待审批 |
| 2 | GET | `/requests` | 提现列表 |
| 3 | POST | `/requests` | 新建代理商提现 |
| 4 | GET | `/requests/{id}` | 详情 + 审批 |
| 5 | POST | `/requests/{id}/approvals` | 审批动作 |

## 接口详情

### 1. `GET /api/admin/v1/finance/agent-withdraw/summary`
- 返回 `{ stats:[...], approvals:[...] }`，数据源 `finance_agent_withdraw`/`finance_agent_withdraw_approval`。

### 2. `GET /api/admin/v1/finance/agent-withdraw/requests`
- **参数**：`status`,`agentId`,`dateFrom`,`dateTo`,`keyword`,`pageNo`,`pageSize`。
- **响应**：`{ total, list:[ { id,agentName,amount,status,owner,updatedAt } ] }`。

### 3. `POST /api/admin/v1/finance/agent-withdraw/requests`
- **Body**：`{ "agentId":12, "amount":150000, "currency":"CNY", "bankAccount":"HDFC-***1234", "remark":"月结" }`.
- **逻辑**：创建请求并生成审批链；校验代理商余额（`agent_finance_snapshot`）。

### 4. `GET /api/admin/v1/finance/agent-withdraw/requests/{id}`
- 返回 `{ request, approvals, logs }`。

### 5. `POST /api/admin/v1/finance/agent-withdraw/requests/{id}/approvals`
- **Body**：`{ "stage":"FINANCE", "decision":"APPROVED|REJECTED", "comment":"..." }`.
- **逻辑**：更新审批；通过后进入 `PAYING` 并执行代付；拒绝则标记 `FAILED` 并通知代理商。
