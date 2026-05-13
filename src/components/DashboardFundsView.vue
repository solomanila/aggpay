<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import http from '../services/http.js';
import { AREA_TYPE_MAP } from '../data/constants.js';

defineProps({ data: { type: Object, default: null } });

// ── Constants ─────────────────────────────────────────────────────────
const DATE_OPTIONS = [
  { label: 'Today',      value: 'today' },
  { label: 'This Week',  value: 'week'  },
  { label: 'This Month', value: 'month' },
];

const CURRENCY_OPTIONS = Object.entries(AREA_TYPE_MAP).map(([id, info]) => ({
  id: Number(id),
  currencyCode: info.currencyCode,
  label: info.label,
}));

// ── Filter state ──────────────────────────────────────────────────────
const dateType         = ref('month');
const dateDropdownOpen = ref(false);
const dateDropdownRef  = ref(null);

const merchantOptions      = ref([]);
const merchantSearch       = ref('');
const merchantDropdownOpen = ref(false);
const merchantDropdownRef  = ref(null);
const pendingPlatformIds   = ref([]);
const appliedPlatformIds   = ref([]);

const currencyDropdownOpen = ref(false);
const currencyDropdownRef  = ref(null);
const pendingAreaTypes     = ref([]);
const appliedAreaTypes     = ref([]);

const refIdDropdownOpen = ref(false);
const refIdDropdownRef  = ref(null);
const pendingRefId      = ref('');
const appliedRefId      = ref('');

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
const pageStart = computed(() => (tablePage.value - 1) * tablePageSize.value + 1);
const pageEnd   = computed(() => Math.min(tablePage.value * tablePageSize.value, tableTotal.value));

// ── Date ──────────────────────────────────────────────────────────────
const currentDateLabel = computed(
  () => DATE_OPTIONS.find(d => d.value === dateType.value)?.label ?? 'This Month'
);

const selectDate = (val) => {
  dateType.value = val;
  dateDropdownOpen.value = false;
  tablePage.value = 1;
  fetchFundFlow();
};

// ── Merchant dropdown ─────────────────────────────────────────────────
const filteredMerchants = computed(() => {
  const q = merchantSearch.value.trim().toLowerCase();
  return q
    ? merchantOptions.value.filter(m => m.account.toLowerCase().includes(q))
    : merchantOptions.value;
});

const allMerchantsSelected = computed(() =>
  filteredMerchants.value.length > 0 &&
  filteredMerchants.value.every(m => pendingPlatformIds.value.includes(m.platformId))
);

const toggleSelectAllMerchants = () => {
  if (allMerchantsSelected.value) {
    const ids = filteredMerchants.value.map(m => m.platformId);
    pendingPlatformIds.value = pendingPlatformIds.value.filter(id => !ids.includes(id));
  } else {
    const ids = filteredMerchants.value.map(m => m.platformId);
    pendingPlatformIds.value = [...new Set([...pendingPlatformIds.value, ...ids])];
  }
};

const openMerchantDropdown = () => {
  pendingPlatformIds.value = [...appliedPlatformIds.value];
  merchantSearch.value = '';
  merchantDropdownOpen.value = true;
};

const applyMerchantFilter = () => {
  appliedPlatformIds.value = [...pendingPlatformIds.value];
  merchantDropdownOpen.value = false;
  tablePage.value = 1;
  fetchFundFlow();
};

const merchantBtnLabel = computed(() => {
  const n = appliedPlatformIds.value.length;
  if (!n) return 'Merchant';
  if (n === 1) return merchantOptions.value.find(m => m.platformId === appliedPlatformIds.value[0])?.account ?? '1 selected';
  return `${n} selected`;
});

// ── Currency dropdown ─────────────────────────────────────────────────
const allCurrenciesSelected = computed(() =>
  CURRENCY_OPTIONS.every(c => pendingAreaTypes.value.includes(c.id))
);

const toggleSelectAllCurrencies = () => {
  pendingAreaTypes.value = allCurrenciesSelected.value ? [] : CURRENCY_OPTIONS.map(c => c.id);
};

