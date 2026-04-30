# 通道利润日统计 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 每天凌晨 1 点自动统计前一天各通道成交额与通道费收入，写入 admin schema 统计表，并提供分页查询 API。

**Architecture:** pay-service 新增按 (channel_id, platform_id) 分组的查询接口；admin-service 定时拉取数据后结合本地 merchant_channel_config.fee_rate 计算通道费收入，upsert 到 channel_profit_stat 表；admin-service 同时暴露 CRUD 查询接口给前端。

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, Lombok, Spring @Scheduled, OpenFeign, MySQL (admin schema)

---

## File Map

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/common-core/src/main/java/com/letsvpn/common/core/dto/ChannelPlatformStatDTO.java` | 新增 | pay-service 按 (channel_id, platform_id) 返回的 DTO |
| `backend/pay-service/src/main/java/com/letsvpn/pay/mapper/ext/ExtChannelProfitStatMapper.java` | 修改 | 新增 `queryDailyStatsByPlatform` 方法 |
| `backend/pay-service/src/main/java/com/letsvpn/pay/service/core/DashboardMetricsService.java` | 修改 | 新增 `getDailyChannelPlatformStats` 方法 |
| `backend/pay-service/src/main/java/com/letsvpn/pay/controller/DashboardMetricsController.java` | 修改 | 新增 GET `/dashboard/dailyChannelPlatformStats` 接口 |
| `backend/admin-service/src/main/java/com/letsvpn/admin/client/PayServiceClient.java` | 修改 | 新增 Feign 方法 `getDailyChannelPlatformStats` |
| `backend/admin-service/src/main/java/com/letsvpn/admin/service/PayServiceFacade.java` | 修改 | 新增 `fetchDailyChannelPlatformStats` 方法 |
| `backend/admin-service/src/main/java/com/letsvpn/admin/entity/ChannelProfitStat.java` | 新增 | MyBatis-Plus 实体，对应 channel_profit_stat 表 |
| `backend/admin-service/src/main/java/com/letsvpn/admin/mapper/ChannelProfitStatMapper.java` | 新增 | extends BaseMapper |
| `backend/admin-service/src/main/java/com/letsvpn/admin/service/ChannelProfitStatService.java` | 新增 | 查询逻辑 + upsert 逻辑 |
| `backend/admin-service/src/main/java/com/letsvpn/admin/controller/ChannelProfitStatController.java` | 新增 | GET /admin/channel-profit/list |
| `backend/admin-service/src/main/java/com/letsvpn/admin/scheduler/ChannelProfitStatScheduler.java` | 新增 | @Scheduled cron 每天 01:00 |
| `backend/docs/channel_profit_stat.sql` | 新增 | 建表 SQL |

---

## Task 1: 新增 ChannelPlatformStatDTO（common-core）

**Files:**
- Create: `backend/common-core/src/main/java/com/letsvpn/common/core/dto/ChannelPlatformStatDTO.java`

- [ ] **Step 1: 创建 DTO 文件**

```java
package com.letsvpn.common.core.dto;

import lombok.Data;
import java.math.BigDecimal;

/** pay-service 按 (channel_id, platform_id) 分组的日统计数据 */
@Data
public class ChannelPlatformStatDTO {
    private String     statDate;      // yyyy-MM-dd
    private Long       channelId;
    private String     channelName;
    private Integer    platformId;    // 商户平台ID；无订单时为 null
    private BigDecimal systemAmount;  // 该商户在该通道当日成交额
}
```

- [ ] **Step 2: 确认编译通过**

```bash
cd backend && mvn compile -pl common-core -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/common-core/src/main/java/com/letsvpn/common/core/dto/ChannelPlatformStatDTO.java
git commit -m "feat: add ChannelPlatformStatDTO for per-platform channel stats"
```

---

## Task 2: pay-service Mapper 新增按 platform 分组的查询

**Files:**
- Modify: `backend/pay-service/src/main/java/com/letsvpn/pay/mapper/ext/ExtChannelProfitStatMapper.java`

- [ ] **Step 1: 在 ExtChannelProfitStatMapper 末尾新增方法**

在现有 `queryDailyStats` 方法之后，添加：

```java
import com.letsvpn.common.core.dto.ChannelPlatformStatDTO;
```

（已有的 import 保持不变，只加这一行）

然后在接口体内新增：

```java
/**
 * 统计指定日期所有通道在 order_info 中 status=1 的 real_amount，
 * 按 (channel_id, platform_id) 分组，用于计算各商户的通道费收入。
 * 无订单的通道不返回（LEFT JOIN 无意义，需在调用方补零）。
 */
