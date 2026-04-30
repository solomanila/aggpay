# 首页（index.html）后端接口需求（首页 / Home）

## 页面解读
- 页面 DOM 由 `index.html` + `src/App.vue` 组成：头部 `HeaderBar`（`src/components/HeaderBar.vue`）展示当前用户、告警数量、时区/语言下拉；侧栏 `SidebarMenu`（`src/components/SidebarMenu.vue`）按照 `menuItems` 菜单（`src/data/mock.js`）顺序展开，初始选中“首页”。
- 主内容区域在未选中其他菜单时使用 `src/App.vue:512-620` 中的 fallback 模板：包含 Hero（宣传文案 + CTA + 指标 `hero.metrics`）、`StatsGrid`（`src/components/StatsGrid.vue`）呈现四张 KPI 卡片，以及 `ChannelCards`（`src/components/ChannelCards.vue`）展示可搜索/筛选的跨国通道列表。
- Hero 区域 CTA「发起接入申请」需要调起后端创建业务接入需求；StatsGrid 需要汇总今日收付款、风控事件、通道可用率等实时指标；ChannelCards 需要支持关键词（`channelKeyword`）与状态（`statusFilter`）筛选、展示通道费率/限额/覆盖范围、并提供“查看详情”按钮。
- Header 中“通道下单错误数量”“异常”“通知”三个胶囊需要实时数据；时区/语言选择需要保存到用户偏好。侧栏底部显示“数据刷新”时间，来源于告警聚合时间或缓存时间戳。

## 数据模型映射（pay.sql）
- `order_info`：收/付订单主表，统计今日收款/出款金额、笔数、成功率，筛选条件依赖 `status`、`create_time`、`pay_config_id`。
- `order_req_record`：通道请求日志，提供通道 SLA、延迟、错误率、请求耗时等支撑 ChannelCards 过滤与状态判定。
- `order_build_error`、`order_notify_record`、`order_callback`：用于 Header 告警 pill 的“通道下单错误数量/异常/通知”统计。
- `pay_platform_info`、`merchant_info`：Hero 指标“在运营国家”与 CTA 关联的平台、商户密钥、区域信息；菜单切换时可能带入 `platformId`。
- `pay_config_info`、`pay_config_channel`、`pay_config_parameter`、`pay_config_notify`：通道基本信息、费率、限额、覆盖、状态字段等；`pay_config_channel.json_param` 储存业务类型等结构化配置。
- `pay_ip_white`、`order_virtual_account`：在“查看详情”抽屉展示 IP 白名单、虚拟账户信息。

## 服务规范（vpn/pay-service）
- 新增 `HomeController`（位于 `vpn/pay-service` 模块）并通过 Spring Cloud Gateway 暴露 `/api/admin/v1/**`，统一返回 `R<T>` 包装：`{ "code":0, "message":"OK", "data":{...}, "traceId":"mdc-id" }`。
- 所有接口需要携带平台/租户上下文：`platformId`（可选）+ JWT（`AuthContextHolder` 获取 userId/角色）。统计类接口需要返回 `traceId`（与 `order_build_error.mdc_id` 对应）供问题追踪。
- 返回数据需支持 i18n：Hero/CTA/Menu 文案提供 `i18nKey`，默认中文值与 `language` 绑定；数字字段使用统一格式（amount 统一为分/元）。

## 接口矩阵
| # | Method & Path | 模块 | 用途 |
| --- | --- | --- | --- |
| 1 | `GET /api/admin/v1/home/context` | pay-service | Header + Sidebar 上下文、告警摘要、菜单列表 |
| 2 | `PATCH /api/admin/v1/me/preferences` | user-service/pay-service | 更新时区/语言偏好 |
| 3 | `GET /api/admin/v1/home/hero` | pay-service | Hero 区域文案、指标、CTA |
| 4 | `GET /api/admin/v1/home/summary-stats` | pay-service | StatsGrid 四张指标卡 |
| 5 | `GET /api/admin/v1/home/channel-states` | pay-service | 通道卡片数据（含筛选/分页） |
| 6 | `GET /api/admin/v1/pay-channels/{channelId}` | pay-service | “查看详情”抽屉，返回通道全量配置与 SLA |
| 7 | `POST /api/admin/v1/integration-requests` | pay-service | Hero CTA 创建接入需求 |

## 接口详情