const openCurrencyDropdown = () => {
  pendingAreaTypes.value = [...appliedAreaTypes.value];
  currencyDropdownOpen.value = true;
};

const applyCurrencyFilter = () => {
  appliedAreaTypes.value = [...pendingAreaTypes.value];
  currencyDropdownOpen.value = false;
  tablePage.value = 1;
  fetchFundFlow();
};

const currencyBtnLabel = computed(() => {
  const n = appliedAreaTypes.value.length;
  if (!n) return 'Currency';
  if (n === 1) return CURRENCY_OPTIONS.find(c => c.id === appliedAreaTypes.value[0])?.currencyCode ?? '1 selected';
  return `${n} selected`;
});

// ── RefId dropdown ────────────────────────────────────────────────────
const openRefIdDropdown = () => {
  pendingRefId.value = appliedRefId.value;
  refIdDropdownOpen.value = true;
};

const applyRefIdFilter = () => {
  appliedRefId.value = pendingRefId.value.trim();
  refIdDropdownOpen.value = false;
  tablePage.value = 1;
  fetchFundFlow();
};

const refIdBtnLabel = computed(() => appliedRefId.value || 'Ref ID');

// ── API ───────────────────────────────────────────────────────────────
const fetchMerchants = async () => {
  try {
    const { data: resp } = await http.get('/admin/fund-flow/merchants');
    merchantOptions.value = resp?.data ?? resp ?? [];
  } catch (e) {
    console.error('Failed to load merchants', e);
  }
};

const fetchFundFlow = async () => {
  tableLoading.value = true;
  tableError.value = '';
  try {
    const params = {
      dateType: dateType.value,
      pageNum: tablePage.value,
      pageSize: tablePageSize.value,
    };
    if (appliedPlatformIds.value.length) params.platformIds = appliedPlatformIds.value.join(',');
    if (appliedAreaTypes.value.length)   params.areaTypes   = appliedAreaTypes.value.join(',');
    if (appliedRefId.value)              params.refId        = appliedRefId.value;

    const { data: resp } = await http.get('/admin/fund-flow/list', { params });
    const payload = resp?.data ?? resp;
    tableRows.value  = payload?.records ?? [];
    tableTotal.value = payload?.total   ?? 0;
    tablePage.value  = Number(payload?.current ?? tablePage.value);
  } catch (e) {
    console.error('Failed to load fund flow', e);
    tableError.value = '加载失败，请稍后重试';
    tableRows.value  = [];
    tableTotal.value = 0;
  } finally {
    tableLoading.value = false;
  }
};

// ── Pagination ────────────────────────────────────────────────────────
const changePage = (next) => {
  if (next < 1 || next > totalPages.value || next === tablePage.value) return;
  tablePage.value = next;
  fetchFundFlow();
};

