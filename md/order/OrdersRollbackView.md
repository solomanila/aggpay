# OrdersRollbackView 后端接口需求

## 页面定位
- `OrdersRollbackView.vue` 展示“订单/代付回滚”任务的指标、任务列表、审批队列与时间线。
- 主要操作包括创建回滚任务、审批、跟踪执行过程和记录关键节点，接口前缀 `/api/admin/v1/orders/rollback/**`。

## 现有数据来源（pay.sql & pay-service）
- `order_info`：可提供代付订单原始数据。
- `order_req_record`：用于分析触发回滚的 SLA 指标。
- 需要新增回滚任务表、步骤表、审批表，与 `PaymentsRollbackView.md` 规划一致但聚焦代付场景。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `payout_rollback_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `channel_id` bigint NOT NULL,
  `mode` varchar(16) NOT NULL COMMENT 'AUTO|MANUAL',
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVING|PROCESSING|DONE|FAILED',
  `owner_user_id` bigint DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`order_id`) REFERENCES `order_info`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代付回滚任务';

CREATE TABLE IF NOT EXISTS `payout_rollback_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`task_id`) REFERENCES `payout_rollback_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代付回滚审批';

CREATE TABLE IF NOT EXISTS `payout_rollback_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `event_time` datetime NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `event_type` varchar(32) DEFAULT 'INFO',
  FOREIGN KEY (`task_id`) REFERENCES `payout_rollback_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代付回滚时间线';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/orders/rollback/summary` | 指标卡 | 今日任务、成功率、进行中、待审批 |
| 2 | GET | `/api/admin/v1/orders/rollback/tasks` | 任务列表 | 支持状态/模式/通道/商户筛选 |
| 3 | GET | `/api/admin/v1/orders/rollback/tasks/{taskId}` | 任务详情 | 包含订单信息、审批、时间线 |
| 4 | POST | `/api/admin/v1/orders/rollback/tasks` | 新建任务 | 支持自动/手动触发 |
| 5 | POST | `/api/admin/v1/orders/rollback/tasks/{taskId}/approvals` | 审批操作 | 审批、驳回、指派 |

## 接口详情

### 1. `GET /api/admin/v1/orders/rollback/summary`
- **查询参数**：`platformId`,`range=TODAY|WEEK`.
- **返回**：`{ stats:[...], approvals:[...], timeline:[...] }`。
  - `stats`: `payout_rollback_task` 聚合今日任务数、成功率（status='DONE'）、进行中、待审批。
  - `approvals`: 最近 `payout_rollback_approval` 状态 `PENDING` 的记录。
  - `timeline`: 从 `payout_rollback_timeline` 读取近 5 条事件。

### 2. `GET /api/admin/v1/orders/rollback/tasks`
- **查询参数**：`status`,`mode`,`channelId`,`merchant`,`dateFrom`,`dateTo`,`keyword`,`pageNo`,`pageSize`。
- **返回**：`{ "total": 120, "list": [ { taskId,merchant,channel,amount,mode,status,owner,updatedAt } ] }`。
- **数据**：联表 `payout_rollback_task`、`order_info`、`pay_config_channel`。

### 3. `GET /api/admin/v1/orders/rollback/tasks/{taskId}`
- **返回**：`{ task, order, approvals, timeline }`。
  - `task`: `payout_rollback_task`。
  - `order`: 关联 `order_info`，用于展示商户、通道、原始金额。
  - `approvals`: `payout_rollback_approval`。
  - `timeline`: `payout_rollback_timeline`。
- **权限**：`ORDER_ROLLBACK_VIEW`。

### 4. `POST /api/admin/v1/orders/rollback/tasks`
- **Body**：`{ "orderId":123, "channelId":22, "mode":"AUTO", "amount":820000, "reason":"上游失败", "ownerUserId":1001 }`.
- **逻辑**：校验订单存在且可回滚；写 `payout_rollback_task`，自动创建审批记录（若 `mode=MANUAL`）；若为自动任务则直接进入 `PROCESSING` 并推送至作业队列；写 `payout_rollback_timeline` 首条事件。
- **响应**：新任务 ID 及初始状态。

### 5. `POST /api/admin/v1/orders/rollback/tasks/{taskId}/approvals`
- **Body**：`{ "stage":"FINANCE", "decision":"APPROVED|REJECTED|TRANSFER", "comment":"..." }`.
- **逻辑**：更新 `payout_rollback_approval`；若所有阶段通过则将任务状态置 `PROCESSING` 并启动回滚脚本；拒绝则状态 `FAILED` 并写通知。
- **时间线**：每次审批写入 `payout_rollback_timeline`，用于前端“时间线”展示。
