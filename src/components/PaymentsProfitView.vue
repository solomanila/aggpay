<script setup>
import { computed, onMounted, ref } from 'vue';
import http from '../services/http.js';

defineProps({
  data: { type: Object, required: true }
});

// ── State ─────────────────────────────────────────────────────────
const activeTab = ref('payin');

const searchName   = ref('');
const searchStart  = ref('');
const searchEnd    = ref('');

const tableRows    = ref([]);
const tableTotal   = ref(0);
const tablePage    = ref(1);
const tablePageSize = ref(20);
const tableLoading = ref(false);
const tableError   = ref('');

const paginationSizes = [10, 20, 50];
const totalPages = computed(() =>
  tableTotal.value ? Math.max(1, Math.ceil(tableTotal.value / tablePageSize.value)) : 1
);

// ── Columns ───────────────────────────────────────────────────────
const payinCols = [
  { key: 'statDate',          label: '日期' },
  { key: 'channelName',       label: '名称' },
  { key: 'channelType',       label: '通道类型' },
  { key: 'systemAmount',      label: '系统计算金额' },
  { key: 'collectionAmount',  label: '代收户金额' },
  { key: 'channelFeeIncome',  label: '通道费收入' },
  { key: 'droppedOrderIncome', label: '掉单收入' },
  { key: 'channelCost',       label: '通道成本' },
  { key: 'otherCost',         label: '其他成本' },
  { key: 'frozenAmount',      label: '冻结金额' },
  { key: 'adjustment',        label: '调差' },
  { key: 'profit',            label: '利润' }
];

const payoutCols = [
  { key: 'statDate',         label: '日期' },
  { key: 'channelName',      label: '名称' },
  { key: 'channelType',      label: '通道类型' },
  { key: 'systemAmount',     label: '系统计算金额' },
  { key: 'channelFeeIncome', label: '通道费收入' },
  { key: 'channelCost',      label: '通道成本' },
  { key: 'otherCost',        label: '其他成本' },
  { key: 'frozenAmount',     label: '冻结金额' },
  { key: 'adjustment',       label: '调差' },
  { key: 'profit',           label: '利润' }
];

const cols = computed(() => activeTab.value === 'payin' ? payinCols : payoutCols);

// ── API ───────────────────────────────────────────────────────────
const fetchList = async () => {
  tableLoading.value = true;
  tableError.value   = '';
  try {
    const params = { pageNum: tablePage.value, pageSize: tablePageSize.value };
    if (searchName.value.trim())  params.channelName = searchName.value.trim();
    if (searchStart.value.trim()) params.startDate   = searchStart.value.trim();
    if (searchEnd.value.trim())   params.endDate     = searchEnd.value.trim();

    const { data: res } = await http.get('/admin/channel-profit/list', { params });
    const payload = res?.data ?? res;
    tableRows.value     = payload?.records ?? [];
    tableTotal.value    = payload?.total   ?? 0;
    tablePage.value     = payload?.current ?? tablePage.value;
    tablePageSize.value = payload?.size    ?? tablePageSize.value;
  } catch (err) {
    console.error('Failed to load channel profit stats', err);
    tableError.value = '无法加载利润报表数据';
    tableRows.value  = [];
    tableTotal.value = 0;
  } finally {
    tableLoading.value = false;
  }
};

const handleSearch = () => { tablePage.value = 1; fetchList(); };
const handleReset  = () => {
  searchName.value  = '';
  searchStart.value = '';
  searchEnd.value   = '';
  tablePage.value   = 1;
  fetchList();
};
const changePage     = (n) => { if (n < 1 || n > totalPages.value || n === tablePage.value) return; tablePage.value = n; fetchList(); };
const changePageSize = (s) => { tablePageSize.value = s; tablePage.value = 1; fetchList(); };
const switchTab      = (t) => { activeTab.value = t; tablePage.value = 1; fetchList(); };

const fmt = (v) => (v == null ? '' : v);

onMounted(fetchList);
</script>

