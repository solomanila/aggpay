# OrdersPayoutQueueView 后端接口需求

## 页面定位
- `OrdersPayoutQueueView.vue` 展示“订单/代付队列”的实时状态：各通道队列深度、出队速率、状态、重试率与提醒。
- 页面主要关注队列监控、限流与重试策略，接口前缀 `/api/admin/v1/orders/payout-queue/**`。

## 现有数据来源（pay-service）
- 现有 pay-service 没有队列表，只能从 MQ/Redis 统计获取，需新增快照表或对接监控缓存。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `payout_queue_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel_id` bigint NOT NULL,
  `queue_depth` int NOT NULL,
  `dequeue_rate` decimal(10,2) DEFAULT 0 COMMENT '笔/分钟',
  `status` varchar(16) DEFAULT 'NORMAL' COMMENT 'NORMAL|SLOW|BLOCK',
  `retry_rate` decimal(5,2) DEFAULT 0,
  `snapshot_time` datetime NOT NULL,
  `remark` varchar(255) DEFAULT NULL,
  KEY `idx_channel_time` (`channel_id`,`snapshot_time`),
  FOREIGN KEY (`channel_id`) REFERENCES `pay_config_channel`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代付队列快照';

CREATE TABLE IF NOT EXISTS `payout_queue_notice` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel_id` bigint DEFAULT NULL,
  `title` varchar(128) NOT NULL,
  `content` varchar(255) DEFAULT NULL,
  `level` varchar(16) DEFAULT 'INFO',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='队列提醒';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/orders/payout-queue/summary` | 指标卡 | 今日队列深度、出队速率、异常通道 |
| 2 | GET | `/api/admin/v1/orders/payout-queue` | 队列列表 | 按通道、状态筛选，返回最新快照 |
| 3 | GET | `/api/admin/v1/orders/payout-queue/{channelId}` | 通道详情 | 历史曲线、重试统计 |
| 4 | POST | `/api/admin/v1/orders/payout-queue/{channelId}/actions` | 控制操作 | 手动暂停/恢复/限流提示 |
| 5 | GET | `/api/admin/v1/orders/payout-queue/notices` | 提醒 | 最近提醒、告警 |

## 接口详情

### 1. `GET /api/admin/v1/orders/payout-queue/summary`
- **查询参数**：`platformId`（可选）。
- **返回**：`{ stats:[...], notices:[...] }`。
  - `stats`: 汇总最新 `payout_queue_snapshot` 数据（深度、速率、异常数）。
  - `notices`: 读取 `payout_queue_notice` 最近记录，按级别排序。

### 2. `GET /api/admin/v1/orders/payout-queue`
- **查询参数**：`status`,`channelId`,`keyword`.
- **逻辑**：对每个通道取最近一次快照（`ORDER BY snapshot_time DESC LIMIT 1`），返回 `{ channelName, depth, rate, status, retryRate, updatedAt }`。
- **性能**：可通过 Redis/TSDB 提供缓存，接口仅作兜底。

### 3. `GET /api/admin/v1/orders/payout-queue/{channelId}`
- **返回**：`{ latest, history, retryStats }`。
  - `latest`: 最近快照。
  - `history`: 过去 1 小时 `payout_queue_snapshot` 折线数据。
  - `retryStats`: 从 `order_req_record`/`order_build_error` 统计该通道的重试率。
- **权限**：`ORDER_QUEUE_VIEW`。

### 4. `POST /api/admin/v1/orders/payout-queue/{channelId}/actions`
- **Body**：`{ "action":"PAUSE|RESUME|THROTTLE", "remark":"原因" }`.
- **逻辑**：调用后台任务（如 MQ 管理、限流系统）执行操作，并写 `payout_queue_notice` 记录操作结果；当 action=THROTTLE 时可同步 `pay_config_limit`（参考限流模块）。
- **响应**：操作状态 + 最新快照。

### 5. `GET /api/admin/v1/orders/payout-queue/notices`
- **返回**：`payout_queue_notice` 最近 10 条 `{id,title,content,level,createdAt}`，用于页面“提醒”。
