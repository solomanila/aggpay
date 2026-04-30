# PaymentsChannelView 后端接口需求

## 页面定位
- `PaymentsChannelView.vue` 位于“支付/通道配置”，展示通道权重、限额、费率、区域、状态、维护计划、调参项目等，供策略/运维统一管理。
- 关键模块：指标卡（启用/活跃/平均费率/告警）、筛选 chips、通道列表表格、路由调优项目（进度条）、维护窗口、公告。

## 现有数据来源（pay.sql & pay-service）
- `pay_config_channel`：包含渠道基础配置（`title`,`json_param`,`status`）与 `pay_config_id` 关联。
- `pay_config_info`：描述支付配置信息（`third_service`,`call_method`,`area_type`），可与通道多对一。
- `pay_config_parameter`：存储通道参数，可映射费率、限额 JSON。
- `order_req_record`：可统计通道请求数、成功率、平均响应，支撑“活跃路由”、“告警中的通道”指标。
- `order_info`：按 `pay_config_channel_id` 聚合可得交易量、限额使用。
- 现有 `IPayConfigChannelService`、`ExtPayConfigLimitMapper`(限流) 能复用基本 CRUD 与限额校验逻辑。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `pay_channel_profile` (
  `channel_id` bigint PRIMARY KEY COMMENT 'pay_config_channel.id',
  `pay_config_id` int NOT NULL,
  `channel_code` varchar(64) NOT NULL,
  `region` varchar(32) DEFAULT NULL,
  `business_type` varchar(32) DEFAULT NULL COMMENT 'UPI/WALLET/BANK/CARD',
  `weight_percent` decimal(5,2) DEFAULT 0,
  `daily_limit` decimal(18,2) DEFAULT NULL,
  `fee_rate` decimal(5,2) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ENABLED',
  `alert_level` varchar(16) DEFAULT 'NORMAL',
  `owner` varchar(64) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道运行配置';

CREATE TABLE IF NOT EXISTS `pay_channel_routing_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `progress` tinyint DEFAULT 0,
  `eta` datetime DEFAULT NULL,
  `status` varchar(16) DEFAULT 'OPEN',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_channel_status` (`channel_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道调优项目';

CREATE TABLE IF NOT EXISTS `pay_channel_maintenance` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel_id` bigint NOT NULL,
  `window_start` datetime NOT NULL,
  `window_end` datetime NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'PLANNED',
  `created_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道维护窗口';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/payments/channels/summary` | 指标卡 + 筛选枚举 + 公告 | 聚合通道数量、活跃度、费率、告警 |
| 2 | GET | `/api/admin/v1/payments/channels` | 通道分页列表 | 支持状态/业务类型/关键字过滤 |
| 3 | GET | `/api/admin/v1/payments/channels/{channelId}` | 通道详情 | 返回参数、限额、费率、实时 SLA |
| 4 | POST | `/api/admin/v1/payments/channels/{channelId}/routing-tasks` | 新增/更新调优项目 | 维护路由调优模块 |
| 5 | POST | `/api/admin/v1/payments/channels/{channelId}/maintenance` | 维护窗口创建/关闭 | 对应页面“维护计划”列表 |

## 接口详情

### 1. `GET /api/admin/v1/payments/channels/summary`
- **查询参数**：`platformId`（可选，默认当前登录平台）、`timezone`。
- **返回**：
  - `stats`: 基于 `pay_config_channel.status` 和 `order_req_record` 最近 15 分钟请求数/成功率。
  - `filters`: 枚举 `['全部','UPI','Wallet','Bank','Card']` 从 `pay_channel_profile.business_type` distinct。
  - `notices`: 通知来自 `order_notify_record` 中类型 `CHANNEL_NOTICE` 或最新 `pay_channel_maintenance`.
- **实现**：数据每日跑批写 Redis，接口兜底 DB 查询。

### 2. `GET /api/admin/v1/payments/channels`
- **查询参数**：`status`,`businessType`,`region`,`keyword`,`pageNo`,`pageSize`.
- **返回**：`PageResult`，每条 `{channelId,name,region,weightPercent,dailyLimit,feeRate,status,alertLevel,owner,updatedAt}`，并附 `telemetry`（成功率、平均响应）从 `order_req_record`.
- **排序**：默认按 `alertLevel`（紧急优先）再按最近 `order_req_record.req_time`.

### 3. `GET /api/admin/v1/payments/channels/{channelId}`
- **返回**：`{basic, params, routingTasks, maintenance}`。
  - `basic`: `pay_config_channel` + `pay_channel_profile`.
  - `params`: 读取 `pay_config_parameter`（脱敏 secret）。
  - `metrics`: `order_req_record`、`order_info` 聚合（交易额、成功率、限额使用）。
  - `routingTasks`: `pay_channel_routing_task` 列表。
- **安全**：需 `PAYMENT_CHANNEL_VIEW` 权限；secret 参数仅 `SUPER_ADMIN` 可见。

### 4. `POST /api/admin/v1/payments/channels/{channelId}/routing-tasks`
- **Body**：`{ "title":"UPI 夜间限流", "owner":"策略", "progress":40, "eta":"2024-05-22T12:00:00Z", "status":"OPEN" }`.
- **逻辑**：若 body 带 `id` 则更新；否则插入并记录到 `order_notify_record` 作为公告。
- **响应**：最新任务列表。

### 5. `POST /api/admin/v1/payments/channels/{channelId}/maintenance`
- **Body**：`{ "windowStart":"2024-05-22T02:00:00Z", "windowEnd":"2024-05-22T03:00:00Z", "reason":"银行升级", "status":"PLANNED|DONE|CANCELLED" }`.
- **逻辑**：写 `pay_channel_maintenance`，若开始时间 < 当前 + 10min 触发 webhook（可通过 `order_notify_record` 触发）。
- **扩展**：提供查询参数 `?status=PLANNED` 拉取列表；若 `status=DONE` 同时把窗口推送到 `order_build_error` 关联告警解除。
