# OrdersQueryView 后端接口需求

## 页面定位
- `OrdersQueryView.vue` 用于“订单/查单记录”，展示商户查询请求、原因、状态、负责人及处理时间线。
- 功能点包括：查单 KPI、列表、状态筛选、处理时间线、提醒。接口前缀 `/api/admin/v1/orders/query/**`。

## 现有数据来源
- 当前数据库没有查单记录表，需要新增。
- 可复用 `order_info`（被查询订单）、`order_notify_record`（通知/回调）辅助。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `order_query_record` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `merchant_id` int DEFAULT NULL,
  `channel_id` bigint DEFAULT NULL,
  `reason` varchar(255) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|PROCESSING|DONE|FAILED',
  `owner_user_id` bigint DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`order_id`) REFERENCES `order_info`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查单记录';

CREATE TABLE IF NOT EXISTS `order_query_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `query_id` bigint NOT NULL,
  `event_time` datetime NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `event_type` varchar(32) DEFAULT 'INFO',
  `remark` varchar(255) DEFAULT NULL,
  FOREIGN KEY (`query_id`) REFERENCES `order_query_record`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查单时间线';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/orders/query/summary` | 指标卡 | 今日查单、完成率、处理中、待复核 |
| 2 | GET | `/api/admin/v1/orders/query` | 查单列表 | 按状态/原因/负责人/时间筛选 |
| 3 | POST | `/api/admin/v1/orders/query` | 新建查单 | 商户/客服发起查单请求 |
| 4 | GET | `/api/admin/v1/orders/query/{queryId}` | 详情 | 查单记录、时间线、关联订单 |
| 5 | POST | `/api/admin/v1/orders/query/{queryId}/status` | 更新状态 | 标记处理结果、记录时间线 |

## 接口详情

### 1. `GET /api/admin/v1/orders/query/summary`
- **查询参数**：`date`（默认当天）。
- **返回**：`{ stats:[...], timeline:[...], notices:[...] }`。
  - `stats`: `order_query_record` 聚合今日查单、完成率、处理中、待复核。
  - `timeline`: `order_query_timeline` 近 5 条事件。
  - `notices`: `order_notify_record` type=`ORDER_QUERY_NOTICE`。

### 2. `GET /api/admin/v1/orders/query`
- **查询参数**：`status`,`reason`,`owner`,`merchant`,`channel`,`dateFrom`,`dateTo`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：查询 `order_query_record`，`keyword` 可匹配 `order_id`、`merchant` 名称（需 join `merchant_info`）。
- **响应**：`{ "total": 520, "list": [ { id,merchant,channel,reason,status,owner,updatedAt } ] }`。

### 3. `POST /api/admin/v1/orders/query`
- **Body**：`{ "orderId":123, "merchantId":56, "channelId":22, "reason":"客户反馈未到账", "ownerUserId":1002 }`.
- **处理**：创建 `order_query_record`（状态 `PENDING`），写 `order_query_timeline` 首条事件并发送通知给负责团队。
- **响应**：新建 `queryId`。

### 4. `GET /api/admin/v1/orders/query/{queryId}`
- **返回**：`{ query, order, timeline }`。
  - `query`: `order_query_record`。
  - `order`: 对应 `order_info`。
  - `timeline`: `order_query_timeline`。
- **权限**：`ORDER_QUERY_VIEW`。

### 5. `POST /api/admin/v1/orders/query/{queryId}/status`
- **Body**：`{ "status":"PROCESSING|DONE|FAILED", "comment":"已与上游确认", "ownerUserId":1002 }`.
- **逻辑**：更新 `order_query_record.status/owner/comment`，追加 `order_query_timeline`；若 `status=DONE`，可自动通知商户（写 `order_notify_record`）。