<template>
  <div class="profit-view">
    <section class="panel list-panel">
      <!-- Tabs -->
      <div class="tab-bar">
        <button type="button" class="tab-btn" :class="{ active: activeTab === 'payin' }"  @click="switchTab('payin')">收款通道</button>
        <button type="button" class="tab-btn" :class="{ active: activeTab === 'payout' }" @click="switchTab('payout')">出款通道</button>
      </div>

      <!-- Search bar -->
      <div class="search-bar">
        <input v-model="searchName" class="search-input" placeholder="输入名称" @keyup.enter="handleSearch" />
        <div class="date-range">
          <input v-model="searchStart" type="date" class="date-input" />
          <span class="date-sep">-</span>
          <input v-model="searchEnd"   type="date" class="date-input" />
        </div>
        <button type="button" class="btn btn-primary" @click="handleSearch">
          <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:middle;margin-right:4px"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>搜索
        </button>
        <button type="button" class="btn btn-ghost" @click="handleReset">
          <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:middle;margin-right:4px"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>重置
        </button>
      </div>

      <!-- Toolbar -->
      <div class="toolbar">
        <button type="button" class="btn btn-ghost">下载</button>
        <button type="button" class="btn btn-ghost">导入</button>
      </div>

      <!-- Table -->
      <div class="table-wrapper">
        <p v-if="tableError"   class="error-text">{{ tableError }}</p>
        <p v-else-if="tableLoading" class="muted">正在加载...</p>
        <table v-if="!tableLoading">
          <thead>
            <tr>
              <th v-for="col in cols" :key="col.key">{{ col.label }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!tableRows.length">
              <td :colspan="cols.length" class="empty-cell">暂无数据</td>
            </tr>
            <tr v-for="(row, i) in tableRows" :key="row.id ?? i">
              <td v-for="col in cols" :key="col.key">{{ fmt(row[col.key]) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="pagination-bar">
        <span class="page-info">共 {{ tableTotal }} 条</span>
        <div class="pagination-controls">
          <button type="button" class="ghost-btn" :disabled="tablePage <= 1"            @click="changePage(tablePage - 1)">&lt;</button>
          <span class="page-num">{{ tablePage }}</span>
          <button type="button" class="ghost-btn" :disabled="tablePage >= totalPages"   @click="changePage(tablePage + 1)">&gt;</button>
          <select :value="tablePageSize" class="size-select" @change="changePageSize(Number($event.target.value))">
            <option v-for="s in paginationSizes" :key="s" :value="s">{{ s }} 条/页</option>
          </select>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.profit-view { display: flex; flex-direction: column; gap: 24px; }

.panel {
  border-radius: 24px; padding: 24px;
  background: rgba(18, 19, 37, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.list-panel { display: flex; flex-direction: column; gap: 14px; }

/* Tabs */
.tab-bar {
  display: flex; gap: 4px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08); margin-bottom: 4px;
}
.tab-btn {
  background: transparent; border: none;
  color: rgba(255, 255, 255, 0.55); padding: 8px 18px; font-size: 14px;
  cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -1px;
}
.tab-btn.active { color: #60a5fa; border-bottom-color: #60a5fa; }

/* Search */
.search-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.search-input {
  flex: 1; min-width: 180px; max-width: 280px;
  background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px; color: #fff; padding: 8px 14px; font-size: 13px; outline: none;
}
.search-input::placeholder { color: rgba(255, 255, 255, 0.35); }
.search-input:focus { border-color: rgba(127, 133, 249, 0.6); }

.date-range { display: flex; align-items: center; gap: 6px; }
.date-input {
  background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px; color: rgba(255, 255, 255, 0.85); padding: 8px 10px; font-size: 13px;
  outline: none; cursor: pointer; width: 135px;
}
.date-input:focus { border-color: rgba(127, 133, 249, 0.6); }
.date-input::-webkit-calendar-picker-indicator { filter: invert(0.8); }
.date-sep { color: rgba(255, 255, 255, 0.5); font-size: 14px; }

/* Buttons */
.btn { display: inline-flex; align-items: center; padding: 8px 16px; border-radius: 10px; font-size: 13px; cursor: pointer; border: none; white-space: nowrap; }
.btn-primary { background: rgba(100, 116, 255, 0.85); color: #fff; }
.btn-primary:hover { background: rgba(100, 116, 255, 1); }
.btn-ghost { background: rgba(255, 255, 255, 0.07); color: rgba(255, 255, 255, 0.8); border: 1px solid rgba(255, 255, 255, 0.15); }
.btn-ghost:hover { background: rgba(255, 255, 255, 0.12); }

.toolbar { display: flex; gap: 10px; }

/* Table */
.muted { color: rgba(255, 255, 255, 0.7); }
.error-text { color: #ff8e8e; }
.table-wrapper { overflow-x: auto; }
.table-wrapper table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table-wrapper th,
.table-wrapper td { padding: 10px 12px; border-bottom: 1px solid rgba(255, 255, 255, 0.07); text-align: left; white-space: nowrap; }
.table-wrapper th { color: rgba(255, 255, 255, 0.6); font-weight: 500; background: rgba(255, 255, 255, 0.02); }
.table-wrapper td { color: rgba(255, 255, 255, 0.88); }
.empty-cell { text-align: center; padding: 28px; color: rgba(255, 255, 255, 0.45); }

/* Pagination */
.pagination-bar { display: flex; align-items: center; justify-content: flex-end; gap: 12px; flex-wrap: wrap; }
.page-info { font-size: 13px; color: rgba(255, 255, 255, 0.55); margin-right: auto; }
.pagination-controls { display: flex; align-items: center; gap: 8px; }
.ghost-btn { border: 1px solid rgba(255, 255, 255, 0.2); background: transparent; color: rgba(255, 255, 255, 0.8); width: 30px; height: 30px; border-radius: 8px; cursor: pointer; font-size: 13px; }
.ghost-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.page-num { font-size: 13px; color: #fff; min-width: 20px; text-align: center; }
.size-select { background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.15); color: rgba(255, 255, 255, 0.8); padding: 4px 10px; border-radius: 8px; font-size: 13px; cursor: pointer; }
</style>