### 1. `GET /api/admin/v1/home/context`
- **作用**：渲染 `HeaderBar` + `SidebarMenu` + 顶部告警 pill（参见 `src/components/HeaderBar.vue`）。  
- **请求参数**：`platformId`（可选，int，过滤当前平台的数据）、`includeMenu`（bool，默认 `true`，为 false 时只返回 user/alerts）。  
- **响应字段**：
  | 字段 | 说明 | 数据来源/处理 |
  | --- | --- | --- |
  | `user` | `{ id,name,role,avatarUrl }` | 由统一账号服务，下发角色以控制菜单。 |
  | `preferences` | `{ timezone, language }` | 用户偏好表（可存于 `user-service` 或 pay-service 扩展表）。若为空则按 `timezones[2]` / `languages[0]` 默认。 |
  | `alerts.channelErrorCount` | 近 15 分钟 `order_build_error` 中 `create_time>=now()-15m` 且 `status` 未关闭记录数。 |
  | `alerts.incidents` | 风险/异常条数，可由 `order_req_record.error=1` 聚合。 |
  | `alerts.notifications` | `order_notify_record` 或 `order_callback` 中 `status=0` 的待确认条数。 |
  | `alerts.lastUpdated` | 聚合时间戳（ISO8601），同时供侧栏“数据刷新”。 |
  | `timezones` / `languages` | 下拉可选值；若需动态配置可来自配置中心。 |
  | `menus` | `[{id,label,badge,description,children:[{id,label,description}]}]`，顺序与 `src/data/mock.js` 一致；需要根据角色过滤。 |
- **缓存策略**：用户 + 平台维度缓存 15 秒；`alerts` 中的 count 使用 Redis 计数器，超时时间 5 秒。

### 2. `PATCH /api/admin/v1/me/preferences`
- **用途**：Header 时区/语言变更。  
- **Body**：
```json
{
  "timezone": "UTC+05:30",
  "language": "zh-CN"
}
```
- **校验**：`timezone` 必须属于 `home/context.timezones`，`language` 必须属于 `home/context.languages`。  
- **处理**：将偏好写入 `sys_user_preference`（新表）或 user-service；同时刷新 Redis 缓存。响应返回最新 `preferences`。  
- **幂等**：若值未变化直接返回 200 + 当前值。  

### 3. `GET /api/admin/v1/home/hero`
- **用途**：渲染 `hero` 数据块（`src/App.vue:524-541`）。  
- **请求参数**：`platformId`（可选）、`areaType`（可选）、`language`（默认为用户语言）。  
- **响应示例**：
```json
{
  "code": 0,
  "data": {
    "subtitle": "全球多通道资金服务",
    "title": "实时收付监控中心",
    "description": "聚合 UPI、银行转账与电子钱包...",
    "metrics": [
      { "label": "在运营国家", "value": 12 },
      { "label": "活跃通道", "value": 32 },
      { "label": "分钟级 SLA", "value": "99.95%" }
    ],
    "cta": {
      "title": "新增业务接入",
      "description": "3 分钟提交需求...",
      "action": "发起接入申请"
    }
  },
  "traceId": "mdc-123"
}
```
- **数据逻辑**：
  - `metrics[0]`：`SELECT COUNT(DISTINCT area_type) FROM pay_platform_info WHERE nullify=0 ...`
  - `metrics[1]`：`pay_config_channel.status=1` 的通道数量。
  - `metrics[2]`：15 分钟内 `order_req_record.error=0` 占比，格式化为百分比字符串。
  - CTA 文案可放配置中心；action 字段用于按钮文案。  
- **缓存**：刷新频率 30 秒，可由定时任务写入 Redis。