@Results({
    @Result(column = "channel_id",   property = "channelId"),
    @Result(column = "channel_name", property = "channelName"),
    @Result(column = "platform_id",  property = "platformId"),
    @Result(column = "system_amount",property = "systemAmount"),
    @Result(column = "stat_date",    property = "statDate")
})
@Select("SELECT " +
        "    c.id              AS channel_id, " +
        "    c.title           AS channel_name, " +
        "    o.platform_id     AS platform_id, " +
        "    #{statDate}       AS stat_date, " +
        "    COALESCE(SUM(o.real_amount), 0) AS system_amount " +
        "FROM pay_config_channel c " +
        "INNER JOIN order_info o " +
        "    ON o.pay_config_channel_id = c.id " +
        "   AND o.status = 1 " +
        "   AND DATE(o.create_time) = #{statDate} " +
        "GROUP BY c.id, c.title, o.platform_id")
List<ChannelPlatformStatDTO> queryDailyStatsByPlatform(@Param("statDate") String statDate);
```

- [ ] **Step 2: 确认编译通过**

```bash
cd backend && mvn compile -pl pay-service -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/pay-service/src/main/java/com/letsvpn/pay/mapper/ext/ExtChannelProfitStatMapper.java
git commit -m "feat: add queryDailyStatsByPlatform to ExtChannelProfitStatMapper"
```

---

## Task 3: pay-service Service 和 Controller 新增接口

**Files:**
- Modify: `backend/pay-service/src/main/java/com/letsvpn/pay/service/core/DashboardMetricsService.java`
- Modify: `backend/pay-service/src/main/java/com/letsvpn/pay/controller/DashboardMetricsController.java`

- [ ] **Step 1: DashboardMetricsService 中注入 Mapper 并新增方法**

在 `DashboardMetricsService` 类的字段区，确认 `ExtChannelProfitStatMapper` 已注入。若未注入，添加：

```java
private final com.letsvpn.pay.mapper.ext.ExtChannelProfitStatMapper extChannelProfitStatMapper;
```

（放在其他 `private final` 字段之后，Lombok `@RequiredArgsConstructor` 会自动注入）

然后在类末尾新增方法：

```java
public List<com.letsvpn.common.core.dto.ChannelPlatformStatDTO> getDailyChannelPlatformStats(String date) {
    return extChannelProfitStatMapper.queryDailyStatsByPlatform(date);
}
```

- [ ] **Step 2: DashboardMetricsController 新增接口方法**

在 `DashboardMetricsController` 类末尾（`getChannelsByIds` 之后）新增：

```java
@GetMapping("/dashboard/dailyChannelPlatformStats")
public R<List<com.letsvpn.common.core.dto.ChannelPlatformStatDTO>> getDailyChannelPlatformStats(
        @RequestParam("date") String date) {
    return R.success(dashboardMetricsService.getDailyChannelPlatformStats(date));
}
```

并在文件顶部 import 区补充（若缺少）：

```java
import com.letsvpn.common.core.dto.ChannelPlatformStatDTO;
```

- [ ] **Step 3: 确认编译通过**

```bash
cd backend && mvn compile -pl pay-service -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/pay-service/src/main/java/com/letsvpn/pay/service/core/DashboardMetricsService.java \
        backend/pay-service/src/main/java/com/letsvpn/pay/controller/DashboardMetricsController.java
