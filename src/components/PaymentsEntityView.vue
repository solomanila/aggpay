<script setup>
import { computed, onMounted, ref } from 'vue';
import http from '../services/http';

const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});

// ── 主体配置列表 ──────────────────────────────────────────────────
const searchShortCode = ref('');
const tableRows = ref([]);
const tableTotal = ref(0);
const tablePage = ref(1);
const tablePageSize = ref(20);
const tableLoading = ref(false);
const tableError = ref('');

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
    if (searchShortCode.value.trim()) {
      params.shortCode = searchShortCode.value.trim();
    }
    const { data: response } = await http.get('/admin/pay/dashboard/payConfigInfoList', { params });
    const payload = response?.data ?? response;
    tableRows.value = payload?.records ?? [];
    tableTotal.value = payload?.total ?? 0;
    tablePage.value = payload?.current ?? payload?.pageNum ?? tablePage.value;
    tablePageSize.value = payload?.size ?? payload?.pageSize ?? tablePageSize.value;
  } catch (error) {
    console.error('Failed to load pay config info list', error);
    tableError.value = '无法加载主体配置列表';
    tableRows.value = [];
    tableTotal.value = 0;
  } finally {
    tableLoading.value = false;
  }
};

const handleSearch = () => {
  tablePage.value = 1;
  fetchList();
};

const handleReset = () => {
  searchShortCode.value = '';
  tablePage.value = 1;
  fetchList();
};

const changePage = (next) => {
  if (next < 1 || next > totalPages.value || next === tablePage.value) return;
  tablePage.value = next;
  fetchList();
};

const changePageSize = (size) => {
  tablePageSize.value = size;
  tablePage.value = 1;
  fetchList();
};

onMounted(fetchList);
</script>

<template>
  <div class="entity-view">
    <!-- ── 主体配置列表 ─────────────────────────────────────────── -->
    <section class="panel config-list-panel">
      <div class="search-bar">
        <input
          v-model="searchShortCode"
          class="search-input"
          placeholder="输入名称"
          @keyup.enter="handleSearch"
        />
        <button type="button" class="btn btn-primary" @click="handleSearch">搜索</button>
        <button type="button" class="btn btn-ghost" @click="handleReset">重置</button>
      </div>

      <div class="toolbar">
        <button type="button" class="btn btn-create">新建</button>
      </div>

      <div class="table-wrapper">
        <p v-if="tableError" class="error-text">{{ tableError }}</p>
        <p v-else-if="tableLoading" class="muted">正在加载...</p>
        <table v-if="!tableLoading">
          <thead>
            <tr>
              <th>名称</th>
              <th>域名</th>
              <th>代理</th>
              <th>备注</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!tableRows.length">
              <td colspan="6" class="empty-cell">暂无数据</td>
            </tr>
            <tr v-for="(row, index) in tableRows" :key="row.id ?? index">
              <td>{{ row.shortCode ?? '—' }}</td>
              <td>{{ row.url ?? '—' }}</td>
              <td>{{ row.reqDomain ?? '—' }}</td>
              <td>{{ row.remark ?? '—' }}</td>
              <td>{{ formatDateTime(row.createTime) }}</td>
              <td class="action-cell">
                <button type="button" class="icon-btn edit-btn" title="编辑">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                </button>
                <button type="button" class="icon-btn delete-btn" title="删除">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                    <path d="M10 11v6M14 11v6"/>
                    <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
                  </svg>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-bar">
        <div class="page-info">共 {{ tableTotal }} 条</div>
        <div class="pagination-controls">
          <button type="button" class="ghost-btn" :disabled="tablePage <= 1" @click="changePage(tablePage - 1)">
            &lt;
          </button>
          <span class="page-num">{{ tablePage }}</span>
          <button type="button" class="ghost-btn" :disabled="tablePage >= totalPages" @click="changePage(tablePage + 1)">
            &gt;
          </button>
          <select :value="tablePageSize" class="size-select" @change="changePageSize(Number($event.target.value))">
            <option v-for="size in paginationSizes" :key="size" :value="size">{{ size }} 条/页</option>
          </select>
        </div>
      </div>
    </section>

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
        <p class="eyebrow">状态筛选</p>
        <h3>主体视图</h3>
      </header>
      <div class="chips">
        <button v-for="filter in data.filters" :key="filter" type="button">{{ filter }}</button>
      </div>
    </section>

    <section class="entities">
      <article v-for="entity in data.entities" :key="entity.id" class="panel entity-card">
        <header>
          <div>
            <p class="eyebrow">{{ entity.region }}</p>
            <h3>{{ entity.name }}</h3>
          </div>
          <span class="status">{{ entity.status }}</span>
        </header>
        <p class="industry">行业：{{ entity.industry }}</p>
        <p class="limit">额度：{{ entity.limit }}</p>
        <div class="meta">
          <span>Owner · {{ entity.owner }}</span>
        </div>
      </article>
    </section>

    <section class="compliance-docs">
      <article class="panel compliance">
        <header>
          <p class="eyebrow">合规进度</p>
          <h3>配置完成度</h3>
        </header>
        <ul>
          <li v-for="item in data.compliance" :key="item.id">
            <div>
              <p class="label">{{ item.label }}</p>
              <p class="percent">{{ item.percent }}%</p>
            </div>
            <div class="progress">
              <span :style="{ width: `${item.percent}%` }" />
            </div>
          </li>
        </ul>
      </article>

      <article class="panel documents">
        <header>
          <p class="eyebrow">资料库</p>
          <h3>合同与报告</h3>
        </header>
        <ul>
          <li v-for="doc in data.documents" :key="doc.id">
            <div>
              <p class="title">{{ doc.title }}</p>
              <p class="meta">{{ doc.type }}</p>
            </div>
            <span class="status">{{ doc.status }}</span>
          </li>
        </ul>
      </article>
    </section>

    <section class="panel approvals">
      <header>
        <p class="eyebrow">审批中心</p>
        <h3>待跟进</h3>
      </header>
      <div class="approval-list">
        <div v-for="approval in data.approvals" :key="approval.id" class="approval-row">
          <div>
            <p class="title">{{ approval.title }}</p>
            <p class="meta">{{ approval.stage }} · {{ approval.owner }}</p>
          </div>
          <button type="button">查看</button>
        </div>
      </div>
    </section>

    <section class="panel notices">
      <header>
        <p class="eyebrow">提醒</p>
        <h3>最新更新</h3>
      </header>
      <ul>
        <li v-for="notice in data.notices" :key="notice.id">{{ notice.text }}</li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.entity-view {
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
  background: linear-gradient(135deg, rgba(77, 98, 255, 0.65), rgba(10, 16, 32, 0.95));
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.2em;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
}

