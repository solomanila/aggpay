# PaymentsReconcileView 后端接口需求

## 页面定位
- `PaymentsReconcileView.vue` 位于“支付/通道对账”，聚合对账文件状态、差异笔数/金额、处理动作、时间线与公告。
- 模块：指标卡（对账文件/差异/已消除/待处理）、筛选器、文件表格（通道、币种、差异、状态、Owner）、行动列表、时间线、通知。

## 现有数据来源（pay.sql & pay-service）
- `order_info`：提供交易金额、入账/出账，可与对账结果比对。
- `order_notify_record`：可记录对账通知、差异处理日志。
- `PayConfigChannel`, `PayConfigInfo`：获取通道及币种信息。
- 暂无对账文件表，需新增。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `pay_reconcile_file` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `platform_id` int NOT NULL,
  `pay_config_channel_id` bigint NOT NULL,
  `file_date` date NOT NULL,
  `currency` varchar(8) NOT NULL,
  `diff_count` int DEFAULT 0,
  `diff_amount` decimal(18,2) DEFAULT 0,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner` varchar(64) DEFAULT NULL,
  `file_url` varchar(512) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_channel_date` (`pay_config_channel_id`,`file_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道对账文件';

CREATE TABLE IF NOT EXISTS `pay_reconcile_action` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `file_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `progress` tinyint DEFAULT 0,
  `eta` datetime DEFAULT NULL,
  `status` varchar(16) DEFAULT 'OPEN',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_file_status` (`file_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账处理行动';

CREATE TABLE IF NOT EXISTS `pay_reconcile_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `file_id` bigint DEFAULT NULL,
  `event_time` datetime NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `event_type` varchar(16) DEFAULT 'SYSTEM',
  KEY `idx_file_time` (`file_id`,`event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账时间线';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/payments/reconcile/summary` | 指标卡 | 聚合每日对账状态 |
| 2 | GET | `/api/admin/v1/payments/reconcile/files` | 文件列表 | 支持状态/通道/日期过滤 |
| 3 | GET | `/api/admin/v1/payments/reconcile/files/{fileId}` | 文件详情 | 含差异条目、行动、时间线 |
| 4 | POST | `/api/admin/v1/payments/reconcile/files` | 上传/登记对账文件 | 记录差异、负责人 |
| 5 | POST | `/api/admin/v1/payments/reconcile/files/{fileId}/actions` | 更新处理行动 | 驱动页面“行动列表” |

## 接口详情

### 1. `GET /api/admin/v1/payments/reconcile/summary`
- **参数**：`platformId`,`date`（默认当天）。
- **返回**：
  - `stats`: 
    - `今日对账文件`=`count(pay_reconcile_file where file_date=date)`.
    - `差异笔数`/`差异金额`=sum `diff_count/diff_amount`.
    - `已消除`=status='RESOLVED'.
    - `待处理`=status in ('PENDING','IN_PROGRESS').
  - `filters`: 状态枚举/币种列表。
- **补充**：`diff_amount` 支持按 `currency` 汇总 + 折算（使用配置汇率表或财务服务）。

### 2. `GET /api/admin/v1/payments/reconcile/files`
- **参数**：`status`,`channelId`,`currency`,`dateFrom/dateTo`,`pageNo/pageSize`.
- **返回**：`PageResult` of `{fileId,channelName,currency,diffCount,diffAmount,status,owner,updatedAt}`。
- **排序**：按 `updated_at desc`。
- **数据**：join `pay_reconcile_file` + `pay_config_channel`.

### 3. `GET /api/admin/v1/payments/reconcile/files/{fileId}`
- **返回**：`{file, actions, timelines, diffOrders}`。
  - `file`: `pay_reconcile_file`.
  - `actions`: `pay_reconcile_action`.
  - `timelines`: `pay_reconcile_timeline`.
  - `diffOrders`: 从 `order_info` 中查询 `order_id, real_amount, status` 与差异项匹配（可通过 `order_notify_record` 中记录的 diff keys）。
- **权限**：`PAYMENT_RECONCILE_VIEW`.

### 4. `POST /api/admin/v1/payments/reconcile/files`
- **Body**：`{ "payConfigChannelId":1, "fileDate":"2024-05-20", "currency":"INR", "diffCount":4, "diffAmount":260000, "fileUrl":"s3://", "owner":"finance" }`.
- **逻辑**：插入 `pay_reconcile_file`，状态默认 `PENDING`；同时写 `pay_reconcile_timeline` 事件“文件上传”。
- **返回**：新 `fileId`。
- **校验**：同一天同一通道只允许一个 OPEN 状态文件。

### 5. `POST /api/admin/v1/payments/reconcile/files/{fileId}/actions`
- **Body**：`{ "title":"UPI 差异补单", "owner":"ops", "progress":56, "status":"OPEN|DONE", "eta":"2024-05-20T12:00:00Z" }`.
- **逻辑**：新增或更新 `pay_reconcile_action`；若 `status='DONE'` 且全部行动完成则把 `pay_reconcile_file.status` 置为 `RESOLVED` 并推送公告。
- **时间线**：动作更新也写入 `pay_reconcile_timeline`。
