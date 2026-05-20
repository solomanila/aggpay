<script setup>
import { computed, onMounted, ref } from 'vue';
import http from '../services/http.js';
import DateTimeRangePicker from './DateTimeRangePicker.vue';

defineProps({ data: { type: Object, default: null } });

const statusOptions = [
  { label: '进行中', value: 0 },
  { label: '成功',   value: 1 },
  { label: '取消',   value: 2 },
  { label: '失败',   value: 3 },
];

const statusLabel = (v) => {
  const opt = statusOptions.find(o => o.value === v);
  return opt ? opt.label : (v === null || v === undefined ? '—' : String(v));
};

const merchantOptions = ref([]);
const channelOptions  = ref([]);

const filterId           = ref('');
const filterOtherOrderId = ref('');
const filterCreateStart  = ref('');
const filterCreateEnd    = ref('');
const filterPayStart     = ref('');
const filterPayEnd       = ref('');
const filterChannelId    = ref('');
const filterStatus       = ref('');
const filterAccount      = ref('');

const tableRows     = ref([]);
const tableTotal    = ref(0);
const tablePage     = ref(1);
const tablePageSize = ref(20);
const tableLoading  = ref(false);
const tableError    = ref('');

const paginationSizes = [10, 20, 50];

const totalPages = computed(() =>
  tableTotal.value ? Math.max(1, Math.ceil(tableTotal.value / tablePageSize.value)) : 1
);

const tableColumns = [
  { key: 'id',           label: 'ID' },
  { key: 'orderId',      label: '订单号' },
  { key: 'utr',          label: 'UTR' },
  { key: 'reqAmount',    label: '请求金额' },
  { key: 'realAmount',   label: '实际金额' },
  { key: 'title',        label: '渠道' },
  { key: 'account',      label: '商户' },
  { key: 'status',       label: '状态' },
  { key: 'refundStatus', label: '退款状态' },
  { key: 'bank',         label: 'Bank' },
  { key: 'payerVPA',     label: 'PayerVPA' },
  { key: 'phone',        label: '手机号' },
  { key: 'name',         label: '姓名' },
  { key: 'times',        label: '创建/支付/结算时间' },
  { key: 'actions',      label: '操作' },
];

const formatDateTime = (value) => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', { hour12: false });
};