git commit -m "feat: expose dailyChannelPlatformStats endpoint in pay-service"
```

---

## Task 4: admin-service Feign Client 和 Facade 新增方法

**Files:**
- Modify: `backend/admin-service/src/main/java/com/letsvpn/admin/client/PayServiceClient.java`
- Modify: `backend/admin-service/src/main/java/com/letsvpn/admin/service/PayServiceFacade.java`

- [ ] **Step 1: PayServiceClient 末尾新增 Feign 方法**

在接口末尾（`getChannelsByIds` 之后）新增：

```java
@GetMapping("/dashboard/dailyChannelPlatformStats")
R<List<com.letsvpn.common.core.dto.ChannelPlatformStatDTO>> getDailyChannelPlatformStats(
        @RequestParam("date") String date);
```

并在文件顶部 import 区补充（若缺少）：

```java
import com.letsvpn.common.core.dto.ChannelPlatformStatDTO;
```

- [ ] **Step 2: PayServiceFacade 末尾新增 facade 方法**

```java
public List<com.letsvpn.common.core.dto.ChannelPlatformStatDTO> fetchDailyChannelPlatformStats(String date) {
    R<List<com.letsvpn.common.core.dto.ChannelPlatformStatDTO>> response =
            payServiceClient.getDailyChannelPlatformStats(date);
    if (response == null || !R.isSuccess(response.getCode()) || response.getData() == null) {
        log.warn("Failed to fetch daily channel platform stats from pay-service for date={}", date);
        return Collections.emptyList();
    }
    return response.getData();
}
```

并在文件顶部 import 区补充（若缺少）：

```java
import com.letsvpn.common.core.dto.ChannelPlatformStatDTO;
```

- [ ] **Step 3: 确认编译通过**

```bash
cd backend && mvn compile -pl admin-service -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/admin-service/src/main/java/com/letsvpn/admin/client/PayServiceClient.java \
        backend/admin-service/src/main/java/com/letsvpn/admin/service/PayServiceFacade.java
