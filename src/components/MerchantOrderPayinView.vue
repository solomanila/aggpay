<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import http from '../services/http';

const orderIdFilter = ref('');
const startDate = ref('');
const endDate = ref('');
const statusFilter = ref('');

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待支付', value: '0' },
  { label: '支付成功', value: '1' },
  { label: '支付失败', value: '2' },
  { label: '已关闭', value: '3' }
];

const tableRows = ref([]);
const tableTotal = ref(0);
const tablePage = ref(1);
const tablePageSize = ref(10);
const tableLoading = ref(false);
const tableError = ref('');
const paginationSizes = [10, 20, 50];

const totalPages = computed(() =>
  tableTotal.value ? Math.max(1, Math.ceil(tableTotal.value / tablePageSize.value)) : 1
);

const statusLabel = (val) => {
  const opt = statusOptions.find((o) => o.value === String(val ?? ''));
  return opt ? opt.label : val ?? '—';
};

const syncStatusLabel = (val) => {
  if (val === null || val === undefined) return '—';
  const map = { 0: '无退款', 1: '退款中', 2: '退款成功', 3: '退款失败' };
  return map[val] ?? String(val);
};

const formatDateTime = (value) => {
  if (!value) return '—';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : d.toLocaleString('zh-CN', { hour12: false });
};

