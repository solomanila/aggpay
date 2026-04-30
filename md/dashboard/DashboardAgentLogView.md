# DashboardAgentLogView 后端接口需求

## 页面定位
- `DashboardAgentLogView.vue` 展示代理账户关键操作：Hero、统计、筛选、操作日志、审批流、洞察、审计时间线。
- 接口前缀 `/api/admin/v1/dashboard/agents/**`，涉及充值/限额/风控/审批操作的读写。

## 数据映射
- `order_virtual_account`: 可关联代理账户、余额信息。
- `order_info`: 与代理相关的充值/代付订单（若 `user_id` 对应代理）。
- `order_build_error`: 风控拦截事件。

## 新增表
代理操作日志与审批过程 pay.sql 中不存在，需新增：

```sql
CREATE TABLE IF NOT EXISTS `agent_operation_log` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_code` varchar(64) NOT NULL,
  `platform_id` int NOT NULL,
  `op_type` varchar(32) NOT NULL COMMENT '充值/限额/风控/审批',
  `amount_text` varchar(128) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `operator` varchar(64) NOT NULL,
  `occur_time` datetime NOT NULL,
  `extra` json DEFAULT NULL,
  KEY `idx_agent_op_time` (`agent_code`,`occur_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `agent_approval_flow` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_code` varchar(64) NOT NULL,
  `platform_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `stage` varchar(64) NOT NULL,
  `owner` varchar(64) NOT NULL,
  `status` varchar(32) DEFAULT 'PENDING',
  `eta_text` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `agent_insight` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `content` varchar(512) NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 接口清单
| # | Method | Path | 功能 |
|---|---|---|---|
| 1 | GET | `/api/admin/v1/dashboard/agents/logs` | Hero、统计、操作日志（分页） |
| 2 | GET | `/api/admin/v1/dashboard/agents/approvals` | 审批流、洞察、审计时间线 |
| 3 | POST | `/api/admin/v1/dashboard/agents/logs` | 新增人工操作日志 |

## 接口详情

### 1. `GET /api/admin/v1/dashboard/agents/logs`
- **参数**：`platformId`、`opType`（ALL/充值/限额/风控/审批）、`status`、`keyword`（agent/operator）、`startTime`、`endTime`、`page`、`size`。
- **返回**：
  - `hero`: `sync` 时间来自定时聚合。
  - `stats`: 4 张卡片——`今日操作`(count of logs today)、`自动通过率`(自动审批 logs / 总审批 logs)、`风控拦截`(op_type='风控' status=拦截)、`活跃代理`(distinct agent 今日操作)。来源 `agent_operation_log`。
  - `filters`: 固定数组 + `active` 标记。
  - `operations`: `agent_operation_log` 分页结果 `{id,agent,type,amount,status,operator,time,extra}`，`extra` 用于 UI tooltip。

### 2. `GET /api/admin/v1/dashboard/agents/approvals`
- **参数**：`platformId`。
- **返回**：
  - `approvals`: `agent_approval_flow` `status in ('PENDING','RUNNING')` -> `{id,title,stage,owner,eta,status}`。
  - `insights`: `agent_insight` 最近 N 条 -> UI “洞察”。
  - `auditTimeline`: 结合 `agent_operation_log` (审批类) + `order_notify_record` 生成 `{time,action,owner}`。

### 3. `POST /api/admin/v1/dashboard/agents/logs`
- **Body**：`{ "agentCode":"Agent SEA-01", "opType":"充值", "amountText":"+¥420,000", "status":"成功", "extra":{"channel":"INR"} }`。
- **行为**：
  - 仅内部自动化/手工接口调用。校验 `agentCode` 存在（可在 `order_virtual_account` ext 字段）。
  - 保存到 `agent_operation_log`；若 `opType`=审批则同步 `agent_approval_flow`。
  - 返回新建记录 + `traceId`。

## 运行要求
- 所有接口记录审计日志（who/when/traceId）。
- `agent_operation_log` 建议写入消息队列，由大数据平台消费生成实时指标；接口读取 Redis 聚合以支撑 Hero/统计卡片。
- 需对敏感操作（限额/风控）做字段脱敏，向前端仅返回部分金额或标记 `***`。
