# MerchantsBoardView 后端接口需求

## 页面定位
- `MerchantsBoardView.vue` 是“商户/面板”，聚焦重点商户的实时经营数据：收款、成本、毛利率、风险、提醒、时间线。
- 需要实时汇总，支持按标签筛选，接口前缀 `/api/admin/v1/merchants/board/**`。

## 现有数据来源
- `order_info`：可按商户、时间周期聚合收款金额。
- `pay_config_info/pay_config_channel`：用于映射商户接入通道。
- `merchant_profile`（见 MerchantsListView）可提供标签、风险等级等基础数据。
- 需新增商户级别的 KPI 快照表和风险提醒表。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `merchant_kpi_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` int NOT NULL,
  `stat_time` datetime NOT NULL,
  `revenue` decimal(18,2) DEFAULT 0,
  `cost` decimal(18,2) DEFAULT 0,
  `gross_margin_pct` decimal(5,2) DEFAULT 0,
  `risk_score` decimal(5,2) DEFAULT 0,
  `success_rate` decimal(5,2) DEFAULT 0,
  KEY `idx_merchant_time` (`merchant_id`,`stat_time`),
  FOREIGN KEY (`merchant_id`) REFERENCES `pay_platform_info`(`platform_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户 KPI 快照';

CREATE TABLE IF NOT EXISTS `merchant_alert` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` int NOT NULL,
  `title` varchar(128) NOT NULL,
  `action` varchar(128) DEFAULT NULL,
  `severity` varchar(16) DEFAULT 'INFO',
  `status` varchar(16) DEFAULT 'OPEN',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`merchant_id`) REFERENCES `pay_platform_info`(`platform_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户提醒/风险';

CREATE TABLE IF NOT EXISTS `merchant_board_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` int NOT NULL,
  `event_time` datetime NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `event_type` varchar(32) DEFAULT 'INFO',
  FOREIGN KEY (`merchant_id`) REFERENCES `pay_platform_info`(`platform_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户面板时间线';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/merchants/board/summary` | 指标卡 | 商户看板 KPI、最新同步时间 |
| 2 | GET | `/api/admin/v1/merchants/board` | 商户列表 | 按标签/风险筛选重点商户 |
| 3 | GET | `/api/admin/v1/merchants/board/{merchantId}` | 商户看板详情 | KPI、提醒、时间线 |
| 4 | POST | `/api/admin/v1/merchants/board/{merchantId}/alerts` | 创建/更新提醒 | 风险/动作管理 |
| 5 | GET | `/api/admin/v1/merchants/board/timeline` | 全局时间线 | 面板级事件流 |

## 接口详情

### 1. `GET /api/admin/v1/merchants/board/summary`
- **查询参数**：`tag`,`riskLevel`（可选）。
- **返回**：`{ stats:[...], syncTime:"..." }`。
  - `stats`: 基于 `merchant_kpi_snapshot` 最新记录计算，包含总营收、平均毛利率、风险分布、成功率等。
  - `syncTime`: 最近一次快照写入时间。

### 2. `GET /api/admin/v1/merchants/board`
- **查询参数**：`tag`,`riskLevel`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：`merchant_profile` join `merchant_kpi_snapshot` 最新记录，返回 `{merchantId,name,revenue,cost,margin,risk,updatedAt}`。
- **排序**：默认按风险、营收排序。

### 3. `GET /api/admin/v1/merchants/board/{merchantId}`
- **返回**：`{ kpi, alerts, timeline }`。
  - `kpi`: `merchant_kpi_snapshot` 的最近 N 条数据（用于折线图）。
  - `alerts`: `merchant_alert` 状态 `OPEN` 的记录。
  - `timeline`: `merchant_board_timeline`。
- **权限**：`MERCHANT_VIEW`。

### 4. `POST /api/admin/v1/merchants/board/{merchantId}/alerts`
- **Body**：`{ "title":"毛利下滑", "action":"与商户沟通", "severity":"HIGH", "status":"OPEN|CLOSED" }`.
- **逻辑**：创建或更新 `merchant_alert`；状态变更写 `merchant_board_timeline`；可同步 `order_notify_record`。
- **响应**：最新提醒列表。

### 5. `GET /api/admin/v1/merchants/board/timeline`
- **查询参数**：`merchantId`（可选）、`limit=20`。
- **返回**：`merchant_board_timeline` 近期事件，供页面“处理进度”使用。
