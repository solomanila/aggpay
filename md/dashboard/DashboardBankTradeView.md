# DashboardBankTradeView 后端接口需求

## 页面定位
- `DashboardBankTradeView.vue` 用于 BankTrade 接口运行情况：Hero、统计卡、服务健康、队列深度、吞吐曲线、告警、发布/集成进度与通知。
- 接口统一挂载 `/api/admin/v1/dashboard/banktrade/**`，面向 BankTrade 运维、开发和清算角色。

## 现有数据映射
- `order_req_record`: BankTrade 订单请求、吞吐、成功率、延迟；可通过 `class_name` 或 `pay_config_channel` 标记 BankTrade 相关请求。
- `order_notify_record`: 通知 BankTrade 回调；可计算错误峰值、队列重试。
- `order_build_error`: 生成 BankTrade 相关告警。

## 新增表
BankTrade 模块需要结构化保存子服务健康、队列、集成计划：

```sql
CREATE TABLE IF NOT EXISTS `bank_trade_service_metric` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `service_name` varchar(64) NOT NULL,
  `platform_id` int NOT NULL,
  `status` varchar(16) NOT NULL,
  `latency_ms` int NOT NULL,
  `success_rate` decimal(5,2) NOT NULL,
  `snapshot_time` datetime NOT NULL,
  KEY `idx_bt_service_time` (`service_name`,`snapshot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bank_trade_queue_metric` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `queue_name` varchar(64) NOT NULL,
  `platform_id` int NOT NULL,
  `depth` int NOT NULL,
  `wait_ms` int NOT NULL,
  `snapshot_time` datetime NOT NULL,
  KEY `idx_bt_queue_time` (`queue_name`,`snapshot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bank_trade_integration` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `owner` varchar(64) NOT NULL,
  `progress` tinyint DEFAULT 0,
  `eta` varchar(64) DEFAULT NULL,
  `status` varchar(32) DEFAULT 'RUNNING',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 接口清单
| # | Method | Path | 功能 |
|---|---|---|---|
| 1 | GET | `/api/admin/v1/dashboard/banktrade/summary` | Hero、统计、服务健康、队列、吞吐 |
| 2 | GET | `/api/admin/v1/dashboard/banktrade/incidents` | 告警、发布时间线、通知 |
| 3 | GET | `/api/admin/v1/dashboard/banktrade/integrations` | 项目进度列表 |
| 4 | POST | `/api/admin/v1/dashboard/banktrade/incidents/{id}/ack` | 告警确认/备注 |

## 接口详情

### 1. `GET /api/admin/v1/dashboard/banktrade/summary`
- **查询参数**：`platformId`、`window`（默认 30 分钟）、`timezone`。
- **返回**：
  - `hero`: 静态文案 + `sync`。
  - `stats`: `order_req_record` `pay_config_channel.third_service='BankTrade'` 计算今日交易量/成功率/延迟/错误峰值。
  - `serviceHealth`: 最近一次 `bank_trade_service_metric` 记录；字段 `{name,status,latency,success}`。
  - `queues`: `bank_trade_queue_metric` 最新快照；`wait`=毫秒 -> 秒。
  - `throughput`: `order_req_record` 5 分钟粒度 `success`/`failed` 数。

### 2. `GET /api/admin/v1/dashboard/banktrade/incidents`
- **参数**：`platformId`、`severity`、`sinceMinutes`（默认 120）。
- **返回**：
  - `incidents`: 筛选 `order_build_error` by `class_name LIKE '%BankTrade%'` + severity (映射 500 错=高, 超时=中)。字段 `{id,severity,title,detail,action}`。
  - `releaseTimeline`: 读取 `ops_timeline_event` event_type='BANKTRADE_RELEASE'（若共用 timeline 表）。
  - `notices`: `order_notify_record` class='BankTrade' 最近 5 条 message。

### 3. `GET /api/admin/v1/dashboard/banktrade/integrations`
- **参数**：`platformId`。
- **响应**：`{ "list": [ {id,title,owner,progress,eta,status} ] }` from `bank_trade_integration`。
- **扩展**：支持 `PATCH /{id}` 更新进度（可在后续补充）。

### 4. `POST /api/admin/v1/dashboard/banktrade/incidents/{id}/ack`
- **Body**：`{ "action":"ACK", "comment":"切换备用线路" }`。
- **行为**：更新 `order_build_error` 扩展字段 `ack_status`，记录 `ack_user`、`ack_time`；若 `action`=IGNORE 需保留历史。

## 其他要求
- 统计任务需将 BankTrade 指标独立出维度 `service_name`，便于 drill-down。
- 告警 ACK 必须落审计日志（`order_notify_record`），并在 10 分钟内同步给值班机器人（Telegram/BOT）。
- 接口默认缓存 10 秒，`?forceRefresh=true` 时走实时计算（需限流 10 QPS）。
