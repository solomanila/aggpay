# BankRealtimeBoardView 后端接口需求

## 页面定位
- `BankRealtimeBoardView.vue` 位于“银行户管理/公户实时面板”，实时监控重点账户的余额、交易速率、风险等级、告警及时间线。
- 页面包含指标卡、账户卡片、告警列表、时间线，需要高频刷新（默认 30 秒）。

## 现有数据来源（pay.sql & pay-service）
- `bank_account`（见 `BankAccountsView.md`）提供账户基础信息。
- `order_info` / `order_req_record` 提供实时交易额、TPS，可用作指标。
- 需要新增实时指标、告警、时间线表以供 Dashboard 使用。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `bank_account_metric_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `snapshot_time` datetime NOT NULL,
  `balance` decimal(18,2) NOT NULL,
  `inflow_rate` decimal(18,2) DEFAULT 0 COMMENT '单位:金额/分',
  `outflow_rate` decimal(18,2) DEFAULT 0,
  `txn_per_min` int DEFAULT 0,
  `limit_usage_pct` decimal(5,2) DEFAULT 0,
  `risk_level` varchar(16) DEFAULT 'LOW',
  KEY `idx_account_time` (`account_id`,`snapshot_time` DESC),
  FOREIGN KEY (`account_id`) REFERENCES `bank_account`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行账户实时快照';

CREATE TABLE IF NOT EXISTS `bank_account_alert` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `severity` varchar(16) NOT NULL COMMENT 'LOW/MEDIUM/HIGH',
  `action` varchar(64) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'OPEN',
  `triggered_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `acked_by` bigint DEFAULT NULL,
  `acked_at` datetime DEFAULT NULL,
  FOREIGN KEY (`account_id`) REFERENCES `bank_account`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公户告警';

CREATE TABLE IF NOT EXISTS `bank_account_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint DEFAULT NULL,
  `event_time` datetime NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `event_type` varchar(16) DEFAULT 'OPS',
  `data` json DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公户时间线';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/bank/realtime-board/summary` | 指标卡 | 汇总余额、速率、告警、限额使用 |
| 2 | GET | `/api/admin/v1/bank/realtime-board/accounts` | 实时账户列表 | 支持状态/风险过滤，返回快照 |
| 3 | GET | `/api/admin/v1/bank/realtime-board/accounts/{accountId}` | 单账户详情 | 包含最近快照、历史曲线、告警、时间线 |
| 4 | POST | `/api/admin/v1/bank/realtime-board/alerts/{alertId}/ack` | 告警处理 | 将告警状态更新为 ACKED/CLOSED |
| 5 | GET | `/api/admin/v1/bank/realtime-board/timeline` | 全局时间线 | 组合账户事件与维护事件 |

## 接口详情

### 1. `GET /api/admin/v1/bank/realtime-board/summary`
- **查询参数**：`platformId`（可选）、`timezone`。
- **返回**：`{ stats:[...], alertsSummary:{high,medium,low}, lastSync:"2024-05-20T10:32:00Z" }`。
  - `stats`：以最近一次 `bank_account_metric_snapshot` 聚合计算总余额、平均交易速率（`avg(txn_per_min)`）、告警数量 (`bank_account_alert.status='OPEN'`)、限额使用率。
  - `lastSync`：最近的快照时间；若滞后超过 SLA（60s）触发 notice。

### 2. `GET /api/admin/v1/bank/realtime-board/accounts`
- **查询参数**：`status`,`riskLevel`,`keyword`,`pageNo`,`pageSize`.
- **逻辑**：联表 `bank_account` + 最新 `bank_account_metric_snapshot`（可通过子查询 max(snapshot_time) 取最新）返回 `{accountId,name,balance,status,txnPerMin,riskLevel}`。
- **补充**：附带 `alerts` 数量 (`bank_account_alert` 未关闭 count) 供前端显示红点。
- **刷新**：支持 `If-Modified-Since` 头部避免重复查询。

### 3. `GET /api/admin/v1/bank/realtime-board/accounts/{accountId}`
- **返回**：
  - `profile`: `bank_account`.
  - `metrics`: 最近 N 条 `bank_account_metric_snapshot`.
  - `alerts`: 该账户 OPEN 告警列表。
  - `timeline`: 最近 10 条 `bank_account_timeline`.
  - `orders`: 可选 `?includeOrders=true` 时调用订单服务查询实时交易。
- **安全**：`BANK_ACCOUNT_VIEW`; `timeline.data` 中敏感字段需脱敏。

### 4. `POST /api/admin/v1/bank/realtime-board/alerts/{alertId}/ack`
- **Body**：`{ "action":"ACK|CLOSE", "comment":"已补充余额" }`.
- **逻辑**：更新 `bank_account_alert.status` & `acked_by/acked_at`；若 `action=CLOSE` 记录在 `bank_account_timeline`；返回最新 alert 对象。
- **权限**：`BANK_ACCOUNT_ALERT_EDIT`.

### 5. `GET /api/admin/v1/bank/realtime-board/timeline`
- **查询参数**：`rangeMinutes`（默认 60）、`accountId`（可选）。
- **数据**：从 `bank_account_timeline` 获取事件，并可混入 `bank_supplier_maintenance`/`bank_account_maintenance`（转换为 timeline 条目）以及 `order_notify_record` 中 type=`BANK_BALANCE_EVENT`。
- **返回**：`{ "events": [ {id,time,title,owner,eventType} ] }`，用于页面“时间线”模块。
