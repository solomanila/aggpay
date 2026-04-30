# OpsTelegramView 后端接口需求

## 页面定位
- `OpsTelegramView.vue` 位于“运营工具/电报通知”，展示 Telegram 频道的订阅数、类型、状态、告警等。
- 需求：按频道类型筛选、列表管理、实时告警、配置维护。接口前缀 `/api/admin/v1/ops/telegram/**`。

## 数据来源
- pay.sql 无 telegram 表，需要新增频道配置与告警表。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `telegram_channel` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel_name` varchar(128) NOT NULL,
  `channel_type` varchar(32) NOT NULL COMMENT 'ALERT|BOT|ANNOUNCE',
  `subscribers` int DEFAULT 0,
  `status` varchar(16) DEFAULT 'ENABLED',
  `webhook_url` varchar(256) DEFAULT NULL,
  `bot_token` varchar(128) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Telegram 频道配置';

CREATE TABLE IF NOT EXISTS `telegram_alert` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `channel_id` bigint NOT NULL,
  `title` varchar(128) NOT NULL,
  `action` varchar(128) DEFAULT NULL,
  `severity` varchar(16) DEFAULT 'INFO',
  `status` varchar(16) DEFAULT 'OPEN',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`channel_id`) REFERENCES `telegram_channel`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Telegram 告警';
```

## 接口清单
| # | Method | Path | 功能 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/admin/v1/ops/telegram/summary` | 指标卡 | 频道数、订阅数、告警数 |
| 2 | GET | `/api/admin/v1/ops/telegram/channels` | 频道列表 | 支持类型/状态筛选 |
| 3 | GET | `/api/admin/v1/ops/telegram/channels/{channelId}` | 详情 | 配置、订阅、告警 |
| 4 | POST | `/api/admin/v1/ops/telegram/channels` | 新增/更新频道 | 维护 webhook、token |
| 5 | POST | `/api/admin/v1/ops/telegram/channels/{channelId}/alerts` | 告警操作 | 创建/关闭告警 |

## 接口详情

### 1. `GET /api/admin/v1/ops/telegram/summary`
- **返回**：`{ stats:[...], alerts:[...] }`，其中 stats 包含频道数量、订阅总数、告警数。

### 2. `GET /api/admin/v1/ops/telegram/channels`
- **查询参数**：`status`,`channelType`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：查询 `telegram_channel`；`keyword` 匹配 `channel_name`；返回 `{id,name,type,subscribers,status,updatedAt}`。

### 3. `GET /api/admin/v1/ops/telegram/channels/{channelId}`
- **返回**：`{ channel, alerts }`。
  - `channel`: 频道配置（隐藏 token，或仅超级管理员可见）。
  - `alerts`: `telegram_alert`。

### 4. `POST /api/admin/v1/ops/telegram/channels`
- **Body**：`{ "id":null, "channelName":"Finance Alert", "channelType":"ALERT", "status":"ENABLED", "botToken":"xxx", "webhookUrl":"https://...", "ownerUserId":103 }`.
- **逻辑**：创建或更新 `telegram_channel`，敏感字段加密；保存后触发 `order_notify_record`。

### 5. `POST /api/admin/v1/ops/telegram/channels/{channelId}/alerts`
- **Body**：`{ "alertId":null, "title":"订阅增长异常", "action":"核查机器人", "severity":"MEDIUM", "status":"OPEN|CLOSED" }`.
- **处理**：维护 `telegram_alert`；状态变化写通知。*** End Patch
