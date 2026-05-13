<script setup>
import { computed, onMounted, ref } from 'vue';
import http from '../services/http.js';
import { AREA_TYPE_MAP } from '../data/constants.js';

defineProps({ data: { type: Object, default: null } });

// ── Tabs ──────────────────────────────────────────────────────────────
const TABS = [
  { key: 'summary-today',   label: 'Summary/Today'   },
  { key: 'summary-payin',   label: 'Summary/PayIn'   },
  { key: 'summary-payout',  label: 'Summary/PayOut'  },
  { key: 'merchant-payin',  label: 'Merchant/PayIn'  },
  { key: 'merchant-payout', label: 'Merchant/PayOut' },
];
const activeTab = ref('summary-payin');

// ── Date filter ───────────────────────────────────────────────────────
const DATE_OPTIONS = [
  { label: 'Today',              value: 'today' },
  { label: 'This Week',          value: 'week'  },
  { label: 'This Month',         value: 'month' },
  { label: 'Previous 2 Months',  value: '2m'    },
];
const dateType         = ref('2m');
const dateDropdownOpen = ref(false);

const currentDateLabel = computed(
  () => DATE_OPTIONS.find(d => d.value === dateType.value)?.label ?? 'Previous 2 Months'
);

const selectDate = (val) => {
  dateType.value = val;
  dateDropdownOpen.value = false;
  tablePage.value = 1;
  fetchData();
};

const resetDate = (e) => {
  e.stopPropagation();
  dateType.value = '2m';
  tablePage.value = 1;
  fetchData();
};

// ── Currency filter ───────────────────────────────────────────────────
const CURRENCY_OPTIONS = Object.entries(AREA_TYPE_MAP).map(([id, info]) => ({
  areaType: Number(id),
  currencyCode: info.currencyCode,
  label: info.label,
}));

const selectedAreaType     = ref(2); // default INR
const currencyDropdownOpen = ref(false);

const currentCurrencyLabel = computed(
  () => CURRENCY_OPTIONS.find(c => c.areaType === selectedAreaType.value)?.currencyCode ?? ''
);

const selectCurrency = (areaType) => {
  selectedAreaType.value = areaType;
  currencyDropdownOpen.value = false;
  tablePage.value = 1;
  fetchData();
};

// ── Table state ───────────────────────────────────────────────────────
const tableRows     = ref([]);
const tableTotal    = ref(0);
const tablePage     = ref(1);
const tablePageSize = ref(20);
const tableLoading  = ref(false);
const tableError    = ref('');

const totalPages = computed(() =>
  tableTotal.value ? Math.max(1, Math.ceil(tableTotal.value / tablePageSize.value)) : 1
);
const pageStart = computed(() => tableTotal.value ? (tablePage.value - 1) * tablePageSize.value + 1 : 0);
const pageEnd   = computed(() => Math.min(tablePage.value * tablePageSize.value, tableTotal.value));

// ── Fetch ─────────────────────────────────────────────────────────────
const fetchData = async () => {
  if (activeTab.value !== 'summary-payin') return;
  tableLoading.value = true;
  tableError.value = '';
  try {
    const params = {
      pageNum:  tablePage.value,
      pageSize: tablePageSize.value,
    };
    if (dateType.value)            params.dateType  = dateType.value;
    if (selectedAreaType.value)    params.areaType  = selectedAreaType.value;
    const { data: res } = await http.get('/admin/pay/dashboard/payinSummaryPage', { params });
    const payload = res?.data ?? res;
    tableRows.value  = payload?.records ?? [];
    tableTotal.value = payload?.total   ?? 0;
    tablePage.value  = Number(payload?.current ?? tablePage.value);
  } catch (e) {
    console.error('Failed to load payin summary', e);
    tableError.value = '加载失败';
    tableRows.value  = [];
    tableTotal.value = 0;
  } finally {
    tableLoading.value = false;
  }
};

const changePage = (next) => {
  if (next < 1 || next > totalPages.value || next === tablePage.value) return;
  tablePage.value = next;
  fetchData();
};

