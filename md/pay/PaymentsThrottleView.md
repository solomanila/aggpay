# PaymentsThrottleView 后端接口需求

## 页面定位
- `PaymentsThrottleView.vue` 位于“支付/通道限流”，展示限流策略数量、触发次数、解除记录、受影响流量、限流曲线、扩容计划及公告。
- 页面内容：指标卡、筛选器（自动/手动/进行中/已解除）、通道限流表、曲线信息、计划进度条、通知。

## 现有数据来源（pay-service）
- `ExtPayConfigLimitMapper` 当前已使用 `PayConfigLimit`、`PayConfigLimitRecord` 表（未在 pay.sql 中，需要纳入 schema）来存储限流阈值、限额使用及日志。
- `order_req_record`：能够计算实时 TPS、限额使用及触发原因。
- `pay_config_channel`：提供通道与区域信息。

## 新增/补充数据结构
1. 将 `PayConfigLimit` / `PayConfigLimitRecord` 补入 `pay.sql`（若不存在）：

```sql
CREATE TABLE IF NOT EXISTS `pay_config_limit` (
  `pay_config_id` int PRIMARY KEY,
  `limit_amount` decimal(18,2) NOT NULL,
  `total_amount` decimal(18,2) DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0=禁用,1=限流, -1=预警',
  `begin_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `end_time` datetime DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道限额配置';

CREATE TABLE IF NOT EXISTS `pay_config_limit_record` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `pay_config_id` int NOT NULL,
  `limit_amount` decimal(18,2) NOT NULL,
  `total_amount` decimal(18,2) DEFAULT 0,
  `status` tinyint NOT NULL,
  `begin_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `end_time` datetime DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  KEY `idx_pay_config_id` (`pay_config_id`,`begin_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限流操作日志';
```

2. 为页面新增限流事件与计划表：

```sql
CREATE TABLE IF NOT EXISTS `pay_channel_limit_event` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `pay_config_channel_id` bigint NOT NULL,
  `trigger_reason` varchar(64) NOT NULL,
  `limit_desc` varchar(64) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ONGOING',
  `ownership` varchar(32) DEFAULT NULL COMMENT 'riskbot/ops',
  `triggered_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `released_at` datetime DEFAULT NULL,
  KEY `idx_channel_status` (`pay_config_channel_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限流事件';

CREATE TABLE IF NOT EXISTS `pay_channel_limit_plan` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `progress` tinyint DEFAULT 0,
  `eta` datetime DEFAULT NULL,
  `status` varchar(16) DEFAULT 'OPEN',
  KEY `idx_event` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扩容/解限计划';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/payments/throttle/summary` | 指标卡 | 聚合限流策略、触发次数、解除情况 |
| 2 | GET | `/api/admin/v1/payments/throttle/events` | 限流事件列表 | 支持状态、触发原因筛选 |
| 3 | GET | `/api/admin/v1/payments/throttle/events/{eventId}` | 事件详情 | 包含限流曲线、计划、日志 |
| 4 | POST | `/api/admin/v1/payments/throttle/events` | 手工触发/解除限流 | 写入事件 + 更新 `PayConfigLimit` |
| 5 | POST | `/api/admin/v1/payments/throttle/events/{eventId}/plans` | 更新扩容计划 | 对应页面“计划”列表 |

## 接口详情

### 1. `GET /api/admin/v1/payments/throttle/summary`
- **查询参数**：`platformId`,`range=TODAY|HOUR`.
- **返回**：
  - `stats`: 
    - `限流策略`=`count(pay_config_limit where status in (0,1))`.
    - `今日触发`=`count(pay_channel_limit_event where DATE(triggered_at)=today)`.
    - `已解除`=`released_at IS NOT NULL`.
    - `受限流量`：`SUM(order_info.real_amount)` for channels currently limited。
  - `filters`: `[全部,自动,手动,...]` derived from `ownership`.
- **附加**：`curves` 字段拉取 `order_req_record` 中最近 30 分钟 TPS vs `pay_config_limit.limit_amount`。

### 2. `GET /api/admin/v1/payments/throttle/events`
- **查询参数**：`status`,`ownership`,`channelId`,`pageNo/pageSize`.
- **返回**：`PageResult` of `{eventId,channelName,reason,limitDesc,status,owner,updatedAt}`。
- **数据**：`pay_channel_limit_event` join `pay_config_channel` + `pay_config_limit`.
- **排序**：按 `status` (进行中优先) + `triggered_at desc`.

### 3. `GET /api/admin/v1/payments/throttle/events/{eventId}`
- **返回**：
  - `event`: `pay_channel_limit_event`.
  - `metrics`: from `order_req_record` (TPS, error rate) + `pay_config_limit`.
  - `plans`: `pay_channel_limit_plan`.
  - `records`: `pay_config_limit_record`（历史操作）。
- **权限**：`PAYMENT_THROTTLE_VIEW`。

### 4. `POST /api/admin/v1/payments/throttle/events`
- **Body**：`{ "payConfigChannelId":1, "triggerReason":"上游超时", "limitDesc":"850 TPS", "ownership":"riskbot", "action":"OPEN|CLOSE" }`.
- **逻辑**：
  - `action=OPEN`：新增事件，设置 `PayConfigLimit.status=1` 并写记录。
  - `action=CLOSE`：更新事件 `released_at`、`status='CLOSED'`，`PayConfigLimit.status=0`。
- **响应**：事件详情。
- **审计**：写 `order_notify_record` (type=`THROTTLE_EVENT`).

### 5. `POST /api/admin/v1/payments/throttle/events/{eventId}/plans`
- **Body**：`{ "title":"UPI 扩容", "owner":"SRE", "progress":62, "eta":"2024-05-21T12:00:00Z", "status":"OPEN|DONE" }`.
- **逻辑**：写 `pay_channel_limit_plan`; 当所有计划完成时自动关闭事件/解除限流。
- **时间序列**：同时把更新写到 `pay_channel_limit_record.remark`.
