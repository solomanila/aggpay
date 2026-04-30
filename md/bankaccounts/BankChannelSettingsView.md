# BankChannelSettingsView 后端接口需求

## 页面定位
- `BankChannelSettingsView.vue` 位于“银行户管理/通道设置”，负责管理银行账户与支付通道之间的绑定关系、权重、备用通道 (fallback) 及调优计划。
- 页面展示指标卡、绑定列表、筛选器、计划进度条、公告。

## 现有数据来源（pay.sql & pay-service）
- 现有 `pay_config_channel`、`pay_config_info` 记录通道配置；`bank_account`（见 `BankAccountsView.md`）提供账户信息。
- `order_info`, `order_req_record` 可提供通道流量和限额使用率，用于计算权重/告警。
- 尚无账户与通道之间的绑定表，需要新增。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `bank_account_channel_binding` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `pay_config_channel_id` bigint NOT NULL,
  `weight_percent` decimal(5,2) DEFAULT 0,
  `status` varchar(16) DEFAULT 'ENABLED',
  `fallback_channel_id` bigint DEFAULT NULL,
  `priority` tinyint DEFAULT 1,
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_account_channel` (`account_id`,`pay_config_channel_id`),
  FOREIGN KEY (`account_id`) REFERENCES `bank_account`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`pay_config_channel_id`) REFERENCES `pay_config_channel`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行账户通道绑定关系';

CREATE TABLE IF NOT EXISTS `bank_account_channel_plan` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `binding_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `progress` tinyint DEFAULT 0,
  `eta` datetime DEFAULT NULL,
  `status` varchar(16) DEFAULT 'OPEN',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`binding_id`) REFERENCES `bank_account_channel_binding`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绑定调优计划';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/bank/channel-settings/summary` | 指标卡/计划 | 聚合绑定数量、启用数、平均权重、告警 |
| 2 | GET | `/api/admin/v1/bank/channel-settings` | 绑定列表 | 过滤状态/账户/通道，支持分页 |
| 3 | GET | `/api/admin/v1/bank/channel-settings/{bindingId}` | 绑定详情 | 包含权重、fallback、实时 SLA、计划 |
| 4 | POST | `/api/admin/v1/bank/channel-settings` | 创建/更新绑定 | 指定 account/channel/weight/fallback |
| 5 | POST | `/api/admin/v1/bank/channel-settings/{bindingId}/plans` | 计划维护 | 维护调优进度和 ETA |

## 接口详情

### 1. `GET /api/admin/v1/bank/channel-settings/summary`
- **查询参数**：`accountId`（可选）、`platformId`（可选）。
- **返回**：`{ stats:[...], plans:[...], notices:[...] }`。
  - `stats`：`bank_account_channel_binding` 统计绑定总数、状态、平均 `weight_percent`；`setting-alerts` 根据 `order_req_record` 中 SLA 告警（例如成功率低于阈值）计数。
  - `plans`：从 `bank_account_channel_plan` 读取进行中的计划。
  - `notices`：`order_notify_record` type=`BANK_CHANNEL_SETTING`.

### 2. `GET /api/admin/v1/bank/channel-settings`
- **查询参数**：`status`,`accountId`,`channelId`,`keyword`,`pageNo`,`pageSize`.
- **逻辑**：联表 `bank_account` 和 `pay_config_channel` 返回 `{bindingId,accountName,channelName,weight,status,fallback,updatedAt,owner}`，并附 `metrics`（30 分钟成功率、TPS）来自 `order_req_record`。
- **排序**：按 `status`（维护中优先）+ 最近更新时间。

### 3. `GET /api/admin/v1/bank/channel-settings/{bindingId}`
- **返回**：
  - `binding`: `bank_account_channel_binding`.
  - `account`: `bank_account`。
  - `channel`: `pay_config_channel`.
  - `metrics`: `order_req_record` 统计成功率、延迟、限额使用。
  - `plans`: `bank_account_channel_plan`.
- **扩展**：`?includeHistory=true` 时加载最近 5 条权重调整记录（可来自 `bank_account_channel_plan` 或 `order_notify_record`）。

### 4. `POST /api/admin/v1/bank/channel-settings`
- **Body**：`{ "id":null, "accountId":3, "payConfigChannelId":17, "weightPercent":38.0, "status":"ENABLED", "fallbackChannelId":22, "priority":1, "ownerUserId":1002, "remark":"UPI主路由" }`.
- **校验**：确保 account/channel 存在；weight 合理（0-100）；同一个 account+channel 不可重复。
- **逻辑**：写入 `bank_account_channel_binding`；若 `status` 改为 `MAINTENANCE` 同步通知 fallback；写 `order_notify_record`。
- **响应**：更新后的 binding。

### 5. `POST /api/admin/v1/bank/channel-settings/{bindingId}/plans`
- **Body**：`{ "planId":null, "title":"UPI 权重调优", "owner":"策略", "progress":52, "eta":"2024-05-21T10:00:00Z", "status":"OPEN|DONE" }`.
- **逻辑**：插入/更新 `bank_account_channel_plan`；当所有计划完成时，如 `status='DONE'` 则给 binding 添加 `completedAt` 并触发 notice；若 `status='CANCELLED'` 需记录原因。