git commit -m "feat: add fetchDailyChannelPlatformStats to admin Feign client and facade"
```

---

## Task 5: 建表 SQL 文件

**Files:**
- Create: `backend/docs/channel_profit_stat.sql`

- [ ] **Step 1: 创建 SQL 文件**

```sql
-- 通道利润日统计表（admin schema）
-- 由 admin-service 定时任务每天凌晨 01:00 写入
CREATE TABLE IF NOT EXISTS `channel_profit_stat` (
  `id`                   BIGINT        NOT NULL AUTO_INCREMENT             COMMENT '主键',
  `stat_date`            DATE          NOT NULL                            COMMENT '统计日期',
  `channel_id`           BIGINT        NOT NULL                            COMMENT 'pay_config_channel.id',
  `channel_name`         VARCHAR(128)  NOT NULL                            COMMENT '通道名称（快照）',
  `system_amount`        DECIMAL(18,4) NOT NULL DEFAULT 0                  COMMENT '系统计算金额：当日 status=1 的 real_amount 汇总',
  `channel_fee_income`   DECIMAL(18,4) NOT NULL DEFAULT 0                  COMMENT '通道费收入：Σ(商户金额 × merchant_channel_config.fee_rate)',
  `collection_amount`    DECIMAL(18,4)     NULL DEFAULT NULL               COMMENT '代收户金额（人工录入）',
  `dropped_order_income` DECIMAL(18,4)     NULL DEFAULT NULL               COMMENT '掉单收入（人工录入）',
  `channel_cost`         DECIMAL(18,4)     NULL DEFAULT NULL               COMMENT '通道成本（人工录入）',
  `other_cost`           DECIMAL(18,4)     NULL DEFAULT NULL               COMMENT '其他成本（人工录入）',
  `frozen_amount`        DECIMAL(18,4)     NULL DEFAULT NULL               COMMENT '冻结金额（人工录入）',
  `adjustment`           DECIMAL(18,4)     NULL DEFAULT NULL               COMMENT '调差（人工录入）',
  `profit`               DECIMAL(18,4)     NULL DEFAULT NULL               COMMENT '利润（人工录入）',
  `create_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  `update_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_channel` (`stat_date`, `channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道利润日统计';
```

- [ ] **Step 2: Commit**

```bash
git add backend/docs/channel_profit_stat.sql
git commit -m "feat: add channel_profit_stat DDL"
```

---

## Task 6: admin-service Entity 和 Mapper

**Files:**
- Create: `backend/admin-service/src/main/java/com/letsvpn/admin/entity/ChannelProfitStat.java`
- Create: `backend/admin-service/src/main/java/com/letsvpn/admin/mapper/ChannelProfitStatMapper.java`

- [ ] **Step 1: 创建 Entity**

```java
package com.letsvpn.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("channel_profit_stat")
public class ChannelProfitStat {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("stat_date")
    private String statDate;         // yyyy-MM-dd，存为 VARCHAR 方便查询过滤

    @TableField("channel_id")
    private Long channelId;

    @TableField("channel_name")
    private String channelName;

    @TableField("system_amount")
    private BigDecimal systemAmount;

    @TableField("channel_fee_income")
    private BigDecimal channelFeeIncome;

    @TableField("collection_amount")
    private BigDecimal collectionAmount;

    @TableField("dropped_order_income")
    private BigDecimal droppedOrderIncome;

    @TableField("channel_cost")
    private BigDecimal channelCost;

    @TableField("other_cost")
    private BigDecimal otherCost;

    @TableField("frozen_amount")
    private BigDecimal frozenAmount;

    @TableField("adjustment")
    private BigDecimal adjustment;

    @TableField("profit")
    private BigDecimal profit;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
```

> **注意**：`stat_date` 建表是 `DATE` 类型，但 MyBatis-Plus 用 `String` 映射（格式 `yyyy-MM-dd`）更便于范围查询的字符串比较，MySQL 会自动做隐式转换。

- [ ] **Step 2: 创建 Mapper**

```java
package com.letsvpn.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.letsvpn.admin.entity.ChannelProfitStat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChannelProfitStatMapper extends BaseMapper<ChannelProfitStat> {
}
```

- [ ] **Step 3: 确认编译通过**

```bash
cd backend && mvn compile -pl admin-service -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/admin-service/src/main/java/com/letsvpn/admin/entity/ChannelProfitStat.java \
        backend/admin-service/src/main/java/com/letsvpn/admin/mapper/ChannelProfitStatMapper.java
git commit -m "feat: add ChannelProfitStat entity and mapper"
```

---

## Task 7: admin-service Service（查询 + upsert）

**Files:**
- Create: `backend/admin-service/src/main/java/com/letsvpn/admin/service/ChannelProfitStatService.java`

- [ ] **Step 1: 创建 Service**

```java
package com.letsvpn.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.admin.entity.ChannelProfitStat;
import com.letsvpn.admin.entity.MerchantChannelConfig;
import com.letsvpn.admin.mapper.ChannelProfitStatMapper;
import com.letsvpn.admin.mapper.MerchantChannelConfigMapper;
import com.letsvpn.common.core.dto.ChannelPlatformStatDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelProfitStatService {

    private final ChannelProfitStatMapper statMapper;
    private final MerchantChannelConfigMapper channelConfigMapper;

    /**
     * 分页查询通道利润统计列表。
     * 支持按 channelName 模糊过滤、按日期范围过滤。
     */
    public Page<ChannelProfitStat> list(String channelName, String startDate, String endDate,
                                         long pageNum, long pageSize) {
        long idx  = Math.max(pageNum, 1L);
        long size = Math.max(1L, Math.min(pageSize, 200L));

        LambdaQueryWrapper<ChannelProfitStat> wrapper = Wrappers.<ChannelProfitStat>lambdaQuery()
                .orderByDesc(ChannelProfitStat::getStatDate)
                .orderByAsc(ChannelProfitStat::getChannelId);

        if (StringUtils.hasText(channelName)) {
            wrapper.like(ChannelProfitStat::getChannelName, channelName.trim());
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(ChannelProfitStat::getStatDate, startDate.trim());
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(ChannelProfitStat::getStatDate, endDate.trim());
        }
        return statMapper.selectPage(new Page<>(idx, size), wrapper);
    }

    /**
     * 将 pay-service 返回的 (channel_id, platform_id, amount) 明细聚合后 upsert 到统计表。
     * 计算逻辑：
     *   system_amount      = Σ(所有商户的 platformAmount)
     *   channel_fee_income = Σ(platformAmount × merchant_channel_config.fee_rate)
     *
     * 使用 ON DUPLICATE KEY UPDATE 保证重复跑同一天幂等。
     */
    public void upsertDailyStat(String statDate, List<ChannelPlatformStatDTO> rawStats) {
        if (rawStats == null || rawStats.isEmpty()) {
            log.info("No channel platform stats to upsert for date={}", statDate);
            return;
        }

        // 按 channelId 分组
        Map<Long, List<ChannelPlatformStatDTO>> byChannel = rawStats.stream()
                .collect(Collectors.groupingBy(ChannelPlatformStatDTO::getChannelId));

        for (Map.Entry<Long, List<ChannelPlatformStatDTO>> entry : byChannel.entrySet()) {
            Long channelId = entry.getKey();
            List<ChannelPlatformStatDTO> platformStats = entry.getValue();

            String channelName = platformStats.get(0).getChannelName();
            BigDecimal systemAmount = platformStats.stream()
                    .map(ChannelPlatformStatDTO::getSystemAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal channelFeeIncome = platformStats.stream()
                    .map(dto -> {
                        if (dto.getPlatformId() == null) return BigDecimal.ZERO;
                        MerchantChannelConfig cfg = channelConfigMapper.selectOne(
                                Wrappers.<MerchantChannelConfig>lambdaQuery()
                                        .eq(MerchantChannelConfig::getPayConfigChannelId, channelId)
                                        .eq(MerchantChannelConfig::getPlatformId, dto.getPlatformId())
                                        .last("LIMIT 1"));
                        if (cfg == null || cfg.getFeeRate() == null) return BigDecimal.ZERO;
                        return dto.getSystemAmount().multiply(cfg.getFeeRate());
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 查是否已有当天该通道的记录
            ChannelProfitStat existing = statMapper.selectOne(
                    Wrappers.<ChannelProfitStat>lambdaQuery()
                            .eq(ChannelProfitStat::getStatDate, statDate)
                            .eq(ChannelProfitStat::getChannelId, channelId));

            if (existing != null) {
                existing.setChannelName(channelName);
                existing.setSystemAmount(systemAmount);
                existing.setChannelFeeIncome(channelFeeIncome);
                statMapper.updateById(existing);
            } else {
                ChannelProfitStat stat = new ChannelProfitStat();
                stat.setStatDate(statDate);
                stat.setChannelId(channelId);
                stat.setChannelName(channelName);
                stat.setSystemAmount(systemAmount);
                stat.setChannelFeeIncome(channelFeeIncome);
                statMapper.insert(stat);
            }
        }
        log.info("Upserted channel profit stats for date={}, channels={}", statDate, byChannel.size());
    }
}
```

- [ ] **Step 2: 确认编译通过**

```bash
cd backend && mvn compile -pl admin-service -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/admin-service/src/main/java/com/letsvpn/admin/service/ChannelProfitStatService.java
git commit -m "feat: add ChannelProfitStatService with list and upsert logic"
```

---

## Task 8: admin-service Controller

**Files:**
- Create: `backend/admin-service/src/main/java/com/letsvpn/admin/controller/ChannelProfitStatController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.letsvpn.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.letsvpn.admin.entity.ChannelProfitStat;
import com.letsvpn.admin.service.ChannelProfitStatService;
import com.letsvpn.common.core.response.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/channel-profit")
@RequiredArgsConstructor
public class ChannelProfitStatController {

    private final ChannelProfitStatService service;

    @GetMapping("/list")
    public R<Page<ChannelProfitStat>> list(
            @RequestParam(value = "channelName", required = false) String channelName,
            @RequestParam(value = "startDate",   required = false) String startDate,
            @RequestParam(value = "endDate",     required = false) String endDate,
            @RequestParam(value = "pageNum",     defaultValue = "1")  long pageNum,
            @RequestParam(value = "pageSize",    defaultValue = "20") long pageSize) {
        return R.success(service.list(channelName, startDate, endDate, pageNum, pageSize));
    }
}
```

- [ ] **Step 2: 确认编译通过**

```bash
cd backend && mvn compile -pl admin-service -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/admin-service/src/main/java/com/letsvpn/admin/controller/ChannelProfitStatController.java
git commit -m "feat: add ChannelProfitStatController GET /admin/channel-profit/list"
```

---

## Task 9: admin-service 定时任务 Scheduler

**Files:**
- Create: `backend/admin-service/src/main/java/com/letsvpn/admin/scheduler/ChannelProfitStatScheduler.java`
- Verify: `backend/admin-service/src/main/java/com/letsvpn/admin/AdminServiceApplication.java` 已有 `@EnableScheduling`

- [ ] **Step 1: 检查 AdminServiceApplication 是否已有 @EnableScheduling**

```bash
grep -n "EnableScheduling" backend/admin-service/src/main/java/com/letsvpn/admin/AdminServiceApplication.java
```

若无输出，则在 `AdminServiceApplication.java` 的类注解处添加 `@EnableScheduling`：

```java
@SpringBootApplication
@EnableScheduling          // ← 新增这行
@EnableFeignClients(...)
public class AdminServiceApplication { ... }
```

并在 import 区添加：
```java
import org.springframework.scheduling.annotation.EnableScheduling;
```

- [ ] **Step 2: 创建 Scheduler**

```java
package com.letsvpn.admin.scheduler;

import com.letsvpn.admin.service.ChannelProfitStatService;
import com.letsvpn.admin.service.PayServiceFacade;
import com.letsvpn.common.core.dto.ChannelPlatformStatDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChannelProfitStatScheduler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PayServiceFacade payServiceFacade;
    private final ChannelProfitStatService channelProfitStatService;

    /** 每天凌晨 01:00 统计前一天各通道成交额和通道费收入 */
    @Scheduled(cron = "0 0 1 * * ?")
    public void runDailyChannelStat() {
        String yesterday = LocalDate.now().minusDays(1).format(DATE_FMT);
        log.info("ChannelProfitStatScheduler start, statDate={}", yesterday);
        try {
            List<ChannelPlatformStatDTO> stats = payServiceFacade.fetchDailyChannelPlatformStats(yesterday);
            channelProfitStatService.upsertDailyStat(yesterday, stats);
            log.info("ChannelProfitStatScheduler done, statDate={}", yesterday);
        } catch (Exception e) {
            log.error("ChannelProfitStatScheduler failed, statDate={}", yesterday, e);
        }
    }
}
```

- [ ] **Step 3: 确认编译通过**

```bash
cd backend && mvn compile -pl admin-service -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/admin-service/src/main/java/com/letsvpn/admin/scheduler/ChannelProfitStatScheduler.java \
        backend/admin-service/src/main/java/com/letsvpn/admin/AdminServiceApplication.java
git commit -m "feat: add ChannelProfitStatScheduler, runs daily at 01:00"
```

---

## Task 10: 整体编译验证

- [ ] **Step 1: 全量编译**

```bash
cd backend && mvn compile -pl common-core,pay-service,admin-service --also-make -q
```
Expected: BUILD SUCCESS（三个模块全部通过）

- [ ] **Step 2: 确认接口可达（可选，需服务已启动）**

```bash
curl -s "http://localhost:8082/admin/channel-profit/list?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <token>" | python -m json.tool
```
Expected: JSON 响应，`data.records` 为数组（初始为空）

---

## 自检

| 需求 | 对应 Task |
|------|----------|
| pay-service 按 (channel_id, platform_id) 分组查询 | Task 2 |
| pay-service 暴露 Feign 接口 | Task 3 |
| common-core DTO | Task 1 |
| admin Feign Client + Facade | Task 4 |
| admin 建表 SQL 输出到 backend/docs | Task 5 |
| admin Entity + Mapper | Task 6 |
| admin Service（list + upsert + fee_rate 计算）| Task 7 |
| admin Controller GET /admin/channel-profit/list | Task 8 |
| admin Scheduler 01:00 每日运行 | Task 9 |
| 全量编译验证 | Task 10 |
