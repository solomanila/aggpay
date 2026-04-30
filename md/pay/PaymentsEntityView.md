# PaymentsEntityView 后端接口需求

## 页面定位
- `PaymentsEntityView.vue` 位于“支付/主体配置”，展示业务主体（平台）资质、限额、风控状态、合同文档及审批流，支持按状态/区域筛选、查看 KYC 进度与触发审批动作。
- 页面组件：指标卡（主体数量/启用/合规/待审批）、筛选器、主体列表卡片（名称、区域、行业、状态、限额、Owner）、合规进度条、文档列表、提醒与审批待办。

## 现有数据来源（pay.sql & pay-service）
- `pay_platform_info`：平台基础信息（`platform_id`,`platform_no`,`title`,`domain`,`area_type`,`nullify`）可作为“主体”主表。
- `pay_config_info`、`pay_config_channel`、`pay_config_parameter`：反映主体所绑定的支付配置、路由、参数，用于展示业务能力与限额字段。
- `merchant_info`：每个 `pay_config_id + platform_id` 的 AppId、私钥，可推算 KYC/合同提交率。
- `pay_config_ip`、`pay_ip_white`：可衡量回调 IP、白名单配置完善度。
- Java 侧已有 `IPayPlatformInfoService`、`IPayConfigInfoService`、`IMerchantInfoService` 等基础 CRUD，可直接复用。

## 新增/扩展数据结构
- 需为主体运营信息落库，新增以下表与字段：

