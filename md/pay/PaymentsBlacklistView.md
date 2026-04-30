# PaymentsBlacklistView 后端接口需求

## 页面定位
- `PaymentsBlacklistView.vue` 位于“支付/黑名单”，统一管理主体/账户/设备级黑名单及复核、批量导入、联动行动。
- 页面模块：指标卡（条目/启用/待复核/同步成功率）、筛选 chips（主体/账户/设备/待复核）、黑名单表格、行动项、复核列表、公告。

## 现有数据来源（pay-service）
- `AccountBlacklistService` 暂在 Redis 中维护黑名单集合，但缺少持久化表。
- `pay_ip_white` 可代表白名单，逻辑类似，可复用 CRUD 模式。
- `order_info` / `order_req_record` 可用于判断关联订单/设备。

## 新增数据结构
```sql
CREATE TABLE IF NOT EXISTS `pay_global_blacklist` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `target_type` varchar(16) NOT NULL COMMENT 'ENTITY|ACCOUNT|DEVICE',
  `target_value` varchar(128) NOT NULL,
  `reason` varchar(255) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `risk_level` varchar(16) DEFAULT 'MEDIUM',
  `owner` varchar(64) DEFAULT NULL,
  `source` varchar(64) DEFAULT NULL COMMENT '系统/人工/风控模型',
  `extra` json DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_type_value` (`target_type`,`target_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局黑名单';

CREATE TABLE IF NOT EXISTS `pay_global_blacklist_review` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `blacklist_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL COMMENT 'RISK|OPS|LEGAL',
  `status` varchar(16) DEFAULT 'PENDING',
  `eta` datetime DEFAULT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_black_stage` (`blacklist_id`,`stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑名单复核';

CREATE TABLE IF NOT EXISTS `pay_global_blacklist_action` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `blacklist_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `owner` varchar(64) DEFAULT NULL,
  `progress` tinyint DEFAULT 0,
  `eta` datetime DEFAULT NULL,
  `status` varchar(16) DEFAULT 'OPEN',
  KEY `idx_black_action` (`blacklist_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑名单联动动作';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/payments/blacklist/summary` | 指标卡 | 梳理黑名单总量/启用/待复核/同步率 |
| 2 | GET | `/api/admin/v1/payments/blacklist` | 列表 | 支持类型/状态/关键字/Owner |
| 3 | POST | `/api/admin/v1/payments/blacklist` | 新增黑名单条目 | 主体/账户/设备 |
| 4 | POST | `/api/admin/v1/payments/blacklist/{id}/status` | 更新状态/复核结果 | 触发 Redis 同步 |
| 5 | GET | `/api/admin/v1/payments/blacklist/{id}` | 详情 | 包含复核、行动、相关订单 |

## 接口详情

### 1. `GET /api/admin/v1/payments/blacklist/summary`
- **返回**：
  - `stats`: 
    - `黑名单条目`=`count(*)`.
    - `启用`=`status='ENABLED'`.
    - `待复核`=`status='PENDING'`.
    - `同步成功率`：通过 Redis 校验（写 `order_notify_record` type=`BLACKLIST_SYNC`，统计成功/失败）。
  - `actions`: 近 5 条 `pay_global_blacklist_action` 进度。
  - `reviews`: `pay_global_blacklist_review` stage 未完成列表。
- **缓存**：30s。

### 2. `GET /api/admin/v1/payments/blacklist`
- **查询参数**：`targetType`,`status`,`riskLevel`,`owner`,`keyword`,`pageNo/pageSize`.
- **返回**：`PageResult` of `{id,targetType,targetValue,reason,status,riskLevel,owner,updatedAt}`。
- **扩展字段**：`extra` JSON (例如 `{"deviceId": "...", "merchantId": ...}`) 需要脱敏。

### 3. `POST /api/admin/v1/payments/blacklist`
- **Body**：`{ "targetType":"ACCOUNT", "targetValue":"UPI-AC55678", "reason":"代付失败", "riskLevel":"HIGH", "owner":"risk", "source":"riskbot", "extra":{...} }`.
- **逻辑**：写 `pay_global_blacklist` 状态= `PENDING`; 创建默认 `pay_global_blacklist_review` 流程（Risk->Ops->Legal），并在 Redis 中打标（`redisService.addToBlacklist`）。
- **响应**：新 ID。
- **校验**：存在重复时返回 409。

### 4. `POST /api/admin/v1/payments/blacklist/{id}/status`
- **Body**：`{ "status":"ENABLED|FROZEN|REMOVED|PENDING", "comment":"", "stage":"RISK" }`.
- **逻辑**：
  - 更新 `pay_global_blacklist.status`。
  - 若传 `stage`，同步更新 `pay_global_blacklist_review`，并在全部 stage 完成后自动切状态 `ENABLED`。
  - 当状态 `REMOVED` 时从 Redis 集合删除。
- **审计**：写 `order_notify_record` & 追加 `pay_global_blacklist_action` 记录（如“同步至风控系统”）。

### 5. `GET /api/admin/v1/payments/blacklist/{id}`
- **返回**：
  - `basic`: `pay_global_blacklist`.
  - `reviews`: `pay_global_blacklist_review`.
  - `actions`: `pay_global_blacklist_action`.
  - `relatedOrders`: 根据 `targetType` 关联 `order_info`（主体 -> `platform_id`; 账户 -> `upi`/`extend1`; 设备 -> `extend2`）最近 20 条。
- **权限**：`PAYMENT_BLACKLIST_VIEW`; 若 `targetType=DEVICE` 需额外安全审批。