// ── Formatting ────────────────────────────────────────────────────────
const fmtDate = (val) => {
  if (!val) return '—';
  const d = new Date(val);
  if (isNaN(d)) return val;
  const pad = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}, ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const fmtNum = (val) => {
  if (val === null || val === undefined || val === '') return '—';
  const n = Number(val);
  if (isNaN(n)) return String(val);
  return n.toLocaleString('en', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
};

// ── Outside click ─────────────────────────────────────────────────────
const handleOutsideClick = (e) => {
  if (merchantDropdownOpen.value  && merchantDropdownRef.value  && !merchantDropdownRef.value.contains(e.target))  merchantDropdownOpen.value  = false;
  if (currencyDropdownOpen.value  && currencyDropdownRef.value  && !currencyDropdownRef.value.contains(e.target))  currencyDropdownOpen.value  = false;
  if (refIdDropdownOpen.value     && refIdDropdownRef.value     && !refIdDropdownRef.value.contains(e.target))     refIdDropdownOpen.value     = false;
  if (dateDropdownOpen.value      && dateDropdownRef.value      && !dateDropdownRef.value.contains(e.target))      dateDropdownOpen.value      = false;
};

onMounted(async () => {
  document.addEventListener('click', handleOutsideClick);
  await fetchMerchants();
  await fetchFundFlow();
});

onUnmounted(() => {
  document.removeEventListener('click', handleOutsideClick);
});
</script>

<template>
  <div class="funds-view">
    <section class="panel channel-stats-panel">

      <!-- ── Filter Bar ──────────────────────────────────────────── -->
      <div class="filter-bar">

        <!-- Date -->
        <div class="filter-group" ref="dateDropdownRef">
          <span class="filter-label">Date <span class="required">*</span></span>
          <div class="filter-date-row">
            <button class="date-chip" @click.stop="dateDropdownOpen = !dateDropdownOpen">
              {{ currentDateLabel }}
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="refresh-icon"><path d="M23 4v6h-6M1 20v-6h6"/><path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/></svg>
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
        </div>

        <!-- Merchant -->
        <div class="filter-group" ref="merchantDropdownRef">
          <button class="filter-chip" :class="{ 'has-value': appliedPlatformIds.length }" @click.stop="openMerchantDropdown">
            <span class="chip-icon">T</span>
            {{ merchantBtnLabel }}
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M6 9l6 6 6-6"/></svg>
          </button>
          <div v-if="merchantDropdownOpen" class="multi-dropdown" @click.stop>
            <input v-model="merchantSearch" class="dropdown-search" placeholder="Search the list" autofocus />
            <div class="dropdown-list">
              <label class="dropdown-item select-all">
                <input type="checkbox" :checked="allMerchantsSelected" @change="toggleSelectAllMerchants" />
                <span>Select all</span>
              </label>
              <label v-for="m in filteredMerchants" :key="m.platformId" class="dropdown-item">
                <input type="checkbox" :value="m.platformId" v-model="pendingPlatformIds" />
                <span>{{ m.account }}</span>
              </label>
              <p v-if="!filteredMerchants.length" class="empty-hint">无匹配项</p>
            </div>
            <div class="dropdown-footer">
              <button class="add-filter-btn" @click="applyMerchantFilter">Add filter</button>
            </div>
          </div>
        </div>

        <!-- Currency -->
        <div class="filter-group" ref="currencyDropdownRef">
          <button class="filter-chip" :class="{ 'has-value': appliedAreaTypes.length }" @click.stop="openCurrencyDropdown">
            <span class="chip-icon">T</span>
            {{ currencyBtnLabel }}
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M6 9l6 6 6-6"/></svg>
          </button>
          <div v-if="currencyDropdownOpen" class="multi-dropdown" @click.stop>
            <div class="dropdown-list">
              <label class="dropdown-item select-all">
                <input type="checkbox" :checked="allCurrenciesSelected" @change="toggleSelectAllCurrencies" />
                <span>Select all</span>
              </label>
              <label v-for="c in CURRENCY_OPTIONS" :key="c.id" class="dropdown-item">
                <input type="checkbox" :value="c.id" v-model="pendingAreaTypes" />
                <span>{{ c.currencyCode }} · {{ c.label }}</span>
              </label>
            </div>
            <div class="dropdown-footer">
              <button class="add-filter-btn" @click="applyCurrencyFilter">Add filter</button>
            </div>
          </div>
        </div>

        <!-- Ref ID -->
        <div class="filter-group" ref="refIdDropdownRef">
          <button class="filter-chip" :class="{ 'has-value': appliedRefId }" @click.stop="openRefIdDropdown">
            <span class="chip-icon">T</span>
            {{ refIdBtnLabel }}
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M6 9l6 6 6-6"/></svg>
          </button>
          <div v-if="refIdDropdownOpen" class="refid-dropdown" @click.stop>
            <input
              v-model="pendingRefId"
              class="dropdown-search"
              placeholder="Enter Ref ID..."
              @keydown.enter="applyRefIdFilter"
              autofocus
            />
            <div class="dropdown-footer">
              <button class="add-filter-btn" @click="applyRefIdFilter">Add filter</button>
            </div>
          </div>
        </div>

      </div>

      <!-- ── Table header ────────────────────────────────────────── -->
      <div class="table-header-row">
        <span class="table-title">Merchant Account Log</span>
        <span v-if="tableLoading" class="loading-hint">加载中...</span>
        <span v-if="tableError" class="error-hint">{{ tableError }}</span>
      </div>

      <!-- ── Table ───────────────────────────────────────────────── -->
      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>ID <span class="sort-arrow">↑</span></th>
              <th>Merchant <span class="sort-arrow">↑</span></th>
              <th class="num-col"><span class="sort-arrow">↑</span> Amount</th>
              <th class="num-col"><span class="sort-arrow">↑</span> Remain</th>
              <th>Currency <span class="sort-arrow">↑</span></th>
              <th>Reason <span class="sort-arrow">↑</span></th>
              <th>Ref ID <span class="sort-arrow">↑</span></th>
              <th>Local Time <span class="sort-arrow">↑</span></th>
              <th class="num-col"><span class="sort-arrow">↑</span> Order Amount</th>
              <th class="num-col"><span class="sort-arrow">↑</span> Order Fee</th>
              <th>Type <span class="sort-arrow">↑</span></th>
              <th>Out Trade No <span class="sort-arrow">↑</span></th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!tableRows.length && !tableLoading">
              <td colspan="12" class="empty-cell">暂无数据</td>
            </tr>
            <tr v-for="row in tableRows" :key="row.id">
              <td class="id-cell">{{ row.id }}</td>
              <td>{{ row.merchant ?? '—' }}</td>
              <td class="num-col">{{ fmtNum(row.amount) }}</td>
              <td class="num-col">{{ fmtNum(row.remain) }}</td>
              <td>{{ row.currency ?? '—' }}</td>
              <td>{{ row.reason ?? '—' }}</td>
              <td class="refid-cell">{{ row.refId ?? '—' }}</td>
              <td class="time-cell">{{ fmtDate(row.localTime) }}</td>
              <td class="num-col">{{ row.orderAmount != null ? fmtNum(row.orderAmount) : '—' }}</td>
              <td class="num-col">{{ row.orderFee != null ? fmtNum(row.orderFee) : '—' }}</td>
              <td>
                <span class="type-badge" :class="row.type === 'INCREASE' ? 'increase' : 'decrease'">
                  {{ row.type === 'INCREASE' ? 'Increase' : 'Decrease' }}
                </span>
              </td>
              <td class="trade-no-cell">{{ row.outTradeNo ?? '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- ── Pagination ──────────────────────────────────────────── -->
      <div class="pagination-bar">
        <span class="page-count">
          Rows {{ tableTotal ? pageStart : 0 }}-{{ pageEnd }} of first {{ tableTotal }}
        </span>
        <div class="page-nav">
          <button class="nav-btn" :disabled="tablePage <= 1" @click="changePage(tablePage - 1)">&#8249;</button>
          <button class="nav-btn" :disabled="tablePage >= totalPages" @click="changePage(tablePage + 1)">&#8250;</button>
        </div>
      </div>

    </section>
  </div>
</template>

<style scoped>
.funds-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.panel {
  border-radius: 24px;
  padding: 24px;
  background: rgba(18, 19, 37, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.channel-stats-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ── Filter Bar ──────────────────────────────────────────────────────── */
.filter-bar {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-group { position: relative; }

.filter-label {
  display: block;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.required { color: #60a5fa; }

.filter-date-row { position: relative; }

.date-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(96, 165, 250, 0.2);
  border: 1px solid rgba(96, 165, 250, 0.5);
  color: #93c5fd;
  padding: 7px 14px;
  border-radius: 999px;
  font-size: 13px;
  cursor: pointer;
  font-weight: 500;
  transition: background 0.15s;
}
.date-chip:hover { background: rgba(96, 165, 250, 0.3); }
.refresh-icon { opacity: 0.8; }

.date-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  background: #1a1d2e;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  overflow: hidden;
  z-index: 200;
  min-width: 140px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.5);
}

.date-opt {
  display: block;
  width: 100%;
  text-align: left;
  padding: 10px 16px;
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.12s;
}
.date-opt:hover, .date-opt.active {
  background: rgba(96, 165, 250, 0.15);
  color: #93c5fd;
}

.filter-chip {
  display: flex;
  align-items: center;
  gap: 7px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.14);
  color: rgba(255, 255, 255, 0.75);
  padding: 7px 14px;
  border-radius: 999px;
  font-size: 13px;
  cursor: pointer;
  margin-top: 20px;
  transition: background 0.15s, border-color 0.15s;
  white-space: nowrap;
}
.filter-chip:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.25);
}
.filter-chip.has-value {
  border-color: rgba(96, 165, 250, 0.5);
  color: #93c5fd;
  background: rgba(96, 165, 250, 0.1);
}
.chip-icon { font-size: 11px; font-weight: 700; opacity: 0.7; }

.multi-dropdown,
.refid-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  background: #1a1d2e;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  z-index: 200;
  min-width: 220px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.6);
  overflow: hidden;
}

