# DashboardMerchantView 后端接口需求

## 页面定位
- `DashboardMerchantView.vue` 显示商户视角的运营指标：Hero、重点商户 spotlight、KPI、商户表格、留存卡片、工单、活动、通知及风险信号。
- 所有接口前缀 `/api/admin/v1/dashboard/merchants/**`，需根据登录用户角色（运营/风控）控制字段。

## 现有数据关联
- `merchant_info`: 商户主体、所属平台、密钥、区域（用于 spotlight、表格基础信息）。
- `order_info`: 统计 GMV、成功率、退款率、留存指标（通过用户数据或 order_info 扩展字段 `user_id`）。
- `order_build_error`: 作为风险信号（异常率等）输入。

## 新增表
- `merchant_ticket`：支持 tickets 列表。
- `merchant_campaign`：存储营销活动提升效果。
- `merchant_risk_signal`：记录风险信号与动作。

```sql
CREATE TABLE IF NOT EXISTS `merchant_ticket` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` int NOT NULL,
  `platform_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `status` varchar(32) DEFAULT 'OPEN',
  `eta` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_ticket_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `merchant_campaign` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `name` varchar(255) NOT NULL,
  `lift_percent` decimal(6,2) DEFAULT NULL,
  `status` varchar(32) DEFAULT 'ONGOING',
  `effective_time` datetime DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `merchant_risk_signal` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` int NOT NULL,
  `platform_id` int NOT NULL,
  `signal` varchar(255) NOT NULL,
  `metric_value` varchar(64) DEFAULT NULL,
  `action` varchar(255) DEFAULT NULL,
  `status` varchar(32) DEFAULT 'OPEN',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_risk_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 接口清单
| # | Method | Path | 功能 |
|---|---|---|---|
| 1 | GET | `/api/admin/v1/dashboard/merchants/summary` | Hero、spotlight、KPI、留存数据 |
| 2 | GET | `/api/admin/v1/dashboard/merchants/list` | 商户表格（分页、筛选） |
| 3 | GET | `/api/admin/v1/dashboard/merchants/insights` | Tickets、Campaigns、Notices、Risk signals |
| 4 | POST | `/api/admin/v1/dashboard/merchants/tickets` | 创建/更新运营工单 |

## 接口详情

### 1. `GET /api/admin/v1/dashboard/merchants/summary`
- **参数**：`platformId`、`timezone`、`spotlightCount`（默认 3）。
- **输出**：
  - `hero`: 静态文案 + `sync` 时间（Redis）。
  - `spotlight`: TOP N 商户 `merchant_info` + `order_info` 统计 `{merchantId,name,country,gmvp,success,risk,alert}`；`alert` 来源 `merchant_risk_signal` 最新记录。
  - `kpis`: 活跃商户（`order_info` 当日有交易的 merchant 数）、GMV、风控放行率（`order_info.status`）、退款率（`status=REFUND`）。
  - `retention`: 7/14/30 天用户留存，可从用户表（若外部）或 `order_info.user_id` 统计；支持 fallback=0.

### 2. `GET /api/admin/v1/dashboard/merchants/list`
- **参数**：`platformId`、`keyword`、`region`、`businessType`、`tier`、`owner`、`page`、`size`、`orderBy`（`today|success|dispute`）。
- **响应**：`{ "total": 400, "list": [ { merchantId,name,region,today,success,dispute,tier,owner,tags } ] }`。
- **实现**：
  - `today`: `order_info` 当日 `SUM(real_amount)`。
  - `success`: `成功订单 / 总订单`。
  - `dispute`: `status=DISPUTE` 占比（若无字段可复用 `extend` 存 dispute ratio）。
  - `tier`: 维护在 `merchant_info.extend1`（JSON）。
  - `tags`: 由 `pay_config_channel` 绑定 merchant relationships + `merchant_info.extend2` JSON。

### 3. `GET /api/admin/v1/dashboard/merchants/insights`
- **参数**：`platformId`。
- **返回**：
  - `tickets`: `merchant_ticket` `status in ('OPEN','IN_PROGRESS')`，字段 `{id,merchantId,title,owner,eta,status}`。
  - `campaigns`: `merchant_campaign` 最近 5 条（`lift_percent` -> `lift`）。
  - `notices`: 复用 `ops_timeline_event` 表 `event_type='MERCHANT_NOTICE'`。
  - `riskSignals`: `merchant_risk_signal` `status='OPEN'` 列表，包含 `action` 建议。

### 4. `POST /api/admin/v1/dashboard/merchants/tickets`
- **Body**：`{ "merchantId": 1001, "title": "BR-Tech PIX 对账", "owner": "Clearing", "eta": "处理中", "status": "OPEN" }`。
- **处理**：
  - 校验 `merchantId` 在 `merchant_info` 存在且与 `platformId` 匹配。
  - 插入/更新 `merchant_ticket`；生成审计日志（`order_notify_record`）。
  - 若 `status`=CLOSED，自动在 `merchant_risk_signal` 上写 `status='RESOLVED'`。

## 业务约束
- 列表默认仅包含当前用户可见的商户（根据角色 owner 过滤）。
- 统计均需支持缓存（30s），但 `list` 支持 `forceRefresh` 以实时查询（限制 QPS）。
- 风险信号/工单涉及敏感信息，响应需脱敏联系人/密钥；接口调用记录 `traceId` 绑定用户。
