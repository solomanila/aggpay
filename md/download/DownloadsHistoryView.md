# DownloadsHistoryView 后端接口需求

## 页面定位
- `DownloadsHistoryView.vue` 是“下载管理/历史记录”页面，展示导出任务的状态、类型、有效期、负责人和更新时间。
- 需要 REST 接口来查询导出任务、筛选状态、查看详情与下载链接。建议路径 `/api/admin/v1/downloads/history/**`。

## 现有数据来源
- pay.sql 未包含导出任务表，需要新增。
- `order_notify_record` 可记录生成通知，`DownloadsHistoryView` 仅依赖导出任务数据，不涉及订单表。

## 新增数据结构
```sql
CREATE TABLE IF NOT EXISTS `download_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_name` varchar(128) NOT NULL,
  `task_type` varchar(32) NOT NULL COMMENT 'ORDER|BILLING|LEDGER|CUSTOM',
  `status` varchar(16) DEFAULT 'QUEUED' COMMENT 'QUEUED|RUNNING|SUCCESS|FAILED|EXPIRED',
  `file_url` varchar(512) DEFAULT NULL,
  `expires_at` datetime DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `params` json DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导出任务';

CREATE TABLE IF NOT EXISTS `download_task_log` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `event_time` datetime NOT NULL,
  `status` varchar(16) NOT NULL,
  `message` varchar(255) DEFAULT NULL,
  FOREIGN KEY (`task_id`) REFERENCES `download_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导出任务日志';
```

## 接口清单
| # | Method | Path | 功能 |
|---|---|---|---|
| 1 | GET | `/summary` | KPI：任务数、成功率、失败、过期 |
| 2 | GET | `/tasks` | 导出任务列表，支持状态/类型筛选 |
| 3 | GET | `/tasks/{taskId}` | 任务详情，包含下载链接与日志 |
| 4 | POST | `/tasks` | 创建导出任务（触发后台生成文件） |
| 5 | POST | `/tasks/{taskId}/retry` | 失败任务重试 |

## 接口详情

### 1. `GET /api/admin/v1/downloads/history/summary`
- **参数**：`dateRange`（可选）。
- **返回**：`{ stats:[ {id:'download-total',value,meta}, ... ] }`，数据来自 `download_task` 聚合。

### 2. `GET /api/admin/v1/downloads/history/tasks`
- **参数**：`status`,`taskType`,`dateFrom`,`dateTo`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：按条件查询 `download_task`；`keyword` 模糊匹配 `task_name`。
- **响应**：`{ "total": 132, "list": [ { id,taskName,taskType,status,expiresAt,owner,updatedAt } ] }`。

### 3. `GET /api/admin/v1/downloads/history/tasks/{taskId}`
- **返回**：`{ task, logs }`。
  - `task`: `download_task` 基本信息 + `fileUrl`（若 `status=SUCCESS` 且未过期）。
  - `logs`: `download_task_log` 记录。
- **安全**：下载链接仅当前用户或具备权限者可见；链接可采用一次性签名。

### 4. `POST /api/admin/v1/downloads/history/tasks`
- **Body**：`{ "taskName":"导出账单", "taskType":"BILLING", "params":{...} }`。
- **逻辑**：插入 `download_task` 状态 `QUEUED`，写 MQ/调度任务生成文件；返回任务 ID；文件生成后更新 `status`、`fileUrl`、`expiresAt`。

### 5. `POST /api/admin/v1/downloads/history/tasks/{taskId}/retry`
- **Body**：`{ "reason":"重试原因" }`。
- **逻辑**：只有 `FAILED`/`EXPIRED` 状态可重试；后台重新执行生成流程，更新状态并重置 `expiresAt`。
