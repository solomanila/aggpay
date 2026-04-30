# PaymentsProfitView 后端接口需求

## 页面定位
- `PaymentsProfitView.vue` 位于“支付/利润报表”，聚合主体/通道/区域维度的收入、成本、毛利、趋势及成本构成，并提供公告。
- 页面模块：指标卡（今日毛利/毛利率/成本/交易量）、筛选器（主体/通道/区域）、汇总列表（名称、类型、收入、成本、毛利、趋势）、图表（毛利趋势、成本构成）、通知。

## 现有数据来源（pay.sql & pay-service）
- `order_info`：包含 `req_amount`,`real_amount`,`settle_amount`,`pay_config_channel_id`，可计算收入和结算成本。
- `order_req_record`：可补充请求量、失败率用于成本分摊。
- `pay_config_info`、`pay_config_channel`：提供主体/通道/区域映射。
- 目前无利润汇总表，需新增。

## 新增数据结构
```sql
CREATE TABLE IF NOT EXISTS `pay_channel_fee_config` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `pay_config_channel_id` bigint NOT NULL,
  `fee_rate` decimal(6,4) DEFAULT NULL,
  `fixed_fee` decimal(10,2) DEFAULT 0,
  `currency` varchar(8) DEFAULT 'CNY',
  `effective_date` date NOT NULL,
  `expire_date` date DEFAULT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_channel_effective` (`pay_config_channel_id`,`effective_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道费率配置';

CREATE TABLE IF NOT EXISTS `pay_profit_daily` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `dimension_type` varchar(16) NOT NULL COMMENT 'ENTITY|CHANNEL|REGION',
  `dimension_id` varchar(64) NOT NULL,
  `revenue_amount` decimal(18,2) DEFAULT 0,
  `cost_amount` decimal(18,2) DEFAULT 0,
  `gross_profit` decimal(18,2) DEFAULT 0,
  `gross_margin` decimal(6,3) DEFAULT 0,
  `txn_volume` decimal(18,2) DEFAULT 0,
  `trend` decimal(6,3) DEFAULT 0,
  `currency` varchar(8) DEFAULT 'CNY',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_dimension_day` (`stat_date`,`dimension_type`,`dimension_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='利润日统计';

CREATE TABLE IF NOT EXISTS `pay_profit_notice` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `title` varchar(128) NOT NULL,
  `content` varchar(512) DEFAULT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='利润公告';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/payments/profit/summary` | 指标卡 | 今日毛利、毛利率、成本、交易量 |
| 2 | GET | `/api/admin/v1/payments/profit/breakdown` | 汇总列表 | 支持按主体/通道/区域筛选 |
| 3 | GET | `/api/admin/v1/payments/profit/charts` | 图表数据 | 毛利趋势、成本构成 |
| 4 | GET | `/api/admin/v1/payments/profit/notices` | 公告 | 展示提醒 |
| 5 | POST | `/api/admin/v1/payments/profit/recalculate` | 重新生成统计 | 给财务触发日结任务 |

## 接口详情

### 1. `GET /api/admin/v1/payments/profit/summary`
- **查询参数**：`date`（默认当天）、`platformId`（可选）。
- **返回**：
  - `stats`: 
    - `今日毛利`=`SUM(gross_profit)` (dimension=ENTITY, stat_date=date)。
    - `毛利率`=`SUM(gross_profit)/SUM(revenue_amount)`.
    - `成本`=`SUM(cost_amount)`.
    - `交易量`=`SUM(txn_volume)` (以订单实付金额)。
  - `filters`: `[全部,主体,通道,区域]`.
- **来源**：`pay_profit_daily`；若当日数据未生成，实时从 `order_info` 计算（`cost_amount=real_amount*fee_rate+fixed_fee` 以 `pay_channel_fee_config` 取有效费率）。

### 2. `GET /api/admin/v1/payments/profit/breakdown`
- **查询参数**：`dimensionType=ENTITY|CHANNEL|REGION`,`date`,`keyword`,`pageNo/pageSize`.
- **返回**：`PageResult` of `{id,name,type,revenue,cost,margin,trend}`。
- **数据**：`pay_profit_daily` join `pay_platform_info` (ENTITY) / `pay_config_channel` (CHANNEL) / 区域映射 (REGION)。
- **趋势**：`trend` 字段直接来自表（表示相对上一日增长率）。若参数 `compareDate` 提供，则重新计算 `(current-compare)/compare`.

### 3. `GET /api/admin/v1/payments/profit/charts`
- **查询参数**：`dimensionType`,`dimensionId`,`range=7d|30d`.
- **返回**：
  - `grossSeries`: 最近 N 天 `gross_profit`.
  - `costSeries`: `cost_amount`.
  - `composition`: `{label,value}` 代表成本构成（上游手续费、FX、资金成本），需从 `pay_channel_fee_config` + 财务服务计算，可落入 `pay_profit_daily.extra`（可选 JSON）。
- **实现**：优先读 `pay_profit_daily`; fallback 到实时 SQL。

### 4. `GET /api/admin/v1/payments/profit/notices`
- **返回**：`pay_profit_notice` 最近 10 条 `{id,title,content,owner,createTime}`。
- **用途**：填充页面“notice”模块。

### 5. `POST /api/admin/v1/payments/profit/recalculate`
- **Body**：`{ "date":"2024-05-20", "dimension":"ALL|ENTITY|CHANNEL|REGION" }`.
- **动作**：触发 pay-service 定时任务重新聚合 `order_info` 数据并写入 `pay_profit_daily`。记录执行日志到 `order_notify_record` (type=`PROFIT_JOB`)，失败写 `order_build_error`。
- **权限**：`FINANCE_ADMIN`。
