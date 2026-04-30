# RocketMQ 整合设计文档

**日期：** 2026-04-15  
**项目：** lets-vpn-backend (Spring Cloud Alibaba 支付系统)  
**范围：** pay-service 内部，替换 Redis List 消息队列

---

## 背景

当前系统在支付回调成功后，通过 `redisService.leftPush(PayConstant.pay_success_list, id)` 将订单 ID 写入 Redis List，再由定时任务消费推送到上游平台。这种方式存在以下问题：

- Redis 重启或网络抖动时消息丢失
- 没有重试机制，推送失败只能依赖定时补偿
- 没有死信队列，失败消息难以追踪

引入 RocketMQ 解决以上问题，同时保留 Redis List 代码作为降级备份。

---

## 方案选择

采用 **方案 A：局部替换** — 仅在 `pay-service` 内部用 RocketMQ 替换 Redis List，不跨服务。Feign 调用 user-service 开通 VIP 保持不变。

---

## 依赖版本

| 依赖 | 版本 | 说明 |
|---|---|---|
| `rocketmq-spring-boot-starter` | 2.2.3 | 兼容 Spring Boot 2.7.x |
| Spring Cloud Alibaba | 2021.0.4.0 | 已有，不变 |

在根 `pom.xml` 的 `<properties>` 中新增：
```xml
<rocketmq.version>2.2.3</rocketmq.version>
```

在 `pay-service/pom.xml` 的 `<dependencies>` 中新增：
```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>${rocketmq.version}</version>
</dependency>
```

---

## Topic 设计

| Topic | Tag | 生产者 | 消费者 | 说明 |
|---|---|---|---|---|
| `pay_order_success` | `PAY_SUCCESS` | `PayNotifyService` | `PaySuccessConsumer` | 支付成功 |
| `pay_order_success` | `REFUND_UPDATE` | `PayNotifyService` | `PaySuccessConsumer` | 退款状态变更 |

- **消费组：** `pay-success-consumer-group`
- **死信 Topic：** `%DLQ%pay-success-consumer-group`（自动生成）
- **消费模式：** 集群消费（默认），保证每条消息只被一个实例消费

---

## 消息体

新建 `src/main/java/com/letsvpn/pay/dto/PaySuccessMessage.java`：

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaySuccessMessage {
    private Long orderId;      // 数据库主键 id
    private String orderNo;    // 业务订单号
    private Integer status;    // 1=支付成功, 6/7/8/9=退款状态
    private Long timestamp;    // 发送时间戳
}
```

---

## 生产者改动

**文件：** `PayNotifyService.java` → `endStep()` 方法

两处 `redisService.leftPush(PayConstant.pay_success_list, id.toString())` 替换：

**支付成功分支（status=1）：**
```java
// 替换前
Long i = redisService.leftPush(PayConstant.pay_success_list, id.toString());

// 替换后
rocketMQTemplate.convertAndSend(
    "pay_order_success:PAY_SUCCESS",
    PaySuccessMessage.builder()
        .orderId(id)
        .orderNo(orderId)
        .status(1)
        .timestamp(System.currentTimeMillis())
        .build()
);
```

**退款状态分支（status=6/7/8/9）：**
```java
// 替换前
Long i = redisService.leftPush(PayConstant.pay_success_list, id.toString());

// 替换后
rocketMQTemplate.convertAndSend(
    "pay_order_success:REFUND_UPDATE",
    PaySuccessMessage.builder()
        .orderId(id)
        .orderNo(orderId)
        .status(record.getStatus())
        .timestamp(System.currentTimeMillis())
        .build()
);
```

`PayNotifyService` 中注入 `RocketMQTemplate`：
```java
@Autowired
private RocketMQTemplate rocketMQTemplate;
```

---

## 消费者

新建 `src/main/java/com/letsvpn/pay/task/PaySuccessConsumer.java`：

```java
@Component
@RocketMQMessageListener(
    topic = "pay_order_success",
    selectorExpression = "PAY_SUCCESS || REFUND_UPDATE",
    consumerGroup = "pay-success-consumer-group"
)
public class PaySuccessConsumer implements RocketMQListener<PaySuccessMessage> {

    @Autowired
    private PayPushPanService payPushPanService;

    @Override
    public void onMessage(PaySuccessMessage message) {
        payPushPanService.pushSuccessOrderInfo(message.getOrderId());
    }
}
```

**幂等说明：** `pushSuccessOrderInfo` 内部调用 `_pushSuccessOrderInfo`，该方法已有 `noticeStatus == 100` 判断，重复消费安全。

---

## 配置

**`pay-service/src/main/resources/application.yml` 新增：**
```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: pay-service-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
```

**`pay-service/src/main/resources/application-prod.yml` 新增：**
```yaml
rocketmq:
  name-server: ${ROCKETMQ_NAME_SERVER:127.0.0.1:9876}
```

---

## 降级策略

- `PayConstant.pay_success_list` 常量保留不删除
- `PayPushPanService._extracted()` 方法保留（当前已注释），紧急时可还原
- 若 MQ 发送抛出异常，建议 `try-catch` 后记录 `orderBuildErrorService`，不影响回调主流程返回

---

## 改动文件清单

| 文件 | 操作 |
|---|---|
| `backend/pom.xml` | 新增 `<rocketmq.version>2.2.3</rocketmq.version>` |
| `pay-service/pom.xml` | 新增 `rocketmq-spring-boot-starter` 依赖 |
| `pay-service/src/main/resources/application.yml` | 新增 `rocketmq` 配置块 |
| `pay-service/src/main/resources/application-prod.yml` | 新增生产环境 `rocketmq.name-server` |
| `com/letsvpn/pay/dto/PaySuccessMessage.java` | **新建** 消息体 DTO |
| `com/letsvpn/pay/task/PaySuccessConsumer.java` | **新建** MQ 消费者 |
| `com/letsvpn/pay/service/core/PayNotifyService.java` | 2 处 `leftPush` 替换为 MQ 发送 |

---

## 不在本次范围内

- user-service 订阅 MQ 消息（VIP 激活保持 Feign 同步调用）
- 事务消息（当前幂等机制已足够）
- RocketMQ Dashboard / 监控接入
