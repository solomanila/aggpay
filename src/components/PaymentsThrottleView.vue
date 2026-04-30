<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import http from '../services/http';

const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});

// ── 限流计划列表 ───────────────────────────────────────────────────
const searchChannelId = ref('');

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

const planTypeOptions = [
  '按照设置固定速度限流',
  '按照动态速率限流'
];

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
    if (searchChannelId.value.trim()) params.channelId = searchChannelId.value.trim();
    const { data: response } = await http.get('/admin/channel-limit/list', { params });
    const payload = response?.data ?? response;
    tableRows.value = payload?.records ?? [];
    tableTotal.value = payload?.total ?? 0;
    tablePage.value = payload?.current ?? tablePage.value;
    tablePageSize.value = payload?.size ?? tablePageSize.value;
  } catch (error) {
    console.error('Failed to load channel limit plans', error);
    tableError.value = '无法加载限流配置';
    tableRows.value = [];
    tableTotal.value = 0;
  } finally {
    tableLoading.value = false;
  }
};

const handleSearch = () => { tablePage.value = 1; fetchList(); };
const handleReset = () => { searchChannelId.value = ''; tablePage.value = 1; fetchList(); };
const changePage = (next) => {
  if (next < 1 || next > totalPages.value || next === tablePage.value) return;
  tablePage.value = next;
  fetchList();
};
const changePageSize = (size) => { tablePageSize.value = size; tablePage.value = 1; fetchList(); };

const toggleStatus = async (row) => {
  const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  try {
    await http.patch('/admin/channel-limit/status', null, {
      params: { id: row.id, status: newStatus }
    });
    row.status = newStatus;
  } catch (error) {
    console.error('Failed to update status', error);
  }
};

const deleteRow = async (row) => {
  try {
    await http.delete('/admin/channel-limit/delete', { params: { id: row.id } });
    tableRows.value = tableRows.value.filter(r => r.id !== row.id);
    tableTotal.value = Math.max(0, tableTotal.value - 1);
  } catch (error) {
    console.error('Failed to delete plan', error);
  }
};

// ── Drawer ────────────────────────────────────────────────────────
const drawerVisible = ref(false);
const drawerMode = ref('edit'); // 'edit' | 'create'
const drawerSaving = ref(false);
const drawerError = ref('');

const form = reactive({
  id: null,
  channelId: '',
  planType: '按照设置固定速度限流',
  windowMinutes: 1,
  tps: 0,
  status: 'INACTIVE'
});

const openCreate = () => {
  drawerMode.value = 'create';
  form.id = null;
  form.channelId = '';
  form.planType = '按照设置固定速度限流';
  form.windowMinutes = 1;
  form.tps = 0;
  form.status = 'INACTIVE';
  drawerError.value = '';
  drawerVisible.value = true;
};

const openEdit = async (row) => {
  drawerMode.value = 'edit';
  form.id = row.id;
  form.channelId = row.channelId ?? '';
  form.planType = row.planType ?? '按照设置固定速度限流';
  form.windowMinutes = row.windowMinutes ?? 1;
  form.tps = 0;
  form.status = row.status ?? 'INACTIVE';
  drawerError.value = '';
  drawerVisible.value = true;

  if (row.channelId) {
    try {
      const { data: res } = await http.get(`/admin/channel-profile/${row.channelId}`);
      const profile = res?.data ?? res;
      if (profile?.limitConfig) {
        const cfg = typeof profile.limitConfig === 'string'
          ? JSON.parse(profile.limitConfig)
          : profile.limitConfig;
        form.tps = cfg?.tps ?? 0;
      }
    } catch {
      // 无档案记录时忽略，tps 保持 0
    }
  }
};

const closeDrawer = () => { drawerVisible.value = false; };

const submitDrawer = async () => {
  if (!form.channelId.trim()) { drawerError.value = '支付通道不能为空'; return; }
  drawerSaving.value = true;
  drawerError.value = '';
  try {
    const payload = {
      id: form.id,
      channelId: form.channelId,
      planType: form.planType,
      windowMinutes: Number(form.windowMinutes),
      status: form.status
    };
    if (drawerMode.value === 'create') {
      await http.post('/admin/channel-limit/create', payload);
    } else {
      await http.put('/admin/channel-limit/update', payload);
      if (form.channelId) {
        await http.put(`/admin/channel-profile/${form.channelId}/limit-config`, { tps: Number(form.tps) });
      }
      const row = tableRows.value.find(r => r.id === form.id);
      if (row) {
        row.channelId = form.channelId;
        row.planType = form.planType;
        row.windowMinutes = Number(form.windowMinutes);
        row.status = form.status;
      }
    }
    drawerVisible.value = false;
    if (drawerMode.value === 'create') fetchList();
  } catch (error) {
    console.error('Failed to save plan', error);
    drawerError.value = '保存失败，请重试';
  } finally {
    drawerSaving.value = false;
  }
};

