<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import http from '../services/http';

const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});

// ── 通道配置列表 ──────────────────────────────────────────────────
const activeTab = ref('payin');

const searchTitle = ref('');
const searchNullify = ref('');

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
    if (searchTitle.value.trim()) params.title = searchTitle.value.trim();
    if (searchNullify.value !== '') params.nullify = Number(searchNullify.value);
    const { data: response } = await http.get('/admin/pay/dashboard/channelConfigList', { params });
    const payload = response?.data ?? response;
    tableRows.value = payload?.records ?? [];
    tableTotal.value = payload?.total ?? 0;
    tablePage.value = payload?.current ?? payload?.pageNum ?? tablePage.value;
    tablePageSize.value = payload?.size ?? payload?.pageSize ?? tablePageSize.value;
  } catch (error) {
    console.error('Failed to load channel config list', error);
    tableError.value = '无法加载通道配置列表';
    tableRows.value = [];
    tableTotal.value = 0;
  } finally {
    tableLoading.value = false;
  }
};

const handleSearch = () => { tablePage.value = 1; fetchList(); };
const handleReset = () => { searchTitle.value = ''; searchNullify.value = ''; tablePage.value = 1; fetchList(); };
const changePage = (next) => { if (next < 1 || next > totalPages.value || next === tablePage.value) return; tablePage.value = next; fetchList(); };
const changePageSize = (size) => { tablePageSize.value = size; tablePage.value = 1; fetchList(); };
const switchTab = (tab) => { activeTab.value = tab; tablePage.value = 1; fetchList(); };

const toggleNullify = async (row) => {
  const newNullify = row.nullify === 0 ? 1 : 0;
  try {
    await http.patch('/admin/pay/dashboard/channelConfigNullify', null, {
      params: { id: row.id, nullify: newNullify }
    });
    row.nullify = newNullify;
  } catch (error) {
    console.error('Failed to update nullify status', error);
  }
};

// ── Edit Drawer ───────────────────────────────────────────────────
const drawerVisible = ref(false);
const drawerSaving = ref(false);
const drawerError = ref('');

const parseReqParam = (raw) => {
  if (!raw) return {};
  try { return JSON.parse(raw); } catch { return {}; }
};

const form = reactive({
  id: null,
  title: '',
  thirdService: '',
  areaType: '',
  shortCode: '',
  remark: '',
  channelType: '收款',
  settlementDays: 1,
  settlementStatus: 1,
  nullify: 0,
  minAmount: 0,
  maxAmount: 50000,
  dailyLimit: 10000000,
  pendingDailyLimit: '',
  appId: '',
  privateKey: '',
  secret: '',
  provider: '',
  mode: '',
  submitUtrForwardChatId: '',
  webhook: '',
  riskControl: ''
});

const openEdit = (row) => {
  const extra = parseReqParam(row.reqParam);
  form.id = row.id;
  form.title = row.title ?? '';
  form.thirdService = row.thirdService ?? '';
  form.areaType = row.areaType != null ? String(row.areaType) : '';
  form.shortCode = row.shortCode ?? '';
  form.remark = row.remark ?? '';
  form.channelType = extra.channelType ?? '收款';
  form.settlementDays = extra.settlementDays ?? 1;
  form.settlementStatus = extra.settlementStatus ?? 1;
  form.nullify = row.nullify ?? 0;
  form.minAmount = extra.minAmount ?? 0;
  form.maxAmount = extra.maxAmount ?? 50000;
  form.dailyLimit = extra.dailyLimit ?? 10000000;
  form.pendingDailyLimit = extra.pendingDailyLimit ?? '';
  form.appId = row.appId ?? '';
  form.privateKey = row.privateKey ?? '';
  form.secret = extra.secret ?? '';
  form.provider = extra.provider ?? '';
  form.mode = extra.mode ?? '';
  form.submitUtrForwardChatId = extra.submitUtrForwardChatId ?? '';
  form.webhook = extra.webhook ?? '';
  form.riskControl = extra.riskControl ?? '';
  drawerError.value = '';
  drawerVisible.value = true;
};

const closeDrawer = () => { drawerVisible.value = false; };