const formatAmount = (value) => {
  if (value === null || value === undefined || value === '') return '—';
  const num = Number(value);
  return Number.isNaN(num) ? value : num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

const fetchOrders = async () => {
  tableLoading.value = true;
  tableError.value = '';
  try {
    const params = { pageNum: tablePage.value, pageSize: tablePageSize.value };
    if (orderIdFilter.value.trim()) params.orderId = orderIdFilter.value.trim();
    if (startDate.value) params.startDate = startDate.value;
    if (endDate.value) params.endDate = endDate.value;
    if (statusFilter.value !== '') params.status = Number(statusFilter.value);

    const { data: resp } = await http.get('/admin/merchant/orders/payin', { params });
    const payload = resp?.data ?? resp;
    tableRows.value = payload?.records ?? [];
    tableTotal.value = payload?.total ?? 0;
    tablePage.value = payload?.current ?? tablePage.value;
    tablePageSize.value = payload?.size ?? tablePageSize.value;
  } catch {
    tableError.value = '加载失败，请稍后重试';
    tableRows.value = [];
    tableTotal.value = 0;
  } finally {
    tableLoading.value = false;
  }
};

const handleSearch = () => {
  tablePage.value = 1;
  fetchOrders();
};

const handleReset = () => {
  orderIdFilter.value = '';
  startDate.value = '';
  endDate.value = '';
  statusFilter.value = '';
  tablePage.value = 1;
  fetchOrders();
};

const changePage = (next) => {
  if (next < 1 || next > totalPages.value || next === tablePage.value) return;
  tablePage.value = next;
  fetchOrders();
};

const changePageSize = (size) => {
  tablePageSize.value = size;
  tablePage.value = 1;
  fetchOrders();
};

onMounted(fetchOrders);
</script>

<template>
  <div class="payin-view">
    <div class="page-header">
      <h2 class="page-title">收款订单</h2>
    </div>

    <!-- 查询条件 -->
    <section class="filter-bar panel">
      <div class="filter-row">
        <label class="filter-item">
          <span class="filter-label">订单号</span>
          <input
            v-model="orderIdFilter"
            type="text"
            placeholder="订单号 / 商户订单号"
            class="filter-input"
            @keyup.enter="handleSearch"
          />
        </label>
        <label class="filter-item">
          <span class="filter-label">开始日期</span>
          <input v-model="startDate" type="date" class="filter-input" />
        </label>
        <label class="filter-item">
          <span class="filter-label">结束日期</span>
          <input v-model="endDate" type="date" class="filter-input" />
        </label>
        <label class="filter-item">
          <span class="filter-label">状态</span>
          <select v-model="statusFilter" class="filter-select">
            <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
        </label>
      </div>
      <div class="filter-actions">
        <button type="button" class="btn-primary" @click="handleSearch">查询</button>
        <button type="button" class="btn-ghost" @click="handleReset">重置</button>
      </div>
    </section>

    <!-- 订单表格 -->
    <section class="panel table-section">
      <p v-if="tableError" class="status-text error">{{ tableError }}</p>
      <p v-else-if="tableLoading" class="status-text">加载中...</p>

      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>No.</th>
              <th>商户订单号</th>
              <th>金额</th>
              <th>实付金额</th>
              <th>状态</th>
              <th>退款状态</th>
              <th>创建时间</th>
              <th>结算时间</th>
              <th>付款时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!tableLoading && !tableRows.length">
              <td colspan="9" class="empty-cell">暂无数据</td>
            </tr>
            <tr v-for="(row, idx) in tableRows" :key="row.id ?? row.orderId ?? idx">
              <td class="cell-no">{{ (tablePage - 1) * tablePageSize + idx + 1 }}</td>
              <td class="cell-id">{{ row.frontId || row.orderId || '—' }}</td>
              <td class="cell-amount">{{ formatAmount(row.reqAmount) }}</td>
              <td class="cell-amount accent">{{ formatAmount(row.realAmount) }}</td>
              <td>
                <span class="status-badge" :class="`status-${row.status}`">
                  {{ statusLabel(row.status) }}
                </span>
              </td>
              <td>{{ syncStatusLabel(row.syncStatus) }}</td>
              <td class="cell-time">{{ formatDateTime(row.createTime) }}</td>
              <td class="cell-time">{{ formatDateTime(row.settleTime) }}</td>
              <td class="cell-time">{{ formatDateTime(row.payTime) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-bar">
        <span class="page-info">共 {{ tableTotal }} 条 · 第 {{ tablePage }} / {{ totalPages }} 页</span>
        <div class="page-controls">
          <button type="button" class="ghost-btn" :disabled="tablePage <= 1" @click="changePage(tablePage - 1)">
            上一页
          </button>
          <button type="button" class="ghost-btn" :disabled="tablePage >= totalPages" @click="changePage(tablePage + 1)">
            下一页
          </button>
          <label>
            <span>每页</span>
            <select :value="tablePageSize" @change="changePageSize(Number($event.target.value))">
              <option v-for="size in paginationSizes" :key="size" :value="size">{{ size }}</option>
            </select>
          </label>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.payin-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1200px;
}

.page-header { margin-bottom: 4px; }

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.panel {
  background: rgba(18, 19, 37, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  padding: 20px 24px;
}

/* 查询条件 */
.filter-bar { display: flex; flex-direction: column; gap: 16px; }

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 160px;
}

.filter-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 0.03em;
}

.filter-input,
.filter-select {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  outline: none;
  height: 36px;
}

.filter-input::placeholder { color: rgba(255, 255, 255, 0.25); }
.filter-input:focus,
.filter-select:focus { border-color: rgba(139, 92, 246, 0.5); }

.filter-actions { display: flex; gap: 10px; }

.btn-primary {
  padding: 8px 20px;
  background: rgba(139, 92, 246, 0.85);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-primary:hover { background: rgba(139, 92, 246, 1); }

.btn-ghost {
  padding: 8px 20px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-ghost:hover { background: rgba(255, 255, 255, 0.1); }

/* 表格 */
.table-section { padding: 0; }
.table-wrapper { overflow-x: auto; }

.status-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  padding: 20px 24px;
  margin: 0;
}
.status-text.error { color: #f87171; }

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

th {
  padding: 12px 16px;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 0.04em;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  white-space: nowrap;
}

td {
  padding: 12px 16px;
  color: rgba(255, 255, 255, 0.75);
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  white-space: nowrap;
}

tr:last-child td { border-bottom: none; }
tr:hover td { background: rgba(255, 255, 255, 0.02); }

.cell-no { color: rgba(255, 255, 255, 0.35); width: 48px; }
.cell-id { font-family: 'Courier New', monospace; font-size: 12px; }
.cell-amount { font-weight: 600; }
.cell-amount.accent { color: #34d399; }
.cell-time { font-size: 12px; color: rgba(255, 255, 255, 0.55); }
.empty-cell { text-align: center; color: rgba(255, 255, 255, 0.3); padding: 40px; }

/* 状态徽章 */
.status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 100px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.03em;
}
.status-0 { background: rgba(251, 191, 36, 0.15); color: #fbbf24; }
.status-1 { background: rgba(52, 211, 153, 0.15); color: #34d399; }
.status-2 { background: rgba(248, 113, 113, 0.15); color: #f87171; }
.status-3 { background: rgba(255, 255, 255, 0.08); color: rgba(255, 255, 255, 0.45); }

/* 分页 */
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.page-info { font-size: 13px; color: rgba(255, 255, 255, 0.4); }

.page-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ghost-btn {
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
}
.ghost-btn:disabled { opacity: 0.3; cursor: default; }
.ghost-btn:not(:disabled):hover { background: rgba(255, 255, 255, 0.1); }

.page-controls label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
}

.page-controls select {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  padding: 4px 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  outline: none;
}
</style>
