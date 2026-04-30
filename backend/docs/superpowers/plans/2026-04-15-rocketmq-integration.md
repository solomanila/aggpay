# RocketMQ 整合实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 pay-service 中整合 RocketMQ，替换现有的 Redis List (`pay_success_list`) 消息队列，实现支付成功/退款状态变更的可靠异步通知。

**Architecture:** `PayNotifyService.endStep()` 支付成功或退款状态更新后，向 RocketMQ Topic `pay_order_success` 发送消息；新增 `PaySuccessConsumer` 监听该 Topic，调用已有的 `PayPushPanService.pushSuccessOrderInfo()` 完成后续推送。改动仅在 pay-service 内部，Feign 调用 user-service 开通 VIP 的逻辑保持不变。

**Tech Stack:** Spring Boot 2.7.14, Spring Cloud Alibaba 2021.0.4.0, rocketmq-spring-boot-starter 2.2.3, RocketMQ 4.x/5.x NameServer

---

## 文件清单

| 操作 | 文件路径 | 说明 |
|---|---|---|
| 修改 | `backend/pom.xml` | 新增 `rocketmq.version` 属性 |
| 修改 | `backend/pay-service/pom.xml` | 新增 `rocketmq-spring-boot-starter` 依赖 |
| 修改 | `backend/pay-service/src/main/resources/application.yml` | 新增 rocketmq 配置 |
| 修改 | `backend/pay-service/src/main/resources/application-prod.yml` | 新增生产环境 rocketmq.name-server |
| 新建 | `backend/pay-service/src/main/java/com/letsvpn/pay/dto/PaySuccessMessage.java` | 消息体 DTO |
| 新建 | `backend/pay-service/src/main/java/com/letsvpn/pay/task/PaySuccessConsumer.java` | MQ 消费者 |
| 修改 | `backend/pay-service/src/main/java/com/letsvpn/pay/service/core/PayNotifyService.java` | 2 处 leftPush 替换为 MQ 发送 |

---