const submitEdit = async () => {
  if (!form.title) { drawerError.value = '名称不能为空'; return; }
  drawerSaving.value = true;
  drawerError.value = '';
  try {
    const reqParam = JSON.stringify({
      channelType: form.channelType,
      settlementDays: Number(form.settlementDays),
      settlementStatus: form.settlementStatus,
      minAmount: Number(form.minAmount),
      maxAmount: Number(form.maxAmount),
      dailyLimit: Number(form.dailyLimit),
      pendingDailyLimit: form.pendingDailyLimit,
      secret: form.secret,
      provider: form.provider,
      mode: form.mode,
      submitUtrForwardChatId: form.submitUtrForwardChatId,
      webhook: form.webhook,
      riskControl: form.riskControl
    });
    await http.put('/admin/pay/dashboard/channelConfigUpdate', {
      id: form.id,
      title: form.title,
      areaType: form.areaType !== '' ? Number(form.areaType) : null,
      remark: form.remark,
      nullify: form.nullify,
      thirdService: form.thirdService,
      shortCode: form.shortCode,
      reqParam,
      appId: form.appId,
      privateKey: form.privateKey
    });
    // Refresh row in table
    const row = tableRows.value.find(r => r.id === form.id);
    if (row) {
      row.title = form.title;
      row.areaType = form.areaType !== '' ? Number(form.areaType) : row.areaType;
      row.remark = form.remark;
      row.nullify = form.nullify;
      row.thirdService = form.thirdService;
      row.shortCode = form.shortCode;
      row.appId = form.appId;
      row.privateKey = form.privateKey;
      row.reqParam = reqParam;
    }
    drawerVisible.value = false;
  } catch (error) {
    console.error('Failed to update channel config', error);
    drawerError.value = '保存失败，请重试';
  } finally {
    drawerSaving.value = false;
  }
};

onMounted(fetchList);
</script>

