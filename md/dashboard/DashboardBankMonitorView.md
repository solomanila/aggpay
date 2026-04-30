# DashboardBankMonitorView 后端接口需求

## 页面定位
- `DashboardBankMonitorView.vue` 聚焦银行探活/策略：Hero、统计卡、银行列表、阈值设置、告警、自动化任务、维护窗口、公告。
- 接口前缀 `/api/admin/v1/dashboard/bankmonitor/**`，面向运维、策略、风控角色。

## 数据来源
- `pay_config_channel`：若某渠道类型绑定银行，可作为 bank 信息的基础来源（区域、延迟、状态）。
- `order_req_record`: 计算各银行接口延迟、成功率、告警触发。
- `order_build_error`: 生成当前告警列表。

## 新增表
要保存银行监控主数据、阈值策略与自动化任务：

```sql
CREATE TABLE IF NOT EXISTS `bank_monitor_bank` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `bank_code` varchar(32) UNIQUE NOT NULL,
  `bank_name` varchar(128) NOT NULL,
  `region` varchar(64) NOT NULL,
  `platform_id` int NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'green',
  `latency_ms` int DEFAULT NULL,
  `incident_count` int DEFAULT 0,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bank_monitor_threshold` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `bank_code` varchar(32) NOT NULL,
  `metric` varchar(32) NOT NULL,
  `value` varchar(64) NOT NULL,
  `action` varchar(128) DEFAULT NULL,
  `platform_id` int NOT NULL,
  UNIQUE KEY `uniq_bank_metric` (`bank_code`,`metric`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bank_monitor_automation` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `owner` varchar(64) NOT NULL,
  `progress` tinyint DEFAULT 0,
  `eta_time` datetime DEFAULT NULL,
  `status` varchar(32) DEFAULT 'RUNNING'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bank_monitor_maintenance` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `bank_code` varchar(32) NOT NULL,
  `window_text` varchar(128) NOT NULL,
  `note` varchar(255) DEFAULT NULL,
  `platform_id` int NOT NULL,
  `status` varchar(32) DEFAULT 'PLANNED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 接口清单
| # | Method | Path | 功能 |
|---|---|---|---|
| 1 | GET | `/api/admin/v1/dashboard/bankmonitor/summary` | Hero、统计卡、银行列表、阈值 |
| 2 | GET | `/api/admin/v1/dashboard/bankmonitor/ops` | 告警、自动化、维护、公告 |
| 3 | PATCH | `/api/admin/v1/dashboard/bankmonitor/banks/{bankCode}/thresholds` | 更新阈值/动作 |

## 接口详情

### 1. `GET /api/admin/v1/dashboard/bankmonitor/summary`
- **参数**：`platformId`、`region`（可选）。
- **返回**：
  - `hero`: `sync` = 最近一次探活任务时间（`bank_monitor_bank.update_time`）。
  - `stats`: 通过 `bank_monitor_bank` & `bank_monitor_threshold` 统计 `监控银行数/活跃告警/启用策略/探活成功率`。
  - `banks`: `bank_monitor_bank` join `order_req_record` latency -> `{bankCode,name,region,latency,status,incidents}`；`status` 由最新告警 + SLA 决定。
  - `thresholds`: `bank_monitor_threshold` top3 条 `{id,label,value,action}` 供 UI 展示。

### 2. `GET /api/admin/v1/dashboard/bankmonitor/ops`
- **参数**：`platformId`、`severity`。
- **返回**：
  - `alerts`: `order_build_error` where `class_name LIKE '%BankMonitor%' OR extend1=bankCode` -> `severity/title/detail/action`。
  - `automation`: `bank_monitor_automation` `status in ('RUNNING','PENDING')`。
  - `maintenance`: `bank_monitor_maintenance` `status in ('PLANNED','RUNNING')` -> `{bankCode,window,note}`。
  - `notices`: `ops_timeline_event` `event_type='BANK_MONITOR_NOTICE'`。

### 3. `PATCH /api/admin/v1/dashboard/bankmonitor/banks/{bankCode}/thresholds`
- **Body**：`{ "metric":"SUCCESS_RATE", "value":"96", "action":"自动切备用" }`。
- **行为**：
  - 若存在记录则更新；没有则插入 `bank_monitor_threshold`。
  - 同步配置至策略引擎（通过 MQ/配置中心）并写审计日志。
  - 响应最新 `thresholds` 列表。

## 业务/安全
- 角色控制：只有 `BANK_MONITOR_ADMIN` 可修改阈值，`VIEWER` 仅调用 GET。
- 探活数据需由后端定时探测任务写入 `bank_monitor_bank`（`latency_ms`、`status`），接口不直接访问探活服务。
- 告警 ACK/策略更新需持久化 `traceId` + 操作人，用于 SOX 审计。