### 4. `GET /api/admin/v1/home/summary-stats`
- **用途**：填充 `StatsGrid`（`src/components/StatsGrid.vue`）。  
- **查询参数**：`platformId`（可选）、`timezone`（影响时间窗口）、`range`（默认 `TODAY`，可选 `HOUR`, `DAY`, `WEEK`）。  
- **响应数据结构**：
```json
{
  "list": [
    { "id": "today-payins", "label": "今日收款成功", "value": 1392220, "currency": "CNY", "successRate": 0.981, "trend": 0.124, "trendLabel": "环比" },
    { "id": "today-payouts", "label": "今日出款成功", "value": 982566, "currency": "CNY", "avgArrivalSec": 72, "trend": 0.062, "trendLabel": "环比" },
    { "id": "risk-events", "label": "风控事件", "value": 14, "resolved": 11, "trend": -0.031, "trendLabel": "同比" },
    { "id": "uptime", "label": "通道可用率", "value": 0.99985, "windowMinutes": 15, "trend": 0.0002, "trendLabel": "周变化" }
  ],
  "traceId": "mdc-..."
}
```
- **字段映射**：
  - `today-payins.value`：`order_info` 中 `status=SUCCESS`、`order_type=PAYIN`（可新增字段或根据渠道类型）且 `pay_time` 在当天的 `SUM(real_amount)`。
  - `today-payouts`：`order_info` 中 `order_type=PAYOUT`，同时统计平均到账耗时（`notice_time - create_time`）。
  - `risk-events`：`order_build_error` 中 `status=OPEN` + `order_notify_record` 中 `error` 标记数量，按 1 小时聚合；`resolved` 来自 `status=CLOSED` 且在当日关闭的数量。
  - `uptime`：`order_req_record` 中 15 分钟内 `error=0` 的请求占比。  
- **说明**：`value` 尽量返回原始数值（如分、秒、数量），前端自行格式化；`trend` 为相对增幅。

### 5. `GET /api/admin/v1/home/channel-states`
- **用途**：填充 `ChannelCards`（`src/components/ChannelCards.vue`），支持搜索与状态筛选。  
- **查询参数**：
  | 参数 | 类型 | 说明 |
  | --- | --- | --- |
  | `keyword` | string | 模糊匹配 `pay_config_info.title`、`pay_config_channel.title`、国家、业务类型。 |
  | `status` | enum | `stable / monitor / critical / all`，与 UI 的「平稳/监控/紧急」映射。 |
  | `businessType` | string | 过滤 `json_param` 中业务标签。 |
  | `areaType` | int | 区域或国家代码。 |
  | `priorityOnly` | bool | 仅显示 `priority=true`（即 SLA 异常或配置中标记重点）的通道。 |
  | `page` / `size` | 分页，默认 1 / 20。 |
  | `sort` | enum | `successRate, latency, updatedAt` 等。 |
- **响应结构**：
```json
{
  "total": 32,
  "page": 1,
  "size": 12,
  "list": [
    {
      "id": 10001,
      "country": "印度尼西亚",
      "countryTag": "Indonesia",
      "status": "平稳",
      "statusCode": "stable",
      "risk": "低",
      "businessTypes": ["IGaming","Drama"],
      "payinRate": 0.065,
      "payoutRate": 0.035,
      "period": "T+1",
      "coverage": ["本地银行","QRIS"],
      "limit": { "currency": "IDR", "singleMax": 80000, "singleMin": 1000 },
      "description": "强实时路由...",
      "priority": true,
      "metrics": { "successRate": 0.9845, "latency": 1.9, "sla": 0.9912, "todayVolume": 1200000 },
      "updatedAt": "2026-03-21T12:30:00+05:30"
    }
  ],
  "traceId": "mdc-..."
}
```
- **数据来源**：
  - 基础信息：`pay_config_channel`（title、status、json_param）、`pay_config_info`（area_type、route 描述）。
  - 费率/限额：`pay_config_parameter`（可约定字段 key，如 `payin_rate`, `payout_rate`, `limit_single_max`），结合 `merchant_info` 的扩展字段。
  - `status`：根据最近 15 分钟 `order_req_record` 统计 `successRate`（`error=0`/总请求），加权 `pay_config_channel.status` 字段：`>=99% -> 平稳`，`97%-99% -> 监控`，`<97% -> 紧急`。
  - `limit.currency`：来自 `pay_config_parameter` 或 `pay_config_info.remark` 中配置。
  - `priority`：`json_param.priority=true` 或 SLA 低于 96% / 错误数大于阈值即 true。
- **分页实现**：推荐 MyBatis-Plus + ES/Redis 缓存组合；通道总数 < 100，可直接缓存列表并在查询时过滤。