<template>
  <div class="channel-view">
    <!-- ── 通道配置列表 ─────────────────────────────────────────── -->
    <section class="panel config-list-panel">
      <div class="tab-bar">
        <button type="button" class="tab-btn" :class="{ active: activeTab === 'payin' }" @click="switchTab('payin')">收款通道</button>
        <button type="button" class="tab-btn" :class="{ active: activeTab === 'payout' }" @click="switchTab('payout')">出款通道</button>
      </div>

      <div class="search-bar">
        <input v-model="searchTitle" class="search-input" placeholder="输入名称" @keyup.enter="handleSearch" />
        <select v-model="searchNullify" class="search-select">
          <option value="">状态</option>
          <option value="0">启用</option>
          <option value="1">未启用</option>
        </select>
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
              <th>货币</th>
              <th>名称</th>
              <th>域名</th>
              <th>描述</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!tableRows.length">
              <td colspan="7" class="empty-cell">暂无数据</td>
            </tr>
            <tr v-for="(row, index) in tableRows" :key="row.id ?? index">
              <td>{{ row.areaType ?? '—' }}</td>
              <td>{{ row.title ?? '—' }}</td>
              <td class="url-cell">{{ row.url ?? '—' }}</td>
              <td>{{ row.remark ?? '—' }}</td>
              <td>
                <button type="button" class="toggle-switch" :class="{ on: row.nullify === 0 }" @click="toggleNullify(row)">
                  <span class="toggle-thumb" />
                </button>
              </td>
              <td>{{ formatDateTime(row.createTime) }}</td>
              <td class="action-cell">
                <button type="button" class="icon-btn edit-btn" title="编辑" @click="openEdit(row)">
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

    <section class="routing-maintenance">
      <article class="panel routing">
        <header>
          <p class="eyebrow">路由策略</p>
          <h3>执行进度</h3>
        </header>
        <div class="routing-list">
          <div v-for="route in data.routing" :key="route.id" class="routing-row">
            <div>
              <p class="title">{{ route.title }}</p>
              <p class="meta">{{ route.owner }}</p>
            </div>
            <div class="progress">
              <span :style="{ width: `${route.progress}%` }" />
            </div>
            <span class="percent">{{ route.progress }}%</span>
          </div>
        </div>
      </article>
      <article class="panel maintenance">
        <header>
          <p class="eyebrow">维护窗口</p>
          <h3>计划</h3>
        </header>
        <ul>
          <li v-for="item in data.maintenance" :key="item.id">
            <div>
              <p class="label">{{ item.channel }}</p>
              <p class="meta">{{ item.note }}</p>
            </div>
            <span class="window">{{ item.window }}</span>
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

    <!-- ── Edit Drawer Overlay ────────────────────────────────── -->
    <Teleport to="body">
      <div v-if="drawerVisible" class="drawer-overlay" @click.self="closeDrawer">
        <div class="drawer">
          <div class="drawer-header">
            <span class="drawer-title">编辑</span>
            <button type="button" class="drawer-close" @click="closeDrawer">✕</button>
          </div>

          <div class="drawer-body">
            <!-- 名称 -->
            <div class="form-row">
              <label class="form-label required">名称</label>
              <input v-model="form.title" class="form-input" placeholder="名称" />
            </div>

            <!-- 通道 -->
            <div class="form-row">
              <label class="form-label required">通道</label>
              <input v-model="form.thirdService" class="form-input" placeholder="通道名称" />
            </div>

            <!-- 货币 -->
            <div class="form-row">
              <label class="form-label">货币</label>
              <input v-model="form.areaType" class="form-input" placeholder="货币代码 (如 INR)" />
            </div>

            <!-- 主体 -->
            <div class="form-row">
              <label class="form-label">主体</label>
              <input v-model="form.shortCode" class="form-input" placeholder="主体标识" />
            </div>

            <!-- 备注 -->
            <div class="form-row">
              <label class="form-label">备注</label>
              <input v-model="form.remark" class="form-input" placeholder="备注" />
            </div>

            <!-- 通道类型 -->
            <div class="form-row">
              <label class="form-label">通道类型</label>
              <select v-model="form.channelType" class="form-select">
                <option value="收款">收款</option>
                <option value="出款">出款</option>
              </select>
            </div>

            <!-- 结算周期 + 结算状态 -->
            <div class="form-row-split">
              <div class="form-col">
                <label class="form-label">结算周期（天）</label>
                <input v-model.number="form.settlementDays" type="number" class="form-input" min="1" />
              </div>
              <div class="form-col">
                <label class="form-label">结算状态</label>
                <div class="toggle-wrap">
                  <button
                    type="button"
                    class="toggle-switch"
                    :class="{ on: form.settlementStatus === 1 }"
                    @click="form.settlementStatus = form.settlementStatus === 1 ? 0 : 1"
                  >
                    <span class="toggle-thumb" />
                  </button>
                  <span class="toggle-label">{{ form.settlementStatus === 1 ? 'ON' : 'OFF' }}</span>
                </div>
              </div>
            </div>

            <!-- 启用 + 单笔最小金额 -->
            <div class="form-row-split">
              <div class="form-col">
                <label class="form-label">启用</label>
                <div class="toggle-wrap">
                  <button
                    type="button"
                    class="toggle-switch"
                    :class="{ on: form.nullify === 0 }"
                    @click="form.nullify = form.nullify === 0 ? 1 : 0"
                  >
                    <span class="toggle-thumb" />
                  </button>
                  <span class="toggle-label">{{ form.nullify === 0 ? 'ON' : 'OFF' }}</span>
                </div>
              </div>
              <div class="form-col">
                <label class="form-label required">单笔最小金额</label>
                <input v-model.number="form.minAmount" type="number" class="form-input" min="0" />
              </div>
            </div>

            <!-- 单笔最大金额 + 当日限额 -->
            <div class="form-row-split">
              <div class="form-col">
                <label class="form-label required">单笔最大金额</label>
                <input v-model.number="form.maxAmount" type="number" class="form-input" min="0" />
              </div>
              <div class="form-col">
                <label class="form-label">当日限额</label>
                <input v-model.number="form.dailyLimit" type="number" class="form-input" min="0" />
              </div>
            </div>

            <!-- 当天待结算限额 -->
            <div class="form-row">
              <label class="form-label">当天待结算限额</label>
              <input v-model="form.pendingDailyLimit" type="number" class="form-input" placeholder="" />
            </div>

            <!-- 成本配置 -->
            <div class="section-card">
              <div class="section-card-title">成本配置</div>
              <button type="button" class="btn btn-ghost btn-sm">新增规则</button>
            </div>

            <!-- 通道参数 -->
            <div class="section-card">
              <div class="section-card-title">通道参数</div>

              <div class="form-row">
                <label class="form-label">商户号</label>
                <input v-model="form.appId" class="form-input" placeholder="merchant_info.app_id" />
              </div>

              <div class="form-row">
                <label class="form-label">key</label>
                <input v-model="form.privateKey" class="form-input" placeholder="merchant_info.private_key" />
              </div>

              <div class="form-row">
                <label class="form-label">secret</label>
                <input v-model="form.secret" class="form-input" />
              </div>

              <div class="form-row">
                <label class="form-label">provider</label>
                <input v-model="form.provider" class="form-input" />
              </div>

              <div class="form-row">
                <label class="form-label">mode</label>
                <input v-model="form.mode" class="form-input" />
              </div>

              <div class="form-row">
                <label class="form-label">submitUtrForwardChatId</label>
                <input v-model="form.submitUtrForwardChatId" class="form-input" />
              </div>
            </div>

            <!-- Webhook -->
            <div class="section-card">
              <div class="section-card-title">Webhook</div>
              <input v-model="form.webhook" class="form-input" placeholder="https://..." />
            </div>

            <!-- 风控 -->
            <div class="section-card">
              <div class="section-card-title">风控</div>
              <select v-model="form.riskControl" class="form-select">
                <option value=""></option>
              </select>
            </div>

            <p v-if="drawerError" class="drawer-error">{{ drawerError }}</p>
          </div>

          <div class="drawer-footer">
            <button type="button" class="btn btn-ghost" @click="closeDrawer">取消</button>
            <button type="button" class="btn btn-primary" :disabled="drawerSaving" @click="submitEdit">
              {{ drawerSaving ? '保存中...' : '提交' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.channel-view {
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
  background: linear-gradient(135deg, rgba(70, 95, 255, 0.65), rgba(11, 16, 32, 0.95));
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

/* ── 通道配置列表 ─────────────────────────────────────────────── */
.config-list-panel { display: flex; flex-direction: column; gap: 14px; }

.tab-bar {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  margin-bottom: 4px;
}

.tab-btn {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.55);
  padding: 8px 18px;
  font-size: 14px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}

.tab-btn.active { color: #60a5fa; border-bottom-color: #60a5fa; }

.search-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }

.search-input {
  flex: 1; min-width: 180px; max-width: 360px;
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
  padding: 8px 14px; font-size: 13px; cursor: pointer; min-width: 120px;
}

.btn { padding: 8px 18px; border-radius: 10px; font-size: 13px; cursor: pointer; border: none; }
.btn-sm { padding: 5px 12px; font-size: 12px; }
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
.url-cell { max-width: 200px; overflow: hidden; text-overflow: ellipsis; }
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
.routing-maintenance { display: grid; gap: 24px; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); }
.routing-list { display: flex; flex-direction: column; gap: 12px; margin-top: 12px; }
.routing-row { display: grid; grid-template-columns: 1fr 1fr 60px; gap: 12px; align-items: center; padding: 12px 0; border-bottom: 1px solid rgba(255, 255, 255, 0.05); }
.routing-row .progress { height: 6px; border-radius: 999px; background: rgba(255, 255, 255, 0.08); overflow: hidden; }
.routing-row .progress span { display: block; height: 100%; border-radius: inherit; background: linear-gradient(120deg, rgba(126, 143, 255, 0.8), rgba(99, 228, 255, 0.8)); }
.maintenance ul, .notices ul { list-style: none; padding: 0; margin: 12px 0 0; display: flex; flex-direction: column; gap: 12px; }
.maintenance li, .notices li { padding: 12px; border-radius: 16px; background: rgba(255, 255, 255, 0.02); display: flex; justify-content: space-between; align-items: center; }

/* ── Edit Drawer ─────────────────────────────────────────────── */
.drawer-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0, 0, 0, 0.55);
  display: flex; justify-content: flex-end;
}

