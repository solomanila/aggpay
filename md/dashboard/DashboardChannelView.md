# DashboardChannelView 后端接口需求

## 页面定位
- `DashboardChannelView.vue` 负责展示重点支付通道的实时健康度：顶部 Hero、过滤维度、统计卡片、吞吐趋势、通道表格、路由策略、服务商负载、异动告警与操作清单。
- 访问入口：仪表板 > 通道视图，所有接口统一挂载在 `/api/admin/v1/dashboard/channels/**`。

## 现有数据映射
- `pay_config_info`、`pay_config_channel`、`pay_config_parameter`: 通道主信息、区域、费率、权重与配置。
- `order_req_record`: 计算实时成功率、延迟、吞吐趋势（`entries`）以及 provider 负载（按 `pay_config_channel_id` 聚合 TPS）。
- `order_info`: 支付/代付订单量用于 `stats`、`volume`、`trend`。
- `order_build_error`: 生成“异动告警”列表。

## 新增表
- 页面需要维护“路由策略”“操作清单”数据，新增：

```sql
CREATE TABLE IF NOT EXISTS `channel_reroute_plan` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `channel_id` bigint NOT NULL COMMENT 'pay_config_channel.id',
  `title` varchar(255) NOT NULL,
  `detail` varchar(512) DEFAULT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `eta_time` datetime DEFAULT NULL,
  `status` varchar(32) DEFAULT 'PENDING',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_reroute_channel` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `ops_checklist_item` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `category` varchar(32) DEFAULT 'CHANNEL',
  `title` varchar(255) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `due_time` datetime DEFAULT NULL,
  `status` varchar(32) DEFAULT 'OPEN',
  `extra` json DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_checklist_platform_status` (`platform_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 接口清单
| # | Method | Path | 功能 |
|---|---|---|---|
| 1 | GET | `/api/admin/v1/dashboard/channels/summary` | Hero、过滤条件、统计卡片、吞吐趋势 |
| 2 | GET | `/api/admin/v1/dashboard/channels/list` | 通道表格 + 健康度、延迟、趋势 |
| 3 | GET | `/api/admin/v1/dashboard/channels/operations` | 路由策略、provider 负载、异动告警、操作清单 |
| 4 | POST | `/api/admin/v1/dashboard/channels/reroute` | 新建/更新 reroute 策略 |

## 接口详情

### 1. `GET /api/admin/v1/dashboard/channels/summary`
- **参数**：`platformId`、`businessType`（UPI/Wallet/Bank/Card）、`window`（默认 30min）。
- **返回**：
  - `hero`: 来自配置中心或 `pay_platform_info`（标题、描述、同步时间）。
  - `filters`: `pay_config_channel` 表内 distinct 业务类型映射 + active 标记。
  - `stats`: `order_req_record` + `order_info` 计算当前峰值 QPS、成功率、平均延迟、触发告警数。
  - `throughput`: `entries` 数组 = 最近 `window` 按 5 分钟切片的 pay-in/pay-out 请求量。`peak`、`imbalance` 由 aggregator 计算。
- **实现**：定时任务写 Redis，接口读取；查询维度 `pay_config_channel.channel_group`（若无则用 `json_param` 中 `category`）。

### 2. `GET /api/admin/v1/dashboard/channels/list`
- **参数**：`platformId`、`keyword`、`status`（green/amber/red）、`country`、`orderBy`（`success|latency|volume`）、`page`、`size`。
- **响应**：`{ "total": 60, "list": [ { id,name,country,latency,success,volume,trend,status,channelId } ] }`。
- **数据**：
  - `name`: `pay_config_channel.title`。
  - `latency/success`: `order_req_record` 15 分钟聚合。
  - `volume`: `order_info` 15 分钟金额占比。
  - `trend`: 与上一窗口对比。
  - `status`: 依据 SLA 阈值（>=99% green, 96-99 amber, <96 red）。

### 3. `GET /api/admin/v1/dashboard/channels/operations`
- **参数**：`platformId`。
- **返回**：
  - `reroutePlans`: `channel_reroute_plan` `status in ('PENDING','RUNNING')`。
  - `providerLoad`: `order_req_record` 按上游 `third_service` 聚合 TPS->`load`。
  - `alerts`: `order_build_error` 针对 `channelId`、`severity` 的摘要 + `action` 建议。
  - `checklist`: `ops_checklist_item` category='CHANNEL'，支持 stale 筛选 >24h。

### 4. `POST /api/admin/v1/dashboard/channels/reroute`
- **Body**：`{ "channelId":123, "title":"UPI 南区降级", "detail":"...", "owner":"Router", "eta":"2026-03-21T12:00:00+05:30" }`。
- **处理**：写 `channel_reroute_plan`，并同步到路由服务（可通过 MQ）；若 body 包含 `id` 则更新。
- **响应**：最新 `reroutePlan` 对象。

## 业务/性能/安全
- 接口读写均需 `CHANNEL_DASHBOARD_VIEW` 权限；写操作需额外的 `CHANNEL_ROUTING_EDIT`。
- 对 `order_req_record` 的统计需走异步任务（60s）+ Redis；查询 `list` 支持实时 refresh=force 以命中 ClickHouse/ES。
- Reroute 创建需记录操作日志至 `order_notify_record`，并附带 `traceId`（MDC）。
- Provider 负载/告警支持 WebSocket 推送（扩展 point-to-point channel），文档约定 SSE endpoint `/api/admin/v1/dashboard/channels/stream`（后续实现）。
