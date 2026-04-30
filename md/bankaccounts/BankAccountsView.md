# BankAccountsView 后端接口需求

## 页面定位
- `BankAccountsView.vue` 位于“银行户管理/账户”，展示所有银行公户的余额、限额、状态、币种、负责人及维护计划。
- 页面需要支持按状态过滤、展示维护窗口、查看公告，并与后端保持近实时同步。

## 现有数据来源（pay.sql & pay-service）
- 现有 `order_virtual_account` 仅存储用户虚拟账户，与企业公户维度不符。
- `order_info`、`order_req_record` 可作为余额、限额使用率计算的原始数据（通过 bank account -> pay channel -> order 关系）。
- 尚无存放银行账户档案的表，需要新增。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `bank_account` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `supplier_id` bigint NOT NULL,
  `account_code` varchar(64) NOT NULL UNIQUE,
  `bank_name` varchar(128) NOT NULL,
  `branch` varchar(128) DEFAULT NULL,
  `account_no` varchar(64) NOT NULL,
  `currency` varchar(8) NOT NULL,
  `status` varchar(16) DEFAULT 'ENABLED',
  `balance` decimal(18,2) DEFAULT 0,
  `daily_limit` decimal(18,2) DEFAULT NULL,
  `limit_usage_pct` decimal(5,2) DEFAULT 0,
  `owner_user_id` bigint DEFAULT NULL,
  `risk_level` varchar(16) DEFAULT 'LOW',
  `platform_scope` varchar(64) DEFAULT NULL COMMENT '可服务的平台/区域',
  `tags` varchar(128) DEFAULT NULL,
  `last_reconcile_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`supplier_id`) REFERENCES `bank_supplier`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行公户信息';

CREATE TABLE IF NOT EXISTS `bank_account_maintenance` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `window_start` datetime NOT NULL,
  `window_end` datetime NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'PLANNED',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`account_id`) REFERENCES `bank_account`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行账户维护';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/bank/accounts/summary` | 指标卡 | 统计账户数量、启用数、余额总额、限额使用率 |
| 2 | GET | `/api/admin/v1/bank/accounts` | 账户列表 | 按状态/币种/区域/关键字过滤，支持分页 |
| 3 | GET | `/api/admin/v1/bank/accounts/{accountId}` | 账户详情 | 返回基础信息、余额曲线、维护安排、关联通道 |
| 4 | POST | `/api/admin/v1/bank/accounts` | 新建/更新账户 | 维护档案、限额、负责人 |
| 5 | POST | `/api/admin/v1/bank/accounts/{accountId}/maintenance` | 维护窗口 | 新增/更新账户级维护计划 |

## 接口详情

### 1. `GET /api/admin/v1/bank/accounts/summary`
- **查询参数**：`currency`（可选，空则统计多币种并换算 CNY）、`timezone`。
- **实现**：聚合 `bank_account` 计算总余额（按汇率配置换算）、启用/冻结数量、平均 `limit_usage_pct`；结合 `bank_account_maintenance` 找出未来 24 小时维护计划。
- **返回**：`{ stats: [...], maintenance: [...], notices: [...] }`；`notices` 来源 `order_notify_record`（type=`BANK_ACCOUNT_NOTICE`）或 `last_reconcile_at` 超期提醒。

### 2. `GET /api/admin/v1/bank/accounts`
- **查询参数**：`status`,`currency`,`region`,`keyword`,`pageNo`,`pageSize`。
- **数据**：从 `bank_account` 模糊匹配 `account_code/bank_name/account_no`。`balance` 与 `limit_usage_pct` 支持从 Redis 缓存读取，否则回源 DB。
- **响应**：`{ "total": 186, "list": [ { id,name,bankName,region,status,balance,dailyLimit,currency,ownerName,riskLevel,updatedAt } ] }`。
- **扩展**：`?withMaintenance=true` 附带最近维护窗口。

### 3. `GET /api/admin/v1/bank/accounts/{accountId}`
- **返回**：
  - `profile`: `bank_account`.
  - `maintenance`: `bank_account_maintenance`.
  - `channels`: 通过 `bank_account_channel_binding`（见 `BankChannelSettingsView.md`）列出绑定通道及权重。
  - `balanceTimeline`: 读取 `bank_account_metric_snapshot`（见 `BankRealtimeBoardView.md`）近 24 小时数据。
  - `orders`: 调用订单服务查询最近 10 条与该账户相关的出入款（通过 channel->order 映射）。
- **权限**：`BANK_ACCOUNT_VIEW`; 敏感字段（账号、支行）需脱敏。

### 4. `POST /api/admin/v1/bank/accounts`
- **Body**：`{ "id":null, "supplierId":1, "bankName":"HDFC", "accountNo":"****1234", "currency":"INR", "status":"ENABLED", "dailyLimit":20000000, "ownerUserId":1021, "riskLevel":"LOW", "tags":["UPI","核心"], "platformScope":"IN-ALL" }`.
- **流程**：校验 `supplier_id` 存在，生成唯一 `account_code`；写 `bank_account`；同步到实时看板缓存；写日志到 `order_notify_record`。
- **响应**：最新账户信息。

### 5. `POST /api/admin/v1/bank/accounts/{accountId}/maintenance`
- **Body**：`{ "maintenanceId":null, "windowStart":"2024-05-22T23:00:00+08:00", "windowEnd":"2024-05-23T00:00:00+08:00", "reason":"对账脚本", "status":"PLANNED" }`.
- **逻辑**：插入/更新 `bank_account_maintenance`; 若状态改为 `DONE`，在 `bank_account` 记录最后维护完成时间；若接近窗口自动创建 `order_notify_record` 告警。
