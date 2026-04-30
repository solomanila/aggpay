# OrdersPayoutView 后端接口需求

## 页面定位
- `OrdersPayoutView.vue` 负责展示“订单/出款订单”的整体指标、订单列表、审批队列及提醒。
- 数据需覆盖今日出款金额/成功率/风险订单/待审批等，接口前缀 `/api/admin/v1/orders/payout/**`。

## 现有数据来源（pay.sql & pay-service）
- `order_info` 中的出款订单可通过 `status` + `pay_config_channel_id` 区分。
- `order_req_record`：可用于统计请求成功率、超时信息。
- `order_notify_record`：记录审批/通知，可用于提醒模块。
- 需要新增审批任务表以显示“审批队列”。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `payout_approval_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL COMMENT 'FINANCE|RISK|OPS',
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVED|REJECTED',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_stage_status` (`stage`,`status`),
  FOREIGN KEY (`order_id`) REFERENCES `order_info`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出款审批任务';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/orders/payout/summary` | 指标卡 | 今日出款、成功率、风险订单、待审批 |
| 2 | GET | `/api/admin/v1/orders/payout` | 订单列表 | 按状态/商户/通道/审批状态分页 |
| 3 | GET | `/api/admin/v1/orders/payout/{orderId}` | 详情 | 订单、审批、通知、回调 |
| 4 | POST | `/api/admin/v1/orders/payout/{orderId}/approvals` | 审批操作 | 审批通过/驳回/指派 |
| 5 | GET | `/api/admin/v1/orders/payout/notices` | 提醒 | 最新通知、待办 |

## 接口详情

### 1. `GET /api/admin/v1/orders/payout/summary`
- **查询参数**：`platformId`,`timezone`,`range=TODAY|HOUR`。
- **返回**：`{ stats:[...], approvals:[...], notices:[...] }`。
  - `stats`: `order_info` 聚合（过滤出款订单）；成功率来自 `order_req_record`；风险订单来源 `order_build_error` + 风控标签。
  - `approvals`: 最近 5 条 `payout_approval_task` 状态 `PENDING`；用于页面“审批队列”。
  - `notices`: `order_notify_record` type=`PAYOUT_NOTICE`。

### 2. `GET /api/admin/v1/orders/payout`
- **查询参数**：`status`,`merchant`,`channel`,`owner`,`dateFrom`,`dateTo`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：筛选 `order_info`（出款类型）并联表 `payout_approval_task` 获取当前审批节点。
- **响应**：`{ "total": 2840, "list": [ { orderId,merchant,channel,amount,status,owner,approvalStage,updatedAt } ] }`。
- **扩展**：`?withRisk=true` 附带风险评分（可存于 `order_info.extend1`）。

### 3. `GET /api/admin/v1/orders/payout/{orderId}`
- **返回**：
  - `order`: `order_info` 详情。
  - `approvalFlow`: 按阶段排序的 `payout_approval_task`。
  - `reqRecords`: `order_req_record` 最近记录。
  - `callbacks`: `order_callback` / `order_notify_record`。
- **权限**：`ORDER_PAYOUT_VIEW`；输出 `owner` 的真实姓名需脱敏。

### 4. `POST /api/admin/v1/orders/payout/{orderId}/approvals`
- **Body**：`{ "taskId":123, "decision":"APPROVED|REJECTED|TRANSFER", "comment":"...", "assignTo":1002 }`.
- **逻辑**：更新 `payout_approval_task`，若全部通过则将 `order_info.notice_status` 标记为成功并触发实际代付；若拒绝则写 `order_notify_record` 并回调商户。
- **响应**：最新审批节点与订单状态。

### 5. `GET /api/admin/v1/orders/payout/notices`
- **查询参数**：`platformId`,`limit=10`。
- **返回**：`order_notify_record` 中与出款相关的提醒（审批超时、失败回执等）。用于填充页面“最新动态”。
