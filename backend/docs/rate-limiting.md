# /pay/req 限流系统设计文档

> 版本：1.0 · 日期：2026-04-18

---

## 一、背景与目标

`/pay/req` 是支付系统的核心下单入口。原有防护仅在 Spring Cloud Gateway 层做了 IP 限流（`RateLimiterConfig.java`），缺乏业务层多维度保护，存在以下风险：

| 风险 | 缺失的防护 |
|------|-----------|
| 恶意 IP 刷单 | ✗ 无业务层 IP 限流 |
| 单用户频繁请求 | ✗ 无用户级别限流 |
| frontId 重复下单 | ✗ 仅 DB 兜底，无 Redis 快速拦截 |
| 商户应用过载 | ✗ 无商户级 TPS |
| 单通道被打爆 | ✗ 无通道级 TPS |
| 系统整体过载 | ✗ 无全局 TPS |

本方案在下单链路上增加 **6 层 Redis Lua 限流**，全部基于原子脚本，无额外锁竞争。通道 TPS 由运营在管理后台热更新，无需重启服务。

---

## 二、系统架构

```
前端管理后台 (payadmin-ui)
    │ PUT /admin/channel-profile/{id}/limit-config {"tps":5000}
    ▼
admin-service
    ├─ 写 MySQL: pay_channel_profile.limit_config
    └─ 写 Redis: payproject:rl:cfg:channel:{payConfigId}:tps = 5000
                        ↑ 实时生效，pay-service 无感知切换

pay-service  /pay/req 请求入口
    ├─ ① IP 限流       (Redis Lua 滑动窗口)  → error 1029
    ├─ ② 用户限流       (Redis Lua 滑动窗口)  → error 1030
    ├─ ③ 全局 TPS      (Redis Lua 固定窗口)  → error 1034
    ├─ ⑥ 通道 TPS      (Redis Lua 固定窗口)  → error 1033
    ├─ ⑨ 商户 TPS      (Redis Lua 固定窗口)  → error 1032
    ├─ ⑩ 幂等检查       (Redis Lua SETNX)    → error 1031
    └─ ⑪ DB 幂等兜底   (原有逻辑保留)
    └─ ⑫ 进入核心下单逻辑
```

---

## 三、关键数据关系

```
aggpay.pay_config_info.id
        ║
        ║ (同一 ID)
        ║
admin.pay_channel_profile.id  ──→  limit_config: {"tps": 5000}
        │
        │ 保存时同步
        ▼
Redis: payproject:rl:cfg:channel:{payConfigId}:tps = "5000"
        │
        │ Lua 脚本读取
        ▼
pay-service 通道 TPS 检查
```

- `pay_channel_profile.id` 与 `pay_config_info.id` 相同，是同一通道在两个数据库中的主键。
- `pay_channel_limit_plan.channel_id` 存储的也是此 ID（字符串形式）。

---

## 四、限流规则详细设计

### 4.1 执行顺序

| 步骤 | 检查项 | 维度 key | 算法 | 默认限制 | 错误码 |
|------|--------|----------|------|----------|--------|
| ① | IP 限流 | `payproject:rl:ip:{ip}` | 滑动窗口 | 100次/分钟 | 1029 |
| ② | 用户限流 | `payproject:rl:user:{userId}` | 滑动窗口 | 20次/分钟 | 1030 |
| ③ | 全局 TPS | `payproject:rl:global` | 固定窗口(1s) | 500/s | 1034 |
| ⑥ | 通道 TPS | `payproject:rl:channel:{payConfigId}` | 固定窗口(1s) | 100/s（Redis动态覆盖）| 1033 |
| ⑨ | 商户 TPS | `payproject:rl:merchant:{appId}` | 固定窗口(1s) | 50/s | 1032 |
| ⑩ | 业务幂等 | `payproject:rl:idempotency:{pid}:{fid}` | SETNX(TTL 300s) | 每frontId唯一 | 1031 |
| ⑪ | DB 幂等兜底 | DB查询 front_id+platform_id | — | 查到则拒绝 | 8007 |

### 4.2 动态配置 Redis Key（运营写入）

| Key | 说明 | 示例值 |
|-----|------|--------|
| `payproject:rl:cfg:channel:{payConfigId}:tps` | 通道 TPS 上限 | `5000` |
| `payproject:rl:cfg:merchant:{appId}:tps` | 商户 TPS 上限 | `200` |
| `payproject:rl:cfg:global:tps` | 全局 TPS 上限 | `1000` |
| `payproject:rl:cfg:ip:limit` | IP 每分钟上限 | `200` |
| `payproject:rl:cfg:user:limit` | 用户每分钟上限 | `50` |

