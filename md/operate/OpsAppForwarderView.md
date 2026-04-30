# OpsAppForwarderView 后端接口需求

## 页面定位
- `OpsAppForwarderView.vue` 属于“运营工具/短信转发 APP”，展示设备实时状态、转发速率、延迟、告警信息。
- 场景：监控多台安卓/转发设备，支持按状态过滤、查看设备详情、触发操作。接口前缀 `/api/admin/v1/ops/app-forwarder/**`。

## 现有数据来源
- pay.sql 无设备管理表，需要新增设备档案、状态快照与告警表。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `forwarder_device` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `device_code` varchar(64) NOT NULL UNIQUE,
  `name` varchar(64) NOT NULL,
  `region` varchar(32) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ONLINE' COMMENT 'ONLINE|OFFLINE|MAINTENANCE',
  `forward_rate` decimal(10,2) DEFAULT 0 COMMENT '条/分钟',
  `latency_ms` int DEFAULT 0,
  `ip_address` varchar(64) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `app_version` varchar(32) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信转发设备';

CREATE TABLE IF NOT EXISTS `forwarder_alert` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `device_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `action` varchar(128) DEFAULT NULL,
  `severity` varchar(16) DEFAULT 'INFO',
  `status` varchar(16) DEFAULT 'OPEN',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`device_id`) REFERENCES `forwarder_device`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信转发告警';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/ops/app-forwarder/summary` | 指标卡 | 设备数、在线率、转发速率、告警 |
| 2 | GET | `/api/admin/v1/ops/app-forwarder/devices` | 设备列表 | 筛选状态/区域/版本，分页 |
| 3 | GET | `/api/admin/v1/ops/app-forwarder/devices/{deviceId}` | 设备详情 | 包含实时指标、版本、告警 |
| 4 | POST | `/api/admin/v1/ops/app-forwarder/devices` | 新增/更新设备 | 维护档案、版本、负责人 |
| 5 | POST | `/api/admin/v1/ops/app-forwarder/devices/{deviceId}/alerts` | 告警维护 | 创建/关闭/指派告警 |

## 接口详情

### 1. `GET /api/admin/v1/ops/app-forwarder/summary`
- **查询参数**：`region`（可选）。
- **返回**：`{ stats:[...], alerts:[...] }`，其中 `stats` 聚合 `forwarder_device`（在线/离线/维护数量、平均转发速率、平均延迟）；`alerts` 返回最新 `forwarder_alert`。

### 2. `GET /api/admin/v1/ops/app-forwarder/devices`
- **查询参数**：`status`,`region`,`appVersion`,`keyword`,`pageNo`,`pageSize`.
- **逻辑**：查询 `forwarder_device` 并返回 `{id,name,region,forwardRate,status,latency,owner,updatedAt}`。
- **扩展**：`?withMetrics=true` 可附加实时指标（可从 Redis/metrics 服务读取）。

### 3. `GET /api/admin/v1/ops/app-forwarder/devices/{deviceId}`
- **返回**：`{ device, metrics, alerts }`。
  - `device`: 设备档案。
  - `metrics`: 延迟、转发速率、错误率等实时数据。
  - `alerts`: 该设备的 `forwarder_alert`。
- **权限**：`OPS_APP_FORWARDER_VIEW`。

### 4. `POST /api/admin/v1/ops/app-forwarder/devices`
- **Body**：`{ "id":null, "deviceCode":"AND-001", "name":"Mumbai-01", "region":"IN", "status":"ONLINE", "forwardRate":120.5, "latencyMs":350, "ownerUserId":101 }`.
- **逻辑**：创建或更新 `forwarder_device`；若 `status` 改为 `MAINTENANCE`，触发 `forwarder_alert` 并写 `order_notify_record`。

### 5. `POST /api/admin/v1/ops/app-forwarder/devices/{deviceId}/alerts`
- **Body**：`{ "alertId":null, "title":"延迟超过 500ms", "action":"重启应用", "severity":"HIGH", "status":"OPEN|CLOSED" }`.
- **逻辑**：维护 `forwarder_alert`，状态变更写通知；可支持 `assignTo` 字段记录责任人。