// ── Formatting ────────────────────────────────────────────────────────
const fmtNum = (val) => {
  if (val === null || val === undefined) return '—';
  const n = Number(val);
  if (isNaN(n)) return String(val);
  return n.toLocaleString('en', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
};

const fmtRate = (val) => {
  if (val === null || val === undefined) return '0%';
  return `${Number(val).toFixed(2)}%`;
};

// ── Outside click ─────────────────────────────────────────────────────
const dateFilterRef     = ref(null);
const currencyFilterRef = ref(null);

const handleOutsideClick = (e) => {
  if (dateDropdownOpen.value     && dateFilterRef.value     && !dateFilterRef.value.contains(e.target))     dateDropdownOpen.value = false;
  if (currencyDropdownOpen.value && currencyFilterRef.value && !currencyFilterRef.value.contains(e.target)) currencyDropdownOpen.value = false;
};

onMounted(() => {
  document.addEventListener('click', handleOutsideClick);
  fetchData();
});
</script>

<template>
  <div class="overview-wrap">

    <!-- ── Tab bar ──────────────────────────────────────────────────── -->
    <div class="tab-bar">
      <button
        v-for="tab in TABS"
        :key="tab.key"
        class="tab-btn"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key; fetchData()"
      >{{ tab.label }}</button>
    </div>

    <!-- ── Summary/PayIn ────────────────────────────────────────────── -->
    <template v-if="activeTab === 'summary-payin'">

      <!-- Filters -->
      <div class="filter-row">

        <!-- Date -->
        <div class="filter-field" ref="dateFilterRef">
          <span class="field-label">Date <span class="req">*</span></span>
          <button class="date-chip" @click.stop="dateDropdownOpen = !dateDropdownOpen">
            {{ currentDateLabel }}
            <svg @click="resetDate" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="reset-icon"><path d="M23 4v6h-6M1 20v-6h6"/><path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/></svg>
          </button>
          <div v-if="dateDropdownOpen" class="date-dropdown" @click.stop>
            <button
              v-for="opt in DATE_OPTIONS"
              :key="opt.value"
              class="date-opt"
              :class="{ active: dateType === opt.value }"
              @click="selectDate(opt.value)"
            >{{ opt.label }}</button>
          </div>
        </div>

        <!-- Currency -->
        <div class="filter-field" ref="currencyFilterRef">
          <span class="field-label">currency <span class="req">*</span></span>
          <button class="date-chip" @click.stop="currencyDropdownOpen = !currencyDropdownOpen">
            {{ currentCurrencyLabel }}
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M6 9l6 6 6-6"/></svg>
          </button>
          <div v-if="currencyDropdownOpen" class="date-dropdown currency-dropdown" @click.stop>
            <button
              v-for="opt in CURRENCY_OPTIONS"
              :key="opt.areaType"
              class="date-opt"
              :class="{ active: selectedAreaType === opt.areaType }"
              @click="selectCurrency(opt.areaType)"
            >{{ opt.currencyCode }} · {{ opt.label }}</button>
          </div>
        </div>

      </div>

      <!-- Table card: PayIn Summary/Merchant-RF -->
      <div class="table-card">
        <div class="card-header">
          <span class="card-title">PayIn Summary/Merchant-RF</span>
          <div class="card-actions">
            <span v-if="tableLoading" class="loading-hint">加载中...</span>
            <span v-if="tableError" class="error-hint">{{ tableError }}</span>
            <button class="more-btn">···</button>
          </div>
        </div>

        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>local_time: Day <span class="sort-arrow">↑</span></th>
                <th>Merchant <span class="sort-arrow">↑</span></th>
                <th>channel <span class="sort-arrow">↑</span></th>
                <th class="num-col"><span class="sort-arrow">↑</span> successAmount</th>
                <th class="num-col"><span class="sort-arrow">↑</span> successRate</th>
                <th class="num-col"><span class="sort-arrow">↑</span> orderNum</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!tableRows.length && !tableLoading">
                <td colspan="6" class="empty-cell">暂无数据</td>
              </tr>
              <tr v-for="(row, i) in tableRows" :key="i">
                <td>{{ row.localTimeDay ?? '—' }}</td>
                <td>{{ row.merchant ?? '—' }}</td>
                <td>{{ row.channel ?? '—' }}</td>
                <td class="num-col">{{ fmtNum(row.successAmount) }}</td>
                <td class="num-col">{{ fmtRate(row.successRate) }}</td>
                <td class="num-col">{{ fmtNum(row.orderNum) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination-bar">
          <span class="page-info">Rows {{ pageStart }}-{{ pageEnd }} of {{ tableTotal }}</span>
          <div class="page-nav">
            <button class="nav-btn" :disabled="tablePage <= 1" @click="changePage(tablePage - 1)">&#8249;</button>
            <button class="nav-btn" :disabled="tablePage >= totalPages" @click="changePage(tablePage + 1)">&#8250;</button>
          </div>
        </div>
      </div>

      <!-- Placeholder: PayIn Amount-RS -->
      <div class="table-card placeholder-card">
        <div class="card-header">
          <span class="card-title">PayIn Amount-RS</span>
        </div>
        <p class="placeholder-text">即将上线</p>
      </div>

    </template>

    <!-- ── Other tabs: placeholder ──────────────────────────────────── -->
    <template v-else>
      <div class="table-card placeholder-card">
        <p class="placeholder-text">{{ TABS.find(t => t.key === activeTab)?.label }} — 即将上线</p>
      </div>
    </template>

  </div>
</template>

<style scoped>
.overview-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ── Tab bar ─────────────────────────────────────────────────────────── */
.tab-bar {
  display: flex;
  gap: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.tab-btn {
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  padding: 10px 20px;
  color: rgba(255, 255, 255, 0.55);
  font-size: 13px;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
  margin-bottom: -1px;
}

.tab-btn:hover { color: rgba(255, 255, 255, 0.85); }

.tab-btn.active {
  color: #60a5fa;
  border-bottom-color: #60a5fa;
}

/* ── Filter row ──────────────────────────────────────────────────────── */
.filter-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: flex-end;
}

.filter-field {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 6px;
  padding: 6px 12px 8px;
  min-width: 180px;
}

.field-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: 4px;
}

.req { color: #60a5fa; }

.date-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}

.reset-icon {
  opacity: 0.6;
  flex-shrink: 0;
}
.reset-icon:hover { opacity: 1; }

.date-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  z-index: 100;
  background: #1a1b2e;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  padding: 6px;
  min-width: 180px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.date-opt {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  padding: 8px 12px;
  text-align: left;
  border-radius: 6px;
  cursor: pointer;
}
.date-opt:hover { background: rgba(255, 255, 255, 0.06); }
.date-opt.active {
  background: rgba(96, 165, 250, 0.15);
  color: #60a5fa;
}

.currency-dropdown {
  max-height: 260px;
  overflow-y: auto;
}

.currency-input {
  background: none;
  border: none;
  outline: none;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  padding: 0;
  width: 120px;
}

/* ── Table card ──────────────────────────────────────────────────────── */
.table-card {
  border-radius: 12px;
  background: rgba(18, 19, 37, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.07);
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.card-title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.more-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  font-size: 18px;
  cursor: pointer;
  padding: 0 4px;
  letter-spacing: 2px;
  line-height: 1;
}
.more-btn:hover { color: rgba(255, 255, 255, 0.9); }

.loading-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.error-hint {
  font-size: 12px;
  color: #f87171;
}

/* ── Table ───────────────────────────────────────────────────────────── */
.table-wrapper {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

thead th {
  padding: 10px 16px;
  text-align: left;
  color: rgba(255, 255, 255, 0.5);
  font-weight: 500;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  white-space: nowrap;
}

thead th.num-col { text-align: right; }

.sort-arrow {
  opacity: 0.5;
  font-size: 11px;
  margin-left: 2px;
}

tbody tr {
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  transition: background 0.1s;
}

tbody tr:hover { background: rgba(255, 255, 255, 0.03); }
tbody tr:last-child { border-bottom: none; }

tbody td {
  padding: 10px 16px;
  color: rgba(255, 255, 255, 0.85);
}

tbody td.num-col { text-align: right; }

.empty-cell {
  text-align: center;
  color: rgba(255, 255, 255, 0.3);
  padding: 40px;
}

/* ── Pagination ──────────────────────────────────────────────────────── */
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.page-info {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
}

.page-nav { display: flex; gap: 2px; }

.nav-btn {
  background: none;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 18px;
  width: 28px;
  height: 28px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.nav-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.nav-btn:disabled {
  opacity: 0.3;
  cursor: default;
}

/* ── Placeholder ─────────────────────────────────────────────────────── */
.placeholder-card {
  padding: 32px 20px;
}

.placeholder-text {
  text-align: center;
  color: rgba(255, 255, 255, 0.3);
  font-size: 13px;
  margin: 0;
}
</style>
