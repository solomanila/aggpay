# OrdersCollectionView 后端接口需求

## 页面定位
- `OrdersCollectionView.vue` 属于“订单/收款订单”，展示收单 KPI、订单列表、筛选、处理时间线与提醒。
- 数据需覆盖实时指标、分页查询及事件流，接口统一放在 `pay-service` 的 `/api/admin/v1/orders/collection/**`。

## 现有数据来源（pay.sql & pay-service）
- `order_info`：核心收单记录，字段 `order_id`,`platform_id`,`pay_config_channel_id`,`status`,`req_amount`,`real_amount`,`create_time`,`pay_time`。
- `order_req_record`：记录请求耗时、错误，可用于 SLA、成功率统计。
- `order_notify_record`：保存通知日志，可作为时间线/提醒的基础。
- `OrderInfoService`、`OrderReqRecordService` 等基础服务已存在，可复用。

## 新增/扩展数据结构
- 为记录处理时间线建议增加事件表：

```sql
CREATE TABLE IF NOT EXISTS `order_collection_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `event_time` datetime NOT NULL,
  `event_type` varchar(32) NOT NULL COMMENT 'AUTO_RETRY|MANUAL_FIX|NOTICE',
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  KEY `idx_order_time` (`order_id`,`event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收款订单时间线';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/orders/collection/summary` | 指标卡 | 今日订单、成功率、风险订单、金额 |
| 2 | GET | `/api/admin/v1/orders/collection` | 订单列表 | 支持状态/商户/通道/风险筛选、分页 |
| 3 | GET | `/api/admin/v1/orders/collection/{orderId}` | 订单详情 | 订单数据、时间线、通知、相关日志 |
| 4 | POST | `/api/admin/v1/orders/collection/{orderId}/actions` | 手动操作 | 触发补单、重试、关闭 |
| 5 | GET | `/api/admin/v1/orders/collection/timeline` | 全局时间线 | 展示最近处理事件和提醒 |

## 接口详情

### 1. `GET /api/admin/v1/orders/collection/summary`
- **查询参数**：`platformId`（可选，默认当前登录平台）、`timezone`、`range=TODAY|HOUR`.
- **返回**：`{ stats:[...], timeline:[...], notices:[...] }`。
  - `stats` 来源：`order_info`（成功/总数、实时交易额）、`order_req_record`（成功率/风险订单数 `error=1`）、`Redis` 缓存指标。
  - `timeline`：`order_collection_timeline` 或 `order_notify_record` 近 5 条事件。
  - `notices`：`order_notify_record` 中 type=`COLLECTION_NOTICE`。

### 2. `GET /api/admin/v1/orders/collection`
- **查询参数**：`status`,`merchant`,`channel`,`riskLevel`,`dateFrom`,`dateTo`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：从 `order_info` 查询，支持多条件过滤；`keyword` 匹配 `order_id`、`front_id`、`other_order_id`。
- **响应**：`{ "total": 12430, "list": [ { orderId,merchantName,channelName,amount,status,risk,updatedAt } ] }`。
- **性能**：按 `create_time`、`status` 建索引；可先查 Elasticsearch/ClickHouse 再 fallback DB。

### 3. `GET /api/admin/v1/orders/collection/{orderId}`
- **返回**：
  - `order`: `order_info` 明细（含 `req_amount`,`real_amount`,`status`,`pay_time`,`notice_status`）。
  - `reqRecords`: 最近的 `order_req_record`（耗时、错误码）。
  - `timeline`: `order_collection_timeline` 与 `order_notify_record` 事件合集。
  - `audit`: 推送/回调日志（来自 `order_notify_record`、`order_callback`）。
- **权限**：`ORDER_VIEW`; 敏感字段（如用户信息）需脱敏。

### 4. `POST /api/admin/v1/orders/collection/{orderId}/actions`
- **Body**：`{ "action":"RETRY|CLOSE|MARK_RISK", "reason":"上游延迟", "owner":"ops" }`.
- **逻辑**：根据 action 调用已有 `OrderInfoService` 或独立任务：
  - `RETRY`: 推送到补单队列（写 `order_collection_timeline`、`order_notify_record`）。
  - `CLOSE`: 更新 `order_info.status` 并通知商户。
  - `MARK_RISK`: 写风险标签（可扩展字段 `order_info.extend1`）。
- **响应**：操作结果（含新的时间线项）。

### 5. `GET /api/admin/v1/orders/collection/timeline`
- **查询参数**：`platformId`,`limit=20`。
- **返回**：聚合 `order_collection_timeline` 与 `order_notify_record`，字段 `{id,time,title,owner,eventType}`。
- **用途**：填充页面“处理进度”与“提醒”列表。