.dropdown-search {
  width: 100%;
  background: rgba(255, 255, 255, 0.04);
  border: none;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  color: #fff;
  padding: 10px 14px;
  font-size: 13px;
  outline: none;
  box-sizing: border-box;
}
.dropdown-search::placeholder { color: rgba(255, 255, 255, 0.35); }

.dropdown-list {
  max-height: 240px;
  overflow-y: auto;
  padding: 4px 0;
}
.dropdown-list::-webkit-scrollbar { width: 4px; }
.dropdown-list::-webkit-scrollbar-track { background: transparent; }
.dropdown-list::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.15); border-radius: 4px; }

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  cursor: pointer;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  transition: background 0.1s;
}
.dropdown-item:hover { background: rgba(255, 255, 255, 0.06); }
.dropdown-item.select-all {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.95);
  font-weight: 500;
}
.dropdown-item input[type="checkbox"] { accent-color: #60a5fa; width: 15px; height: 15px; flex-shrink: 0; }

.empty-hint { padding: 12px 14px; font-size: 12px; color: rgba(255,255,255,0.4); margin: 0; }

.dropdown-footer {
  padding: 8px 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  justify-content: flex-end;
}

.add-filter-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.85);
  padding: 6px 16px;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s;
}
.add-filter-btn:hover { background: rgba(255, 255, 255, 0.14); }

