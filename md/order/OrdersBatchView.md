# OrdersBatchView 后端接口需求

## 页面定位
- `OrdersBatchView.vue` 展示“订单/批量代付”任务的 KPI、任务表格、审批队列和提醒。
- 场景包含批量导入、拆单、执行进度、审批与文件下载。接口前缀 `/api/admin/v1/orders/batch/**`。

## 现有数据来源（pay.sql & pay-service）
- 当前 schema 未提供批量任务表，需要新增。可复用 `order_info` 存储拆分后的单笔订单。
- `build_order_error`、`order_notify_record` 可用作任务日志补充。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `batch_payout_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `batch_no` varchar(64) NOT NULL UNIQUE,
  `merchant_id` int NOT NULL,
  `channel_id` bigint NOT NULL,
  `total_amount` decimal(18,2) NOT NULL,
  `total_count` int NOT NULL,
  `status` varchar(16) DEFAULT 'UPLOADED' COMMENT 'UPLOADED|PARSING|APPROVING|PROCESSING|DONE|FAILED',
  `file_url` varchar(512) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量代付任务';

CREATE TABLE IF NOT EXISTS `batch_payout_detail` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `account_name` varchar(128) DEFAULT NULL,
  `account_no` varchar(64) DEFAULT NULL,
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(16) DEFAULT 'PENDING',
  `error_msg` varchar(255) DEFAULT NULL,
  FOREIGN KEY (`task_id`) REFERENCES `batch_payout_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量代付明细';

CREATE TABLE IF NOT EXISTS `batch_payout_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`task_id`) REFERENCES `batch_payout_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量代付审批';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/orders/batch/summary` | 指标卡 | 今日批量任务、完成率、处理中、待审批 |
| 2 | GET | `/api/admin/v1/orders/batch/tasks` | 任务列表 | 按状态/商户/通道分页 |
| 3 | POST | `/api/admin/v1/orders/batch/tasks` | 新建任务 | 上传文件、解析、写入明细 |
| 4 | GET | `/api/admin/v1/orders/batch/tasks/{taskId}` | 任务详情 | 包含明细、审批、日志 |
| 5 | POST | `/api/admin/v1/orders/batch/tasks/{taskId}/approvals` | 审批动作 | 审批、驳回、提醒 |

## 接口详情

### 1. `GET /api/admin/v1/orders/batch/summary`
- **查询参数**：`date`（默认当天）。
- **返回**：`{ stats:[...], approvals:[...], notices:[...] }`。
  - `stats`: `batch_payout_task` 聚合任务数、完成率（status='DONE'/总数）、处理中、待审批。
  - `approvals`: 最近 `batch_payout_approval` 状态 `PENDING` 的记录。
  - `notices`: `order_notify_record` type=`BATCH_NOTICE`。

### 2. `GET /api/admin/v1/orders/batch/tasks`
- **查询参数**：`status`,`merchantId`,`channelId`,`owner`,`dateFrom`,`dateTo`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：查询 `batch_payout_task`，`keyword` 匹配 `batch_no`；附带 `file_url`、`owner`、`updatedAt`。
- **响应**：`{ "total": 86, "list": [ { id,batchNo,merchantName,channelName,amount,status,fileUrl,owner,updatedAt } ] }`。

### 3. `POST /api/admin/v1/orders/batch/tasks`
- **Body**：`{ "merchantId":1, "channelId":12, "fileUrl":"s3://xxx.csv", "ownerUserId":1001, "remark":"6月工资批量", "skipApproval":false }`.
- **逻辑**：创建 `batch_payout_task`（状态 `UPLOADED`），触发解析任务（可异步）写入 `batch_payout_detail`；若 `skipApproval=false` 则创建默认审批流；返回任务 ID。
- **安全**：校验文件格式、大小；上传日志写 `order_notify_record`。

### 4. `GET /api/admin/v1/orders/batch/tasks/{taskId}`
- **返回**：`{ task, details, approvals }`。
  - `task`: `batch_payout_task`。
  - `details`: `batch_payout_detail`（支持分页和下载）。
  - `approvals`: `batch_payout_approval` 列表。
- **附加**：`?download=true` 时提供明细下载链接（调用下载管理模块）。

### 5. `POST /api/admin/v1/orders/batch/tasks/{taskId}/approvals`
- **Body**：`{ "stage":"FINANCE", "decision":"APPROVED|REJECTED", "comment":"..." }`.
- **逻辑**：更新 `batch_payout_approval`；全部通过后把 `batch_payout_task.status` 置 `PROCESSING` 并触发创建实际订单（写入 `order_info`）；若拒绝则状态 `FAILED` 并发提醒。
