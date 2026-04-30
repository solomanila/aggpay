x# 架构优化工程手册

> **定位**：可直接执行的工程指南，每个任务包含根因分析、分步实施、方案理由和可量化验收标准。
> 适合直接分配给开发者作为任务描述，不需要额外解释。
>
> **代码基准**（审查时间：2026-04-13）
> - 前端：`payadmin-ui`，Vue 3 SPA，`src/App.vue` **993 行**，56 个组件，依赖仅 vue/axios/qrcode
> - 后端：Spring Cloud 微服务（gateway/auth/user/pay/admin），Nacos + Redis + MySQL

---

## 系统全景

```
┌─────────────────────────────────────────────────────┐
│             payadmin-ui (Vue 3 SPA)                 │
│  自定义路由 · Axios · LocalStorage JWT · 暗色主题   │
└────────────────────────┬────────────────────────────┘
                         │ HTTP :8080·
┌────────────────────────▼────────────────────────────┐
│      gateway (Spring Cloud Gateway + WebFlux)       │
│  路由 · JWT 过滤 · 限流 · Swagger 聚合              │
└─────┬──────────┬──────────┬──────────┬──────────────┘
      │          │          │          │
┌─────▼──┐  ┌───▼────┐  ┌──▼─────┐  ┌▼───────────┐
│auth-svc│  │user-svc│  │pay-svc │  │admin-svc   │
│:8085   │  │:8083   │  │:8084   │  │:8086       │
└────────┘  └────────┘  └────────┘  └────────────┘
              基础设施层
  MySQL(aggpay) · Redis/Redisson · Nacos · Telegram Bot
```

---

## 优先级总览