/* ── Table ───────────────────────────────────────────────────────────── */
.table-header-row { display: flex; align-items: center; gap: 12px; }
.table-title { font-size: 14px; font-weight: 600; color: rgba(255, 255, 255, 0.9); }
.loading-hint { font-size: 12px; color: rgba(255, 255, 255, 0.45); }
.error-hint { font-size: 12px; color: #f87171; }

.table-wrapper { overflow-x: auto; }
.table-wrapper::-webkit-scrollbar { height: 4px; }
.table-wrapper::-webkit-scrollbar-track { background: transparent; }
.table-wrapper::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.12); border-radius: 4px; }

table { width: 100%; border-collapse: collapse; font-size: 13px; }

thead th {
  padding: 10px 14px;
  text-align: left;
  color: rgba(255, 255, 255, 0.55);
  font-weight: 500;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  white-space: nowrap;
}
thead th.num-col { text-align: right; }
.sort-arrow { font-size: 10px; opacity: 0.5; }

tbody td {
  padding: 10px 14px;
  color: rgba(255, 255, 255, 0.85);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  white-space: nowrap;
}
tbody tr:hover td { background: rgba(255, 255, 255, 0.025); }
td.num-col { text-align: right; }
td.id-cell { color: #60a5fa; cursor: pointer; }
td.refid-cell, td.trade-no-cell {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
}
td.time-cell { font-size: 12px; color: rgba(255, 255, 255, 0.7); }
.empty-cell { text-align: center; padding: 40px 0; color: rgba(255, 255, 255, 0.35); }

.type-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}
.type-badge.increase { background: rgba(74, 222, 128, 0.12); color: #4ade80; }
.type-badge.decrease { background: rgba(248, 113, 113, 0.12); color: #f87171; }

/* ── Pagination ──────────────────────────────────────────────────────── */
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}
.page-count { font-size: 13px; color: rgba(255, 255, 255, 0.5); }
.page-nav { display: flex; gap: 4px; }
.nav-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.12s;
}
.nav-btn:hover:not(:disabled) { background: rgba(255, 255, 255, 0.08); }
.nav-btn:disabled { opacity: 0.3; cursor: not-allowed; }
</style>
