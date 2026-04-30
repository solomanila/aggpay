# DashboardBankIdView 后端接口需求

## 页面定位
- `DashboardBankIdView.vue` 用于展示 BankId 实名链路运行：Hero、OTP/认证/延迟统计、各国家渠道、Session 详情、告警、待办、通知。
- 接口路径 `/api/admin/v1/dashboard/bankid/**`，面向合规 & 账户团队。

## 现有数据映射
- `order_req_record`: BankId OTP/Auth/Callback 请求日志（可通过 `class_name` or `pay_config_channel.short_code` 标记），用于成功率/延迟/请求占比。
- `order_callback`: 回调成功率、重试次数。
- `order_notify_record`: OTP/Callback 告警记录。

## 新增表
- `bank_id_session_metric`: 保存商户/区域级 OTP、Auth 成功率及 fallback 状态。
- `bank_id_task`: backlog/任务列表。

```sql
CREATE TABLE IF NOT EXISTS `bank_id_session_metric` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `merchant_id` int NOT NULL,
  `region` varchar(64) NOT NULL,
  `otp_success` decimal(5,2) NOT NULL,
  `auth_success` decimal(5,2) NOT NULL,
  `fallback_enabled` tinyint NOT NULL DEFAULT 0,
  `snapshot_time` datetime NOT NULL,
  KEY `idx_bankid_merchant_time` (`merchant_id`,`snapshot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bank_id_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `owner` varchar(64) NOT NULL,
  `progress` tinyint DEFAULT 0,
  `status` varchar(32) DEFAULT 'RUNNING',
  `eta_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 接口清单
| # | Method | Path | 功能 |
|---|---|---|---|
| 1 | GET | `/api/admin/v1/dashboard/bankid/summary` | Hero、统计卡、流程节点、渠道占比 |
| 2 | GET | `/api/admin/v1/dashboard/bankid/sessions` | 商户 Session 成功率列表 |
| 3 | GET | `/api/admin/v1/dashboard/bankid/ops` | 告警、backlog、通知 |

## 接口详情

### 1. `GET /api/admin/v1/dashboard/bankid/summary`
- **参数**：`platformId`、`timezone`、`window`（默认 30 分钟）。
- **返回**：
  - `hero`: i18n 文案 + `sync`。
  - `stats`: 从 `order_req_record` + `order_callback` 计算 OTP 请求数、认证成功率、平均延迟、错误数。
  - `flows`: 以 `flowType` (OTP/Auth/Callback) 聚合 `success`、`latency`、`status`（>=98% green, >=95 amber）。
  - `channels`: `pay_config_channel` + `order_req_record` grouped by `area_type` -> `{country,operator,requests,trend}`。

### 2. `GET /api/admin/v1/dashboard/bankid/sessions`
- **参数**：`platformId`、`merchantId`（可选）、`region`、`page`、`size`。
- **响应**：`{ "total": 120, "list": [ { id,merchantId,merchantName,region,otpSuccess,authSuccess,fallback } ] }`，数据源 `bank_id_session_metric` 最新快照。
- **说明**：若 `fallback` 字段 true 需同步 `merchant_info`/`pay_config_channel` 以提供 fallback 配置详情。

### 3. `GET /api/admin/v1/dashboard/bankid/ops`
- **参数**：`platformId`、`severity`。
- **返回**：
  - `incidents`: `order_build_error` `class_name LIKE '%BankId%'` + severity mapping -> `{id,severity,title,detail,action}`。
  - `backlog`: `bank_id_task` `status in ('RUNNING','PENDING')` -> backlog 列表。
  - `notices`: `ops_timeline_event` `event_type='BANKID_NOTICE'` 近 5 条文本。

## 业务约束
- 敏感字段（OTP 请求明细）不得直接暴露；接口仅输出聚合值。
- `bank_id_session_metric` 每 1 分钟写一次快照，可通过 Flink / ETL 任务从 `order_req_record` 计算。
- 为前端“查看详情”跳转到 BankId 详情页预留 `sessionDetailUrl=/api/.../sessions/{id}`（后续实现，需单条 session log）。
