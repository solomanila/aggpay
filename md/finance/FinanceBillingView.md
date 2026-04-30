# FinanceBillingView 后端接口需求

## 页面定位
- `FinanceBillingView.vue` 位于“财务/账单”，展示平台主体的账单周期、应收/应付、状态、Owner 及审批。
- 需要账单统计、列表、详情及审批操作，REST 前缀 `/api/admin/v1/finance/billing/**`。

## 现有数据来源
- `order_info` 可统计交易金额但没有账单汇总。
- 需新增账单表及审批表。

## 新增/扩展数据结构
```sql
CREATE TABLE IF NOT EXISTS `finance_billing_cycle` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `subject_type` varchar(16) NOT NULL COMMENT 'MERCHANT|PLATFORM',
  `subject_id` int NOT NULL,
  `cycle_start` date NOT NULL,
  `cycle_end` date NOT NULL,
  `receivable_amount` decimal(18,2) DEFAULT 0,
  `payable_amount` decimal(18,2) DEFAULT 0,
  `status` varchar(16) DEFAULT 'PENDING' COMMENT 'PENDING|REVIEW|PAID|CLOSED',
  `owner_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_subject_cycle` (`subject_type`,`subject_id`,`cycle_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单周期汇总';

CREATE TABLE IF NOT EXISTS `finance_billing_approval` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `billing_id` bigint NOT NULL,
  `stage` varchar(32) NOT NULL,
  `status` varchar(16) DEFAULT 'PENDING',
  `owner_user_id` bigint DEFAULT NULL,
  `eta` datetime DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`billing_id`) REFERENCES `finance_billing_cycle`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单审批';
```

## 接口清单
| # | Method | Path | 功能 |
|---|---|---|---|
| 1 | GET | `/summary` | 账单 KPI：周期数、应付、应收、待审批 |
| 2 | GET | `/cycles` | 账单列表（筛选主体/状态/时间） |
| 3 | GET | `/cycles/{billingId}` | 账单详情：明细、审批、日志 |
| 4 | POST | `/cycles` | 生成/更新账单 |
| 5 | POST | `/cycles/{billingId}/approvals` | 审批动作 |

## 接口详情

### 1. `GET /api/admin/v1/finance/billing/summary`
- **参数**：`subjectType`,`dateRange`。
- **返回**：`{ stats:[{id:'bill-total',value,meta},...], approvals:[...]}`
- **数据**：`finance_billing_cycle` 聚合；待审批来自 `finance_billing_approval`。

### 2. `GET /api/admin/v1/finance/billing/cycles`
- **参数**：`subjectType`,`subjectId`,`status`,`dateFrom`,`dateTo`,`keyword`,`pageNo`,`pageSize`。
- **逻辑**：按条件查询 `finance_billing_cycle`；`keyword` 匹配主体名称（join `pay_platform_info`）。
- **响应**：分页列表 `{ billingId,subjectName,cycle,receivable,payable,status,owner,updatedAt }`。

### 3. `GET /api/admin/v1/finance/billing/cycles/{billingId}`
- **返回**：`{ billing, approvals, lineItems }`。
  - `billing`: 基本信息。
  - `approvals`: `finance_billing_approval`。
  - `lineItems`: 根据 `order_info` 聚合或从扩展表 `finance_billing_detail`（可选）读取。

### 4. `POST /api/admin/v1/finance/billing/cycles`
- **Body**：`{ "billingId":null, "subjectType":"MERCHANT", "subjectId":101, "cycleStart":"2024-05-01", "cycleEnd":"2024-05-07", "receivableAmount":320000, "payableAmount":120000, "ownerUserId":2001 }`.
- **逻辑**：生成账单（可由批处理写入），若 `billingId` 存在则更新并重新计算；创建默认审批链。

### 5. `POST /api/admin/v1/finance/billing/cycles/{billingId}/approvals`
- **Body**：`{ "stage":"FINANCE", "decision":"APPROVED|REJECTED", "comment":"..." }`.
- **逻辑**：更新 `finance_billing_approval`；全部通过后将 `finance_billing_cycle.status='PAID'` 并记录付款单据。
