# OpsVpsView 后端接口需求

## 页面定位
- `OpsVpsView.vue` 在“运营工具/VPS”菜单，为单个业务团队提供实时 VPS 列表与告警（偏监控视图），与管理视图相比更聚焦状态监测。
- 数据需求：实时负载、状态、IP、区域、告警提示。接口前缀 `/api/admin/v1/ops/vps/**`。

## 数据来源
- 复用 `ops_vps_instance`、`ops_vps_metric_snapshot`、`ops_vps_alert`（见 `OpsVpsManageView` 文档）；此视图只读，无新增表。

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/ops/vps/summary` | 指标卡 | 在线数、离线数、平均负载、告警 |
| 2 | GET | `/api/admin/v1/ops/vps` | VPS 列表 | 根据状态/区域筛选，返回实时状态 |
| 3 | GET | `/api/admin/v1/ops/vps/{vpsId}` | VPS 详情 | 包含快照、告警、日志 |

## 接口详情

### 1. `GET /api/admin/v1/ops/vps/summary`
- **查询参数**：`region`（可选）。
- **返回**：`{ stats:[...], alerts:[...] }`，内容与管理视图相似但仅包含该团队可见的实例。
- **过滤**：可通过 `tags` 或 `owner_user_id` 限定。

### 2. `GET /api/admin/v1/ops/vps`
- **查询参数**：`status`,`region`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：读取 `ops_vps_instance` + 最新 `ops_vps_metric_snapshot`，返回 `{id,name,region,load,status,ip,updatedAt}`。
- **缓存**：可依赖监控缓存（Redis/Prometheus API），接口只做兜底。

### 3. `GET /api/admin/v1/ops/vps/{vpsId}`
- **返回**：`{ instance, metrics, alerts }`。
  - `instance`: 基础档案。
  - `metrics`: 最近 N 条快照。
  - `alerts`: `ops_vps_alert`。
- **权限**：`OPS_VPS_VIEW`（只读），隐藏 SSH 敏感信息。*** End Patch