## Task 1: 添加 Maven 依赖

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/pay-service/pom.xml`

- [ ] **Step 1: 在根 pom.xml 的 `<properties>` 中添加 rocketmq 版本号**

  打开 `backend/pom.xml`，在 `<properties>` 块中，紧接在 `<jjwt.version>0.11.5</jjwt.version>` 后面添加一行：

  ```xml
  <rocketmq.version>2.2.3</rocketmq.version>
  ```

  添加后 `<properties>` 末尾应为：
  ```xml
      <jjwt.version>0.11.5</jjwt.version>
      <rocketmq.version>2.2.3</rocketmq.version>
  </properties>
  ```

- [ ] **Step 2: 在 pay-service/pom.xml 的 `<dependencies>` 中添加 starter**

  打开 `backend/pay-service/pom.xml`，在 `<dependencies>` 块末尾（最后一个 `</dependency>` 后、`</dependencies>` 前）添加：

  ```xml
  <dependency>
      <groupId>org.apache.rocketmq</groupId>
      <artifactId>rocketmq-spring-boot-starter</artifactId>
      <version>${rocketmq.version}</version>
  </dependency>
  ```

- [ ] **Step 3: 验证 Maven 依赖能解析**

  在 `backend/` 目录下执行：
  ```bash
  mvn dependency:resolve -pl pay-service -am -q
  ```
  预期输出：无 `ERROR`，命令正常退出（exit code 0）。

- [ ] **Step 4: 提交**

  ```bash
  git add backend/pom.xml backend/pay-service/pom.xml
  git commit -m "chore(pay-service): add rocketmq-spring-boot-starter 2.2.3"
  ```

---

## Task 2: 添加 RocketMQ 配置

**Files:**
- Modify: `backend/pay-service/src/main/resources/application.yml`
- Modify: `backend/pay-service/src/main/resources/application-prod.yml`

- [ ] **Step 1: 在 application.yml 末尾追加 rocketmq 配置**

  打开 `backend/pay-service/src/main/resources/application.yml`，在文件末尾追加：

  ```yaml
  rocketmq:
    name-server: 127.0.0.1:9876
    producer:
      group: pay-service-producer-group
      send-message-timeout: 3000
      retry-times-when-send-failed: 2
  ```

- [ ] **Step 2: 在 application-prod.yml 末尾追加生产环境 nameserver**

  打开 `backend/pay-service/src/main/resources/application-prod.yml`，在文件末尾追加：

  ```yaml
  rocketmq:
    name-server: ${ROCKETMQ_NAME_SERVER:127.0.0.1:9876}
    producer:
      group: pay-service-producer-group
      send-message-timeout: 3000
      retry-times-when-send-failed: 2
  ```

  > 生产部署时通过环境变量 `ROCKETMQ_NAME_SERVER` 注入真实地址（如 `rocketmq-namesrv:9876`），不配置则回退到本地。

- [ ] **Step 3: 提交**

  ```bash
  git add backend/pay-service/src/main/resources/application.yml \
          backend/pay-service/src/main/resources/application-prod.yml
  git commit -m "chore(pay-service): add rocketmq configuration"
  ```

---

## Task 3: 创建消息体 DTO

**Files:**
- Create: `backend/pay-service/src/main/java/com/letsvpn/pay/dto/PaySuccessMessage.java`

- [ ] **Step 1: 新建 PaySuccessMessage.java**

  创建文件 `backend/pay-service/src/main/java/com/letsvpn/pay/dto/PaySuccessMessage.java`，内容如下：

  ```java
  package com.letsvpn.pay.dto;

  import lombok.AllArgsConstructor;
  import lombok.Builder;
  import lombok.Data;
  import lombok.NoArgsConstructor;

  /**
   * RocketMQ 支付成功/退款状态变更消息体
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public class PaySuccessMessage {

      /** 数据库主键 id (order_info.id) */
      private Long orderId;

      /** 业务订单号 (order_info.order_id) */
      private String orderNo;

      /**
       * 订单状态：
       * 1=支付成功, 6=待退款, 7=退款中, 8=退款成功, 9=退款失败
       */
      private Integer status;

      /** 消息发送时间戳（毫秒），用于辅助日志排查 */
      private Long timestamp;
  }
  ```

- [ ] **Step 2: 验证编译通过**

  ```bash
  mvn compile -pl pay-service -am -q
  ```
  预期：无 `ERROR`，exit code 0。

- [ ] **Step 3: 提交**

  ```bash
  git add backend/pay-service/src/main/java/com/letsvpn/pay/dto/PaySuccessMessage.java
  git commit -m "feat(pay-service): add PaySuccessMessage DTO for RocketMQ"
  ```

---

## Task 4: 创建 MQ 消费者

**Files:**
- Create: `backend/pay-service/src/main/java/com/letsvpn/pay/task/PaySuccessConsumer.java`

- [ ] **Step 1: 新建 PaySuccessConsumer.java**

  创建文件 `backend/pay-service/src/main/java/com/letsvpn/pay/task/PaySuccessConsumer.java`，内容如下：

  ```java
  package com.letsvpn.pay.task;

  import com.letsvpn.pay.dto.PaySuccessMessage;
  import com.letsvpn.pay.service.core.PayPushPanService;
  import lombok.extern.slf4j.Slf4j;
  import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
  import org.apache.rocketmq.spring.core.RocketMQListener;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.stereotype.Component;

  /**
   * 支付成功/退款状态变更 MQ 消费者
   *
   * <p>消费 Topic: pay_order_success，Tag: PAY_SUCCESS || REFUND_UPDATE
   * <p>幂等保障：PayPushPanService._pushSuccessOrderInfo 内部通过 noticeStatus==100 判断，重复消费安全。
   */
  @Slf4j
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
          log.info("MQ consume pay_order_success: orderId={}, orderNo={}, status={}",
                  message.getOrderId(), message.getOrderNo(), message.getStatus());
          payPushPanService.pushSuccessOrderInfo(message.getOrderId());
      }
  }
  ```

- [ ] **Step 2: 验证编译通过**

  ```bash
  mvn compile -pl pay-service -am -q
  ```
  预期：无 `ERROR`，exit code 0。

- [ ] **Step 3: 提交**

  ```bash
  git add backend/pay-service/src/main/java/com/letsvpn/pay/task/PaySuccessConsumer.java
  git commit -m "feat(pay-service): add PaySuccessConsumer for RocketMQ"
  ```

---

## Task 5: 改造生产者（PayNotifyService）

**Files:**
- Modify: `backend/pay-service/src/main/java/com/letsvpn/pay/service/core/PayNotifyService.java`

**背景：** `endStep()` 方法中有两处 `redisService.leftPush(PayConstant.pay_success_list, id.toString())`：
- 第一处在约第 440 行：支付成功分支（更新 status=1 后）
- 第二处在约第 484 行：退款状态变更分支（status=6/7/8/9）

- [ ] **Step 1: 注入 RocketMQTemplate**

  在 `PayNotifyService` 类的字段注入区（现有 `@Autowired` 字段之后）添加：

  ```java
  @Autowired
  private org.apache.rocketmq.spring.core.RocketMQTemplate rocketMQTemplate;
  ```

  同时在文件顶部 import 区添加（如果 IDE 未自动导入）：
  ```java
  import com.letsvpn.pay.dto.PaySuccessMessage;
  import org.apache.rocketmq.spring.core.RocketMQTemplate;
  ```

- [ ] **Step 2: 替换第一处 leftPush（支付成功分支，约第 440 行）**

  找到以下代码块（位于 `if (StrUtil.isNotEmpty(successCode) && successCode.equals(success) && order.getStatus() != 8)` 分支内）：

  ```java
  int a = orderInfoService.updateByPrimaryKey(record);
  if (a > 0) {
      Long i = redisService.leftPush(PayConstant.pay_success_list, id.toString());
      log.info("successResult:orderId:{}, id:{}, a:{}, i:{}", orderId, id, a, i);
  }
  ```

  替换为：

  ```java
  int a = orderInfoService.updateByPrimaryKey(record);
  if (a > 0) {
      try {
          rocketMQTemplate.convertAndSend(
              "pay_order_success:PAY_SUCCESS",
              PaySuccessMessage.builder()
                  .orderId(id)
                  .orderNo(orderId)
                  .status(1)
                  .timestamp(System.currentTimeMillis())
                  .build()
          );
          log.info("MQ send PAY_SUCCESS: orderId={}, id={}", orderId, id);
      } catch (Exception e) {
          log.error("MQ send PAY_SUCCESS failed, orderId={}, id={}, err={}", orderId, id, e.getMessage(), e);
          orderBuildErrorService.add(0, 0, 0L, "MQ发送失败", orderId, "PAY_SUCCESS MQ发送异常:" + e.getMessage(), "PayNotifyService");
      }
  }
  ```

- [ ] **Step 3: 替换第二处 leftPush（退款状态变更分支，约第 484 行）**

  找到以下代码块（位于 `else if (StrUtil.isNotEmpty(refundCode) && StrUtil.isNotEmpty(refund))` 分支内）：

  ```java
  int a = orderInfoService.updateByPrimaryKey(record);
  if (a > 0) {
      Long i = redisService.leftPush(PayConstant.pay_success_list, id.toString());
      log.info("successResult:orderId:{}, id:{}, a:{}, i:{}", orderId, id, a, i);
  }
  return successResult;
  ```

  替换为：

  ```java
  int a = orderInfoService.updateByPrimaryKey(record);
  if (a > 0) {
      try {
          rocketMQTemplate.convertAndSend(
              "pay_order_success:REFUND_UPDATE",
              PaySuccessMessage.builder()
                  .orderId(id)
                  .orderNo(orderId)
                  .status(record.getStatus())
                  .timestamp(System.currentTimeMillis())
                  .build()
          );
          log.info("MQ send REFUND_UPDATE: orderId={}, id={}, status={}", orderId, id, record.getStatus());
      } catch (Exception e) {
          log.error("MQ send REFUND_UPDATE failed, orderId={}, id={}, err={}", orderId, id, e.getMessage(), e);
          orderBuildErrorService.add(0, 0, 0L, "MQ发送失败", orderId, "REFUND_UPDATE MQ发送异常:" + e.getMessage(), "PayNotifyService");
      }
  }
  return successResult;
  ```

- [ ] **Step 4: 编译验证**

  ```bash
  mvn compile -pl pay-service -am -q
  ```
  预期：无 `ERROR`，exit code 0。

- [ ] **Step 5: 提交**

  ```bash
  git add backend/pay-service/src/main/java/com/letsvpn/pay/service/core/PayNotifyService.java
  git commit -m "feat(pay-service): replace Redis List with RocketMQ in PayNotifyService"
  ```

---

## Task 6: 整体构建验证

**Files:** 无新增，验证所有模块可正常构建。

- [ ] **Step 1: 全量构建**

  ```bash
  cd backend && mvn clean package -DskipTests -q
  ```
  预期：`BUILD SUCCESS`，无 `ERROR`。

- [ ] **Step 2: 本地启动冒烟验证（需本地有 RocketMQ NameServer 运行）**

  若本地有 RocketMQ（`127.0.0.1:9876`），启动 pay-service：
  ```bash
  java -jar pay-service/target/pay-service-1.0-SNAPSHOT.jar
  ```
  观察启动日志，确认出现：
  ```
  RocketMQ connect to name server success
  ```
  以及消费者注册日志：
  ```
  pay-success-consumer-group ... subscribe pay_order_success
  ```

  若本地无 RocketMQ，启动时会打印连接错误日志，但应用仍可启动（RocketMQ 连接失败不阻塞 Spring 上下文）——确认启动后 HTTP 端口（8084）正常响应即可。

- [ ] **Step 3: 提交最终整合 commit**

  ```bash
  git add .
  git commit -m "feat(pay-service): integrate RocketMQ to replace Redis List queue

  - Topic: pay_order_success (PAY_SUCCESS / REFUND_UPDATE tags)
  - Consumer group: pay-success-consumer-group
  - Fallback: Redis List code retained but unused
  - Error handling: MQ send failure logged to order_build_error, does not affect callback response"
  ```

---

## 验收标准

1. `mvn clean package -DskipTests` 全量构建成功
2. pay-service 启动后无致命错误（连接 RocketMQ 失败为 WARN，不影响启动）
3. 模拟支付回调请求后，RocketMQ 控制台（或日志）中可看到 `pay_order_success` Topic 有消息入队
4. 消费者日志打印 `MQ consume pay_order_success: orderId=xxx`
5. 原 `redisService.leftPush(PayConstant.pay_success_list, ...)` 代码不再出现在主流程中