| 优先级 | 任务 | 影响域 | 预估工作量 |
|--------|------|--------|-----------|
| P0 | [回调幂等性（乐观锁 CAS）](#p0-task-1回调幂等性) | 资金安全 | 0.5 天 |
| P0 | [敏感密钥加密存储](#p0-task-2敏感密钥加密存储) | 安全合规 | 1 天 |
| P1 | [回调处理异步化（MQ）](#p1-task-3回调处理异步化) | 稳定性 | 3 天 |
| P1 | [强制对账定时任务](#p1-task-4强制对账定时任务) | 资金核对 | 2 天 |
| P2 | [渠道加权路由实现](#p2-task-5渠道加权路由) | 可用性 | 1 天 |
| P2 | [PayReqService 参数构建重构](#p2-task-6参数构建重构) | 可维护性 | 2 天 |
| P3 | [前端引入 Pinia + vue-router](#p3-task-7前端架构演进) | 可维护性 | 3 天 |
| P3 | [链路追踪 + 监控接入](#p3-task-8可观测性) | 可观测性 | 2 天 |

> **执行顺序约束**：Task-3 依赖 Task-1 先稳定；其余任务互相独立，可并行。

---

## P0-Task-1：回调幂等性

### 问题根因分析

**现状代码位置**：`pay-service` → `PayNotifyService.notify()`

当前用 Redis `SET NX EX 180` 分布式锁做并发保护，存在两个独立漏洞：

**漏洞 A：锁内异常导致半写**
```
线程 1 获得锁
  → 验签通过
  → UPDATE order_info SET status=1  ✓
  → 写 order_notify_record          ✗ (异常)
  → 锁到期释放（180s 后）
线程 2 获得锁
  → 验签通过
  → UPDATE order_info（status 已=1，但仍执行）
  → 下游通知再次触发 → 重复 VIP 开通
```

**漏洞 B：并发窗口覆盖**
- 三方支付商在未收到响应时会立即重试，两个回调间隔可能 < 1ms
- `SET NX` 仅保证"同一时刻只有一个线程进入"，不保证"状态已成功的订单不再被处理"
- 第一个线程执行完毕释放锁后，第二个线程进入，此时 status=1 但代码无判断直接覆盖

**量化风险**：每次重复触发 = 一次 VIP 重复开通或游戏服务器重复充值，直接资金损失。

---

### 实施步骤

**Step 1：DDL 变更（前置，需停机或在线 DDL）**

```sql
-- 在 order_info 表加乐观锁版本号
ALTER TABLE order_info
  ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER update_time;

-- 同步加索引（如果 order_id 已有唯一索引则跳过）
-- ALTER TABLE order_info ADD UNIQUE INDEX uk_order_id (order_id);
```

> 在线 DDL 工具推荐：gh-ost（对主库零锁）。字段加在末尾，不影响现有查询。

**Step 2：实体类加 `@Version` 注解**

文件：`common-data` 模块 → `OrderInfo.java`

```java
import com.baomidou.mybatisplus.annotation.Version;

@TableField("version")
@Version
private Integer version;
```

MyBatis-Plus 会自动将所有 `updateById` 改写为：
```sql
UPDATE order_info SET ..., version = version+1
WHERE id = ? AND version = ?   -- version 不匹配时返回 0 行
```

**Step 3：`PayNotifyService` 修改核心更新逻辑**

```java
// 修改前（不安全）
orderInfoMapper.updateById(order);

// 修改后（CAS 语义）
int rows = orderInfoMapper.updateById(order);
if (rows == 0) {
    // 版本冲突：订单已被另一个线程处理
    log.info("[幂等] 订单 {} 已处理，跳过重复回调", orderId);
    return "success";   // 仍然返回 success，避免三方重试
}
```

**Step 4：缩短 Redis 锁 TTL，明确其职责**

Redis 锁的唯一职责是"在极短并发窗口内防止两个线程同时读取到 status=0 的订单"，不需要 180s：

```java
// 修改前
redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 180, TimeUnit.SECONDS);

// 修改后：5 秒足够处理一次回调
boolean locked = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
if (!locked) {
    log.info("[幂等] 订单 {} 正在处理中，拒绝重入", orderId);
    return "success";
}
```

两道防线说明：
- **Redis 锁（5s）**：防止同一毫秒内的并发请求同时进入处理逻辑
- **DB 乐观锁**：防止 Redis 重启/网络分区等极端情况下锁失效后的重复写入

---

### 理由分析：为什么用 DB 乐观锁而不是纯 Redis SETNX

| 方案 | 优点 | 缺点 |
|------|------|------|
| 纯 Redis SETNX（现状） | 实现简单 | Redis 重启/主从切换时锁丢失；TTL 设置困难（太短漏过，太长阻塞） |
| DB 悲观锁 `SELECT FOR UPDATE` | 强一致 | 锁争用激烈时性能差；长事务风险 |
| **DB 乐观锁 @Version（推荐）** | 无锁等待，高并发下性能好；DB 事务保证原子性，不依赖 Redis 可用性 | 需要加版本字段；高冲突率下重试次数多（支付回调天然低冲突，无此问题） |

支付回调场景：同一 orderId 的并发冲突概率极低（三方重试间隔通常 > 10s），乐观锁最合适。

---

### 验收标准

- [ ] 用 JMeter 对同一 orderId 发 100 个并发回调，DB 中 status=1 记录唯一，下游通知只触发 1 次
- [ ] Redis 服务重启期间，回调仍能正确处理（乐观锁兜底）
- [ ] 重复回调返回 "success"（不让三方误判失败），但日志标记 `[幂等] 跳过`

---

## P0-Task-2：敏感密钥加密存储

### 问题根因分析

**现状暴露点扫描命令**（执行后记录结果）：
```bash
git grep -rn -i "password\|secret\|privateKey\|token\|appSecret" \
  -- "*.yml" "*.yaml" "*.properties" | grep -v ".example"
```

已知三处暴露：

| 位置 | 内容 | 风险等级 |
|------|------|---------|
| `*/application.yml` | `spring.datasource.password: xxx` | 严重：DB 直接可访问 |
| `MerchantInfo` DB 字段 | `private_key1~4` 明文 | 严重：可伪造任意商户签名 |
| `*/application.yml` 或代码 | Telegram Bot Token | 高：可接管告警机器人 |

**攻击链**：Git 仓库泄露 → 获取 DB 密码 → 导出 MerchantInfo → 获取 privateKey → 向三方支付商伪造签名 → 套现。整条链无需任何入侵，仅凭代码仓库访问权限即可完成。

---

### 实施步骤

**Step 1：全量扫描现有暴露（必须先做，建立基线）**

```bash
# 扫描 Git 历史中的所有提交（包括已删除文件）
docker run --rm -v $(pwd):/path \
  zricethezav/gitleaks:latest detect --source /path --report-format json \
  --report-path /path/gitleaks-report.json

# 查看报告
cat gitleaks-report.json | jq '.[].Description'
```

如果历史中有明文提交，**必须 rotate 对应密钥**（修改 DB 密码、重新生成 Telegram Token 等），仅删除代码不够，历史 commit 中密钥已永久存在。

**Step 2：Nacos 加密配置（推荐，零额外组件）**

Nacos 支持 `cipher-` 前缀 + 对称密钥自动解密（需 Jasypt 或自定义插件）：

```yaml
# Nacos 配置中心存储（明文 key → 加密 value）
spring:
  datasource:
    password: ENC(AES加密后的密文)   # Jasypt 格式

# 启动参数注入主密钥（不进 Git）
-Djasypt.encryptor.password=${JASYPT_MASTER_KEY}
```

部署时通过环境变量或 K8s Secret 注入主密钥：
```bash
export JASYPT_MASTER_KEY="从密钥管理服务获取，不写入任何文件"
```

**Step 3：`MerchantInfo.privateKey` AES-256 加密存储**

在 `common-data` 模块新增 TypeHandler：

```java
// common-data/src/main/java/.../handler/AesEncryptTypeHandler.java
@MappedTypes(String.class)
public class AesEncryptTypeHandler extends BaseTypeHandler<String> {

    // 密钥从环境变量读取，不硬编码
    private static final String KEY = System.getenv("MERCHANT_KEY_ENCRYPT_KEY");

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, AesUtils.encrypt(parameter, KEY));  // 写入时加密
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String encrypted = rs.getString(columnName);
        return encrypted == null ? null : AesUtils.decrypt(encrypted, KEY);  // 读取时解密
    }
    // ... 其他 getNullableResult 重载
}
```

实体类：
```java
// OrderInfo.java 或 MerchantInfo.java
@TableField(value = "private_key1", typeHandler = AesEncryptTypeHandler.class)
private String privateKey1;
```

**Step 4：数据迁移脚本（一次性）**

```sql
-- 迁移前备份
CREATE TABLE merchant_info_backup AS SELECT * FROM merchant_info;

-- 迁移后验证（应用层执行，通过 TypeHandler 读取并重新写入加密值）
-- 伪代码：
-- List<MerchantInfo> all = merchantMapper.selectList(null);  // 读出明文
-- all.forEach(m -> merchantMapper.updateById(m));            // TypeHandler 自动加密写回
```

**Step 5：CI 门禁（防止后续引入）**

`.github/workflows/security-scan.yml`（或 GitLab CI 等价配置）：

```yaml
security-scan:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v3
      with:
        fetch-depth: 0   # 需要完整历史
    - name: Run gitleaks
      uses: gitleaks/gitleaks-action@v2
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

此步骤失败会阻断 PR 合并，永久防止明文密钥再次进入仓库。

---

### 理由分析：为什么优先 Nacos 而不是 HashiCorp Vault

| 方案 | 适合场景 | 额外成本 |
|------|---------|---------|
| **Nacos 加密配置（推荐）** | 已有 Nacos 基础设施，直接复用 | 仅需 Jasypt 依赖，< 0.5 天 |
| HashiCorp Vault | 企业级，动态 secret，审计日志完整 | 需独立部署 Vault 集群，运维复杂度高 |
| AWS/GCP Secret Manager | 云原生，免运维 | 依赖云厂商，需改造配置读取逻辑 |

当前架构已有 Nacos，选 Nacos 方案可在现有基础设施内解决问题，Vault 留作后期演进目标。

---

### 验收标准

- [ ] `git grep -rn "password:" -- "*.yml"` 无明文密码输出
- [ ] DB 中 `merchant_info.private_key1` 字段值为密文（Base64 或 Hex 格式，非明文 JSON）
- [ ] 应用正常启动，支付签名验证通过（TypeHandler 解密透明）
- [ ] CI gitleaks 扫描通过，阻断流水线验证 OK
- [ ] Telegram Bot Token 已从代码中移除，通过环境变量注入

---

## P1-Task-3：回调处理异步化

### 问题根因分析

**现状同步链路**（`PayNotifyService.notify()` 内）：

```
HTTP 请求到达（三方支付商）
  ↓ 写 order_callback（DB 写）               ~10ms
  ↓ IP 白名单校验（DB 查询）                  ~5ms
  ↓ IPayNotifyHandle 参数提取 + 验签          ~20ms
  ↓ 金额容差校验                              ~1ms
  ↓ 区域时间窗口校验                          ~1ms
  ↓ UPDATE order_info（DB 写，含锁）          ~15ms
  ↓ 写 order_notify_record（DB 写）           ~10ms
  ↓ 推送 Redis List pay_success_list          ~3ms
  ↓ 下游 HTTP 通知（VIP 服务 / 游戏服务器）  ~200~2000ms ← 主要瓶颈
  ↓ 返回 "success" 给三方
总计：~265ms ~ 2065ms
```

**问题**：主流三方支付商的重试策略：
- 未收到响应 → 5s 后重试
- 合计重试 3~5 次，间隔递增

下游服务（VIP 服务、游戏服务器）响应慢时，必然触发重试风暴：一笔订单产生 3~5 个回调 → 幂等锁承压 → 日志告警噪音 → 最坏情况下幂等失效导致重复充值。

**Redis List 的隐患**：`pay_success_list` 无 ACK 机制——Consumer 崩溃后消息直接丢失，无死信队列，无重试，无监控。

---

### 实施步骤

> **前置依赖**：Task-1（乐观锁幂等）必须先完成，否则 MQ 重试会导致重复充值。

**Step 1：选型决策**

推荐 **RocketMQ**，理由见后文。依赖：

```xml
<!-- pay-service / pom.xml -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.3.0</version>
</dependency>
```

**Step 2：重构 `/pay/notify` 接口——快速返回**

```java
// PayNotifyController.java → 改造后
@PostMapping("/pay/notify/{platformNo}/{configId}")
public String notify(@PathVariable String platformNo,
                     @PathVariable String configId,
                     HttpServletRequest request) {

    // 1. 写审计日志（必须同步，三方需要知道我们收到了）
    String rawBody = extractRawBody(request);
    orderCallbackMapper.insert(OrderCallback.of(platformNo, configId, rawBody));

    // 2. 发 MQ 消息（< 5ms）
    PayNotifyMessage msg = PayNotifyMessage.builder()
        .platformNo(platformNo)
        .configId(configId)
        .rawBody(rawBody)
        .receiveTime(Instant.now())
        .build();
    rocketMQTemplate.syncSend("PAY_NOTIFY_TOPIC", msg);

    // 3. 立即返回（< 50ms 总耗时）
    return "success";
}
```

**Step 3：实现 MQ Consumer**

```java
@Component
@RocketMQMessageListener(
    topic = "PAY_NOTIFY_TOPIC",
    consumerGroup = "pay-notify-consumer",
    consumeMode = ConsumeMode.ORDERLY   // 同一 orderId 顺序消费，防乱序
)
public class PayNotifyConsumer implements RocketMQListener<PayNotifyMessage> {

    @Override
    public void onMessage(PayNotifyMessage msg) {
        try {
            payNotifyService.processCallback(msg);  // 原有核心逻辑迁移至此
        } catch (Exception e) {
            log.error("[回调处理失败] platform={} config={}", msg.getPlatformNo(), msg.getConfigId(), e);
            throw e;  // 抛出异常 → RocketMQ 自动重试
        }
    }
}
```

**Step 4：重试与死信配置**

```yaml
# application.yml
rocketmq:
  consumer:
    max-reconsume-times: 5   # 最多重试 5 次

# RocketMQ 控制台或代码中配置死信队列
# 死信 Topic：%DLQ%pay-notify-consumer
# 死信处理：Telegram 告警 + 写 order_build_error
```

重试退避策略（RocketMQ 默认）：
```
第 1 次失败 → 10s 后重试
第 2 次失败 → 30s 后重试
第 3 次失败 → 60s 后重试
第 4 次失败 → 120s 后重试
第 5 次失败 → 进入死信队列
```

**Step 5：死信消费者（告警兜底）**

```java
@RocketMQMessageListener(topic = "%DLQ%pay-notify-consumer", consumerGroup = "pay-dlq-consumer")
public class PayNotifyDlqConsumer implements RocketMQListener<PayNotifyMessage> {
    @Override
    public void onMessage(PayNotifyMessage msg) {
        orderBuildErrorMapper.insert(OrderBuildError.ofDlq(msg));
        telegramBot.sendAlert("⚠️ 回调处理彻底失败，已进入死信：orderId=" + msg.getOrderId());
    }
}
```

---

### 理由分析：为什么用 RocketMQ 而不是 Kafka 或继续用 Redis List

| 方案 | 优点 | 缺点 | 适合场景 |
|------|------|------|---------|
| Redis List（现状） | 无额外组件 | 无 ACK、无重试、无死信、无监控，消息可丢 | 允许丢消息的通知 |
| **RocketMQ（推荐）** | 事务消息、顺序消费、死信队列、延迟消息原生支持；Spring Boot Starter 成熟 | 需部署 RocketMQ | 金融级消息可靠性 |
| Kafka | 极高吞吐，分布式强 | 事务消息配置复杂；Consumer Group 模型不支持顺序消费 | 日志流、大数据管道 |
| RabbitMQ | 消息路由灵活 | 大消息量下性能不如 RocketMQ | 企业内部消息路由 |

支付回调核心诉求是**可靠性**（不丢消息）和**顺序性**（同一订单）而非高吞吐，RocketMQ 最匹配。

---

### 验收标准

- [ ] 回调接口 P99 响应时间 < 300ms（用 wrk 压测）
- [ ] 下游通知服务停机期间，回调消息堆积在 MQ 中，服务恢复后自动消费，不丢失
- [ ] 连续失败 5 次的消息进入死信队列并触发 Telegram 告警
- [ ] 重复投递的消息（MQ 至少一次语义）通过乐观锁幂等拦截，不触发重复充值

---

## P1-Task-4：强制对账定时任务

### 问题根因分析

**当前风险矩阵**：

```
amountCheck 字段（PayConfigChannel）：
  = 1  → 校验金额（±2% 上限，-50% 下限）
  = -1 → 完全跳过金额校验（任意金额触发成功）
```

任何能访问 admin 配置接口的账号，将 `amountCheck` 改为 `-1` 后，三方回调可用 `real_amount=0.01` 触发订单成功，实际支付金额与系统记录金额差额无任何捕获机制。

**账本缺口**：`bank_bookkeeping_entry` 表已建立，但代码中未找到在支付成功时同步写入的位置——账本数据与订单数据存在不一致风险，无法做资金核对。

---

### 实施步骤

**Step 1：确认并补全账本写入**

在 `PayNotifyService.processCallback()` 的订单状态更新事务内追加写入（与 Task-3 的 Consumer 内做）：

```java
@Transactional(rollbackFor = Exception.class)
public void processCallback(PayNotifyMessage msg) {
    // ... 现有逻辑：验签、金额校验、更新 order_info ...

    int rows = orderInfoMapper.updateById(order);  // 乐观锁
    if (rows == 0) return;  // 幂等跳过

    // 新增：同一事务内写账本，强一致
    bookkeepingEntryMapper.insert(
        BookkeepingEntry.builder()
            .orderId(order.getOrderId())
            .merchantId(order.getMerchantId())
            .platformNo(order.getPlatformNo())
            .requestAmount(order.getAmount())    // 请求金额（分）
            .realAmount(order.getRealAmount())   // 实收金额（分）
            .entryType(EntryType.PAY_IN)
            .entryTime(LocalDateTime.now())
            .build()
    );
}
```

关键：`@Transactional` 保证 `order_info` 和 `bank_bookkeeping_entry` 同时成功或同时回滚，消除账本黑洞。

**Step 2：`pay_platform_info` 表补充对账接口配置**

如果表中不存在以下字段，需 DDL 添加：

```sql
ALTER TABLE pay_platform_info
  ADD COLUMN reconcile_type TINYINT DEFAULT 0 COMMENT '对账方式 0=无 1=文件 2=API',
  ADD COLUMN reconcile_url VARCHAR(512) DEFAULT '' COMMENT '对账文件下载URL或API地址',
  ADD COLUMN reconcile_cron VARCHAR(64) DEFAULT '0 0 2 * * ?' COMMENT '对账执行时间';
```

**Step 3：选型 xxl-job（推荐）而不是 Spring @Scheduled**

```xml
<!-- admin-service / pom.xml -->
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.4.0</version>
</dependency>
```

选 xxl-job 而不是 `@Scheduled` 的原因：
- 多实例部署时 `@Scheduled` 会重复执行，xxl-job 保证单次
- xxl-job 提供执行日志、手动触发、失败告警，运维友好
- 已有 Nacos，xxl-job 可直接注册

**Step 4：对账任务核心逻辑**

```java
@XxlJob("dailyReconcileJob")
public void dailyReconcile() {
    LocalDate yesterday = LocalDate.now().minusDays(1);

    List<PayPlatformInfo> platforms = platformMapper.selectReconcileEnabled();
    for (PayPlatformInfo platform : platforms) {
        try {
            reconcileOnePlatform(platform, yesterday);
        } catch (Exception e) {
            log.error("[对账失败] platform={}", platform.getPlatformNo(), e);
            telegramBot.sendAlert("对账任务异常：" + platform.getPlatformNo());
        }
    }
}

private void reconcileOnePlatform(PayPlatformInfo platform, LocalDate date) {
    // 1. 拉取三方对账数据（文件或 API，按 reconcile_type 路由）
    List<ThirdPartyOrder> thirdOrders = reconcileClient.fetch(platform, date);

    // 2. 查询系统内当日订单
    List<OrderInfo> sysOrders = orderInfoMapper.selectByDateAndPlatform(date, platform.getPlatformNo());

    // 3. 差异比对
    ReconcileResult result = ReconcileComparator.compare(thirdOrders, sysOrders);

    // 4. 处理三类差异
    result.getLongPositions().forEach(o -> {
        // 长款：三方有 + 系统无 → 补单（需人工审核，先标记）
        orderBuildErrorMapper.insert(OrderBuildError.ofLongPosition(o));
    });
    result.getShortPositions().forEach(o -> {
        // 短款：系统有 + 三方无 → 挂起，等三方补充数据
        orderBuildErrorMapper.insert(OrderBuildError.ofShortPosition(o));
    });
    result.getAmountMismatches().forEach(pair -> {
        // 金额不符：记录差额
        orderBuildErrorMapper.insert(OrderBuildError.ofAmountMismatch(pair));
    });

    // 5. 汇总告警
    if (!result.isClean()) {
        telegramBot.sendAlert(String.format(
            "[对账] %s %s：长款%d笔 短款%d笔 金额不符%d笔",
            platform.getPlatformNo(), date,
            result.getLongPositions().size(),
            result.getShortPositions().size(),
            result.getAmountMismatches().size()
        ));
    }
}
```

---

### 理由分析：为什么对账是 P1 而不是 P2

金额校验可被 `amountCheck=-1` 绕过，这不是理论风险——任何拥有 admin 配置权限的内部人员或被盗账号均可触发。对账是唯一能事后发现此类操纵的机制。没有对账，资金损失可能数日后才被发现（发现途径：财务人工核账），且无法追溯具体订单。

---

### 验收标准

- [ ] 对账任务每日 T+1 02:00 自动执行，执行日志在 xxl-job 控制台可查
- [ ] 手动构造一笔金额不符的测试订单，执行对账后出现在 `order_build_error` 并收到 Telegram 告警
- [ ] `bank_bookkeeping_entry` 日终总金额与 `order_info` 日终成功金额一致（允许时区误差 ±1 分钟内的订单）
- [ ] 多实例部署时对账任务不重复执行（xxl-job 路由策略：第一个）

---

## P2-Task-5：渠道加权路由

### 问题根因分析

**现状定位方式**：
```bash
# 在 pay-service 中搜索渠道选择逻辑
grep -rn "PayConfigChannel\|selectChannel\|getChannel" pay-service/src/ --include="*.java"
```

`PayConfigChannel` 表的 `weight`/`share` 字段含义是"该渠道应承接的流量比例"，但若选择逻辑退化为 `LIMIT 1`（顺序取第一个），则：
- 权重配置无效，等同于单渠道
- 渠道故障时无法平滑切流（需改代码或改排序才能切换）
- 灰度发布新渠道无法控制流量比例

---

### 实施步骤

**Step 1：实现 `WeightedRandom` 工具类**

```java
// common-core/src/main/java/.../util/WeightedRandom.java
public class WeightedRandom {

    /**
     * 从列表中按 weight 字段随机选取一个元素
     * 时间复杂度 O(n)，支付渠道数量通常 < 20，足够
     */
    public static <T> T select(List<T> items, ToIntFunction<T> weightGetter) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("渠道列表为空");
        }
        int totalWeight = items.stream().mapToInt(weightGetter).sum();
        if (totalWeight <= 0) {
            // 所有渠道权重为 0（全部熔断）
            throw new NoAvailableChannelException("所有渠道不可用");
        }
        int rand = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (T item : items) {
            cumulative += weightGetter.applyAsInt(item);
            if (rand < cumulative) {
                return item;
            }
        }
        return items.get(items.size() - 1);  // 浮点误差兜底
    }
}
```

**Step 2：渠道列表 Redis 缓存（支持无重启切流）**

```java
// PayConfigChannelService.java
private static final String CHANNEL_CACHE_KEY = "pay:channel:enabled:%s";  // %s = configId
private static final long CACHE_TTL_SECONDS = 60;

public List<PayConfigChannel> getEnabledChannels(Long configId) {
    String cacheKey = String.format(CHANNEL_CACHE_KEY, configId);

    // 先查缓存
    List<PayConfigChannel> cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) return cached;

    // 缓存 miss → 查 DB（weight > 0 的渠道，按 weight 降序）
    List<PayConfigChannel> channels = channelMapper.selectEnabled(configId);

    // 写缓存，60s TTL
    redisTemplate.opsForValue().set(cacheKey, channels, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
    return channels;
}
```

`admin-service` 提供主动刷新接口（修改渠道配置后立即生效，不等 TTL 过期）：

```java
// AdminChannelController.java
@PostMapping("/admin/channel/{configId}/refresh-cache")
public void refreshChannelCache(@PathVariable Long configId) {
    String cacheKey = String.format("pay:channel:enabled:%s", configId);
    redisTemplate.delete(cacheKey);
    log.info("[渠道缓存] configId={} 缓存已刷新", configId);
}
```

**Step 3：熔断降级（连续失败自动下线渠道）**

```java
// PayChannelCircuitBreaker.java
private static final String FAIL_COUNT_KEY = "pay:channel:fail:%s";     // %s = channelId
private static final int CIRCUIT_THRESHOLD = 5;
private static final long HALF_OPEN_TTL = 30;  // 30s 后自动半开

public boolean isAvailable(Long channelId) {
    String key = String.format(FAIL_COUNT_KEY, channelId);
    Long failCount = redisTemplate.opsForValue().increment(key, 0);  // 读取不递增
    return failCount == null || failCount < CIRCUIT_THRESHOLD;
}

public void recordFailure(Long channelId) {
    String key = String.format(FAIL_COUNT_KEY, channelId);
    Long count = redisTemplate.opsForValue().increment(key);
    if (count == 1) {
        redisTemplate.expire(key, HALF_OPEN_TTL, TimeUnit.SECONDS);
    }
    if (count >= CIRCUIT_THRESHOLD) {
        telegramBot.sendAlert("渠道 " + channelId + " 连续失败 " + count + " 次，已熔断");
    }
}

public void recordSuccess(Long channelId) {
    redisTemplate.delete(String.format(FAIL_COUNT_KEY, channelId));  // 成功则重置
}
```

选渠道时过滤熔断中的渠道：

```java
List<PayConfigChannel> available = channels.stream()
    .filter(c -> circuitBreaker.isAvailable(c.getId()))
    .collect(Collectors.toList());
PayConfigChannel selected = WeightedRandom.select(available, PayConfigChannel::getWeight);
```

---

### 理由分析：TTL 为什么选 60s

- **太短（< 10s）**：每分钟 DB 查询次数激增，高峰期每秒数百次支付请求 = 数百次 DB 查询
- **太长（> 5min）**：修改权重后生效慢，灰度切流体验差
- **60s**：配合主动刷新接口，正常情况 60s 生效，紧急切流可立即刷新

---

### 验收标准

- [ ] 权重 [7:3] 的两渠道，1000 次路由结果在 65%~75% 区间（概率验证）
- [ ] 修改 DB 中 `weight` 后，60s 内新请求按新权重路由（不需重启）
- [ ] 点击 `/admin/channel/{id}/refresh-cache` 后立即生效
- [ ] 一个渠道连续失败 5 次后熔断，收到 Telegram 告警；30s 后自动半开
- [ ] 所有渠道熔断时返回明确错误（不静默失败），支付请求快速失败

---

## P2-Task-6：参数构建重构

### 问题根因分析

**现状定位**：`pay-service` → `PayReqService` → `extracted1()` ~ `extracted4()` 方法

这四个方法的问题模式：**一个方法做三件不相关的事**。典型反例：

```java
// extracted1() 的可能结构（推测，需对照实际代码）
private void extracted1(Map<String, Object> params, PayConfigInfo config, ...) {
    // 金额转换
    params.put("amount", order.getAmount() / 100);           // 元
    params.put("amount_fen", order.getAmount());              // 分
    params.put("amount_cent", order.getAmount() * 10);        // 厘

    // URL 编码（完全不同的关注点混在一起）
    params.put("notify_url", config.getNotifyUrl());
    params.put("notify_url_encoded", URLEncoder.encode(config.getNotifyUrl(), "UTF-8"));
    params.put("notify_url_base64", Base64.encode(config.getNotifyUrl()));

    // 时间戳（又一个不同关注点）
    params.put("timestamp", System.currentTimeMillis());
    params.put("timestamp_s", System.currentTimeMillis() / 1000);
    params.put("time_str", DateUtil.format(new Date(), "yyyyMMddHHmmss"));
}
```

这种结构导致：
1. 无法单测单个逻辑（测金额转换必须构造完整 PayConfigInfo 和订单对象）
2. 新增三方渠道时，不清楚哪些参数名是"必须提供"的，哪些是"这个渠道特有"的
3. Bug 修复时，修改金额逻辑可能意外影响 URL 编码逻辑

---

### 实施步骤

> **重构策略**：Strangler Fig（绞杀者模式）——新建类，逐个方法迁移，保持旧代码运行，全部迁移后删除旧方法。不做大爆炸式重写。

**Step 1：建立参数容器值对象**

```java
// pay-service/src/main/java/.../param/PayParamContext.java
@Builder
@Getter
public class PayParamContext {

    // 金额系列（单位：已转换好，直接取用）
    private final String amountYuan;        // "100.00"（元，两位小数）
    private final long amountFen;           // 10000（分）
    private final long amountLi;            // 100000（厘）
    private final String amountStr;         // "100"（元，无小数，部分渠道需要）

    // 回调 URL 系列（编码：已处理好，直接取用）
    private final String notifyUrlRaw;
    private final String notifyUrlEncoded;   // URLEncoder.encode
    private final String notifyUrlBase64;    // Base64
    private final String notifyUrlDouble;    // 双重 URLEncode

    // 时间戳系列
    private final long timestampMs;          // 毫秒
    private final long timestampS;           // 秒
    private final String timeYMDHMS;         // "20240101120000"
    private final String timeISO;            // "2024-01-01T12:00:00"

    // 商户凭证
    private final String appId;
    private final String appKey;
    private final String merchantNo;
    // privateKey 系列（解密后）
    private final String privateKey1;
    private final String privateKey2;
}
```

**Step 2：建立 `PayParamBuilder`，每个方法独立可测**

```java
// pay-service/src/main/java/.../service/PayParamBuilder.java
@Component
public class PayParamBuilder {

    public AmountSection buildAmounts(long amountFen) {
        Preconditions.checkArgument(amountFen > 0, "金额必须大于 0");
        return AmountSection.builder()
            .amountFen(amountFen)
            .amountYuan(new BigDecimal(amountFen).movePointLeft(2).toPlainString())
            .amountLi(amountFen * 10L)
            .amountStr(String.valueOf(amountFen / 100))
            .build();
    }

    public UrlSection buildUrls(String notifyUrl) {
        Objects.requireNonNull(notifyUrl, "notifyUrl 不能为空");
        try {
            return UrlSection.builder()
                .raw(notifyUrl)
                .encoded(URLEncoder.encode(notifyUrl, StandardCharsets.UTF_8))
                .base64(Base64.getEncoder().encodeToString(notifyUrl.getBytes(StandardCharsets.UTF_8)))
                .doubleEncoded(URLEncoder.encode(
                    URLEncoder.encode(notifyUrl, StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8))
                .build();
        } catch (Exception e) {
            throw new PayParamException("URL 编码失败: " + notifyUrl, e);
        }
    }

    public TimestampSection buildTimestamps(Instant now) {
        return TimestampSection.builder()
            .ms(now.toEpochMilli())
            .s(now.getEpochSecond())
            .ymdHms(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(LocalDateTime.ofInstant(now, ZoneOffset.UTC)))
            .iso(now.toString())
            .build();
    }

    public CredentialSection buildCredentials(MerchantInfo merchant) {
        return CredentialSection.builder()
            .appId(merchant.getAppId())
            .appKey(merchant.getAppKey())
            .merchantNo(merchant.getMerchantNo())
            .privateKey1(merchant.getPrivateKey1())  // TypeHandler 已解密
            .build();
    }

    /** 组合为完整 Context，供 IPayThirdRequest 使用 */
    public PayParamContext build(long amountFen, String notifyUrl,
                                  Instant now, MerchantInfo merchant) {
        AmountSection amounts = buildAmounts(amountFen);
        UrlSection urls = buildUrls(notifyUrl);
        TimestampSection timestamps = buildTimestamps(now);
        CredentialSection credentials = buildCredentials(merchant);

        return PayParamContext.builder()
            .amountYuan(amounts.getAmountYuan())
            .amountFen(amounts.getAmountFen())
            // ... 展开所有字段
            .notifyUrlRaw(urls.getRaw())
            .notifyUrlEncoded(urls.getEncoded())
            // ... 展开所有字段
            .build();
    }
}
```

**Step 3：`IPayThirdRequest` 实现迁移**

各三方渠道的请求实现，从直接操作 `Map<String,Object> params` 改为从 `PayParamContext` 取值：

```java
// 迁移前（紧耦合）
public void buildRequest(Map<String, Object> params, PayConfigInfo config, ...) {
    params.put("amount", params.get("amount_yuan"));  // 依赖上游塞好的 key
}

// 迁移后（明确依赖）
public void buildRequest(PayParamContext ctx, PayConfigInfo config) {
    return Map.of(
        "amount", ctx.getAmountYuan(),
        "notify_url", ctx.getNotifyUrlEncoded(),
        "timestamp", ctx.getTimestampS()
    );
}
```

**Step 4：删除 `extracted1~4`**

全部渠道迁移后，删除原方法，此时编译器会给出所有未迁移的调用点（零漏网之鱼）。

---

### 理由分析：为什么用 Strangler Fig 而不是一次性重写

一次性重写风险：56 个 View 组件（前端类比）或 N 个三方渠道实现同时修改，上线时若出现回归 bug，难以定位是哪个渠道的问题。

Strangler Fig 方案：每次只迁移一个渠道，在测试环境验证该渠道的支付链路完整通过，再合并。问题范围可精确定位到"刚迁移的那个渠道"。

---

### 验收标准

- [ ] `grep -rn "extracted[1-4]" pay-service/src/` 无输出
- [ ] `PayParamBuilder` 中每个 `buildXxx` 方法有单元测试，覆盖边界值（0 分、最大 Long、空 URL、特殊字符 URL）
- [ ] 新增三方渠道只需实现 `IPayThirdRequest`，不需要修改 `PayReqService`
- [ ] 所有现有渠道回归测试通过（金额、URL、时间戳参数与迁移前一致）

---

## P3-Task-7：前端架构演进

### 问题根因分析

**现状数据（基于代码扫描）**：

| 问题 | 具体位置 | 行数占比 |
|------|---------|---------|
| 56 个组件 import | App.vue L3~57 | 55 行 |
| 50 个 mock 数据 import | App.vue L59~118 | 60 行 |
| 认证状态 + 登录/退出逻辑 | App.vue L122~285 | 164 行 |
| Dashboard 轮询 + 数据处理 | App.vue L125~353 | 229 行 |
| 格式化工具函数 | App.vue L158~204 | 47 行 |
| 50 个 isXxxActive computed | App.vue L386~587 | 202 行 |
| handleMenuSelect 路由逻辑 | App.vue L588~603 | 16 行 |
| 50 个 v-else-if 渲染分支 | App.vue L631~753 | 123 行 |
| 样式 | App.vue L812~993 | 182 行 |
| **总计** | | **993 行** |

**核心问题**：单文件承担了路由引擎、认证系统、数据层、格式化库、模板引擎五个角色。任何新增页面需要在至少 4 个地方同时改动（加 import、加 mock import、加 isXxxActive computed、加 v-else-if 分支）。

---

### 实施步骤（渐进式，每步独立可合并）

> 每个 Step 完成后都是可工作状态，可单独上线。

**Step 1：提取格式化函数（风险最低，10 分钟）**

新建文件 `src/utils/formatters.js`，将 App.vue L158~204 的 4 个函数迁移过来：

```javascript
// src/utils/formatters.js

export const formatCurrency = (value) => {
  if (value === null || value === undefined) return '--'
  const num = Number(value)
  if (Number.isNaN(num)) return '--'
  return `¥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

export const formatCount = (value) => {
  if (value === null || value === undefined) return '--'
  const num = Number(value)
  if (Number.isNaN(num)) return '--'
  return num.toLocaleString('zh-CN')
}

export const formatPercentChange = (value) => {
  if (value === null || value === undefined) return '—'
  const num = Number(value)
  if (Number.isNaN(num)) return '—'
  return `${num > 0 ? '+' : ''}${num.toFixed(2)}%`
}

export const formatRateValue = (value) => {
  if (value === null || value === undefined) return '--'
  const num = Number(value)
  if (Number.isNaN(num)) return '--'
  return `${num.toFixed(2)}%`
}
```

App.vue 修改：
```javascript
// 删除 L158~204 的函数定义，改为 import
import { formatCurrency, formatCount, formatPercentChange, formatRateValue } from './utils/formatters'
```

**收益**：任意 View 组件可直接 import 使用，不需要通过 props 从 App.vue 传入。App.vue 减少 **~45 行**。

---

**Step 2：安装 Pinia，拆分 authStore**

```bash
npm install pinia
```

在 `src/main.js` 中注册：
```javascript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

const app = createApp(App)
app.use(createPinia())
app.mount('#app')
```

新建 `src/stores/auth.js`，迁移 App.vue L122~285（认证相关逻辑）：

```javascript
// src/stores/auth.js
import { defineStore } from 'pinia'
import { ref } from 'vue'
import http, { setAuthToken, getStoredToken } from '../services/http'

export const useAuthStore = defineStore('auth', () => {
  const isAuthenticated = ref(false)
  const authProfile = ref(null)

  // 迁移自 App.vue L254~259
  const bootstrapAuthState = () => {
    const stored = getStoredToken()
    if (stored) isAuthenticated.value = true
  }

  // 迁移自 App.vue L261~268
  const handleLoginSuccess = (payload) => {
    const token = payload?.token
    if (!token) return
    authProfile.value = payload?.profile ?? null
    setAuthToken(token)
    isAuthenticated.value = true
  }

  // 迁移自 App.vue L271~285
  const handleLogout = async () => {
    const token = getStoredToken()
    try {
      if (token) await http.post('/auth/admin/logout', { token })
    } catch (e) {
      console.error('Logout request failed', e)
    } finally {
      setAuthToken('')
      authProfile.value = null
      isAuthenticated.value = false
    }
  }

  return { isAuthenticated, authProfile, bootstrapAuthState, handleLoginSuccess, handleLogout }
})
```

App.vue 修改（删除 L122~285，改为）：
```javascript
import { useAuthStore } from './stores/auth'
const authStore = useAuthStore()
const { isAuthenticated, authProfile } = storeToRefs(authStore)
// onMounted: authStore.bootstrapAuthState()
// @success: authStore.handleLoginSuccess
// @logout: authStore.handleLogout
```

**收益**：App.vue 减少约 **170 行**，认证逻辑可在任意组件中访问，无需 props 透传。

---

**Step 3：拆分 dashboardStore**

新建 `src/stores/dashboard.js`，迁移 App.vue L125~353（Dashboard 数据与轮询）：

```javascript
// src/stores/dashboard.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http from '../services/http'
import { cloneDeep } from '../utils/helpers'
import { dashboardOverview as dashboardOverviewMock } from '../data/mock'
import { formatCurrency, formatCount, formatPercentChange, formatRateValue } from '../utils/formatters'

const SUMMARY_POLL_INTERVAL = 5 * 60 * 1000

export const useDashboardStore = defineStore('dashboard', () => {
  const dashboardOverviewData = ref(cloneDeep(dashboardOverviewMock))
  const homeMetrics = ref({ operatingCountries: [], activeChannelCount: 0, minuteLevelSla: '99.95%' })
  const homeMetricsLoading = ref(false)
  const homeMetricsError = ref('')
  const dashboardSummaryLoading = ref(false)
  const dashboardSummaryError = ref('')

  let pollTimer = null

  const fetchHomeMetrics = async () => { /* 迁移自 App.vue L301~321 */ }
  const fetchDashboardSummary = async () => { /* 迁移自 App.vue L323~343 */ }
  const startPolling = () => { /* 迁移自 App.vue L240~245 */ }
  const stopPolling = () => { /* 迁移自 App.vue L247~252 */ }

  return { dashboardOverviewData, homeMetrics, homeMetricsLoading, homeMetricsError,
           fetchHomeMetrics, fetchDashboardSummary, startPolling, stopPolling }
})
```

**收益**：App.vue 再减少约 **230 行**，当前 App.vue 已降至约 **550 行**。

---

**Step 4：安装 vue-router，消灭 50 个 computed + v-else-if 链**

```bash
npm install vue-router
```

新建 `src/router/index.js`——将 App.vue L386~753 的 50 个 computed + v-else-if 转为路由配置：

```javascript
// src/router/index.js
import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

// 懒加载：只有访问对应路由时才加载组件（优化首屏）
const routes = [
  { path: '/',         redirect: '/dashboard/overview' },
  { path: '/login',    component: () => import('../components/LoginPanel.vue'), meta: { public: true } },

  // Dashboard 模块
  { path: '/dashboard/overview',    component: () => import('../components/DashboardOverview.vue') },
  { path: '/dashboard/channel',     component: () => import('../components/DashboardChannelView.vue') },
  { path: '/dashboard/merchant',    component: () => import('../components/DashboardMerchantView.vue') },
  // ... 其余 47 个路由按同样模式

  // 银行模块
  { path: '/bank/suppliers',        component: () => import('../components/BankSuppliersView.vue') },
  // ...

  // 订单模块
  { path: '/orders/collection',     component: () => import('../components/OrdersCollectionView.vue') },
  // ...
]

const router = createRouter({
  history: createWebHashHistory(),  // Hash 模式：无需服务端配置
  routes,
})

// 路由守卫：替代 v-if="!isAuthenticated"
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isAuthenticated) {
    return '/login'
  }
})

export default router
```

`src/main.js` 注册路由：
```javascript
app.use(router)
```

App.vue 模板从 120 行 v-else-if 链简化为：
```html
<!-- App.vue template（删除所有 v-else-if，改为） -->
<main class="main-content">
  <RouterView />
</main>
```

同时，SidebarMenu 的菜单跳转改为 `router.push('/dashboard/overview')` 而非 `emit('select', { parentId, childId })`。

**收益**：App.vue 减少约 **400 行**（50 computed + 全部组件 import + v-else-if 链）。

**附加收益**：
- 浏览器刷新保留当前页面（现在刷新回首页）
- URL 可复制分享给同事
- 浏览器前进/后退按钮生效
- 路由组件按需加载，首屏体积减小

---

**Step 5：mock.js 按模块拆分**

将 105KB 的 `src/data/mock.js` 拆分为按模块的文件：

```
src/data/
  mock/
    dashboard.js    # dashboardOverview, dashboardChannelView, ...
    bank.js         # bankSuppliersView, bankAccountsView, ...
    orders.js       # ordersCollectionView, ordersPayoutView, ...
    merchants.js    # merchantsListView, merchantsBoardView
    payments.js     # paymentsEntityView, paymentsChannelView, ...
    finance.js      # financeBillingView, ...
    system.js       # systemBillingView, systemUsersView, ...
    ops.js          # opsSmsView, opsVpsView, ...
    common.js       # menuItems, channels, user, alerts, ...
  index.js          # 仅在 DEV 环境 re-export，生产环境空 export
```

各路由组件改为按需 import 自己模块的 mock（而非全部从顶层 import）：

```javascript
// DashboardOverview.vue（组件内）
import { dashboardOverview } from '../data/mock/dashboard'
// 仅加载 dashboard.js，不加载其他 100KB mock 数据
```

生产环境自动 tree-shake：
```javascript
// src/data/index.js
export * from import.meta.env.DEV ? './mock/index' : './empty'
```

**收益**：生产包体积减少 **≥ 80KB**（mock.js 不被打包进生产），开发环境不受影响。

---

**最终状态预测**：

| 阶段 | App.vue 行数 | 说明 |
|------|-------------|------|
| 当前 | 993 行 | 现状 |
| Step 1 完成 | ~948 行 | 提取格式化函数 |
| Step 2 完成 | ~778 行 | 拆 authStore |
| Step 3 完成 | ~550 行 | 拆 dashboardStore |
| Step 4 完成 | ~150 行 | 引入 vue-router |
| Step 5 完成 | ~100 行 | 仅剩 layout 骨架 + 样式 |

---

### 理由分析：为什么分 5 步而不是一次性引入

如果同时引入 Pinia + vue-router + 拆 mock.js，一旦出 bug 难以定位是哪部分引入的问题。分步骤每次只引入一个新概念，每步完成后在浏览器验证所有页面可正常访问，问题范围清晰。

### 为什么用 Hash 路由而不是 HTML5 History 路由

Hash 路由（`#/dashboard/overview`）无需服务端配置——当前 Nginx/网关直接 serve `index.html` 即可，不需要配置 `try_files $uri /index.html`。History 路由更美观，但需要运维配合，风险更高，等基础设施稳定后再迁移。

---

### 验收标准

- [ ] Step 2 完成后：`grep -n "isAuthenticated\|authProfile\|handleLogin\|handleLogout" src/App.vue` 无输出
- [ ] Step 3 完成后：`grep -n "fetchDashboardSummary\|pollTimer\|SUMMARY_POLL" src/App.vue` 无输出
- [ ] Step 4 完成后：`grep -n "isXxxActive\|v-else-if" src/App.vue` 无输出；浏览器刷新保留页面；前进/后退生效
- [ ] Step 5 完成后：`npm run build` 产物中 `.js` 文件不包含 mock 数据（搜索 mock 特有字符串）
- [ ] 全流程：所有 56 个页面路由可正常访问，Dashboard 轮询在登录后启动、退出后停止

---

## P3-Task-8：可观测性

### 问题根因分析

**当前排障路径**（痛苦的）：
```
用户反馈：某笔订单支付失败
  → 登录各服务机器，grep 日志（每台机器单独 grep）
  → 无 traceId，无法关联 gateway → pay-service → 下游
  → 无 orderId MDC，只能靠时间窗口猜
  → 平均排障时间：30~60 分钟
```

**改造后排障路径**：
```
用户反馈：某笔订单支付失败，orderId=xxx
  → Kibana 搜索：orderId:xxx
  → 找到 traceId=yyy
  → SkyWalking 搜索 traceId=yyy
  → 完整调用链：gateway(2ms) → pay-svc(45ms) → auth-svc(3ms) → db(12ms)
  → 定位到 pay-svc 的具体方法耗时异常
  → 平均排障时间：< 5 分钟
```

---

### 实施步骤

**Step 1：SkyWalking Agent 接入（零代码侵入）**

下载 SkyWalking Java Agent，在各服务 JVM 启动参数中加入：

```bash
-javaagent:/opt/skywalking/skywalking-agent.jar
-Dskywalking.agent.service_name=pay-service
-Dskywalking.collector.backend_service=skywalking-oap:11800
```

Docker/K8s 方式（推荐，镜像统一管理）：
```dockerfile
FROM openjdk:17-slim
COPY --from=skywalking-agent /skywalking-agent /skywalking-agent
ENV JAVA_TOOL_OPTIONS="-javaagent:/skywalking-agent/skywalking-agent.jar"
```

SkyWalking 自动追踪：HTTP 请求、MySQL、Redis、Nacos、RocketMQ（TaskList-3 引入后自动覆盖）。

**Step 2：统一日志 MDC 字段规范**

在 Gateway 的 Filter 中提取/生成 traceId 注入 MDC：

```java
// gateway: TraceIdGatewayFilter.java
@Component
public class TraceIdGatewayFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders()
            .getFirst("X-Trace-Id");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        // 透传给下游
        ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header("X-Trace-Id", traceId).build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }
}
```

各微服务的日志 `logback-spring.xml`：

```xml
<pattern>%d{ISO8601} [%thread] %-5level [traceId=%X{traceId}] [orderId=%X{orderId}] [platformNo=%X{platformNo}] %logger{36} - %msg%n</pattern>
```

业务代码在关键入口处设置 MDC：

```java
// PayNotifyController.java
MDC.put("orderId", orderId);
MDC.put("platformNo", platformNo);
try {
    // 处理逻辑
} finally {
    MDC.clear();
}
```

**Step 3：Prometheus 自定义指标（业务级）**

```java
// PayMetrics.java（@Component）
@Component
public class PayMetrics {
    // 支付成功计数器（按平台分组）
    private final Counter successCounter = Counter.build()
        .name("pay_success_total")
        .help("支付成功次数")
        .labelNames("platform_no", "channel_id")
        .register();

    // 回调处理耗时直方图
    private final Histogram callbackDuration = Histogram.build()
        .name("pay_callback_duration_seconds")
        .help("回调处理耗时（秒）")
        .buckets(0.05, 0.1, 0.3, 0.5, 1.0, 3.0)
        .labelNames("platform_no")
        .register();

    public void recordSuccess(String platformNo, String channelId) {
        successCounter.labels(platformNo, channelId).inc();
    }

    public Timer startCallbackTimer(String platformNo) {
        return callbackDuration.labels(platformNo).startTimer();
    }
}
```

**Step 4：Grafana 看板配置**

推荐基础看板（从官方 Spring Boot Dashboard ID: 12900 导入后修改）：

自定义业务面板：
```
Panel 1：支付成功率（折线图）
  PromQL: rate(pay_success_total[5m]) / rate(pay_request_total[5m]) * 100
  告警：< 95% 时 Telegram 通知

Panel 2：回调处理 P95 延迟
  PromQL: histogram_quantile(0.95, rate(pay_callback_duration_seconds_bucket[5m]))
  告警：> 1s 时告警

Panel 3：各渠道成功率
  PromQL: sum by (platform_no) (rate(pay_success_total[5m]))
  可视化：Heatmap，快速发现异常渠道

Panel 4：熔断渠道数量
  PromQL: pay_circuit_breaker_open_total
  告警：> 0 时立即告警
```

---

### 理由分析：为什么选 SkyWalking 而不是 Zipkin/Jaeger

| 方案 | 优点 | 缺点 |
|------|------|------|
| **SkyWalking（推荐）** | Java Agent 零代码侵入；对 Spring Cloud、MyBatis、Redis、RocketMQ 均有开箱即用支持；UI 完善 | 需部署 OAP + UI |
| Zipkin | 轻量，Spring Cloud Sleuth 原生集成 | 无 Java Agent，需代码 import；功能相对简单 |
| Jaeger | CNCF 标准，OpenTelemetry 兼容 | 配置复杂；Java 生态支持不如 SkyWalking 完善 |

已有 Spring Cloud 技术栈，SkyWalking 的开箱支持范围最广，接入成本最低。

---

### 验收标准

- [ ] 发起一笔测试支付，在 SkyWalking UI 中能看到完整调用链（gateway → pay-svc，含 DB/Redis span）
- [ ] Kibana 搜索 `orderId:TEST_ORDER_001` 能找到该订单的完整日志，含 traceId
- [ ] Grafana 看板实时展示支付成功率，数据 5s 内刷新
- [ ] 人为触发渠道失败（临时改错 IP），Grafana 告警在 1 分钟内发出

---

## 执行路线图

```
Week 1
  Day 1~2: P0-Task-2（密钥加密）← 先做，历史明文需要 rotate
  Day 2~3: P0-Task-1（幂等锁）  ← 并行，互不依赖

Week 2
  Day 1~3: P1-Task-3（MQ 异步化）← 依赖 Task-1 完成
  Day 4~5: P1-Task-4（对账任务）← 并行推进

Week 3
  Day 1~2: P2-Task-5（渠道加权路由）
  Day 3~5: P2-Task-6（参数构建重构）

Week 4（可与 Week 3 并行，不同团队）
  P3-Task-7 Step 1~3（前端 Pinia）
  P3-Task-8（SkyWalking + Prometheus 搭建）

Week 5
  P3-Task-7 Step 4~5（vue-router + mock 拆分）
  P3-Task-8（Grafana 看板配置）
```

> **原则**：每项任务建独立分支，合并前必须通过集成回归（重点关注：回调幂等、金额计算、对账数字）。
