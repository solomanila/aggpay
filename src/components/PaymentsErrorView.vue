<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import http from '../services/http';

const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});

// ── 通道错误记录列表 ───────────────────────────────────────────────
const searchMdcId = ref('');
const searchErrorText = ref('');
const searchPayConfigId = ref('');
const searchAppId = ref('');

const tableRows = ref([]);
const tableTotal = ref(0);
const tablePage = ref(1);
const tablePageSize = ref(20);
const tableLoading = ref(false);
const tableError = ref('');

const merchantOptions = ref([]);
const channelOptions = ref([]);

const paginationSizes = [10, 20, 50];
const totalPages = computed(() =>
  tableTotal.value ? Math.max(1, Math.ceil(tableTotal.value / tablePageSize.value)) : 1
);

const formatDateTime = (value) => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', { hour12: false });
};

const fetchList = async () => {
  tableLoading.value = true;
  tableError.value = '';
  try {
    const params = { pageNum: tablePage.value, pageSize: tablePageSize.value };
    if (searchMdcId.value.trim()) params.mdcId = searchMdcId.value.trim();
    if (searchErrorText.value.trim()) params.errorText = searchErrorText.value.trim();
    if (searchPayConfigId.value !== '') params.payConfigId = Number(searchPayConfigId.value);
    if (searchAppId.value !== '') params.appId = searchAppId.value;
    const { data: response } = await http.get('/admin/pay/dashboard/orderBuildErrorList', { params });
    const payload = response?.data ?? response;
    tableRows.value = payload?.records ?? [];
    tableTotal.value = payload?.total ?? 0;
    tablePage.value = payload?.current ?? tablePage.value;
    tablePageSize.value = payload?.size ?? tablePageSize.value;
  } catch (error) {
    console.error('Failed to load order build error list', error);
    tableError.value = '无法加载错误记录';
    tableRows.value = [];
    tableTotal.value = 0;
  } finally {
    tableLoading.value = false;
  }
};

const loadFilterOptions = async () => {
  try {
    const [merchantRes, channelRes] = await Promise.all([
      http.get('/admin/pay/dashboard/merchants'),
      http.get('/admin/pay/dashboard/channelConfigList', { params: { pageSize: 200 } })
    ]);
    merchantOptions.value = merchantRes.data?.data ?? merchantRes.data ?? [];
    const channelPayload = channelRes.data?.data ?? channelRes.data;
    channelOptions.value = channelPayload?.records ?? [];
  } catch {
    // options stay empty
  }
};

const handleSearch = () => { tablePage.value = 1; fetchList(); };
const handleReset = () => {
  searchMdcId.value = '';
  searchErrorText.value = '';
  searchPayConfigId.value = '';
  searchAppId.value = '';
  tablePage.value = 1;
  fetchList();
};
const changePage = (next) => {
  if (next < 1 || next > totalPages.value || next === tablePage.value) return;
  tablePage.value = next;
  fetchList();
};
const changePageSize = (size) => { tablePageSize.value = size; tablePage.value = 1; fetchList(); };

onMounted(() => {
  fetchList();
  loadFilterOptions();
});
</script>