onMounted(fetchList);
</script>

<template>
  <div class="throttle-view">
    <!-- ── 限流配置列表 ────────────────────────────────────────── -->
    <section class="panel limit-list-panel">
      <div class="toolbar">
        <div class="search-bar">
          <select v-model="searchChannelId" class="search-select" @change="handleSearch">
            <option value="">支付通道</option>
            <option v-for="row in tableRows" :key="row.channelId" :value="row.channelId">{{ row.channelId }}</option>
          </select>
          <button type="button" class="btn btn-primary" @click="handleSearch">搜索</button>
          <button type="button" class="btn btn-ghost" @click="handleReset">重置</button>
        </div>
        <button type="button" class="btn btn-create" @click="openCreate">新建</button>
      </div>

      <div class="table-wrapper">
        <p v-if="tableError" class="error-text">{{ tableError }}</p>
        <p v-else-if="tableLoading" class="muted">正在加载...</p>
        <table v-if="!tableLoading">
          <thead>
            <tr>
              <th>通道</th>
              <th>限流类型</th>
              <th>限流周期 (秒)</th>
              <th>启用</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!tableRows.length">
              <td colspan="5" class="empty-cell">暂无数据</td>
            </tr>
            <tr v-for="(row, index) in tableRows" :key="row.id ?? index">
              <td>{{ row.channelId || '—' }}</td>
              <td>{{ row.planType || '—' }}</td>
              <td>{{ row.windowMinutes ?? '—' }}</td>
              <td>
                <button type="button" class="toggle-switch" :class="{ on: row.status === 'ACTIVE' }" @click="toggleStatus(row)">
                  <span class="toggle-thumb" />
                </button>
              </td>
              <td class="action-cell">
                <button type="button" class="icon-btn edit-btn" title="编辑" @click="openEdit(row)">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                </button>
                <button type="button" class="icon-btn delete-btn" title="删除" @click="deleteRow(row)">
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
        <p class="eyebrow">筛选</p>
        <h3>策略状态</h3>
      </header>
      <div class="chips">
        <button v-for="filter in data.filters" :key="filter" type="button">{{ filter }}</button>
      </div>
    </section>

    <section class="curves-plans">
      <article class="panel curves">
        <header>
          <p class="eyebrow">曲线</p>
          <h3>产能趋势</h3>
        </header>
        <ul>
          <li v-for="curve in data.curves" :key="curve.id">
            <p class="label">{{ curve.label }}</p>
            <p class="detail">{{ curve.detail }}</p>
            <div class="curve-line"><span /></div>
          </li>
        </ul>
      </article>
      <article class="panel plans">
        <header>
          <p class="eyebrow">限流计划</p>
          <h3>执行进度</h3>
        </header>
        <div class="plan-list">
          <div v-for="plan in data.plans" :key="plan.id" class="plan-row">
            <div>
              <p class="title">{{ plan.title }}</p>
              <p class="meta">{{ plan.owner }} · {{ plan.eta }}</p>
            </div>
            <div class="progress"><span :style="{ width: `${plan.progress}%` }" /></div>
            <span class="percent">{{ plan.progress }}%</span>
          </div>
        </div>
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

    <!-- ── Edit / Create Drawer ───────────────────────────────── -->
    <Teleport to="body">
      <div v-if="drawerVisible" class="drawer-overlay" @click.self="closeDrawer">
        <div class="drawer">
          <div class="drawer-header">
            <span class="drawer-title">{{ drawerMode === 'create' ? '新建' : '编辑' }}</span>
            <button type="button" class="drawer-close" @click="closeDrawer">✕</button>
          </div>

          <div class="drawer-body">
            <!-- 支付通道 -->
            <div class="form-row">
              <label class="form-label required">支付通道</label>
              <input v-model="form.channelId" class="form-input" placeholder="通道名称，如 air-in(M352900)-DP" />
            </div>

            <!-- 限流类型 -->
            <div class="form-row">
              <label class="form-label required">限流类型</label>
              <select v-model="form.planType" class="form-select">
                <option v-for="opt in planTypeOptions" :key="opt" :value="opt">{{ opt }}</option>
              </select>
            </div>

            <!-- 限流周期 (秒) -->
            <div class="form-row">
              <label class="form-label required">限流周期 (秒)</label>
              <div class="counter-row">
                <button type="button" class="counter-btn" @click="form.windowMinutes = Math.max(1, form.windowMinutes - 1)">−</button>
                <input v-model.number="form.windowMinutes" type="number" class="counter-input" min="1" />
                <button type="button" class="counter-btn" @click="form.windowMinutes++">+</button>
              </div>
            </div>

            <!-- TPS 限流 (仅编辑模式同步到通道档案) -->
            <div v-if="drawerMode === 'edit'" class="form-row">
              <label class="form-label">TPS 限制（每秒请求数，0 表示不限）</label>
              <div class="counter-row">
                <button type="button" class="counter-btn" @click="form.tps = Math.max(0, form.tps - 100)">−</button>
                <input v-model.number="form.tps" type="number" class="counter-input" min="0" step="100" />
                <button type="button" class="counter-btn" @click="form.tps += 100">+</button>
              </div>
              <p class="form-hint">保存后立即同步至 Redis，无需重启服务</p>
            </div>

            <!-- 启用 -->
            <div class="form-row">
              <label class="form-label">启用</label>
              <div class="toggle-wrap">
                <button
                  type="button"
                  class="toggle-switch"
                  :class="{ on: form.status === 'ACTIVE' }"
                  @click="form.status = form.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'"
                >
                  <span class="toggle-thumb" />
                </button>
                <span class="toggle-label">{{ form.status === 'ACTIVE' ? 'ON' : 'OFF' }}</span>
              </div>
            </div>

            <p v-if="drawerError" class="drawer-error">{{ drawerError }}</p>
          </div>

          <div class="drawer-footer">
            <button type="button" class="btn btn-ghost" @click="closeDrawer">取消</button>
            <button type="button" class="btn btn-primary" :disabled="drawerSaving" @click="submitDrawer">
              {{ drawerSaving ? '保存中...' : '提交' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.throttle-view {
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
  background: linear-gradient(135deg, rgba(78, 95, 255, 0.65), rgba(11, 16, 32, 0.95));
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.2em;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
}

.muted { color: rgba(255, 255, 255, 0.7); }
.error-text { color: #ff8e8e; }

/* ── 限流配置列表 ─────────────────────────────────────────────── */
.limit-list-panel { display: flex; flex-direction: column; gap: 14px; }

.toolbar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.search-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }

.search-select {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px; color: rgba(255, 255, 255, 0.8);
  padding: 8px 14px; font-size: 13px; cursor: pointer; min-width: 160px;
}

.btn { padding: 8px 18px; border-radius: 10px; font-size: 13px; cursor: pointer; border: none; }
.btn-primary { background: rgba(100, 116, 255, 0.85); color: #fff; }
.btn-primary:hover { background: rgba(100, 116, 255, 1); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-ghost { background: rgba(255, 255, 255, 0.07); color: rgba(255, 255, 255, 0.8); border: 1px solid rgba(255, 255, 255, 0.15); }
.btn-ghost:hover { background: rgba(255, 255, 255, 0.12); }
.btn-create { background: rgba(34, 197, 94, 0.2); color: #4ade80; border: 1px solid rgba(34, 197, 94, 0.3); }
.btn-create:hover { background: rgba(34, 197, 94, 0.3); }

.table-wrapper { overflow-x: auto; }
.table-wrapper table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table-wrapper th, .table-wrapper td { padding: 11px 14px; border-bottom: 1px solid rgba(255, 255, 255, 0.07); text-align: left; white-space: nowrap; }
.table-wrapper th { color: rgba(255, 255, 255, 0.6); font-weight: 500; background: rgba(255, 255, 255, 0.02); }
.table-wrapper td { color: rgba(255, 255, 255, 0.88); }
.empty-cell { text-align: center; padding: 28px; color: rgba(255, 255, 255, 0.45); }

.toggle-switch {
  position: relative; width: 40px; height: 22px; border-radius: 999px;
  border: none; background: rgba(255, 255, 255, 0.15); cursor: pointer; transition: background 0.2s; padding: 0;
}
.toggle-switch.on { background: #3b82f6; }
.toggle-thumb {
  position: absolute; top: 3px; left: 3px; width: 16px; height: 16px;
  border-radius: 50%; background: #fff; transition: transform 0.2s;
}
.toggle-switch.on .toggle-thumb { transform: translateX(18px); }

.action-cell { display: flex; gap: 8px; align-items: center; }
.icon-btn { display: flex; align-items: center; justify-content: center; width: 28px; height: 28px; border-radius: 8px; border: none; cursor: pointer; }
.edit-btn { background: rgba(96, 165, 250, 0.15); color: #60a5fa; }
.edit-btn:hover { background: rgba(96, 165, 250, 0.28); }
.delete-btn { background: rgba(248, 113, 113, 0.15); color: #f87171; }
.delete-btn:hover { background: rgba(248, 113, 113, 0.28); }

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
.curves-plans { display: grid; gap: 24px; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); }
.curves ul, .notices ul { list-style: none; padding: 0; margin: 12px 0 0; display: flex; flex-direction: column; gap: 12px; }
.curves li, .notices li { padding: 12px; border-radius: 16px; background: rgba(255, 255, 255, 0.02); }
.curve-line { width: 100%; height: 6px; border-radius: 999px; background: rgba(255, 255, 255, 0.08); overflow: hidden; margin-top: 8px; }
.curve-line span { display: block; width: 70%; height: 100%; border-radius: inherit; background: linear-gradient(120deg, rgba(126, 143, 255, 0.8), rgba(99, 228, 255, 0.8)); }
.plan-list { display: flex; flex-direction: column; gap: 12px; margin-top: 12px; }
.plan-row { display: grid; grid-template-columns: 1fr 1fr 60px; gap: 12px; align-items: center; padding: 12px 0; border-bottom: 1px solid rgba(255, 255, 255, 0.05); }
.plan-row .progress { height: 6px; border-radius: 999px; background: rgba(255, 255, 255, 0.08); overflow: hidden; }
.plan-row .progress span { display: block; height: 100%; border-radius: inherit; background: linear-gradient(120deg, rgba(126, 143, 255, 0.8), rgba(99, 228, 255, 0.8)); }

/* ── Drawer ──────────────────────────────────────────────────── */
.drawer-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0, 0, 0, 0.55);
  display: flex; justify-content: flex-end;
}

.drawer {
  width: 520px; max-width: 95vw;
  background: #13152a;
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  display: flex; flex-direction: column;
  height: 100vh;
}

.drawer-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.drawer-title { font-size: 16px; font-weight: 600; color: #fff; }

.drawer-close {
  background: transparent; border: none; color: rgba(255, 255, 255, 0.6);
  font-size: 18px; cursor: pointer; padding: 4px 8px; line-height: 1;
}
.drawer-close:hover { color: #fff; }

.drawer-body {
  flex: 1; overflow-y: auto; padding: 24px;
  display: flex; flex-direction: column; gap: 20px;
}

.drawer-footer {
  display: flex; justify-content: flex-end; gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.form-row { display: flex; flex-direction: column; gap: 8px; }

.form-label { font-size: 13px; color: rgba(255, 255, 255, 0.7); font-weight: 500; }
.form-label.required::before { content: '* '; color: #f87171; }

.form-input {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px; color: #fff;
  padding: 10px 14px; font-size: 13px; outline: none; width: 100%; box-sizing: border-box;
}
.form-input:focus { border-color: rgba(127, 133, 249, 0.6); }

.form-select {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px; color: rgba(255, 255, 255, 0.9);
  padding: 10px 14px; font-size: 13px; width: 100%; cursor: pointer;
}

.counter-row {
  display: flex; align-items: center;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px; overflow: hidden;
}

.counter-btn {
  background: rgba(255, 255, 255, 0.08); border: none;
  color: rgba(255, 255, 255, 0.8); width: 40px; height: 40px;
  font-size: 18px; cursor: pointer; flex-shrink: 0;
}
.counter-btn:hover { background: rgba(255, 255, 255, 0.15); }

.counter-input {
  flex: 1; background: transparent; border: none;
  color: #fff; text-align: center; font-size: 14px;
  padding: 10px 6px; outline: none;
  -moz-appearance: textfield;
}
.counter-input::-webkit-outer-spin-button,
.counter-input::-webkit-inner-spin-button { -webkit-appearance: none; }

.toggle-wrap { display: flex; align-items: center; gap: 10px; }
.toggle-label { font-size: 13px; color: rgba(255, 255, 255, 0.7); }

.drawer-error { color: #ff8e8e; font-size: 13px; }
.form-hint { font-size: 11px; color: rgba(255, 255, 255, 0.4); margin-top: 4px; }

@media (max-width: 960px) {
  .drawer { width: 100vw; }
}
</style>