.muted {
  color: rgba(255, 255, 255, 0.7);
}

/* ── 主体配置列表 ─────────────────────────────────────────────── */
.config-list-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 200px;
  max-width: 600px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  color: #fff;
  padding: 8px 14px;
  font-size: 13px;
  outline: none;
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.35);
}

.search-input:focus {
  border-color: rgba(127, 133, 249, 0.6);
}

.btn {
  padding: 8px 18px;
  border-radius: 10px;
  font-size: 13px;
  cursor: pointer;
  border: none;
}

.btn-primary {
  background: rgba(100, 116, 255, 0.85);
  color: #fff;
}

.btn-primary:hover {
  background: rgba(100, 116, 255, 1);
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.07);
  color: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.btn-ghost:hover {
  background: rgba(255, 255, 255, 0.12);
}

.btn-create {
  background: rgba(34, 197, 94, 0.2);
  color: #4ade80;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.btn-create:hover {
  background: rgba(34, 197, 94, 0.3);
}

.table-wrapper {
  overflow-x: auto;
}

.table-wrapper table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.table-wrapper th,
.table-wrapper td {
  padding: 11px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  text-align: left;
  white-space: nowrap;
}

.table-wrapper th {
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
  background: rgba(255, 255, 255, 0.02);
}

.table-wrapper td {
  color: rgba(255, 255, 255, 0.88);
}

.empty-cell {
  text-align: center;
  padding: 28px;
  color: rgba(255, 255, 255, 0.45);
}

.action-cell {
  display: flex;
  gap: 8px;
  align-items: center;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
}

.edit-btn {
  background: rgba(96, 165, 250, 0.15);
  color: #60a5fa;
}

.edit-btn:hover {
  background: rgba(96, 165, 250, 0.28);
}

.delete-btn {
  background: rgba(248, 113, 113, 0.15);
  color: #f87171;
}

.delete-btn:hover {
  background: rgba(248, 113, 113, 0.28);
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.page-info {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
  margin-right: auto;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ghost-btn {
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: transparent;
  color: rgba(255, 255, 255, 0.8);
  width: 30px;
  height: 30px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
}

.ghost-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.page-num {
  font-size: 13px;
  color: #fff;
  min-width: 20px;
  text-align: center;
}

.size-select {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.8);
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
}

.error-text {
  color: #ff8e8e;
}

/* ── rest of page ────────────────────────────────────────────── */
.stat-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.stat-card {
  background: rgba(255, 255, 255, 0.02);
}

.stat-card .label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.stat-card .value {
  font-size: 24px;
  font-weight: 600;
  margin: 8px 0 4px;
}

.filters .chips {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}

.filters button {
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 999px;
  background: transparent;
  color: rgba(255, 255, 255, 0.8);
  padding: 6px 16px;
}

.entities {
  display: grid;
  gap: 20px;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.entity-card header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.entity-card .status {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.1);
}

.industry,
.limit,
.meta {
  color: rgba(255, 255, 255, 0.75);
  margin: 6px 0;
}

.compliance-docs {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.compliance ul,
.documents ul,
.notices ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.compliance li {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
}

.compliance .progress {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.compliance .progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(120deg, rgba(126, 143, 255, 0.8), rgba(99, 228, 255, 0.8));
}

.documents li,
.notices li {
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.approvals .approval-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.approval-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
}

.approval-row button {
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  background: transparent;
  color: #fff;
  padding: 4px 12px;
}

@media (max-width: 960px) {
  .hero {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
