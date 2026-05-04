<script setup>
import { computed, onMounted, ref } from 'vue';
import http from '../services/http';

// ── 弹窗状态 ─────────────────────────────────────────────────────
const modalVisible = ref(false);
const formAmount = ref('');
const formUsdtAddress = ref('');
const formCurrency = ref('INR');
const formLoading = ref(false);
const formError = ref('');

const openModal = () => {
  formAmount.value = '';
  formUsdtAddress.value = '';
  formCurrency.value = 'INR';
  formError.value = '';
  modalVisible.value = true;
};

const closeModal = () => {
  modalVisible.value = false;
};

const submitWithdraw = async () => {
  formError.value = '';
  const amount = Number(formAmount.value);
  if (!formAmount.value || isNaN(amount) || amount <= 0) {
    formError.value = '请输入有效的提现金额';
    return;
  }
  if (!formUsdtAddress.value.trim()) {
    formError.value = '请输入 USDT 地址';
    return;
  }
  formLoading.value = true;
  try {
    await http.post('/admin/merchant/withdraw', {
      amount,
      usdtAddress: formUsdtAddress.value.trim(),
      currency: formCurrency.value
    });
    closeModal();
    tablePage.value = 1;
    fetchRecords();
  } catch (e) {
    formError.value = e?.response?.data?.msg || e?.response?.data?.message || '提交失败，请稍后重试';
  } finally {
    formLoading.value = false;
  }
};

// ── 提现记录列表 ──────────────────────────────────────────────────
const statusFilter = ref('');
const startDate = ref('');
const endDate = ref('');

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '审核中',   value: '0' },
  { label: '已通过',   value: '1' },
  { label: '已拒绝',   value: '2' }
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
  const map = { 0: '审核中', 1: '已通过', 2: '已拒绝' };
  return map[val] ?? '—';
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

