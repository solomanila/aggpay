# OrdersApprovalView 后端接口需求

## 页面定位
- `OrdersApprovalView.vue` 对应“订单/审批流程”，展示跨订单类型（收款、出款、批量、回滚等）的审批任务、节点状态、时间线。
- 需要一个统一的审批引擎接口，路径建议 `/api/admin/v1/orders/approvals/**`。

## 现有数据来源
- 订单审批分散在各模块（出款 `payout_approval_task`、回滚 `payout_rollback_approval`、批量 `batch_payout_approval` 等）。
- 为统一展示，需要构建聚合视图或日志表。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `order_approval_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `biz_type` varchar(32) NOT NULL COMMENT 'PAYOUT|ROLLBACK|BATCH|MANUAL',
  `biz_id` bigint NOT NULL COMMENT '关联任务或订单 ID',
  `merchant_id` int DEFAULT NULL,
  `amount` decimal(18,2) DEFAULT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `current_stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_status_stage` (`status`,`current_stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一审批任务快照';

CREATE TABLE IF NOT EXISTS `order_approval_history` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `approval_task_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) NOT NULL,
  `operator_user_id` bigint DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `event_time` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`approval_task_id`) REFERENCES `order_approval_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批节点历史';
```

> `order_approval_task` 可通过定时同步各子表（`payout_approval_task` 等）或改为监听事件实时写入。

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/orders/approvals/summary` | 指标卡 | 展示待审批、通过率、逾期数、平均耗时 |
| 2 | GET | `/api/admin/v1/orders/approvals` | 审批列表 | 按状态/类型/节点/负责人筛选 |
| 3 | GET | `/api/admin/v1/orders/approvals/{approvalId}` | 审批详情 | 返回任务、节点历史、关联业务 |
| 4 | POST | `/api/admin/v1/orders/approvals/{approvalId}` | 审批动作 | 审批、转派、加签 |
| 5 | GET | `/api/admin/v1/orders/approvals/timeline` | 时间线 | 展示关键审批节点变更 |

## 接口详情

### 1. `GET /api/admin/v1/orders/approvals/summary`
- **查询参数**：`bizType`（可选）、`range=TODAY|WEEK`。
- **返回**：`{ stats:[...], timeline:[...] }`。
  - `stats`: `order_approval_task` 聚合待审批数、过去一日平均审批时长（来自 `order_approval_history`）、逾期任务（`updated_at` 超过 SLA）。
  - `timeline`: 最近 5 条 `order_approval_history` 事件。

### 2. `GET /api/admin/v1/orders/approvals`
- **查询参数**：`status`,`bizType`,`stage`,`owner`,`dateFrom`,`dateTo`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：查询 `order_approval_task` 并 join `merchant_info`/`pay_config_channel` 获取展示字段。
- **响应**：`{ "total": 320, "list": [ { id,bizType,merchant,amount,stage,status,owner,updatedAt } ] }`。
- **扩展**：`?includeChildren=true` 时附带对应子任务 ID（如 `payout_approval_task.id`）。

### 3. `GET /api/admin/v1/orders/approvals/{approvalId}`
- **返回**：`{ task, history, bizDetail }`。
  - `task`: `order_approval_task`。
  - `history`: `order_approval_history`（含每个节点操作人、备注）。
  - `bizDetail`: 对应子任务/订单详细信息（按 `bizType` 调用相应服务）。
- **权限**：需要 `ORDER_APPROVAL_VIEW`；不同 `bizType` 可能附加权限。

### 4. `POST /api/admin/v1/orders/approvals/{approvalId}`
- **Body**：`{ "decision":"APPROVED|REJECTED|TRANSFER|ADD_SIGN", "comment":"...", "assignTo":1002, "extra":{...} }`.
- **逻辑**：
  - 更新 `order_approval_task` & `order_approval_history`。
  - 调用对应子模块接口（如出款审批）执行真实操作。
  - `ADD_SIGN` 支持动态插入新的 stage。
- **响应**：最新 `task`/`history`。

### 5. `GET /api/admin/v1/orders/approvals/timeline`
- **查询参数**：`bizType`、`limit=20`。
- **返回**：`order_approval_history` 近期节点事件 `{id,time,stage,operator,status,comment}`，填充页面“时间线”模块。
