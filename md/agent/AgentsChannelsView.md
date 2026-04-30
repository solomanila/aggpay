# AgentsChannelsView 后端接口需求

## 页面定位
- `AgentsChannelsView.vue` 位于“代理商/代理商渠道”，展示代理商的区域覆盖、绑定渠道数、配额、状态、Owner 以及提醒信息。
- 需要支持按状态筛选、查看渠道绑定详情、监控配额使用，接口前缀建议 `/api/admin/v1/agents/channels/**`。

## 现有数据来源（pay.sql & pay-service）
- 现有 schema 未包含代理商相关表；需新增代理商档案、渠道绑定及配额表。
- 可复用 `pay_config_channel`、`order_info` 获取渠道信息与交易量。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `agent_profile` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_code` varchar(64) NOT NULL UNIQUE,
  `name` varchar(128) NOT NULL,
  `region` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'ENABLED' COMMENT 'ENABLED|MAINTENANCE|DISABLED',
  `quota_amount` decimal(18,2) DEFAULT 0 COMMENT '可用配额',
  `quota_used` decimal(18,2) DEFAULT 0,
  `owner_user_id` bigint DEFAULT NULL,
  `contact` varchar(64) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商档案';

CREATE TABLE IF NOT EXISTS `agent_channel_binding` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_id` bigint NOT NULL,
  `channel_id` bigint NOT NULL,
  `quota_percent` decimal(5,2) DEFAULT 0,
  `status` varchar(16) DEFAULT 'ENABLED',
  `fallback_channel_id` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_agent_channel` (`agent_id`,`channel_id`),
  FOREIGN KEY (`agent_id`) REFERENCES `agent_profile`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`channel_id`) REFERENCES `pay_config_channel`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商渠道绑定';

CREATE TABLE IF NOT EXISTS `agent_notice` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `agent_id` bigint DEFAULT NULL,
  `title` varchar(128) NOT NULL,
  `content` varchar(255) DEFAULT NULL,
  `level` varchar(16) DEFAULT 'INFO',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`agent_id`) REFERENCES `agent_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商提醒';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/agents/channels/summary` | 指标卡 | 统计代理商数量、启用数、渠道数、配额使用 |
| 2 | GET | `/api/admin/v1/agents/channels` | 代理商列表 | 按状态/区域筛选，展示渠道覆盖 |
| 3 | GET | `/api/admin/v1/agents/channels/{agentId}` | 代理商渠道详情 | 列出绑定渠道、配额、状态、提醒 |
| 4 | POST | `/api/admin/v1/agents/channels/{agentId}` | 更新档案/配额 | 修改状态、配额、负责人、备注 |
| 5 | POST | `/api/admin/v1/agents/channels/{agentId}/bindings` | 维护渠道绑定 | 新增/更新代理商与渠道关系 |

## 接口详情

### 1. `GET /api/admin/v1/agents/channels/summary`
- **查询参数**：`region`（可选）。
- **返回**：`{ stats:[...], notices:[...] }`。
  - `stats`: `agent_profile` + `agent_channel_binding` 聚合得到代理商总数、启用数、平均渠道数、配额使用率 (`quota_used/quota_amount`)。
  - `notices`: `agent_notice` 最近事件（如渠道停用、配额即将耗尽）。

### 2. `GET /api/admin/v1/agents/channels`
- **查询参数**：`status`,`region`,`keyword`,`pageNo`,`pageSize`.
- **逻辑**：`agent_profile` 结合 `agent_channel_binding` 统计 `channels` 数、`quota` 文案，返回 `{agentId,name,region,channels,quota,status,owner,updatedAt}`。
- **附加**：`?withStats=true` 时返回代理商带来的交易额（`order_info` 聚合 `pay_config_channel_id` 属于该代理的订单）。

### 3. `GET /api/admin/v1/agents/channels/{agentId}`
- **返回**：`{ profile, bindings, notices }`。
  - `profile`: `agent_profile`。
  - `bindings`: `agent_channel_binding` 列表（含渠道名称、配额比例、fallback）。
  - `notices`: `agent_notice` + 关键 `order_notify_record`.
- **权限**：`AGENT_VIEW`。

### 4. `POST /api/admin/v1/agents/channels/{agentId}`
- **Body**：`{ "status":"ENABLED|MAINTENANCE|DISABLED", "quotaAmount":5000000, "ownerUserId":1021, "remark":"新增 SEA 渠道" }`.
- **逻辑**：更新 `agent_profile`（同步 `quota_used` 可由订单任务写入），写 `agent_notice` 记录变更；若状态变为 `DISABLED`，需解绑定通道或减配额。
- **响应**：最新 `profile`。

### 5. `POST /api/admin/v1/agents/channels/{agentId}/bindings`
- **Body**：`{ "channelId":22, "quotaPercent":35.0, "status":"ENABLED", "fallbackChannelId":21 }`.
- **逻辑**：新增或更新 `agent_channel_binding`；如配额变更需同步 `agent_profile.quota_amount`；更新后写 `agent_notice` (type=`CHANNEL_CHANGE`)。
- **响应**：代理商渠道绑定列表。