const fetchRecords = async () => {
  tableLoading.value = true;
  tableError.value = '';
  try {
    const params = { pageNum: tablePage.value, pageSize: tablePageSize.value };
    if (statusFilter.value !== '') params.status = Number(statusFilter.value);
    if (startDate.value) params.startDate = startDate.value;
    if (endDate.value) params.endDate = endDate.value;

    const { data: resp } = await http.get('/admin/merchant/withdraw/page', { params });
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

const handleSearch = () => { tablePage.value = 1; fetchRecords(); };
const handleReset = () => {
  statusFilter.value = '';
  startDate.value = '';
  endDate.value = '';
  tablePage.value = 1;
  fetchRecords();
};
const changePage = (next) => {
  if (next < 1 || next > totalPages.value || next === tablePage.value) return;
  tablePage.value = next;
  fetchRecords();
};
const changePageSize = (size) => {
  tablePageSize.value = size;
  tablePage.value = 1;
  fetchRecords();
};

onMounted(fetchRecords);
</script>

<template>
  <div class="withdraw-view">
    <div class="page-header">
      <h2 class="page-title">提现申请</h2>
    </div>

    <!-- 查询条件 -->
    <section class="filter-bar panel">
      <div class="filter-row">
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
        <button type="button" class="btn-apply" @click="openModal">申请</button>
      </div>
    </section>

    <!-- 提现记录表格 -->
    <section class="panel table-section">
      <p v-if="tableError" class="status-text error">{{ tableError }}</p>
      <p v-else-if="tableLoading" class="status-text">加载中...</p>

      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>No.</th>
              <th>金额</th>
              <th>货币</th>
              <th>USDT 地址</th>
              <th>状态</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!tableLoading && !tableRows.length">
              <td colspan="6" class="empty-cell">暂无数据</td>
            </tr>
            <tr v-for="(row, idx) in tableRows" :key="row.id ?? idx">
              <td class="cell-no">{{ (tablePage - 1) * tablePageSize + idx + 1 }}</td>
              <td class="cell-amount accent">{{ formatAmount(row.amount) }}</td>
              <td>{{ row.currency || '—' }}</td>
              <td class="cell-addr">{{ row.usdtAddress || '—' }}</td>
              <td>
                <span class="status-badge" :class="`status-${row.status}`">
                  {{ statusLabel(row.status) }}
                </span>
              </td>
              <td class="cell-time">{{ formatDateTime(row.createTime) }}</td>
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

    <!-- 提现申请弹窗 -->
    <Teleport to="body">
      <div v-if="modalVisible" class="modal-mask" @click.self="closeModal">
        <div class="modal-box">
          <div class="modal-header">
            <span class="modal-title">提现申请</span>
            <button type="button" class="modal-close" @click="closeModal">✕</button>
          </div>

          <div class="modal-body">
            <div class="field">
              <label class="field-label"><span class="required">*</span> 货币</label>
              <select v-model="formCurrency" class="field-input">
                <option value="INR">INR</option>
              </select>
            </div>

            <div class="field">
              <label class="field-label"><span class="required">*</span> 金额</label>
              <input
                v-model="formAmount"
                type="number"
                min="0"
                step="0.01"
                placeholder=""
                class="field-input"
                @keyup.enter="submitWithdraw"
              />
            </div>

            <div class="field">
              <label class="field-label"><span class="required">*</span> USDT地址(TRC20)</label>
              <input
                v-model="formUsdtAddress"
                type="text"
                placeholder=""
                class="field-input"
                @keyup.enter="submitWithdraw"
              />
            </div>

            <p v-if="formError" class="field-error">{{ formError }}</p>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="closeModal">取消</button>
            <button type="button" class="btn-confirm" :disabled="formLoading" @click="submitWithdraw">
              {{ formLoading ? '提交中...' : '确认' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.withdraw-view {
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

.filter-actions { display: flex; gap: 10px; align-items: center; }

.btn-primary {
  padding: 8px 20px;
  background: rgba(139, 92, 246, 0.85);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  height: 36px;
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
  height: 36px;
  transition: background 0.15s;
}
.btn-ghost:hover { background: rgba(255, 255, 255, 0.1); }

.btn-apply {
  padding: 8px 20px;
  background: rgba(16, 185, 129, 0.85);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  height: 36px;
  transition: background 0.15s;
}
.btn-apply:hover { background: rgba(16, 185, 129, 1); }

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

.cell-no    { color: rgba(255, 255, 255, 0.35); width: 48px; }
.cell-amount { font-weight: 600; }
.cell-amount.accent { color: #34d399; }
.cell-addr  { font-family: 'Courier New', monospace; font-size: 12px; max-width: 320px; overflow: hidden; text-overflow: ellipsis; }
.cell-time  { font-size: 12px; color: rgba(255, 255, 255, 0.55); }
.empty-cell { text-align: center; color: rgba(255, 255, 255, 0.3); padding: 40px; }

.status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 100px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.03em;
}
.status-0 { background: rgba(251, 191, 36, 0.15); color: #fbbf24; }
.status-1 { background: rgba(52, 211, 153, 0.15);  color: #34d399; }
.status-2 { background: rgba(248, 113, 113, 0.15); color: #f87171; }

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

/* ── 弹窗 ──────────────────────────────────────────────────────── */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-box {
  background: #1a1b2e;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  width: 360px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}

.modal-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.modal-close {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.45);
  font-size: 16px;
  cursor: pointer;
  padding: 2px 4px;
  line-height: 1;
  transition: color 0.15s;
}
.modal-close:hover { color: rgba(255, 255, 255, 0.8); }

.modal-body {
  padding: 20px 20px 8px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

.required {
  color: #f87171;
  margin-right: 2px;
}

.field-input {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 6px;
  padding: 9px 12px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  outline: none;
  height: 38px;
  width: 100%;
  box-sizing: border-box;
  transition: border-color 0.15s;
}
.field-input:focus { border-color: rgba(139, 92, 246, 0.6); }

.field-error {
  font-size: 12px;
  color: #f87171;
  margin: 0;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 20px 20px;
}

.btn-cancel {
  padding: 8px 22px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-cancel:hover { background: rgba(255, 255, 255, 0.12); }

.btn-confirm {
  padding: 8px 22px;
  background: rgba(59, 130, 246, 0.9);
  border: none;
  border-radius: 6px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-confirm:hover:not(:disabled) { background: rgba(59, 130, 246, 1); }
.btn-confirm:disabled { opacity: 0.5; cursor: default; }
</style>
