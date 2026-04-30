# DashboardOverviewView 后端接口需求

## 页面定位
- `DashboardOverview.vue` 在“仪表板”菜单默认子页，整合 Hero、快速指标、健康度、告警、区域延迟、容量、时间线等模块。
- 数据既包含实时计算指标（订单、请求、告警）也包含运维/项目类跟进事项，需要统一落在 pay-service 的 `/api/admin/v1/dashboard/overview/**` 路径下并支持多租户平台 `platformId`、时区 `timezone` 过滤。

## 现有数据来源（pay.sql）
- `order_info`：计算今日交易额/笔数、成功率、在途资金等。
- `order_req_record`：统计 SLA、端到端延迟、流量贡献（以 `pay_config_channel_id` 或 `pay_config_id` 聚合）。
- `order_build_error`、`order_notify_record`：填充“关键告警”“处理记录”与异常 pill。
- `pay_config_channel` / `pay_config_info`：提供国家/业务类型映射，用于 Flow breakdown、区域延迟。
- `pay_platform_info`：Hero 指标中的“在营国家”“平台概览”。

## 新增表（页面独有数据）
页面展示的维护计划、行动项、时间线、公告在当前 schema 中不存在，需补充如下表（由 pay-service 维护）：

```sql
CREATE TABLE IF NOT EXISTS `ops_timeline_event` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `event_type` varchar(32) NOT NULL COMMENT 'TIMELINE/NOTICE/MAINTENANCE',
  `title` varchar(255) NOT NULL,
  `detail` varchar(512) DEFAULT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `severity` varchar(16) DEFAULT NULL,
  `occur_time` datetime NOT NULL,
  `eta_time` datetime DEFAULT NULL,
  `status` varchar(32) DEFAULT 'OPEN',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_timeline_platform_time` (`platform_id`, `occur_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `ops_action_item` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `owner` varchar(64) NOT NULL,
  `progress` tinyint NOT NULL DEFAULT 0,
  `category` varchar(32) DEFAULT 'CAPACITY',
  `expect_finish_time` datetime DEFAULT NULL,
  `status` varchar(32) DEFAULT 'PENDING',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_action_platform_status` (`platform_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/dashboard/overview/summary` | Hero + 快速指标 + 健康度 | 聚合 `order_info`、`order_req_record`、`pay_platform_info` | 
| 2 | GET | `/api/admin/v1/dashboard/overview/incidents` | 告警、区域延迟、维护计划 | 组合 `order_build_error`, `ops_timeline_event`
| 3 | GET | `/api/admin/v1/dashboard/overview/operations` | 时间线、容量、行动项、公告 | 读取 `order_req_record` 聚合 & `ops_action_item`, `ops_timeline_event`
| 4 | POST | `/api/admin/v1/dashboard/overview/incidents/{incidentId}/ack` | 告警确认 | 将 `order_build_error` or `ops_timeline_event` 标记为已确认

所有响应统一 `R<T>`：`{ "code":0,"message":"OK","data":{...},"traceId":"mdc-***" }`。

## 接口详情

### 1. `GET /api/admin/v1/dashboard/overview/summary`
- **查询参数**：`platformId`（可选，默认当前登录平台）、`timezone`、`range`=`TODAY|HOUR`。
- **返回**：
  - `hero`: `{eyebrow,title,description,sync:{label,value}}`；`sync.value` 为最近一次指标写入时间（Redis）。
  - `quickStats`: 4 项，来源：
    - `今日交易额/笔数`=`SUM(order_info.real_amount)`/`COUNT(order_info)` （status=成功，时间取 timezone 当天）。
    - `成功率`=`成功/总请求` from `order_req_record`。
    - `风控拦截`：`order_build_error` 中 `status=OPEN` 且 1 小时内；
  - `healthMetrics`: 以 `order_req_record` 分通道的 `latency_p95`、`approval_rate`、`sla`。
  - `flowBreakdown`: `order_info` 根据 `pay_config_channel` 业务类型聚合的占比。
- **缓存**：30s；返回 `traceId` 绑定 Redis 统计任务日志。

### 2. `GET /api/admin/v1/dashboard/overview/incidents`
- **查询参数**：`platformId`、`severity`（可选）、`sinceMinutes`（默认 60）。
- **返回**：
  - `incidents`: `order_build_error` 中 `status=OPEN` 的最近 N 条，字段 `{id,severity,title,className,detail,owner,eta}`；`severity` 依据错误等级映射。
  - `maintenance`: 从 `ops_timeline_event` 中 `event_type='MAINTENANCE'`、`status='PENDING'` 的条目，字段 `{id,label,window,owner}`。
  - `regionalLatencies`: `order_req_record` 最近 15 分钟以国家维度聚合 `{region,latency,diff,status}`。
- **安全**：仅 `DASHBOARD_VIEW` 角色可访问。

### 3. `GET /api/admin/v1/dashboard/overview/operations`
- **查询参数**：`platformId`、`timezone`。
- **返回**：
  - `timeline`: 最近 1 小时的 `ops_timeline_event.event_type='TIMELINE'` + `order_notify_record` 中 `log_text` 关键操作，格式 `{id,time,title,type}`。
  - `capacity`: `{utilization:'82%',change:'+3%'}` 来自 `order_info` 资金在途/额度使用率；`segments` 以 `pay_config_channel` 汇总 `[ {id,label,value} ]`。
  - `actionItems`: `ops_action_item` 列表（进度条+负责人）。
  - `notices`: `ops_timeline_event.event_type='NOTICE'` 近 5 条文本。

### 4. `POST /api/admin/v1/dashboard/overview/incidents/{incidentId}/ack`
- **用途**：在前端“关键告警”中点击“已处理”或“忽略”时调用。
- **Body**：`{ "action": "ACK|IGNORE", "comment": "..." }`。
- **处理**：
  - 若 `incidentId` 属于 `order_build_error`，则更新自定义字段 `ack_status`（需在表上新增列或借助扩展表）并写入 `order_notify_record` 日志。
  - 若属于 `ops_timeline_event`，将 `status='ACKED'` 并记录 `ack_user`。
- **响应**：最新 incident 对象。

## 业务与性能要求
- 所有接口带 `platformId` 过滤，若缺省则根据 `AuthContextHolder` 注入的平台上下文。
- 指标计算通过调度任务写入 Redis，接口从缓存读取并补充实时 Delta，避免直接扫描热表（`order_req_record`、`order_info`）。
- `ops_timeline_event`/`ops_action_item` 的写操作需记录审计日志与触发通知（可复用 `order_notify_record`）。
- 需提供灰度参数（配置中心）来调整 SLA 阈值、latency 告警等，以免频繁修改代码。
