# DashboardFundsView 后端接口需求

## 页面定位
- `DashboardFundsView.vue` 展示跨境资金流动：Hero、资金统计、币种余额、入出流水、清算管道、资金告警、调拨进度。
- 接口前缀 `/api/admin/v1/dashboard/funds/**`，仅财务/清算角色可访问。

## 现有表映射
- `order_info`: 计算今日入账/出账、在途金额（按照 `order_type=PAYIN/PAYOUT`）以及 `ledger` 记录（可复用 `extend` 字段存银行账户）。
- `order_virtual_account`: 银行账户/虚拟户信息，用于 `ledger.account`、`pipeline` 描述。
- `pay_config_channel`: 标记渠道类型、币种。

## 新增表
需要资金快照、调拨任务、管道定义：

```sql
CREATE TABLE IF NOT EXISTS `fund_balance_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `currency` varchar(8) NOT NULL,
  `balance_amount` decimal(20,4) NOT NULL,
  `utilization_percent` tinyint DEFAULT 0,
  `in_transit_amount` decimal(20,4) DEFAULT 0,
  `snapshot_time` datetime NOT NULL,
  KEY `idx_balance_platform_currency` (`platform_id`,`currency`,`snapshot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `fund_pipeline_item` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `label` varchar(64) NOT NULL,
  `items` json NOT NULL,
  `status` varchar(32) DEFAULT 'ACTIVE',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `fund_transfer_task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `owner` varchar(64) NOT NULL,
  `progress` tinyint DEFAULT 0,
  `eta_time` datetime DEFAULT NULL,
  `status` varchar(32) DEFAULT 'PENDING',
  `from_currency` varchar(8) DEFAULT NULL,
  `to_currency` varchar(8) DEFAULT NULL,
  `amount` decimal(20,4) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_transfer_platform_status` (`platform_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 接口清单
| # | Method | Path | 功能 |
|---|---|---|---|
| 1 | GET | `/api/admin/v1/dashboard/funds/summary` | Hero、统计卡片、币种余额、流向 | 
| 2 | GET | `/api/admin/v1/dashboard/funds/ledger` | 实时记账流水 | 
| 3 | GET | `/api/admin/v1/dashboard/funds/alerts` | 清算管道、风险告警、调拨任务 | 
| 4 | POST | `/api/admin/v1/dashboard/funds/transfers` | 创建调拨/回补任务 |

## 接口详情

### 1. `GET /api/admin/v1/dashboard/funds/summary`
- **参数**：`platformId`、`timezone`、`range=TODAY|HOUR`。
- **返回**：
  - `hero`, `stats`: 
    - `全局余额` = 最近 `fund_balance_snapshot` 各币种合计。
    - `在途`= `in_transit_amount` 汇总。
    - `今日入账/出账` = `order_info` (payin/payout) 当天金额。
    - `待结算` = `order_info.notice_status=WAIT_SETTLE` 金额。
  - `balances`: 读取最新 `fund_balance_snapshot` per currency `{currency,value,utilization}`。
  - `streams`: `order_info` grouped by channel type (UPI, Wallet, PIX, Bank) with trend vs 昨日。

### 2. `GET /api/admin/v1/dashboard/funds/ledger`
- **参数**：`platformId`、`direction`（IN/OUT/all）、`currency`、`status`、`page`、`size`。
- **响应**：`{ "total": 2000, "list": [ { id,time,channel,direction,amount,account,status,reference } ] }`。
- **实现**：
  - 数据来源 `order_info` + `order_virtual_account`：`account`=虚拟户/银行账户、`status`= `notice_status` or `create_status`。
  - `reference`= `order_id` 或 `other_order_id`。
  - 结果按 `create_time` 倒序；长列表建议写入 ClickHouse。

### 3. `GET /api/admin/v1/dashboard/funds/alerts`
- **参数**：`platformId`。
- **返回**：
  - `pipelines`: `fund_pipeline_item` records (`items` JSON -> string 数组)。
  - `alerts`: 通过规则检测 `fund_balance_snapshot.utilization_percent`、`order_info` 在途比率等生成 `{id,severity,title,detail,action}`。
  - `transfers`: `fund_transfer_task` `status in ('PENDING','RUNNING')`，字段 `{id,title,owner,progress,eta}`。

### 4. `POST /api/admin/v1/dashboard/funds/transfers`
- **Body**：`{ "title":"INR -> BRL", "fromCurrency":"INR", "toCurrency":"BRL", "amount":4200000, "owner":"Clearing", "eta":"2026-03-21T13:30:00+05:30" }`。
- **行为**：
  - 创建 `fund_transfer_task`（若 body 带 `id` 则更新进度/状态）。
  - 同时向清算系统发送调拨指令（MQ），并将 `traceId` 写入 `fund_transfer_task` 关联日志。
  - 校验余额：`fund_balance_snapshot` 中 `fromCurrency.balance_amount` >= amount。

## 额外要求
- 所有金额字段统一以“分”为单位传输，前端自行格式化。
- 接口需支持回放（`sinceSnapshotId`）来查看历史曲线，便于展示“同步于 12 秒前”功能。
- 操作日志通过 `order_notify_record` 记录，且 `fund_transfer_task` 更新需具备审计 trail。