const formatNumber = (value) => {
  if (value === null || value === undefined || value === '') return '—';
  const num = Number(value);
  if (Number.isNaN(num)) return value;
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

const displayCell = (row, key) => {
  const value = row?.[key];
  if (key === 'status') return statusLabel(value);
  if (value === null || value === undefined || value === '') return '—';
  if (key.toLowerCase().includes('time')) return formatDateTime(value);
  if (key.toLowerCase().includes('amount')) return formatNumber(value);
  return value;
};

const fetchMerchants = async () => {
  try {
    const { data: res } = await http.get('/admin/fund-flow/merchants');
    merchantOptions.value = res?.data ?? res ?? [];
  } catch { merchantOptions.value = []; }
};

const fetchChannels = async () => {
  try {
    const { data: res } = await http.get('/admin/pay/dashboard/allChannelOptions');
    channelOptions.value = res?.data ?? res ?? [];
  } catch { channelOptions.value = []; }
};

const buildParams = () => {
  const p = { pageNum: tablePage.value, pageSize: tablePageSize.value };
  if (filterId.value.trim())           p.id            = Number(filterId.value.trim());
  if (filterOtherOrderId.value.trim()) p.otherOrderId  = filterOtherOrderId.value.trim();
  if (filterCreateStart.value)         p.createStartTime = filterCreateStart.value;
  if (filterCreateEnd.value)           p.createEndTime   = filterCreateEnd.value;
  if (filterPayStart.value)            p.payStartTime    = filterPayStart.value;
  if (filterPayEnd.value)              p.payEndTime      = filterPayEnd.value;
  if (filterChannelId.value !== '')    p.channelId     = Number(filterChannelId.value);
  if (filterStatus.value !== '')       p.status        = Number(filterStatus.value);
  if (filterAccount.value !== '')      p.account       = filterAccount.value;
  return p;
};

const fetchOrders = async () => {
  tableLoading.value = true;
  tableError.value   = '';
  try {
    const { data: res } = await http.get('/admin/pay/dashboard/channelStatPayout', { params: buildParams() });
    const payload = res?.data ?? res;
    tableRows.value     = payload?.records ?? [];
    tableTotal.value    = payload?.total   ?? 0;
    tablePage.value     = payload?.current ?? payload?.pageNum ?? tablePage.value;
    tablePageSize.value = payload?.size    ?? payload?.pageSize ?? tablePageSize.value;
  } catch {
    tableError.value = '无法加载订单数据';
    tableRows.value  = [];
    tableTotal.value = 0;
  } finally {
    tableLoading.value = false;
  }
};

const handleSearch = () => { tablePage.value = 1; fetchOrders(); };

const handleReset = () => {
  filterId.value           = '';
  filterOtherOrderId.value = '';
  filterCreateStart.value  = '';
  filterCreateEnd.value    = '';
  filterPayStart.value     = '';
  filterPayEnd.value       = '';
  filterChannelId.value    = '';
  filterStatus.value       = '';
  filterAccount.value      = '';
  tablePage.value = 1;
  fetchOrders();
};

const changePage = (n) => {
  if (n < 1 || n > totalPages.value || n === tablePage.value) return;
  tablePage.value = n;
  fetchOrders();
};

const changePageSize = (n) => {
  tablePageSize.value = n;
  tablePage.value = 1;
  fetchOrders();
};

// ── 手动回调 ─────────────────────────────────────────────────────────
const callbackingOrderId  = ref('');
const callbackMsg         = ref('');
const confirmRow          = ref(null);
const showConfirmModal    = ref(false);

const openConfirmModal = (row) => {
  confirmRow.value       = row;
  showConfirmModal.value = true;
};

const closeConfirmModal = () => {
  showConfirmModal.value = false;
  confirmRow.value       = null;
};

const doManualCallback = async () => {
  const row = confirmRow.value;
  if (!row) return;
  closeConfirmModal();
  callbackingOrderId.value = row.orderId;
  callbackMsg.value = '';
  try {
    const { data: res } = await http.post('/admin/pay/order/manualCallback', null, {
      params: { orderId: row.orderId },
    });
    if (res?.code === 200) {
      callbackMsg.value = `订单 ${row.orderId} 手动回调成功`;
    } else {
      callbackMsg.value = res?.msg || '操作失败';
    }
  } catch (e) {
    callbackMsg.value = e?.response?.data?.msg || '请求失败';
  } finally {
    callbackingOrderId.value = '';
  }
};

// ── 回调记录 modal ────────────────────────────────────────────────────
const showCallbackModal  = ref(false);
const callbackModalTitle = ref('');
const callbackRecords    = ref([]);
const callbackLoading    = ref(false);
const callbackError      = ref('');

const callbackColumns = [
  { key: 'orderId',    label: '订单号' },
  { key: 'platformNo', label: '签名' },
  { key: 'reqUrl',     label: 'Url' },
  { key: 'param',      label: '请求' },
  { key: 'respCode',   label: '响应Code' },
  { key: 'createTime', label: '创建时间' },
];

const openCallbackModal = async (row) => {
  callbackModalTitle.value = `回调记录 — ${row.orderId}`;
  callbackRecords.value    = [];
  callbackError.value      = '';
  callbackLoading.value    = true;
  showCallbackModal.value  = true;
  try {
    const { data: res } = await http.get('/admin/pay/order/callbackList', {
      params: { orderId: row.orderId },
    });
    callbackRecords.value = res?.data ?? [];
  } catch {
    callbackError.value = '无法加载回调记录';
  } finally {
    callbackLoading.value = false;
  }
};

const closeCallbackModal = () => { showCallbackModal.value = false; };

const truncate = (str, max = 80) => {
  if (!str) return '—';
  return str.length > max ? str.slice(0, max) + '…' : str;
};

onMounted(async () => {
  await Promise.all([fetchMerchants(), fetchChannels()]);
  await fetchOrders();
});
</script>

<template>
  <div class="orders-view">
    <section class="panel">
      <!-- Filter bar -->
      <div class="filter-bar">
        <div class="filter-row">
          <label class="filter-field">
            <span>ID</span>
            <input v-model="filterId" type="text" placeholder="订单 ID" />
          </label>
          <label class="filter-field">
            <span>UTR</span>
            <input v-model="filterOtherOrderId" type="text" placeholder="上游订单号" />
          </label>
          <label class="filter-field">
            <span>商户</span>
            <select v-model="filterAccount" size="1">
              <option value="">全部商户</option>
              <option v-for="m in merchantOptions" :key="m.platformId" :value="m.account">
                {{ m.account }}
              </option>
            </select>
          </label>
          <label class="filter-field">
            <span>渠道</span>
            <select v-model="filterChannelId" size="1">
              <option value="">全部渠道</option>
              <option v-for="c in channelOptions" :key="c.id" :value="c.id">
                {{ c.title || c.id }}
              </option>
            </select>
          </label>
          <label class="filter-field">
            <span>状态</span>
            <select v-model="filterStatus" size="1">
              <option value="">全部状态</option>
              <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </label>
        </div>
        <div class="filter-row">
          <label class="filter-field">
            <span>创建时间</span>
            <DateTimeRangePicker
              v-model:start="filterCreateStart"
              v-model:end="filterCreateEnd"
              placeholder="开始日期 ~ 结束日期"
            />
          </label>
          <label class="filter-field">
            <span>付款时间</span>
            <DateTimeRangePicker
              v-model:start="filterPayStart"
              v-model:end="filterPayEnd"
              placeholder="开始日期 ~ 结束日期"
            />
          </label>
          <div class="filter-actions">
            <button type="button" class="btn-primary" @click="handleSearch">搜索</button>
            <button type="button" class="ghost-btn" @click="handleReset">重置</button>
          </div>
        </div>
      </div>

      <!-- Callback feedback -->
      <p v-if="callbackMsg" class="callback-msg">{{ callbackMsg }}</p>

      <!-- Table -->
      <div class="table-wrapper">
        <p v-if="tableError" class="error-text">{{ tableError }}</p>
        <p v-else-if="tableLoading" class="muted">正在加载数据...</p>
        <table v-if="!tableLoading">
          <thead>
            <tr>
              <th v-for="col in tableColumns" :key="col.key">{{ col.label }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!tableRows.length">
              <td :colspan="tableColumns.length" class="empty-cell">暂无数据</td>
            </tr>
            <tr v-for="(row, index) in tableRows" :key="row.id ?? index">
              <template v-for="col in tableColumns" :key="col.key">
                <td v-if="col.key === 'times'" class="times-cell">
                  <span>{{ formatDateTime(row.createTime) }}</span>
                  <span>{{ formatDateTime(row.payTime) }}</span>
                  <span>{{ formatDateTime(row.createdAt) }}</span>
                </td>
                <td v-else-if="col.key === 'actions'" class="actions-cell">
                  <button
                    class="action-btn callback-btn"
                    :disabled="callbackingOrderId === row.orderId"
                    @click="openConfirmModal(row)"
                  >
                    {{ callbackingOrderId === row.orderId ? '处理中…' : '手动回调' }}
                  </button>
                  <button
                    class="action-btn log-btn"
                    @click="openCallbackModal(row)"
                  >
                    回调记录
                  </button>
                </td>
                <td v-else :class="col.key === 'status' ? `status-${row.status}` : ''">
                  {{ displayCell(row, col.key) }}
                </td>
              </template>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="pagination-bar">
        <div class="page-info">共 {{ tableTotal }} 条 · 第 {{ tablePage }} / {{ totalPages }} 页</div>
        <div class="pagination-controls">
          <button class="ghost-btn" :disabled="tablePage <= 1" @click="changePage(tablePage - 1)">上一页</button>
          <button class="ghost-btn" :disabled="tablePage >= totalPages" @click="changePage(tablePage + 1)">下一页</button>
          <label>
            <span>每页</span>
            <select :value="tablePageSize" @change="changePageSize(Number($event.target.value))">
              <option v-for="s in paginationSizes" :key="s" :value="s">{{ s }}</option>
            </select>
          </label>
        </div>
      </div>
    </section>

    <!-- Manual callback confirm modal -->
    <Teleport to="body">
      <div v-if="showConfirmModal" class="modal-mask" @click.self="closeConfirmModal">
        <div class="modal-box confirm-box">
          <div class="modal-header">
            <span class="modal-title">确认手动回调</span>
            <button class="modal-close" @click="closeConfirmModal">✕</button>
          </div>
          <div class="modal-body confirm-body">
            <p class="confirm-text">确认对以下订单执行手动回调？</p>
            <p class="confirm-order-id">{{ confirmRow?.orderId }}</p>
            <p class="confirm-tip">此操作将强制触发支付成功流程，请确保三方已实际到账。</p>
          </div>
          <div class="confirm-footer">
            <button class="ghost-btn" @click="closeConfirmModal">取消</button>
            <button class="btn-primary btn-danger" @click="doManualCallback">确认执行</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Callback records modal -->
    <Teleport to="body">
      <div v-if="showCallbackModal" class="modal-mask" @click.self="closeCallbackModal">
        <div class="modal-box">
          <div class="modal-header">
            <span class="modal-title">{{ callbackModalTitle }}</span>
            <button class="modal-close" @click="closeCallbackModal">✕</button>
          </div>
          <div class="modal-body">
            <p v-if="callbackLoading" class="muted">正在加载...</p>
            <p v-else-if="callbackError" class="error-text">{{ callbackError }}</p>
            <template v-else>
              <div class="modal-table-wrapper">
                <table class="modal-table">
                  <thead>
                    <tr>
                      <th v-for="col in callbackColumns" :key="col.key">{{ col.label }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-if="!callbackRecords.length">
                      <td :colspan="callbackColumns.length" class="empty-cell">暂无回调记录</td>
                    </tr>
                    <tr v-for="(rec, i) in callbackRecords" :key="i">
                      <td>{{ rec.orderId || '—' }}</td>
                      <td class="mono-cell">{{ truncate(rec.platformNo) }}</td>
                      <td class="url-cell">{{ truncate(rec.reqUrl, 60) }}</td>
                      <td class="param-cell" :title="rec.param">{{ truncate(rec.param) }}</td>
                      <td>{{ rec.respCode ?? '—' }}</td>
                      <td>{{ formatDateTime(rec.createTime) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </template>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.orders-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.panel {
  border-radius: 24px;
  padding: 24px;
  background: rgba(18, 19, 37, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.muted { color: rgba(255, 255, 255, 0.7); }

/* Filter bar */
.filter-bar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 16px;
  padding: 16px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.filter-field input,
.filter-field select {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #fff;
  padding: 6px 10px;
  border-radius: 10px;
  min-width: 130px;
  font-size: 13px;
  appearance: auto;
}

.filter-field select option {
  background: #1a1b2e;
  color: #fff;
}

.filter-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-left: auto;
}

.callback-msg {
  font-size: 13px;
  color: #4ade80;
  padding: 4px 0;
}

/* Table */
.table-wrapper { overflow-x: auto; }

.table-wrapper table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.table-wrapper th,
.table-wrapper td {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  text-align: left;
  white-space: nowrap;
}

.table-wrapper th {
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}

.table-wrapper td { color: rgba(255, 255, 255, 0.9); }

.times-cell {
  white-space: normal;
  min-width: 140px;
}
.times-cell span {
  display: block;
  font-size: 11px;
  line-height: 1.35;
  color: rgba(255, 255, 255, 0.85);
}
.times-cell span:nth-child(2) { color: rgba(255, 255, 255, 0.6); }
.times-cell span:nth-child(3) { color: rgba(255, 255, 255, 0.45); }

.table-wrapper .empty-cell {
  text-align: center;
  padding: 20px;
  color: rgba(255, 255, 255, 0.6);
}

td.status-1 { color: #4ade80; }
td.status-0 { color: #facc15; }
td.status-2 { color: rgba(255, 255, 255, 0.45); }
td.status-3 { color: #f87171; }

/* Action buttons */
.actions-cell {
  display: flex;
  gap: 6px;
  white-space: nowrap;
}

.action-btn {
  border: none;
  border-radius: 999px;
  cursor: pointer;
  font-size: 12px;
  padding: 4px 12px;
  transition: opacity 0.15s;
}
.action-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.callback-btn {
  background: rgba(99, 102, 241, 0.75);
  color: #fff;
}
.callback-btn:hover:not(:disabled) { background: rgba(99, 102, 241, 1); }

.log-btn {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.15);
}
.log-btn:hover { background: rgba(255, 255, 255, 0.18); }

/* Pagination */
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.page-info { font-size: 13px; color: rgba(255, 255, 255, 0.6); }

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-controls label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.pagination-controls select {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #fff;
  padding: 4px 10px;
  border-radius: 10px;
}

/* Buttons */
.btn-primary {
  background: rgba(99, 102, 241, 0.8);
  border: none;
  color: #fff;
  padding: 7px 20px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 13px;
}
.btn-primary:hover { background: rgba(99, 102, 241, 1); }

.ghost-btn {
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: transparent;
  color: #fff;
  padding: 6px 16px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 13px;
}
.ghost-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.error-text { color: #ff8e8e; }

/* Modal */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.65);
  z-index: 9000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-box {
  background: #15162a;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  width: 90vw;
  max-width: 1000px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.modal-title {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}

.modal-close {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  padding: 4px 8px;
  border-radius: 6px;
}
.modal-close:hover { color: #fff; background: rgba(255, 255, 255, 0.08); }

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px 24px;
}

.modal-table-wrapper { overflow-x: auto; }

.modal-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.modal-table th,
.modal-table td {
  padding: 9px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  text-align: left;
}

.modal-table th {
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
  white-space: nowrap;
}

.modal-table td {
  color: rgba(255, 255, 255, 0.88);
}

.mono-cell {
  font-family: monospace;
  font-size: 12px;
  max-width: 180px;
  word-break: break-all;
  white-space: normal;
}

.url-cell {
  font-size: 12px;
  max-width: 200px;
  word-break: break-all;
  white-space: normal;
}

.param-cell {
  font-size: 12px;
  max-width: 240px;
  word-break: break-all;
  white-space: normal;
  cursor: help;
}

.empty-cell {
  text-align: center;
  padding: 20px;
  color: rgba(255, 255, 255, 0.5);
}

/* Confirm modal */
.confirm-box {
  max-width: 420px;
  width: 90vw;
}

.confirm-body {
  padding: 20px 24px 8px;
}

.confirm-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.75);
  margin: 0 0 10px;
}

.confirm-order-id {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  font-family: monospace;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  padding: 8px 12px;
  margin: 0 0 12px;
  word-break: break-all;
}

.confirm-tip {
  font-size: 12px;
  color: #facc15;
  margin: 0;
}

.confirm-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.btn-danger {
  background: rgba(239, 68, 68, 0.8);
}
.btn-danger:hover { background: rgba(239, 68, 68, 1); }
</style>
