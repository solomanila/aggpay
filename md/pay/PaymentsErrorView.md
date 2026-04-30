# PaymentsErrorView 后端接口需求

## 页面定位
- `PaymentsErrorView.vue` 位于“支付/通道错误记录”，以卡片+表格形式展示近期通道异常、影响、状态、负责人、处理动作、根因及公告。
- 模块：指标卡（今日错误/已解决/平均恢复/处理中）、筛选（按通道类型）、错误表格、动作列表、根因库、公告。

## 现有数据来源（pay.sql & pay-service）
- `order_build_error`：已记录构建/下游异常，字段 `mdc_id`,`platform_id`,`pay_config_channel_id`,`class_name`,`error_text` 可用作 incident 原始数据。
- `order_req_record`：可统计某通道失败率、响应耗时，用于评估影响。
- `order_notify_record`：可记录处理日志、复盘结论。
- `IPayConfigChannelService`、`OrderBuildErrorService` 已可按渠道查询错误日志。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `pay_channel_error_incident` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `mdc_id` varchar(64) DEFAULT NULL,
  `platform_id` int NOT NULL,
  `pay_config_channel_id` bigint NOT NULL,
  `error_type` varchar(64) NOT NULL,
  `impact_ratio` decimal(5,2) DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'OPEN',
  `severity` varchar(16) DEFAULT 'MEDIUM',
  `owner` varchar(64) DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_channel_status` (`pay_config_channel_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道异常事件';

CREATE TABLE IF NOT EXISTS `pay_channel_error_action` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `incident_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `progress` tinyint DEFAULT 0,
  `status` varchar(16) DEFAULT 'OPEN',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_incident` (`incident_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错误处理动作';

CREATE TABLE IF NOT EXISTS `pay_channel_error_root_cause` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `incident_id` bigint NOT NULL,
  `channel_id` bigint NOT NULL,
  `cause` varchar(255) NOT NULL,
  `resolution` varchar(255) DEFAULT NULL,
  `confirmed_by` varchar(64) DEFAULT NULL,
  `confirmed_at` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_incident_channel` (`incident_id`,`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='根因库';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/payments/channel-errors/summary` | 指标卡 + 公告 | 聚合今日错误/平均恢复时间 |
| 2 | GET | `/api/admin/v1/payments/channel-errors` | 错误列表 | 支持通道类型/状态/关键字过滤 |
| 3 | GET | `/api/admin/v1/payments/channel-errors/{incidentId}` | 错误详情 | 包含 `order_build_error` 原始日志、动作、根因 |
| 4 | POST | `/api/admin/v1/payments/channel-errors/{incidentId}/actions` | 新增或更新处理动作 | 对应页面“应急动作”进度条 |
| 5 | POST | `/api/admin/v1/payments/channel-errors/{incidentId}/root-cause` | 填写根因/解决方案 | 同步到根因库与公告 |

## 接口详情

### 1. `GET /api/admin/v1/payments/channel-errors/summary`
- **查询参数**：`platformId`、`range=TODAY|HOUR`。
- **返回**：
  - `stats`: 来自 `pay_channel_error_incident` (状态=OPEN/RESOLVED) + `order_build_error`（用于 fallback 数据）。
  - `notices`: 近 5 条 `order_notify_record` 中 `type='CHANNEL_ERROR'`。
- **耗时指标**：计算 `平均恢复` = `avg(TIMESTAMPDIFF(minute, created_at, updated_at))` where status='RESOLVED'.

### 2. `GET /api/admin/v1/payments/channel-errors`
- **查询参数**：`businessType`,`status`,`owner`,`keyword`,`pageNo`,`pageSize`.
- **返回**：`PageResult` of `{incidentId,channelName,errorType,impactRatio,status,owner,updatedAt,severity}`。
- **数据**：join `pay_channel_error_incident` 与 `pay_config_channel`。`impactRatio` 通过 `order_req_record` 最近 15 分钟失败率计算。

### 3. `GET /api/admin/v1/payments/channel-errors/{incidentId}`
- **返回**：`{incident, orderLog, actions, rootCauses}`。
  - `incident`: `pay_channel_error_incident`.
  - `orderLog`: 关联 `order_build_error` (通过 `mdc_id` 或 `pay_config_channel_id`) 只返回脱敏字段。
  - `actions`: `pay_channel_error_action`.
  - `rootCauses`: `pay_channel_error_root_cause`.
- **鉴权**：`PAYMENT_ERROR_VIEW`；下载完整堆栈需 `SUPER_ADMIN`.

### 4. `POST /api/admin/v1/payments/channel-errors/{incidentId}/actions`
- **Body**：`{ "title":"UPI 超时限流策略", "owner":"ops", "progress":52, "status":"OPEN|DONE" }`.
- **逻辑**：插入/更新动作并写 `order_notify_record` 作为处理日志，若进度=100% 自动把 incident 状态置 `RESOLVED`。
- **响应**：最新 `actions` 列表。

### 5. `POST /api/admin/v1/payments/channel-errors/{incidentId}/root-cause`
- **Body**：`{ "cause":"银行侧 CPU 饱和", "resolution":"分流 + 扩容", "confirmedBy":"ops" }`.
- **逻辑**：创建 `pay_channel_error_root_cause`；若 `resolution` 非空，将结果推送到 `order_notify_record` 并在前端“根因库”展示。
- **额外**：允许 `PUT` 更新 existing root cause；操作需 `PAYMENT_ERROR_EDIT`。
