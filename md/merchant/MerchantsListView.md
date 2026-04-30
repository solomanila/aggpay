# MerchantsListView 后端接口需求

## 页面定位
- `MerchantsListView.vue` 位于“商户/列表”，展示商户档案的汇总指标、列表、筛选与操作时间线。
- 需要覆盖主体信息、区域、余额、状态、标签、负责人等字段，并支持实时同步。

## 现有数据来源（pay.sql & pay-service）
- `pay_platform_info`：平台/商户主体基础信息（`platform_id`,`platform_no`,`title`,`domain`,`nullify`）。
- `merchant_info`：存储每个平台在不同支付配置下的秘钥/应用信息，但缺少业务维度字段（余额、标签、负责人等）。
- `order_info`：可统计各商户收款/出款金额。
- 缺少商户标签、余额、运营状态等字段，需新增扩展表。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `merchant_profile` (
  `merchant_id` int PRIMARY KEY COMMENT '对应 pay_platform_info.platform_id',
  `merchant_code` varchar(64) NOT NULL,
  `region` varchar(32) DEFAULT NULL,
  `industry` varchar(32) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ENABLED' COMMENT 'ENABLED|FROZEN|TEST',
  `balance` decimal(18,2) DEFAULT 0,
  `currency` varchar(8) DEFAULT 'CNY',
  `risk_level` varchar(16) DEFAULT 'LOW',
  `tags` varchar(128) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`merchant_id`) REFERENCES `pay_platform_info`(`platform_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户运营档案';

CREATE TABLE IF NOT EXISTS `merchant_timeline` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `merchant_id` int NOT NULL,
  `event_time` datetime NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `event_type` varchar(32) DEFAULT 'INFO',
  `remark` varchar(255) DEFAULT NULL,
  KEY `idx_merchant_time` (`merchant_id`,`event_time`),
  FOREIGN KEY (`merchant_id`) REFERENCES `pay_platform_info`(`platform_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户操作时间线';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/merchants/summary` | 指标卡 | 总商户、启用、冻结、余额 |
| 2 | GET | `/api/admin/v1/merchants` | 商户列表 | 筛选状态/区域/标签/负责人，分页 |
| 3 | GET | `/api/admin/v1/merchants/{merchantId}` | 商户详情 | 基础信息、配置、统计、时间线 |
| 4 | POST | `/api/admin/v1/merchants/{merchantId}` | 更新档案 | 修改状态、标签、负责人、备注 |
| 5 | GET | `/api/admin/v1/merchants/timeline` | 全局时间线 | 展示最近的商户事件/提醒 |

## 接口详情

### 1. `GET /api/admin/v1/merchants/summary`
- **查询参数**：`region`、`industry`（可选）。
- **返回**：`{ stats:[ {id:'mer-total',value,meta}, ... ], timeline:[...], notices:[...] }`。
  - `stats`: `pay_platform_info` + `merchant_profile` 聚合，统计总数、启用/冻结数、余额总和、风险等级分布。
  - `timeline`: `merchant_timeline` 近 5 条事件。
  - `notices`: `order_notify_record` type=`MERCHANT_NOTICE`。

### 2. `GET /api/admin/v1/merchants`
- **查询参数**：`status`,`region`,`tag`,`owner`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：`pay_platform_info` join `merchant_profile`，`keyword` 匹配 `title/platform_no/domain`；列表中附带 `balance`、`risk_level`、`ownerName`。
- **响应**：`{ "total": 420, "list": [ { merchantId,name,region,balance,status,tags,owner,updatedAt } ] }`。
- **扩展**：`?withStats=true` 时额外附带近 7 日收款/出款统计（来自 `order_info`）。

### 3. `GET /api/admin/v1/merchants/{merchantId}`
- **返回**：`{ profile, configs, stats, timeline }`。
  - `profile`: `pay_platform_info` + `merchant_profile`。
  - `configs`: 关联 `pay_config_info`,`merchant_info`（秘钥、回调）。
  - `stats`: `order_info` 汇总的收款/出款金额、成功率、余额变动。
  - `timeline`: `merchant_timeline`。
- **权限**：`MERCHANT_VIEW`; 敏感字段（秘钥）仅 `SUPER_ADMIN` 可见。

### 4. `POST /api/admin/v1/merchants/{merchantId}`
- **Body**：`{ "status":"ENABLED|FROZEN|TEST", "tags":["UPI","核心"], "ownerUserId":1021, "remark":"季度风控关注" }`.
- **逻辑**：更新 `merchant_profile`，必要时同步 `pay_platform_info.nullify`；写入 `merchant_timeline` 和 `order_notify_record`（告知相关团队）。
- **响应**：最新 `profile`。

### 5. `GET /api/admin/v1/merchants/timeline`
- **查询参数**：`merchantId`（可选）、`limit=20`。
- **返回**：`merchant_timeline` 数据 `{id,time,title,owner,eventType}`，当无指定商户时返回全局事件。
- **用途**：填充页面“处理进度”模块。