```sql
CREATE TABLE IF NOT EXISTS `pay_entity_profile` (
  `entity_id` int PRIMARY KEY COMMENT '对应 pay_platform_info.platform_id',
  `entity_code` varchar(64) NOT NULL COMMENT '对外主体编码，默认沿用 platform_no',
  `region` varchar(32) NOT NULL COMMENT '主要运营区域',
  `industry` varchar(32) DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/FROZEN/IN_REVIEW',
  `risk_level` varchar(16) DEFAULT 'LOW',
  `daily_limit` decimal(18,2) DEFAULT NULL,
  `currency` varchar(8) DEFAULT 'CNY',
  `owner_user_id` bigint DEFAULT NULL,
  `timezone` varchar(32) DEFAULT 'UTC+05:30',
  `notes` varchar(512) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主体运营档案';

CREATE TABLE IF NOT EXISTS `pay_entity_compliance_metric` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `entity_id` int NOT NULL,
  `metric_code` varchar(32) NOT NULL COMMENT 'KYC|CONTRACT|RISK',
  `metric_name` varchar(64) NOT NULL,
  `progress_percent` tinyint NOT NULL DEFAULT 0,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_entity_metric` (`entity_id`,`metric_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主体合规进度';

CREATE TABLE IF NOT EXISTS `pay_entity_document` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `entity_id` int NOT NULL,
  `title` varchar(128) NOT NULL,
  `doc_type` varchar(16) NOT NULL COMMENT 'PDF/DOC/IMG',
  `storage_url` varchar(512) NOT NULL,
  `status` varchar(16) DEFAULT 'VALID',
  `uploaded_by` bigint DEFAULT NULL,
  `uploaded_at` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_entity_doc` (`entity_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主体证照/合同';

CREATE TABLE IF NOT EXISTS `pay_entity_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `entity_id` int NOT NULL,
  `title` varchar(128) NOT NULL,
  `stage` varchar(64) NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'OPEN',
  `owner` varchar(64) DEFAULT NULL,
  `apply_user_id` bigint DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_entity_stage` (`entity_id`,`status`,`stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主体审批流';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/payments/entities/summary` | 指标卡 + 合规进度 + 通知 | 统计主体数量、启用/冻结、审批中、合规完成度 |
| 2 | GET | `/api/admin/v1/payments/entities` | 主体列表 | 支持状态/区域/行业/关键字过滤，返回分页数据 |
| 3 | GET | `/api/admin/v1/payments/entities/{entityId}` | 主体详情 | 返回基础信息、限额、合规进度、文档、审批待办 |
| 4 | POST | `/api/admin/v1/payments/entities/{entityId}/status` | 更新状态/限额/Owner | 用于启停主体、调整额度、切换负责人 |
| 5 | POST | `/api/admin/v1/payments/entities/{entityId}/documents` | 上传/维护文档 | 支持上传合同、风控函并写 audit |

> 所有响应封装在 `R<T>`，默认从 `AuthContextHolder` 获取 `platformId`/`userId` 以做权限校验。

## 接口详情

### 1. `GET /api/admin/v1/payments/entities/summary`
- **查询参数**：`region`（可选）、`timezone`（默认实体时区）、`platformGroup`（可选，用于按集团汇总）。
- **返回**：
  - `stats`: `[ { id:'entity-total', value, meta } ]` 由 `pay_platform_info` + `pay_entity_profile.status` 聚合。
  - `compliance`: 来自 `pay_entity_compliance_metric` 的最近进度。
  - `notices`: 最近 5 条 `pay_entity_approval` 状态变更或审批超时提醒（可复用 `order_notify_record` 写入）。
- **实现要点**：通过 MyBatis + Redis 缓存 30s；若 `merchant_info` 中 `app_id` 为空则标记“KYC未完成”指标。

### 2. `GET /api/admin/v1/payments/entities`
- **查询参数**：`status`（默认 ALL）、`region`、`industry`、`keyword`（模糊匹配 `title/platform_no`）、`pageNo/pageSize`。
- **返回**：`PageResult`，每条记录字段 `{entityId, name, region, industry, status, limit, currency, ownerName, activeConfigs, riskLevel, updatedAt}`。
- **数据来源**：`pay_platform_info`（名称、区域）、`pay_entity_profile`（状态、限额、owner）、`pay_config_info`（active config 数）、`merchant_info`（owner = 最近操作人，可记录在 profile）。
- **附加**：返回 `filters` 选项（状态枚举从 profile 表 distinct）。

### 3. `GET /api/admin/v1/payments/entities/{entityId}`
- **路径变量**：`entityId = pay_platform_info.platform_id`.
- **返回**：
  - `basic`: `pay_platform_info` & `pay_entity_profile`.
  - `limits`: 渲染功能/通道/额度列表，可从 `pay_config_info` + `pay_config_channel` 查询关联。
  - `compliance`: `pay_entity_compliance_metric`。
  - `documents`: `pay_entity_document`.
  - `approvals`: `pay_entity_approval` open 阶段。
- **安全**：需要 `PAYMENT_ENTITY_VIEW` 权限；若查看跨平台数据需具备 `SUPER_ADMIN`。

### 4. `POST /api/admin/v1/payments/entities/{entityId}/status`
- **Body**：`{ "status": "ENABLED|FROZEN|IN_REVIEW", "dailyLimit": 8000000, "ownerUserId": 1021, "remark": "" }`.
- **逻辑**：更新 `pay_entity_profile` 对应字段，并写入 `pay_entity_approval`（stage=`STATUS_CHANGE`） + `order_notify_record` 追踪；若状态=FROZEN 同步 `pay_platform_info.nullify=1`。
- **响应**：最新主体对象。
- **审计**：记录到 `order_build_error`? better to落 `pay_entity_approval.status_history`? 另可扩展 `order_notify_record`。

### 5. `POST /api/admin/v1/payments/entities/{entityId}/documents`
- **Body**：`{ "title":"India Pay 风控函", "docType":"PDF", "storageUrl":"s3://...", "status":"PENDING_REVIEW" }`.
- **逻辑**：保存到 `pay_entity_document`，必要时触发 `pay_entity_compliance_metric` 进度更新；支持多文件批量上传。
- **响应**：新文档 ID 与 `uploadedAt`。
- **补充**：若 `status=VALID` 需同步给 OSS；失败写 `order_notify_record`。
