# FinanceAgentBillingView 后端接口需求

## 页面定位
- `FinanceAgentBillingView.vue` 展示“财务/代理商账单”，需要按代理商周期查看账单金额、状态、负责人。
- 接口前缀 `/api/admin/v1/finance/agent-billing/**`。

## 数据来源
- 复用代理商表 `agent_profile`（见 agent 文档），需要新增代理账单表。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `finance_agent_billing` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_id` bigint NOT NULL,
  `cycle_start` date NOT NULL,
  `cycle_end` date NOT NULL,
  `settlement_amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_agent_cycle` (`agent_id`,`cycle_start`),
  FOREIGN KEY (`agent_id`) REFERENCES `agent_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商账单';
```

## 接口清单
| # | Method | Path | 功能 |
|---|---|---|---|
| 1 | GET | `/summary` | KPI：账单数量、金额、待处理 |
| 2 | GET | `/cycles` | 账单列表（按代理商/状态筛选） |
| 3 | GET | `/cycles/{id}` | 详情（含结算明细/日志） |
| 4 | POST | `/cycles` | 创建/更新代理账单 |

## 接口详情

### 1. `GET /api/admin/v1/finance/agent-billing/summary`
- 聚合 `finance_agent_billing`：今日账单、金额、待审批等。

### 2. `GET /api/admin/v1/finance/agent-billing/cycles`
- **参数**：`agentId`,`status`,`dateFrom`,`dateTo`,`keyword`,`pageNo`,`pageSize`。
- **响应**：`{ "total": 56, "list": [ { id,agentName,cycle,amount,status,owner,updatedAt } ] }`。

### 3. `GET /api/admin/v1/finance/agent-billing/cycles/{id}`
- 返回 `{ billing, details }`：`billing` 来自主表，`details` 可基于 `order_info`/`payout_rollback_task` 等生成的代理分润明细（如需另建 `finance_agent_billing_detail`）。

### 4. `POST /api/admin/v1/finance/agent-billing/cycles`
- **Body**：`{ "agentId":12, "cycleStart":"2024-05-01", "cycleEnd":"2024-05-07", "settlementAmount":320000, "status":"PENDING", "ownerUserId":2003 }`.
- **逻辑**：创建或更新账单；可支持 `status`（PENDING/PAID）；更新后写通知并触发审批流程（如果需要可复用 `finance_billing_approval` 结构）。*** End Patch
