# PaymentsFallbackView 后端接口需求

## 页面定位
- `PaymentsFallbackView.vue` 位于“支付/通道兜底”，展示兜底策略数量、当日触发次数、成功率、平均恢复、策略记录、应急动作、手册（playbooks）及通知。
- 组件：指标卡、筛选器（全部/自动/手动/状态）、记录表格（通道、策略、原因、状态、Owner）、动作进度条、手册列表、提醒。

## 现有数据来源（pay-service）
- `order_req_record`、`order_info`：可用来识别兜底触发（例如响应超时、失败率激增）和恢复耗时。
- `pay_config_channel`、`pay_config_info`: 提供通道基础信息。
- 目前没有兜底策略存储表，需要新增。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `pay_channel_fallback_strategy` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `pay_config_channel_id` bigint NOT NULL,
  `strategy_name` varchar(128) NOT NULL,
  `mode` varchar(16) NOT NULL COMMENT 'AUTO|MANUAL',
  `status` varchar(16) DEFAULT 'ENABLED',
  `priority` tinyint DEFAULT 1,
  `fallback_channel_id` bigint DEFAULT NULL COMMENT '切换到的备用通道',
  `owner` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道兜底策略';

CREATE TABLE IF NOT EXISTS `pay_channel_fallback_record` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `strategy_id` bigint NOT NULL,
  `pay_config_channel_id` bigint NOT NULL,
  `trigger_reason` varchar(64) NOT NULL,
  `status` varchar(16) DEFAULT 'ONGOING',
  `owner` varchar(64) DEFAULT NULL,
  `triggered_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `resolved_at` datetime DEFAULT NULL,
  `result` varchar(255) DEFAULT NULL,
  KEY `idx_strategy_status` (`strategy_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兜底执行记录';

CREATE TABLE IF NOT EXISTS `pay_channel_playbook` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `title` varchar(128) NOT NULL,
  `status` varchar(16) DEFAULT 'UPDATED',
  `file_url` varchar(512) DEFAULT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应急手册';

CREATE TABLE IF NOT EXISTS `pay_channel_fallback_action` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `record_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `progress` tinyint DEFAULT 0,
  `eta` datetime DEFAULT NULL,
  KEY `idx_record` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兜底应急动作';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/payments/fallback/summary` | 指标卡 + 通知 | 聚合策略数、触发次数、成功率 |
| 2 | GET | `/api/admin/v1/payments/fallback/records` | 兜底记录列表 | 筛选模式/状态/通道 |
| 3 | GET | `/api/admin/v1/payments/fallback/records/{recordId}` | 记录详情 | 包含动作、手册链接、事件日志 |
| 4 | POST | `/api/admin/v1/payments/fallback/records` | 手工触发兜底 | 记录原因、Owner、动作 |
| 5 | POST | `/api/admin/v1/payments/fallback/records/{recordId}/actions` | 更新应急动作 | 驱动页面“应急动作”卡片 |

## 接口详情

### 1. `GET /api/admin/v1/payments/fallback/summary`
- **查询参数**：`platformId`,`range=TODAY|HOUR`.
- **返回**：
  - `stats`: 
    - `启用兜底策略`=`count(pay_channel_fallback_strategy where status='ENABLED')`.
    - `今日触发`=`count(pay_channel_fallback_record where DATE(triggered_at)=today)`.
    - `成功率`=`resolved/total`.
    - `平均恢复`=`avg(TIMESTAMPDIFF(minute, triggered_at, resolved_at))`.
  - `playbooks`: `pay_channel_playbook` 列表。
  - `notices`: 最近 `order_notify_record` type=`FALLBACK_NOTICE`.

### 2. `GET /api/admin/v1/payments/fallback/records`
- **查询参数**：`mode`,`status`,`channelId`,`owner`,`pageNo/pageSize`.
- **返回**：`PageResult` of `{recordId,channelName,strategy,reason,status,owner,updatedAt}`。
- **数据**：join `pay_channel_fallback_record` + `pay_channel_fallback_strategy` + `pay_config_channel`.
- **过滤**：`mode` 来自 strategy.mode。

### 3. `GET /api/admin/v1/payments/fallback/records/{recordId}`
- **返回**：`{record, actions, metrics, playbooks}`。
  - `record`: `pay_channel_fallback_record`.
  - `actions`: `pay_channel_fallback_action`.
  - `metrics`: `order_req_record` 触发前后的成功率、延迟、流量。
  - `playbooks`: 与 strategy 关联 `pay_channel_playbook`.
- **权限**：`PAYMENT_FALLBACK_VIEW`.

### 4. `POST /api/admin/v1/payments/fallback/records`
- **Body**：`{ "payConfigChannelId":1, "strategyId":12, "triggerReason":"上游延迟", "owner":"riskbot", "mode":"AUTO|MANUAL" }`.
- **逻辑**：
  - 若 `strategyId` 为空则先创建 `pay_channel_fallback_strategy`.
  - 插入 `pay_channel_fallback_record`（status=`ONGOING`）。
  - 可附默认 `pay_channel_fallback_action`（例如“拨测”、“切换 PPC”）。
- **响应**：新 record。
- **校验**：同一通道如已有 `ONGOING` 记录需提示。

### 5. `POST /api/admin/v1/payments/fallback/records/{recordId}/actions`
- **Body**：`{ "title":"UPI PPC 兜底扩容", "owner":"SRE", "progress":48, "eta":"2024-05-20T16:00:00Z" }`.
- **逻辑**：写 `pay_channel_fallback_action`；当所有动作进度=100% 时标记 record `status='DONE'` 并同步 `resolved_at` + `result`。
- **通知**：更新时写入 `order_notify_record` 供页面“提醒”展示。
