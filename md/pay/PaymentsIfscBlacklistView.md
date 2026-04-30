# PaymentsIfscBlacklistView 后端接口需求

## 页面定位
- `PaymentsIfscBlacklistView.vue` 位于“支付/IFSC公户代付黑名单”，管理印度公户代付黑名单条目，含指标卡、筛选器、记录表、批量动作进度、审计时间线与公告。
- 页面字段：账号(IFSC+账号)、原因、状态（待复核/启用/冻结）、Owner、创建/更新时间、动作进度条、审计记录。

## 现有数据来源（pay-service）
- `ExtUpiBlackMapper` 已对接以下表（需纳入 pay.sql）：`UpiBlacklistLog`、`UpiBlacklist`、`ifsc_info`、`ifsc_temp`，涵盖黑名单日志、主体信息和临时导入。
- `order_info`：可用于回溯黑名单触发的代付订单。
- `RedisService`（`AccountBlacklistService`）可缓存黑名单条目，支持校验。

## 新增/补全数据结构
1. 在 pay.sql 中添加/规范以下表：

```sql
CREATE TABLE IF NOT EXISTS `upi_blacklist` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `upi` varchar(100) DEFAULT NULL,
  `ifsc` varchar(32) NOT NULL,
  `account_no` varchar(64) DEFAULT NULL,
  `reason` varchar(128) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner` varchar(64) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_ifsc_account` (`ifsc`,`account_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IFSC 公户黑名单';

CREATE TABLE IF NOT EXISTS `upi_blacklist_log` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int DEFAULT NULL,
  `pay_config_id` int DEFAULT NULL,
  `channel_name` varchar(64) DEFAULT NULL,
  `order_id` varchar(64) DEFAULT NULL,
  `status` tinyint DEFAULT 0,
  `result_text` varchar(255) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IFSC 黑名单触发日志';

CREATE TABLE IF NOT EXISTS `ifsc_info` (
  `ifsc` varchar(32) PRIMARY KEY,
  `bank_name` varchar(128) DEFAULT NULL,
  `branch` varchar(128) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `city` varchar(64) DEFAULT NULL,
  `district` varchar(64) DEFAULT NULL,
  `state` varchar(64) DEFAULT NULL,
  `phone` varchar(32) DEFAULT NULL,
  `pincode` varchar(16) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IFSC 基础信息';

CREATE TABLE IF NOT EXISTS `ifsc_temp` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `ifsc` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `import_batch` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IFSC 批量导入临时表';
```

2. 新增复核记录表：

```sql
CREATE TABLE IF NOT EXISTS `upi_blacklist_audit` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `black_id` bigint NOT NULL,
  `action` varchar(16) NOT NULL COMMENT 'CREATE|REVIEW|ENABLE|FREEZE',
  `operator` varchar(64) DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_black_action` (`black_id`,`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑名单审计';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/payments/ifsc-blacklist/summary` | 指标卡 | 统计条目/启用/待复核/同步成功率 |
| 2 | GET | `/api/admin/v1/payments/ifsc-blacklist` | 列表查询 | 支持状态/Owner/关键字筛选 |
| 3 | POST | `/api/admin/v1/payments/ifsc-blacklist` | 新增黑名单条目 | 手动录入或批量导入 |
| 4 | POST | `/api/admin/v1/payments/ifsc-blacklist/{id}/status` | 复核/启停 | 更新状态并写审计 |
| 5 | GET | `/api/admin/v1/payments/ifsc-blacklist/{id}/audits` | 审计/触发记录 | 展示日志与关联订单 |

## 接口详情

### 1. `GET /api/admin/v1/payments/ifsc-blacklist/summary`
- **返回**：
  - `stats`: 
    - `黑名单数`=`count(upi_blacklist)`.
    - `启用条目`=`status='ENABLED'`.
    - `待复核`=`status='PENDING'`.
    - `同步成功率`：基于 `RedisService` 与 `upi_blacklist_log.status`（成功/总）。
  - `actions`: 当前批量导入任务 (`ifsc_temp` 状态)。
  - `audits`: 最新 `upi_blacklist_audit` 2 条。
- **说明**：同步率 = 近 24 小时 `upi_blacklist_log.status=1` / total。

### 2. `GET /api/admin/v1/payments/ifsc-blacklist`
- **查询参数**：`status`,`owner`,`keyword`(匹配 ifsc/account/upi),`pageNo/pageSize`.
- **返回**：`PageResult` of `{id,account,ifsc,reason,status,owner,createdAt,updatedAt}`。
- **排序**：`update_time desc`，默认过滤 `status!='DELETED'`.

### 3. `POST /api/admin/v1/payments/ifsc-blacklist`
- **Body**：`{ "ifsc":"HDFC0001234", "accountNo":"AC12345", "upi":"upi@bank", "reason":"疑似欺诈", "owner":"risk", "mode":"SINGLE|BATCH", "fileUrl":"" }`.
- **逻辑**：
  - `mode=SINGLE`：直接写 `upi_blacklist`，状态 `PENDING`。
  - `mode=BATCH`：先写 `ifsc_temp`（记录 import_batch），异步任务消费并入库。
  - 同步写 `upi_blacklist_audit`（action=`CREATE`）及 Redis list。
- **响应**：条目 ID 或导入批次号。

### 4. `POST /api/admin/v1/payments/ifsc-blacklist/{id}/status`
- **Body**：`{ "status":"ENABLED|FROZEN|PENDING", "comment":"..." }`.
- **逻辑**：更新 `upi_blacklist.status` 并写 `upi_blacklist_audit`（action=`REVIEW/ENABLE/FREEZE`），必要时调用 `redisService` 刷新缓存。
- **同步**：若启用 -> 调用第三方同步（在 pay-service 以任务形式落 `order_notify_record`）。

### 5. `GET /api/admin/v1/payments/ifsc-blacklist/{id}/audits`
- **返回**：
  - `audits`: `upi_blacklist_audit`.
  - `logs`: `upi_blacklist_log`（限定 `order_id`、`channel_name`、`status`）。
  - `relatedOrders`: 通过 `order_info` 查询 `other_order_id`/`upi` 匹配的订单。
- **权限**：`PAYMENT_IFSC_BLACK_VIEW`.
