# OpsSmsView 后端接口需求

## 页面定位
- `OpsSmsView.vue` 位于“运营工具/SMS/OTP”，用于运营人员监控短信供应商、发送成功率、延迟、告警信息。
- 需要支持渠道分类筛选、列表分页、实时指标与提醒，接口前缀 `/api/admin/v1/ops/sms/**`。

## 现有数据来源（pay.sql & pay-service）
- 当前 schema 无短信渠道表，需要新增供应商配置、统计及告警表。
- 可复用 `order_notify_record` 做消息通知日志；系统可能与外部短信服务交互，需记录凭证与状态。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `sms_provider` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `provider_name` varchar(64) NOT NULL,
  `region` varchar(32) NOT NULL,
  `business_type` varchar(32) DEFAULT 'OTP',
  `status` varchar(16) DEFAULT 'ENABLED' COMMENT 'ENABLED|MAINTENANCE|DISABLED',
  `success_rate` decimal(5,2) DEFAULT 0,
  `latency_ms` int DEFAULT 0,
  `daily_volume` int DEFAULT 0,
  `owner_user_id` bigint DEFAULT NULL,
  `api_key` varchar(128) DEFAULT NULL,
  `api_secret` varchar(128) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信/OTP 供应商';

CREATE TABLE IF NOT EXISTS `sms_alert` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `provider_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `action` varchar(128) DEFAULT NULL,
  `severity` varchar(16) DEFAULT 'INFO',
  `status` varchar(16) DEFAULT 'OPEN',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`provider_id`) REFERENCES `sms_provider`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信告警';

CREATE TABLE IF NOT EXISTS `sms_metric_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `provider_id` bigint NOT NULL,
  `snapshot_time` datetime NOT NULL,
  `send_count` int DEFAULT 0,
  `success_rate` decimal(5,2) DEFAULT 0,
  `latency_ms` int DEFAULT 0,
  `fail_count` int DEFAULT 0,
  KEY `idx_provider_time` (`provider_id`,`snapshot_time`),
  FOREIGN KEY (`provider_id`) REFERENCES `sms_provider`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信指标快照';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/ops/sms/summary` | 指标卡 | 汇总渠道数量、发送量、成功率、告警数 |
| 2 | GET | `/api/admin/v1/ops/sms/providers` | 渠道列表 | 筛选类型/区域/状态，支持分页 |
| 3 | GET | `/api/admin/v1/ops/sms/providers/{providerId}` | 渠道详情 | 包含配置、实时指标、告警 |
| 4 | POST | `/api/admin/v1/ops/sms/providers` | 新增/更新渠道 | 维护供应商配置、秘钥、状态 |
| 5 | POST | `/api/admin/v1/ops/sms/providers/{providerId}/alerts` | 告警维护 | 创建/关闭/指派告警 |

## 接口详情

### 1. `GET /api/admin/v1/ops/sms/summary`
- **查询参数**：`businessType`（OTP/SMS/Email），`region`（可选）。
- **返回**：`{ stats:[...], alerts:[...] }`。
  - `stats`: `sms_provider` 和 `sms_metric_snapshot` 最新数据，包含渠道数、今日发送量、成功率、延迟、告警数量。
  - `alerts`: `sms_alert` 状态 `OPEN` 的前 5 条。

### 2. `GET /api/admin/v1/ops/sms/providers`
- **查询参数**：`status`,`region`,`businessType`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：查询 `sms_provider` 并 join 最新 `sms_metric_snapshot`；`keyword` 匹配 `provider_name`。
- **响应**：`{ "total": 20, "list": [ { id,providerName,region,volume,successRate,latency,status,owner,updatedAt } ] }`。

### 3. `GET /api/admin/v1/ops/sms/providers/{providerId}`
- **返回**：`{ provider, metrics, alerts }`。
  - `provider`: 基础配置（隐藏 `api_secret`）。
  - `metrics`: 最近 N 条快照。
  - `alerts`: `sms_alert` 列表。
- **权限**：`OPS_SMS_VIEW` 和 `SECRET_VIEW`（查看秘钥）。

### 4. `POST /api/admin/v1/ops/sms/providers`
- **Body**：`{ "id":null, "providerName":"Twilio", "region":"US", "businessType":"OTP", "status":"ENABLED", "ownerUserId":1002, "apiKey":"...", "apiSecret":"...", "quota":100000 }`.
- **逻辑**：新增或更新 `sms_provider`；敏感字段加密存储；更新后写 `order_notify_record` 通知运营。

### 5. `POST /api/admin/v1/ops/sms/providers/{providerId}/alerts`
- **Body**：`{ "alertId":null, "title":"成功率低于 95%", "action":"切备用", "severity":"HIGH", "status":"OPEN|CLOSED" }`.
- **逻辑**：维护 `sms_alert`；状态变更写 `order_notify_record` 并推动 webhook。
- **响应**：最新告警列表。
