# PaymentsRollbackView 后端接口需求

## 页面定位
- `PaymentsRollbackView.vue` 位于“支付/通道回滚”，关注手动/自动回滚任务、金额、状态、审批节点、进度条以及公告。
- 页面元素：指标卡（回滚任务/成功率/进行中/待审批）、筛选器、任务表格（标题、通道、模式、金额、状态、Owner）、步骤 KPI、审批列表、提醒。

## 现有数据来源（pay.sql & pay-service）
- `order_info`：可根据 `status` 与 `pay_config_channel_id` 查出回滚涉及的订单金额、笔数。
- `order_req_record`：用于识别触发回滚的请求失败指标。
- 当前 Java 服务未包含回滚任务表，需要新增。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `pay_channel_rollback_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `pay_config_channel_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `mode` varchar(16) NOT NULL COMMENT 'AUTO|MANUAL',
  `amount` decimal(18,2) DEFAULT 0,
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(16) NOT NULL DEFAULT 'PENDING',
  `owner` varchar(64) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_channel_status` (`pay_config_channel_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道回滚任务';

CREATE TABLE IF NOT EXISTS `pay_channel_rollback_step` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `step_code` varchar(32) NOT NULL COMMENT 'APPROVAL|SYNC|ALERT',
  `label` varchar(64) NOT NULL,
  `value` varchar(64) DEFAULT NULL,
  `detail` varchar(255) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'OPEN',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_task_step` (`task_id`,`step_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回滚关键步骤进度';

CREATE TABLE IF NOT EXISTS `pay_channel_rollback_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `stage` varchar(64) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `comment` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_task_stage` (`task_id`,`stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回滚审批实例';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/payments/rollback/summary` | 指标卡 | 结合任务表与订单数据 |
| 2 | GET | `/api/admin/v1/payments/rollback/tasks` | 任务列表 | 筛选模式/状态/通道 |
| 3 | GET | `/api/admin/v1/payments/rollback/tasks/{taskId}` | 任务详情 | 步骤 KPI、审批、相关订单 |
| 4 | POST | `/api/admin/v1/payments/rollback/tasks` | 新建回滚任务 | 支持自动/手动模式 |
| 5 | POST | `/api/admin/v1/payments/rollback/tasks/{taskId}/approve` | 审批动作 | 更新审批节点状态 |

## 接口详情

### 1. `GET /api/admin/v1/payments/rollback/summary`
- **查询参数**：`platformId`,`range=TODAY|WEEK`.
- **返回**：
  - `stats`: 
    - `roll-today`: `count(*)` where `DATE(create_time)=today`.
    - `roll-success`: `成功率 = resolved / total` from `status='COMPLETED'`.
    - `roll-active`: `status IN ('PROCESSING','EXECUTING')`.
    - `roll-pending`: `status='PENDING_APPROVAL'` 并附金额 = `SUM(amount)`.
  - `steps`: `pay_channel_rollback_step` 最后一次快照。
- **备注**：金额对齐 `order_info.settle_amount` 进行二次核对。

### 2. `GET /api/admin/v1/payments/rollback/tasks`
- **查询参数**：`mode`,`status`,`channelId`,`owner`,`pageNo/pageSize`.
- **返回**：分页列表 `{taskId,title,channelName,mode,amount,status,owner,updatedAt}`，附 `diffMinutes`=在队列时间。
- **数据来源**：`pay_channel_rollback_task` join `pay_config_channel`；`amount` 默认 `order_info` 聚合结果。

### 3. `GET /api/admin/v1/payments/rollback/tasks/{taskId}`
- **返回**：
  - `task`: 基本信息。
  - `steps`: `pay_channel_rollback_step`.
  - `approvals`: `pay_channel_rollback_approval`.
  - `orders`: 调用 `order_info`/`order_req_record`（可分页）获取涉及订单列表（字段 `order_id, req_amount, status`）。
- **权限**：`PAYMENT_ROLLBACK_VIEW`; 订单列表需具备订单查看权限。

### 4. `POST /api/admin/v1/payments/rollback/tasks`
- **Body**：`{ "payConfigChannelId":1, "mode":"AUTO|MANUAL", "amount":820000, "currency":"CNY", "reason":"上游超时", "owner":"riskbot" }`.
- **处理**：
  - 插入 `pay_channel_rollback_task`，默认状态 `PROCESSING`（自动）或 `PENDING_APPROVAL`（手动）。
  - 初始化 `pay_channel_rollback_step`（审批通过率/同步/告警）并写 `order_notify_record`.
- **响应**：新任务 ID。

### 5. `POST /api/admin/v1/payments/rollback/tasks/{taskId}/approve`
- **Body**：`{ "stage":"风控评估", "decision":"APPROVED|REJECTED", "comment":"" }`.
- **逻辑**：更新 `pay_channel_rollback_approval`，若所有 stage 完成将任务状态切为 `PROCESSING` 或 `COMPLETED` 并触发行内流程。
- **审计**：写 `order_notify_record`；若拒绝同步 `order_build_error` 生成告警。
