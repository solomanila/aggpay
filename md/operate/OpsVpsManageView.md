# OpsVpsManageView 后端接口需求

## 页面定位
- `OpsVpsManageView.vue` 是“运营工具/VPS 管理”，用于管理 VPS 集群（创建、分组、负载、状态、告警）。
- 需要提供 VPS 档案、监控指标、告警、按状态筛选及批量操作，接口前缀 `/api/admin/v1/ops/vps-manage/**`。

## 现有数据来源
- pay.sql 无 VPS 相关表，需要新增 VPS 档案、监控快照、告警记录。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `ops_vps_instance` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `vps_code` varchar(64) NOT NULL UNIQUE,
  `name` varchar(128) NOT NULL,
  `region` varchar(32) DEFAULT NULL,
  `ip_address` varchar(64) DEFAULT NULL,
  `status` varchar(16) DEFAULT 'ONLINE' COMMENT 'ONLINE|OFFLINE|MAINTENANCE',
  `cpu_usage` decimal(5,2) DEFAULT 0,
  `mem_usage` decimal(5,2) DEFAULT 0,
  `disk_usage` decimal(5,2) DEFAULT 0,
  `owner_user_id` bigint DEFAULT NULL,
  `ssh_user` varchar(64) DEFAULT NULL,
  `ssh_port` int DEFAULT 22,
  `tags` varchar(128) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VPS 实例';

CREATE TABLE IF NOT EXISTS `ops_vps_alert` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `vps_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `action` varchar(128) DEFAULT NULL,
  `severity` varchar(16) DEFAULT 'INFO',
  `status` varchar(16) DEFAULT 'OPEN',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`vps_id`) REFERENCES `ops_vps_instance`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VPS 告警';

CREATE TABLE IF NOT EXISTS `ops_vps_metric_snapshot` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `vps_id` bigint NOT NULL,
  `snapshot_time` datetime NOT NULL,
  `cpu_usage` decimal(5,2) DEFAULT 0,
  `mem_usage` decimal(5,2) DEFAULT 0,
  `disk_usage` decimal(5,2) DEFAULT 0,
  `network_out` decimal(10,2) DEFAULT 0,
  `network_in` decimal(10,2) DEFAULT 0,
  KEY `idx_vps_time` (`vps_id`,`snapshot_time`),
  FOREIGN KEY (`vps_id`) REFERENCES `ops_vps_instance`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VPS 监控快照';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/ops/vps-manage/summary` | 指标卡 | 总实例、在线率、平均负载、告警 |
| 2 | GET | `/api/admin/v1/ops/vps-manage/instances` | VPS 列表 | 筛选状态/区域/标签 |
| 3 | GET | `/api/admin/v1/ops/vps-manage/instances/{vpsId}` | VPS 详情 | 档案、监控、告警 |
| 4 | POST | `/api/admin/v1/ops/vps-manage/instances` | 新建/更新 VPS | 维护档案、SSH 信息 |
| 5 | POST | `/api/admin/v1/ops/vps-manage/instances/{vpsId}/alerts` | 告警维护 | 创建/关闭/指派 |

## 接口详情

### 1. `GET /api/admin/v1/ops/vps-manage/summary`
- **查询参数**：`region`（可选）。
- **返回**：`{ stats:[...], alerts:[...] }`：总实例数、在线/维护数、平均 CPU/内存负载、告警数量。

### 2. `GET /api/admin/v1/ops/vps-manage/instances`
- **查询参数**：`status`,`region`,`tag`,`owner`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：查询 `ops_vps_instance` 并 join 最新 `ops_vps_metric_snapshot`；`keyword` 匹配 `name/vps_code/ip`。
- **响应**：`{ "total": 120, "list": [ { id,name,region,cpuUsage,memUsage,status,ip,owner,updatedAt } ] }`。

### 3. `GET /api/admin/v1/ops/vps-manage/instances/{vpsId}`
- **返回**：`{ instance, metrics, alerts }`。
  - `instance`: VPS 档案（隐藏 ssh_secret）。
  - `metrics`: 近 1 小时快照。
  - `alerts`: `ops_vps_alert`。
- **权限**：`OPS_VPS_MANAGE_VIEW`。

### 4. `POST /api/admin/v1/ops/vps-manage/instances`
- **Body**：`{ "id":null, "vpsCode":"VPS-SEA-01", "name":"SEA-Prod-01", "region":"SEA", "status":"ONLINE", "ipAddress":"10.0.1.10", "ownerUserId":1001, "tags":["gateway"] }`.
- **逻辑**：新增或更新 `ops_vps_instance`；支持 `ssh_user/port` 设置；更新后写 `order_notify_record`。

### 5. `POST /api/admin/v1/ops/vps-manage/instances/{vpsId}/alerts`
- **Body**：`{ "alertId":null, "title":"CPU 超 90%", "action":"扩容/重启", "severity":"HIGH", "status":"OPEN|CLOSED" }`.
- **逻辑**：维护 `ops_vps_alert`；状态变化推送通知/事件。
