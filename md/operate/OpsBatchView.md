# OpsBatchView 后端接口需求

## 页面定位
- `OpsBatchView.vue` 是“运营工具/批量操作”，用于管理批量导入、导出及任务审批，呈现任务列表、审批队列与指标。
- 重点覆盖任务提交、执行、状态跟踪、审批与日志，接口前缀 `/api/admin/v1/ops/batch/**`。

## 现有数据来源
- 需要独立的批量任务表，可与 `OrdersBatchView` 的表结构复用或共享。
- `order_notify_record` 可提供通知日志。

## 新增/扩展数据结构
- 可复用 `batch_payout_task` 等结构，另增加通用批量任务表：

```sql
CREATE TABLE IF NOT EXISTS `ops_batch_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_name` varchar(128) NOT NULL,
  `task_type` varchar(32) NOT NULL COMMENT 'IMPORT|EXPORT|SYNC|OTHER',
  `payload_count` int DEFAULT 0,
  `status` varchar(16) DEFAULT 'QUEUED' COMMENT 'QUEUED|RUNNING|SUCCESS|FAILED',
  `owner_user_id` bigint DEFAULT NULL,
  `file_url` varchar(512) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营批量任务';

CREATE TABLE IF NOT EXISTS `ops_batch_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`task_id`) REFERENCES `ops_batch_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营批量审批';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/ops/batch/summary` | 指标卡 | 今日任务数、成功率、进行中、待审批 |
| 2 | GET | `/api/admin/v1/ops/batch/tasks` | 任务列表 | 筛选类型/状态/Owner |
| 3 | POST | `/api/admin/v1/ops/batch/tasks` | 新建批量任务 | 上传文件/参数并入队 |
| 4 | GET | `/api/admin/v1/ops/batch/tasks/{taskId}` | 任务详情 | 包含执行日志、审批 |
| 5 | POST | `/api/admin/v1/ops/batch/tasks/{taskId}/approvals` | 审批操作 | 审批、驳回、指派 |

## 接口详情

### 1. `GET /api/admin/v1/ops/batch/summary`
- **查询参数**：`date`（默认当天）。
- **返回**：`{ stats:[...], approvals:[...], notices:[...] }`。
  - `stats`: `ops_batch_task` 聚合（任务数、成功率、进行中、失败）。
  - `approvals`: `ops_batch_approval` 状态 `PENDING`。
  - `notices`: `order_notify_record` type=`OPS_BATCH_NOTICE`。

### 2. `GET /api/admin/v1/ops/batch/tasks`
- **查询参数**：`status`,`taskType`,`owner`,`dateFrom`,`dateTo`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：查询 `ops_batch_task`；`keyword` 匹配 `task_name` 或 `remark`；返回 `{id,name,type,payloadCount,status,owner,updatedAt}`。

### 3. `POST /api/admin/v1/ops/batch/tasks`
- **Body**：`{ "taskName":"导入黑名单", "taskType":"IMPORT", "fileUrl":"s3://...", "payloadCount":3200, "ownerUserId":1002, "remark":"Q2黑名单批量" }`.
- **处理**：创建 `ops_batch_task` 状态 `QUEUED`；如需审批，插入 `ops_batch_approval`；触发异步消费任务。

### 4. `GET /api/admin/v1/ops/batch/tasks/{taskId}`
- **返回**：`{ task, approvals, logs }`。
  - `task`: `ops_batch_task`。
  - `approvals`: `ops_batch_approval`。
  - `logs`: 来自任务执行日志（可落 `order_notify_record` 或单独日志表）。

### 5. `POST /api/admin/v1/ops/batch/tasks/{taskId}/approvals`
- **Body**：`{ "stage":"OPS", "decision":"APPROVED|REJECTED", "comment":"..." }`.
- **逻辑**：更新 `ops_batch_approval`；若全部通过则将任务状态置 `RUNNING` 并启动执行；拒绝则 `status='FAILED'`。
