# BankLedgerView 后端接口需求

## 页面定位
- `BankLedgerView.vue` 位于“银行户管理/流水”，展示银行账户收付流水、金额、状态、标签以及处理进度，同时提供时间线与导出功能。

## 现有数据来源（pay.sql & pay-service）
- `order_info`、`order_req_record` 可用来校验某笔银行流水关联的订单、通道、金额，但缺乏银行侧流水明细。
- 需新增独立的银行流水表，并与账户/通道/订单做关联，后端再与对账、记账模块互通。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `bank_account_ledger` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `ledger_no` varchar(64) NOT NULL UNIQUE,
  `direction` varchar(8) NOT NULL COMMENT 'CREDIT|DEBIT',
  `amount` decimal(18,2) NOT NULL,
  `currency` varchar(8) NOT NULL,
  `status` varchar(16) DEFAULT 'POSTED' COMMENT 'POSTED|IN_TRANSIT|PENDING_REVIEW|ERROR',
  `tag` varchar(32) DEFAULT NULL COMMENT 'UPI/WALLET/PIX 等',
  `order_id` bigint DEFAULT NULL,
  `pay_config_channel_id` bigint DEFAULT NULL,
  `ref_no` varchar(128) DEFAULT NULL COMMENT '银行流水号',
  `remark` varchar(255) DEFAULT NULL,
  `occur_time` datetime NOT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`account_id`) REFERENCES `bank_account`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`pay_config_channel_id`) REFERENCES `pay_config_channel`(`id`),
  KEY `idx_account_time` (`account_id`,`occur_time`),
  KEY `idx_status_time` (`status`,`occur_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行账户流水';

CREATE TABLE IF NOT EXISTS `bank_account_ledger_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `ledger_id` bigint NOT NULL,
  `event_time` datetime NOT NULL,
  `event` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `data` json DEFAULT NULL,
  FOREIGN KEY (`ledger_id`) REFERENCES `bank_account_ledger`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水处理时间线';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/bank/ledger/summary` | 指标卡 | 统计今日流水笔数、入账金额、出账金额、异常 |
| 2 | GET | `/api/admin/v1/bank/ledger` | 流水列表 | 按方向/状态/标签/账户过滤，分页 |
| 3 | GET | `/api/admin/v1/bank/ledger/{ledgerId}` | 流水详情 | 包含时间线、关联订单、对账状态 |
| 4 | POST | `/api/admin/v1/bank/ledger/{ledgerId}/status` | 更新状态 | 标记在途/待复核/已记账，写时间线 |
| 5 | GET | `/api/admin/v1/bank/ledger/export` | 导出 | 触发异步导出任务，写入下载记录 |

## 接口详情

### 1. `GET /api/admin/v1/bank/ledger/summary`
- **查询参数**：`accountId`（可选）、`date`（默认当天）。
- **返回**：`{ stats:[ {id:'ledger-total',value,meta}, ... ], timeline:[...], notices:[...] }`。
  - `stats`：从 `bank_account_ledger` 中按 `DATE(occur_time)` 聚合总笔数、入账金额(`direction=CREDIT`)、出账金额(`direction=DEBIT`)、异常笔数 (`status IN ('IN_TRANSIT','PENDING_REVIEW','ERROR')`)。
  - `timeline`：最近 3 条 `bank_account_ledger_timeline` 事件。
  - `notices`：来自 `order_notify_record` 中 type=`BANK_LEDGER_NOTICE` 的提醒（如对账失败）。

### 2. `GET /api/admin/v1/bank/ledger`
- **查询参数**：`accountId`,`direction`,`status`,`tag`,`dateFrom`,`dateTo`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：依据过滤条件检索 `bank_account_ledger`，`keyword` 支持匹配 `ledger_no`、`ref_no`、`order_id`。联表 `bank_account` 返回账户名称、区域、负责人。
- **响应**：`{ "total": 2430, "list": [ { id,accountName,direction,amount,status,tag,occurTime,updatedAt } ] }`。
- **性能**：利用 `idx_account_time`、`idx_status_time` 索引分页；长区间检索建议改用导出接口。

### 3. `GET /api/admin/v1/bank/ledger/{ledgerId}`
- **返回**：
  - `ledger`: `bank_account_ledger` 详情。
  - `timeline`: `bank_account_ledger_timeline`（指派、复核、解决记录）。
  - `order`: 如 `order_id` 非空，查询 `order_info` 获取商户、通道、订单状态。
  - `reconcile`: 若该流水已关联 `pay_reconcile_file`/`pay_reconcile_action`，返回差异处理状态。
  - `raw`: `?includeRaw=true` 时附带脱敏的银行原始报文路径（需 `FINANCE_ADMIN`）。

### 4. `POST /api/admin/v1/bank/ledger/{ledgerId}/status`
- **Body**：`{ "status":"POSTED|IN_TRANSIT|PENDING_REVIEW|ERROR", "remark":"补充说明", "assignTo":1021 }`。
- **逻辑**：更新 `bank_account_ledger.status/remark`，新增 `bank_account_ledger_timeline` 事件（包括指派人）；当状态变为 `ERROR` 自动生成 `bank_account_alert` 告警；状态恢复 `POSTED` 且存在订单时触发 `bank_bookkeeping_entry` 记账；操作写入 `order_notify_record` 便于审计。
- **响应**：最新流水对象。

### 5. `GET /api/admin/v1/bank/ledger/export`
- **查询参数**：同列表接口 + `format=csv|xlsx`。
- **处理**：创建导出任务（写入“下载管理/历史记录”使用的 `downloads_history`），后台根据条件生成文件；响应 `{ taskId }`，前端跳转“下载管理/历史记录”查看下载链接。
- **权限**：`BANK_LEDGER_EXPORT`，导出操作需记录审计日志和触发限流。