<template>
  <div class="error-view">
    <!-- ── 错误记录列表 ────────────────────────────────────────── -->
    <section class="panel error-list-panel">
      <div class="search-bar">
        <input v-model="searchMdcId" class="search-input" placeholder="订单号 / 商户订单号" @keyup.enter="handleSearch" />
        <input v-model="searchErrorText" class="search-input" placeholder="原因" @keyup.enter="handleSearch" />
        <select v-model="searchPayConfigId" class="search-select">
          <option value="">支付通道</option>
          <option v-for="ch in channelOptions" :key="ch.id" :value="ch.id">{{ ch.title ?? ch.id }}</option>
        </select>
        <select v-model="searchAppId" class="search-select">
          <option value="">商户</option>
          <option v-for="m in merchantOptions" :key="m" :value="m">{{ m }}</option>
        </select>
        <button type="button" class="btn btn-primary" @click="handleSearch">搜索</button>
        <button type="button" class="btn btn-ghost" @click="handleReset">重置</button>
      </div>

      <div class="table-wrapper">
        <p v-if="tableError" class="error-text">{{ tableError }}</p>
        <p v-else-if="tableLoading" class="muted">正在加载...</p>
        <table v-if="!tableLoading">
          <thead>
            <tr>
              <th>商户</th>
              <th>订单号</th>
              <th>商户订单号</th>
              <th>通道</th>
              <th>原因</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!tableRows.length">
              <td colspan="6" class="empty-cell">暂无数据</td>
            </tr>
            <tr v-for="(row, index) in tableRows" :key="row.id ?? index">
              <td>{{ row.appId || '—' }}</td>
              <td class="id-cell">{{ row.mdcId || '—' }}</td>
              <td class="id-cell">{{ row.mdcId || '—' }}</td>
              <td>{{ row.title || '—' }}</td>
              <td class="error-cell" :title="row.errorText">{{ row.errorText || '—' }}</td>
              <td>{{ formatDateTime(row.createTime) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-bar">
        <div class="page-info">共 {{ tableTotal }} 条</div>
        <div class="pagination-controls">
          <button type="button" class="ghost-btn" :disabled="tablePage <= 1" @click="changePage(tablePage - 1)">&lt;</button>
          <span class="page-num">{{ tablePage }}</span>
          <button type="button" class="ghost-btn" :disabled="tablePage >= totalPages" @click="changePage(tablePage + 1)">&gt;</button>
          <select :value="tablePageSize" class="size-select" @change="changePageSize(Number($event.target.value))">
            <option v-for="size in paginationSizes" :key="size" :value="size">{{ size }} 条/页</option>
          </select>
        </div>
      </div>
    </section>

    <!-- ── 其余原有内容 ────────────────────────────────────────── -->
    <section class="panel hero gradient">
      <div>
        <p class="eyebrow">{{ data.hero.eyebrow }}</p>
        <h2>{{ data.hero.title }}</h2>
        <p class="muted">{{ data.hero.description }}</p>
      </div>
      <p class="muted">{{ data.hero.sync }}</p>
    </section>

    <section class="stat-grid">
      <article v-for="stat in data.stats" :key="stat.id" class="panel stat-card">
        <p class="label">{{ stat.label }}</p>
        <p class="value">{{ stat.value }}</p>
        <p class="meta">{{ stat.meta }}</p>
      </article>
    </section>

    <section class="panel filters">
      <header>
        <p class="eyebrow">分类筛选</p>
        <h3>通道类型</h3>
      </header>
      <div class="chips">
        <button v-for="filter in data.filters" :key="filter" type="button">{{ filter }}</button>
      </div>
    </section>

    <section class="panel error-table">
      <header>
        <p class="eyebrow">错误列表</p>
        <h3>实时状态</h3>
      </header>
      <table>
        <thead>
          <tr>
            <th>通道</th>
            <th>类型</th>
            <th>影响</th>
            <th>状态</th>
            <th>Owner</th>
            <th>更新时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="error in data.errors" :key="error.id">
            <td>{{ error.channel }}</td>
            <td>{{ error.type }}</td>
            <td>{{ error.impact }}</td>
            <td>{{ error.status }}</td>
            <td>{{ error.owner }}</td>
            <td>{{ error.updated }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="actions-root">
      <article class="panel actions">
        <header>
          <p class="eyebrow">应对动作</p>
          <h3>执行进度</h3>
        </header>
        <div class="action-list">
          <div v-for="action in data.actions" :key="action.id" class="action-row">
            <div>
              <p class="title">{{ action.title }}</p>
              <p class="meta">{{ action.owner }}</p>
            </div>
            <div class="progress">
              <span :style="{ width: `${action.progress}%` }" />
            </div>
            <span class="percent">{{ action.progress }}%</span>
          </div>
        </div>
      </article>
      <article class="panel root-causes">
        <header>
          <p class="eyebrow">根因记录</p>
          <h3>复盘要点</h3>
        </header>
        <ul>
          <li v-for="cause in data.rootCauses" :key="cause.id">
            <div>
              <p class="label">{{ cause.channel }}</p>
              <p class="meta">{{ cause.cause }}</p>
            </div>
            <span class="resolution">{{ cause.resolution }}</span>
          </li>
        </ul>
      </article>
    </section>

    <section class="panel notices">
      <header>
        <p class="eyebrow">提醒</p>
        <h3>最新动态</h3>
      </header>
      <ul>
        <li v-for="notice in data.notices" :key="notice.id">{{ notice.text }}</li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.error-view {
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

.gradient {
  background: linear-gradient(135deg, rgba(79, 95, 255, 0.65), rgba(11, 16, 32, 0.95));
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.2em;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
}

.muted { color: rgba(255, 255, 255, 0.7); }
.error-text { color: #ff8e8e; }

/* ── 错误记录列表 ─────────────────────────────────────────────── */
.error-list-panel { display: flex; flex-direction: column; gap: 14px; }

.search-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }

.search-input {
  flex: 1; min-width: 160px; max-width: 260px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px; color: #fff; padding: 8px 14px; font-size: 13px; outline: none;
}
.search-input::placeholder { color: rgba(255, 255, 255, 0.35); }
.search-input:focus { border-color: rgba(127, 133, 249, 0.6); }

.search-select {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px; color: rgba(255, 255, 255, 0.8);
  padding: 8px 14px; font-size: 13px; cursor: pointer; min-width: 140px;
}

.btn { padding: 8px 18px; border-radius: 10px; font-size: 13px; cursor: pointer; border: none; }
.btn-primary { background: rgba(100, 116, 255, 0.85); color: #fff; }
.btn-primary:hover { background: rgba(100, 116, 255, 1); }
.btn-ghost { background: rgba(255, 255, 255, 0.07); color: rgba(255, 255, 255, 0.8); border: 1px solid rgba(255, 255, 255, 0.15); }
.btn-ghost:hover { background: rgba(255, 255, 255, 0.12); }

.table-wrapper { overflow-x: auto; }
.table-wrapper table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table-wrapper th, .table-wrapper td { padding: 11px 14px; border-bottom: 1px solid rgba(255, 255, 255, 0.07); text-align: left; white-space: nowrap; }
.table-wrapper th { color: rgba(255, 255, 255, 0.6); font-weight: 500; background: rgba(255, 255, 255, 0.02); }
.table-wrapper td { color: rgba(255, 255, 255, 0.88); }
.id-cell { max-width: 160px; overflow: hidden; text-overflow: ellipsis; }
.error-cell { max-width: 320px; overflow: hidden; text-overflow: ellipsis; cursor: help; }
.empty-cell { text-align: center; padding: 28px; color: rgba(255, 255, 255, 0.45); }

.pagination-bar { display: flex; align-items: center; justify-content: flex-end; gap: 12px; flex-wrap: wrap; }
.page-info { font-size: 13px; color: rgba(255, 255, 255, 0.55); margin-right: auto; }
.pagination-controls { display: flex; align-items: center; gap: 8px; }
.ghost-btn { border: 1px solid rgba(255, 255, 255, 0.2); background: transparent; color: rgba(255, 255, 255, 0.8); width: 30px; height: 30px; border-radius: 8px; cursor: pointer; font-size: 13px; }
.ghost-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.page-num { font-size: 13px; color: #fff; min-width: 20px; text-align: center; }
.size-select { background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.15); color: rgba(255, 255, 255, 0.8); padding: 4px 10px; border-radius: 8px; font-size: 13px; cursor: pointer; }

/* ── rest of page ────────────────────────────────────────────── */
.stat-grid { display: grid; gap: 16px; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); }
.stat-card { background: rgba(255, 255, 255, 0.02); }
.stat-card .label { font-size: 12px; color: rgba(255, 255, 255, 0.7); }
.stat-card .value { font-size: 24px; font-weight: 600; margin: 8px 0 4px; }

.filters .chips { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 12px; }
.filters button { border: 1px solid rgba(255, 255, 255, 0.15); border-radius: 999px; background: transparent; color: rgba(255, 255, 255, 0.8); padding: 6px 16px; }

.error-table table { width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 12px; }
.error-table th { text-align: left; padding: 8px 0; color: rgba(255, 255, 255, 0.6); border-bottom: 1px solid rgba(255, 255, 255, 0.05); }
.error-table td { padding: 12px 0; border-bottom: 1px solid rgba(255, 255, 255, 0.05); }

.actions-root { display: grid; gap: 24px; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); }
.action-list { display: flex; flex-direction: column; gap: 12px; margin-top: 12px; }
.action-row { display: grid; grid-template-columns: 1fr 1fr 60px; gap: 12px; align-items: center; padding: 12px 0; border-bottom: 1px solid rgba(255, 255, 255, 0.05); }
.action-row .progress { height: 6px; border-radius: 999px; background: rgba(255, 255, 255, 0.08); overflow: hidden; }
.action-row .progress span { display: block; height: 100%; border-radius: inherit; background: linear-gradient(120deg, rgba(126, 143, 255, 0.8), rgba(99, 228, 255, 0.8)); }

.root-causes ul, .notices ul { list-style: none; padding: 0; margin: 12px 0 0; display: flex; flex-direction: column; gap: 12px; }
.root-causes li, .notices li { padding: 12px; border-radius: 16px; background: rgba(255, 255, 255, 0.02); display: flex; justify-content: space-between; align-items: center; }
</style>