### 6. `GET /api/admin/v1/pay-channels/{channelId}`
- **用途**：通道卡片“查看详情”弹出内容（表字段 + SLA + 安全配置）。  
- **响应字段**：
  | 字段 | 说明 |
  | --- | --- |
  | `basic` | `pay_config_info` & `pay_config_channel` 基本信息：`title`, `shortCode`, `thirdService`, `callMethod`, `url`, `httpType`, `status`, `areaType`, `jsonParam`。 |
  | `parameters` | 来自 `pay_config_parameter` 的 kv 列表（含费率、限额、周期、签名方式等）。 |
  | `merchantKeys` | 关联 `merchant_info` 中 `app_id`, `private_key*`, `create_time`。敏感字段需脱敏。 |
  | `ipWhitelist` | `pay_ip_white` 列表。 |
  | `notifyConfig` | `pay_config_notify` & `pay_config_ip`（回调地址、签名开关、回源 IP）。 |
  | `sla` | 最近 1h、6h、24h 的 `order_req_record` 聚合：`requestCount`, `errorCount`, `successRate`, `avgLatency`, `p95Latency`, `lastErrorAt`（来自 `order_build_error`）。 |
  | `logs` | 最近 3 条 `order_build_error` 或 `order_req_record` 的详情，供排查。 |
- **权限**：需要 `PAY_CHANNEL_VIEW` 权限；返回数据需脱敏（如私钥显示后四位）。  

### 7. `POST /api/admin/v1/integration-requests`
- **触发点**：Hero CTA 按钮；前端弹窗收集需求。  
- **Body**：
```json
{
  "businessName": "New OTT",
  "targetCountries": ["IN","ID"],
  "expectedVolume": 2000000,
  "contact": {
    "name": "Alice",
    "email": "alice@example.com",
    "mobile": "+91-8888888888",
    "telegram": "@alice"
  },
  "requirement": "需要 UPI+Wallet 双轨并行",
  "platformId": 12,
  "attachments": ["oss://.../requirement.pdf"]
}
```
- **处理流程**：
  1. 参数校验（手机号/email/volume 范围）；`platformId` 必须存在于 `pay_platform_info` 且 `nullify=0`。
  2. 写入 `integration_request` 新表：字段含 `id`, `platform_id`, `business_name`, `target_countries`, `expected_volume`, `contact_json`, `requirement`, `status`（`PENDING_REVIEW`）, `create_user`, `create_time`。
  3. 记录操作日志到 `order_notify_record` 或消息队列，通知 BD/Ops。  
  4. 响应 `201 Created`：`{ "requestId": 1009, "status": "PENDING_REVIEW" }`。  
- **幂等**：同一 `businessName+platformId` 在 10 分钟内重复提交需返回已存在的 `requestId`。

## 计算 & 衍生逻辑
- **状态转换**：`channel.status` 由静态字段与实时 SLA 结合；阈值建议写入配置中心以便动态调整。
- **告警统计**：`alerts.channelErrorCount`、`alerts.incidents` 需定时任务写入 Redis，以免每次请求都扫描大表；traceId 取最近一次聚合使用的 `mdc_id` 或自生成 UUID。
- **关键词搜索**：建议在 `pay_config_channel` 上建立全文搜索索引（ES 或 MySQL FULLTEXT）以支持 `keyword`。
- **时间区间**：所有统计接口根据用户 `timezone` 调整开始/结束时间，避免与数据库（默认 UTC）错位。

## 性能 / 安全 / 监控
- 接口需遵循 `AuthContextHolder` 认证，结合 `gateway` 做权限校验；`integration-requests` 接口限制 QPS 并记录审计日志。
- 统计接口默认读取 Redis 预聚合结果，若缓存 miss 则 fallback 到 DB 并异步刷新缓存。
- 返回的 `traceId` 需写入日志 MDC，便于排查 `PayReqService` 相关调用。
- 敏感字段（私钥、IP）要脱敏 + 操作权限控制；`GET /pay-channels/{channelId}` 需记录谁查看了详细信息。

## 待确认 / TODO
1. `sys_user`、`sys_role`、`sys_menu` 表结构未在 `pay.sql` 中提供——需要与 user-service 对齐或在 pay-service 新增映射表以存储菜单、偏好。
2. `integration_request` 新表字段、审批流程是否由 pay-service 托管，抑或转交 CRM/BPM 服务，需要业务确认。
3. Hero 文案、菜单描述等是否需要后台动态配置（配置中心 or CMS）？若需要，需定义配置接口/API/缓存策略。
4. ChannelCards 的 “查看详情” 是否需要带编辑能力？若未来存在编辑，则需额外的 `PUT /pay-channels/{id}`、审计日志、字段校验。