Key 不存在时，自动使用 `application.yml` 中的默认值，无需重启。

### 4.3 Lua 脚本

**`rate_limit_tps.lua`**（固定窗口，TPS 计数器）
```lua
local key    = KEYS[1]
local limit  = tonumber(ARGV[1])
local current = redis.call('INCR', key)
if current == 1 then redis.call('EXPIRE', key, 1) end
if current > limit then return 0 end
return 1
```

**`rate_limit_sliding.lua`**（滑动窗口，IP/用户）
```lua
local key       = KEYS[1]
local limit     = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2])
local now       = tonumber(ARGV[3])
redis.call('ZREMRANGEBYSCORE', key, '-inf', now - window_ms)
local count = redis.call('ZCARD', key)
if tonumber(count) >= limit then return 0 end
redis.call('ZADD', key, now, now .. math.random(1, 999999))
redis.call('PEXPIRE', key, window_ms)
return 1
```

**`idempotency_check.lua`**（SETNX 幂等）
```lua
local key = KEYS[1]
local ttl = tonumber(ARGV[1])
local result = redis.call('SET', key, '1', 'NX', 'EX', ttl)
if result then return 1 end
return 0
```

---

## 五、代码结构

### 5.1 新增文件

| 文件路径 | 说明 |
|---------|------|
| `pay-service/src/main/resources/lua/rate_limit_tps.lua` | TPS 计数 Lua 脚本 |
| `pay-service/src/main/resources/lua/rate_limit_sliding.lua` | 滑动窗口 Lua 脚本 |
| `pay-service/src/main/resources/lua/idempotency_check.lua` | 幂等 SETNX Lua 脚本 |
| `pay-service/.../service/core/PayRateLimitService.java` | 限流服务，封装 6 个检查方法 |
| `admin-service/.../entity/PayChannelProfile.java` | pay_channel_profile 实体 |
| `admin-service/.../mapper/PayChannelProfileMapper.java` | MybatisPlus mapper |
| `admin-service/.../service/PayChannelProfileService.java` | 更新 limit_config + 同步 Redis |
| `admin-service/.../controller/PayChannelProfileController.java` | REST 接口 |

### 5.2 修改文件

| 文件路径 | 改动说明 |
|---------|---------|
| `pay-service/.../util/RedisConstant.java` | 新增 12 个限流相关 key 工厂方法 |
| `pay-service/.../controller/PayReqController.java` | 注入 PayRateLimitService，调用 ①② |
| `pay-service/.../service/core/PayReqService.java` | 注入 PayRateLimitService，调用 ③⑥⑨⑩ |
| `pay-service/src/main/resources/application.yml` | 新增 `pay.ratelimit.*` 默认配置 |
| `src/components/PaymentsThrottleView.vue` | 编辑抽屉增加 TPS 字段，保存时同步调用两个接口 |

### 5.3 PayRateLimitService 核心方法

```java
// ① IP 限流（滑动窗口）
public void checkIpLimit(String ip)           throws WanliException(1029)

// ② 用户限流（滑动窗口）
public void checkUserLimit(Long userId)       throws WanliException(1030)

// ⑩ 业务幂等（SETNX TTL=300s）
public void checkIdempotency(String frontId, int platformId) throws WanliException(1031)

// ⑨ 商户 TPS（固定窗口1s）
public void checkMerchantTps(String appId)    throws WanliException(1032)

// ⑥ 通道 TPS（固定窗口1s，从 Redis 读动态配置）
public void checkChannelTps(Integer payConfigId) throws WanliException(1033)

// ③ 全局 TPS（固定窗口1s）
public void checkGlobalTps()                  throws WanliException(1034)
```

---

## 六、管理后台操作流程

### 6.1 修改通道 TPS 限流（前端）

1. 进入 **支付 → 通道限流** 页面
2. 找到目标通道记录，点击 **编辑** 按钮
3. 抽屉中填写：
   - 限流类型、限流周期（更新 `pay_channel_limit_plan`）
   - **TPS 限制**（每秒请求数，0 表示不限）
4. 点击 **提交**，系统依次：
   - `PUT /admin/channel-limit/update` → 更新 `pay_channel_limit_plan`
   - `PUT /admin/channel-profile/{channelId}/limit-config {"tps": N}` → 写 MySQL + 同步 Redis
