# AgentsListView 后端接口需求

## 页面定位
- `AgentsListView.vue` 位于“代理商/列表”，展示代理商基础档案（区域、配额、状态、Owner）与指标卡。
- 数据需支持状态筛选、分页、更新档案，接口前缀 `/api/admin/v1/agents/**`。

## 现有数据来源
- 代理商实体不存在于当前 schema，需复用 `agent_profile`（见 AgentsChannelsView）并补充配额、结算数据。
- 可利用 `order_info` 聚合代理商贡献交易额。

## 新增/扩展数据结构
- 复用 `agent_profile`。
- 新增代理商结算/资金信息表（若需要进一步细化）：

```sql
CREATE TABLE IF NOT EXISTS `agent_finance_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_id` bigint NOT NULL,
  `stat_date` date NOT NULL,
  `balance` decimal(18,2) DEFAULT 0,
  `quota_used` decimal(18,2) DEFAULT 0,
  `payout_amount` decimal(18,2) DEFAULT 0,
  `fee_amount` decimal(18,2) DEFAULT 0,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_agent_date` (`agent_id`,`stat_date`),
  FOREIGN KEY (`agent_id`) REFERENCES `agent_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商资金快照';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/agents/summary` | 指标卡 | 总代理商、启用数、配额占用、在营区域 |
| 2 | GET | `/api/admin/v1/agents` | 代理商列表 | 分页筛选状态/区域/Owner |
| 3 | GET | `/api/admin/v1/agents/{agentId}` | 代理商详情 | 档案、配额、资金快照、渠道绑定 |
| 4 | POST | `/api/admin/v1/agents` | 新建代理商 | 创建档案、配额、初始渠道绑定 |
| 5 | POST | `/api/admin/v1/agents/{agentId}` | 更新档案 | 调整状态、配额、Owner、备注 |

## 接口详情

### 1. `GET /api/admin/v1/agents/summary`
- **返回**：`{ stats:[ {id:'agent-total',value,meta}, ... ] }`。
  - `stats`: `agent_profile` 聚合数量/状态；`quota` 来自 `agent_finance_snapshot` 最近一日。

### 2. `GET /api/admin/v1/agents`
- **查询参数**：`status`,`region`,`owner`,`keyword`,`pageNo`,`pageSize`.
- **逻辑**：查询 `agent_profile`，`keyword` 匹配 `name/agent_code/contact`；结合 `agent_finance_snapshot` 最新记录返回 `{id,name,region,quota,status,owner,updatedAt}`。

### 3. `GET /api/admin/v1/agents/{agentId}`
- **返回**：`{ profile, finance, channels }`。
  - `profile`: `agent_profile`。
  - `finance`: 最近一次 `agent_finance_snapshot`。
  - `channels`: 来自 `agent_channel_binding`。
- **权限**：`AGENT_VIEW`。

### 4. `POST /api/admin/v1/agents`
- **Body**：`{ "agentCode":"AGT001", "name":"SEA Gateway", "region":"SEA", "quotaAmount":6000000, "ownerUserId":1002, "contact":"ops@xxx.com" }`.
- **逻辑**：创建 `agent_profile`，初始化 `agent_finance_snapshot`（余额=0），可选创建默认渠道绑定；返回新 ID。

### 5. `POST /api/admin/v1/agents/{agentId}`
- **Body**：`{ "status":"ENABLED|MAINTENANCE|DISABLED", "quotaAmount":8000000, "ownerUserId":1020, "remark":"季度调额" }`.
- **逻辑**：更新 `agent_profile`，必要时更新 `agent_finance_snapshot.quota_used`；写操作记录到 `agent_notice`。
- **响应**：最新 `profile` + `finance`。