.drawer {
  width: 580px; max-width: 95vw;
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
  flex: 1; overflow-y: auto; padding: 20px 24px;
  display: flex; flex-direction: column; gap: 16px;
}

.drawer-footer {
  display: flex; justify-content: flex-end; gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.form-row { display: flex; flex-direction: column; gap: 6px; }

.form-row-split { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

.form-col { display: flex; flex-direction: column; gap: 6px; }

.form-label {
  font-size: 13px; color: rgba(255, 255, 255, 0.7);
}

.form-label.required::before {
  content: '* '; color: #f87171;
}

.form-input {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px; color: #fff;
  padding: 9px 14px; font-size: 13px; outline: none; width: 100%;
  box-sizing: border-box;
}

.form-input:focus { border-color: rgba(127, 133, 249, 0.6); }

.form-select {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px; color: rgba(255, 255, 255, 0.9);
  padding: 9px 14px; font-size: 13px; width: 100%; cursor: pointer;
}

.toggle-wrap { display: flex; align-items: center; gap: 10px; padding-top: 4px; }
.toggle-label { font-size: 13px; color: rgba(255, 255, 255, 0.7); }

.section-card {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px; padding: 16px;
  display: flex; flex-direction: column; gap: 12px;
}

.section-card-title {
  font-size: 14px; font-weight: 600; color: rgba(255, 255, 255, 0.9);
  padding-bottom: 8px; border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}

.drawer-error { color: #ff8e8e; font-size: 13px; }

@media (max-width: 960px) {
  .form-row-split { grid-template-columns: 1fr; }
  .drawer { width: 100vw; }
}
</style>