5. **无需重启**，pay-service 下一秒即应用新 TPS

### 6.2 直接通过 Redis CLI 修改（运维）

```bash
# 设置通道 123 的 TPS 为 2000
SET payproject:rl:cfg:channel:123:tps 2000

# 移除限制（恢复默认值 100/s）
DEL payproject:rl:cfg:channel:123:tps

# 临时降低全局 TPS（紧急限速）
SET payproject:rl:cfg:global:tps 200
```

---

## 七、application.yml 默认配置

```yaml
# pay-service/src/main/resources/application.yml
pay:
  ratelimit:
    global:
      tps: 500        # 全局每秒上限
    channel:
      tps: 100        # 通道每秒上限（Redis 无配置时使用）
    merchant:
      tps: 50         # 商户每秒上限
    ip:
      limit: 100      # IP 每窗口上限
      window: 60000   # 窗口大小（毫秒，默认 1 分钟）
    user:
      limit: 20       # 用户每窗口上限
      window: 60000
    idempotency:
      ttl: 300        # 幂等 key 过期时间（秒）
```

---

## 八、错误码说明

| 错误码 | 含义 | 触发条件 |
|--------|------|---------|
| 1029 | IP 请求过于频繁 | 同 IP 在窗口内超过限制次数 |
| 1030 | 用户请求过于频繁 | 同 userId 在窗口内超过限制次数 |
| 1031 | 重复请求 | 相同 frontId+platformId 在 TTL 内再次提交 |
| 1032 | 商户请求超限 | merchant_info.app_id 1 秒内超过 TPS 限制 |
| 1033 | 通道请求超限 | pay_config_info.id 对应通道 1 秒内超过 TPS 限制 |
| 1034 | 系统繁忙请稍后重试 | 全局 TPS 超限 |

---

## 九、后端 REST 接口

### admin-service 新增接口

#### `GET /admin/channel-profile/{id}`
获取通道档案（含 limitConfig）。

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": 123,
    "channelCode": "air-in-dp",
    "name": "AirPay India DP",
    "status": "ACTIVE",
    "limitConfig": "{\"tps\": 5000}",
    "updatedAt": "2026-04-18T10:00:00"
  }
}
```

#### `PUT /admin/channel-profile/{id}/limit-config`
更新限流配置并立即同步 Redis。

**请求体：**
```json
{ "tps": 5000 }
```

**效果：**
1. 更新 `admin.pay_channel_profile.limit_config`
2. 写 `payproject:rl:cfg:channel:{id}:tps = 5000` 到 Redis
3. pay-service 下一秒请求即应用新限制

---

## 十、验证方法

```bash
# 1. 设置通道 1 的 TPS 为 2，然后快速发 3 次请求
redis-cli SET payproject:rl:cfg:channel:1:tps 2
curl "http://localhost:8080/api/pay/req?cid=1&uid=100&..."  # 1st → 200
curl "http://localhost:8080/api/pay/req?cid=1&uid=100&..."  # 2nd → 200
curl "http://localhost:8080/api/pay/req?cid=1&uid=100&..."  # 3rd → {"code":1033}

# 2. 通过管理后台热更新
curl -X PUT http://localhost:8081/admin/channel-profile/1/limit-config \
  -H "Content-Type: application/json" \
  -d '{"tps": 100}'
# → Redis 已更新，无需重启

# 3. 幂等测试（相同 frontId 发两次）
curl "...&fid=ORDER_001&..."   # 1st → 200
curl "...&fid=ORDER_001&..."   # 2nd → {"code":1031}

# 4. 用户限流（需要在 1 分钟内发超过 20 次）
for i in {1..25}; do curl "...&uid=999&fid=UNIQUE_$i&..."; done
# 第 21 次起 → {"code":1030}
```

---

## 十一、未来扩展点

| 功能 | 说明 |
|------|------|
| 商户动态 TPS | 在 `merchant_info` 表增加 `limit_config` 字段，走相同同步逻辑 |
| 限流告警 | 超限时发 Redis Pub/Sub，admin-service 订阅后推 Telegram/DingTalk |
| 漏斗限流 | 将固定窗口改为令牌桶（Redis 4.0+ EXPIRE + DECRBY 原子操作） |
| 黑名单 IP | 在 ① 之前增加 `SISMEMBER` 检查黑名单 Set |
| 多窗口叠加 | IP 同时限 1 分钟/小时两个窗口，防止突刺后仍持续高压 |
