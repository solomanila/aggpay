<script setup>
import { computed, onMounted, ref } from 'vue';
import http from '../services/http';
import { AREA_TYPE_MAP } from '../data/constants';

const areaTypeOptions = Object.entries(AREA_TYPE_MAP).map(([key, val]) => ({
  areaType: Number(key),
  currencyCode: val.currencyCode,
  label: val.label
}));

const paginationSizes = [10, 20, 50];

// ── filter state ─────────────────────────────────────────────────
const searchTitle    = ref('');
const searchCurrency = ref('');
const searchStatus   = ref('');
const searchId       = ref('');

// ── channel options dropdown ─────────────────────────────────────
const channelOptions = ref([]);
const fetchChannelOptions = async () => {
  try {
    const { data: res } = await http.get('/admin/pay/dashboard/allChannelOptions');
    channelOptions.value = res?.data ?? res ?? [];
  } catch {
    channelOptions.value = [];
  }
};

// ── entity options (主体) for drawer ─────────────────────────────
const entityOptions = ref([]);
const fetchEntityOptions = async () => {
  try {
    const { data: res } = await http.get('/admin/pay/dashboard/payConfigInfoOptions');
    entityOptions.value = res?.data ?? res ?? [];
  } catch {
    entityOptions.value = [];
  }
};

// ── table state ──────────────────────────────────────────────────
const tableRows      = ref([]);
const tableTotal     = ref(0);
const tablePage      = ref(1);
const tablePageSize  = ref(20);
const tableLoading   = ref(false);
const tableError     = ref('');

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
    if (searchTitle.value.trim())   params.title    = searchTitle.value.trim();
    if (searchCurrency.value)       params.currency = searchCurrency.value;
    if (searchStatus.value !== '')  params.status   = Number(searchStatus.value);
    if (searchId.value !== '')      params.id       = Number(searchId.value);
    const { data: res } = await http.get('/admin/pay/dashboard/payChannelPage', { params });
    const payload = res?.data ?? res;
    tableRows.value      = payload?.records ?? [];
    tableTotal.value     = payload?.total   ?? 0;
    tablePage.value      = payload?.current ?? payload?.pageNum ?? tablePage.value;
    tablePageSize.value  = payload?.size    ?? payload?.pageSize ?? tablePageSize.value;
  } catch (error) {
    console.error('Failed to load channel page', error);
    tableError.value = '无法加载通道列表';
    tableRows.value  = [];
    tableTotal.value = 0;
  } finally {
    tableLoading.value = false;
  }
};

const handleSearch = () => { tablePage.value = 1; fetchList(); };
const handleReset  = () => {
  searchTitle.value    = '';
  searchCurrency.value = '';
  searchStatus.value   = '';
  searchId.value       = '';
  tablePage.value      = 1;
  fetchList();
};
const changePage     = (next) => {
  if (next < 1 || next > totalPages.value || next === tablePage.value) return;
  tablePage.value = next;
  fetchList();
};
const changePageSize = (size) => { tablePageSize.value = size; tablePage.value = 1; fetchList(); };

// ── drawer state ─────────────────────────────────────────────────
const drawerMode       = ref('');   // 'create' | 'edit'
const drawerEditId     = ref(null);
const drawerSubmitting = ref(false);
const drawerError      = ref('');

// form fields
const fTitle         = ref('');
const fChannel       = ref('');    // 通道名称 → jsonParam.channel
const fCurrency      = ref('');    // 货币 → jsonParam.currency
const fEntityId      = ref('');    // 主体 → payConfigId
const fRemark        = ref('');    // 备注 → jsonParam.remark
const fShareId       = ref(1);     // 通道类型: 1=收款 2=出款
const fSettleDays    = ref(1);     // 结算周期
const fSettleStatus  = ref(true);  // 结算状态 true=ON
const fStatus        = ref(true);  // 启用 true=ON(status=0) false=关(status=1)
const fMinAmount     = ref(0);
const fMaxAmount     = ref(500000);
const fDailyLimit    = ref('');
const fPendingLimit  = ref('');

const drawerOpen = computed(() => drawerMode.value !== '');

const resetForm = () => {
  fTitle.value        = '';
  fChannel.value      = '';
  fCurrency.value     = '';
  fEntityId.value     = '';
  fRemark.value       = '';
  fShareId.value      = 1;
  fSettleDays.value   = 1;
  fSettleStatus.value = true;
  fStatus.value       = true;
  fMinAmount.value    = 0;
  fMaxAmount.value    = 500000;
  fDailyLimit.value   = '';
  fPendingLimit.value = '';
  drawerError.value   = '';
};

const openCreate = () => {
  resetForm();
  drawerMode.value   = 'create';
  drawerEditId.value = null;
};

const openEdit = (row) => {
  resetForm();
  drawerMode.value   = 'edit';
  drawerEditId.value = row.id;
  fTitle.value       = row.title ?? '';
  // parse jsonParam if available
  let jp = {};
  try { jp = row.jsonParam ? JSON.parse(row.jsonParam) : {}; } catch { jp = {}; }
  fChannel.value      = jp.channel      ?? row.thirdService ?? '';
  fCurrency.value     = jp.currency     ?? row.currency     ?? '';
  // configId is pay_config_info.id returned by payChannelPage
  fEntityId.value     = row.configId != null ? String(row.configId) : '';
  fRemark.value       = jp.remark       ?? row.remark       ?? '';
  fShareId.value      = row.shareId     ?? jp.shareId       ?? 1;
  fSettleDays.value   = jp.settleDays   ?? 1;
  fSettleStatus.value = jp.settleStatus !== undefined ? Boolean(jp.settleStatus) : true;
  fStatus.value       = row.status === 0;
  fMinAmount.value    = jp.minAmount    ?? 0;
  fMaxAmount.value    = jp.maxAmount    ?? 500000;
  fDailyLimit.value   = jp.dailyLimit   ?? '';
  fPendingLimit.value = jp.pendingLimit ?? '';
};

const closeDrawer = () => {
  drawerMode.value = '';
  drawerError.value = '';
};

const buildJsonParam = () => JSON.stringify({
  channel:       fChannel.value,
  currency:      fCurrency.value,
  remark:        fRemark.value,
  settleDays:    Number(fSettleDays.value),
  settleStatus:  fSettleStatus.value,
  minAmount:     Number(fMinAmount.value),
  maxAmount:     Number(fMaxAmount.value),
  dailyLimit:    fDailyLimit.value !== '' ? Number(fDailyLimit.value) : null,
  pendingLimit:  fPendingLimit.value !== '' ? Number(fPendingLimit.value) : null,
});

const submitDrawer = async () => {
  if (!fTitle.value.trim()) { drawerError.value = '请输入名称'; return; }
  if (!fEntityId.value)     { drawerError.value = '请选择主体'; return; }
  drawerSubmitting.value = true;
  drawerError.value = '';
  try {
    const payload = {
      payConfigId: Number(fEntityId.value),
      title:       fTitle.value.trim(),
      jsonParam:   buildJsonParam(),
      status:      fStatus.value ? 0 : 1,
      shareId:     Number(fShareId.value),
    };
    if (drawerMode.value === 'create') {
      await http.post('/admin/pay/dashboard/payConfigChannelCreate', payload);
    } else {
      payload.id = drawerEditId.value;
      await http.put('/admin/pay/dashboard/payConfigChannelUpdate', payload);
    }
    closeDrawer();
    fetchList();
  } catch (error) {
    console.error('Failed to submit channel', error);
    drawerError.value = drawerMode.value === 'create' ? '新建失败，请重试' : '编辑失败，请重试';
  } finally {
    drawerSubmitting.value = false;
  }
};

onMounted(() => { fetchChannelOptions(); fetchEntityOptions(); fetchList(); });
</script>

<template>
  <div class="channel-view">
    <section class="panel config-list-panel">
      <!-- filter bar -->
      <div class="search-bar">
        <input
          v-model="searchTitle"
          class="search-input"
          placeholder="输入名称"
          @keyup.enter="handleSearch"
        />

        <select v-model="searchCurrency" class="search-select">
          <option value="">货币</option>
          <option
            v-for="opt in areaTypeOptions"
            :key="opt.areaType"
            :value="opt.currencyCode"
          >{{ opt.currencyCode }} · {{ opt.label }}</option>
        </select>

        <select v-model="searchStatus" class="search-select">
          <option value="">状态</option>
          <option value="0">开</option>
          <option value="1">关</option>
        </select>

        <select v-model="searchId" class="search-select search-select--wide">
          <option value="">通道</option>
          <option
            v-for="ch in channelOptions"
            :key="ch.id"
            :value="ch.id"
          >{{ ch.title }}</option>
        </select>

        <button type="button" class="btn btn-primary" @click="handleSearch">搜索</button>
        <button type="button" class="btn btn-ghost" @click="handleReset">重置</button>
      </div>

      <!-- toolbar -->
      <div class="toolbar">
        <button type="button" class="btn btn-create" @click="openCreate">新建</button>
      </div>

      <!-- table -->
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
              <th>meta</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!tableRows.length">
              <td colspan="8" class="empty-cell">暂无数据</td>
            </tr>
            <tr v-for="(row, index) in tableRows" :key="row.id ?? index">
              <td><span class="currency-tag">{{ row.currency ?? '—' }}</span></td>
              <td>{{ row.title ?? '—' }}</td>
              <td class="url-cell">{{ row.url ?? '—' }}</td>
              <td>{{ row.description ?? '—' }}</td>
              <td>{{ row.configTitle ?? '—' }}</td>
              <td>
                <span class="toggle-switch" :class="{ on: row.status === 0 }">
                  <span class="toggle-thumb" />
                </span>
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

      <!-- pagination -->
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

    <!-- overlay + drawer -->
    <Teleport to="body">
      <div v-if="drawerOpen" class="drawer-overlay" @click.self="closeDrawer" />
      <div v-if="drawerOpen" class="drawer-panel">
        <div class="drawer-header">
          <span class="drawer-title-text">{{ drawerMode === 'create' ? '新建' : '编辑' }}</span>
          <button type="button" class="drawer-close-btn" @click="closeDrawer">✕</button>
        </div>

        <div class="drawer-body">
          <!-- 名称 -->
          <div class="form-group">
            <label class="form-label"><span class="req">*</span>名称</label>
            <input v-model="fTitle" class="form-input" :disabled="drawerSubmitting" />
          </div>

          <!-- 通道 -->
          <div class="form-group">
            <label class="form-label"><span class="req">*</span>通道</label>
            <input v-model="fChannel" class="form-input" :disabled="drawerSubmitting" placeholder="例: AirPay" />
          </div>

          <!-- 货币 -->
          <div class="form-group">
            <label class="form-label">货币</label>
            <select v-model="fCurrency" class="form-select" :disabled="drawerSubmitting">
              <option value=""></option>
              <option v-for="opt in areaTypeOptions" :key="opt.areaType" :value="opt.currencyCode">
                {{ opt.currencyCode }} · {{ opt.label }}
              </option>
            </select>
          </div>

          <!-- 主体 -->
          <div class="form-group">
            <label class="form-label"><span class="req">*</span>主体</label>
            <select v-model="fEntityId" class="form-select" :disabled="drawerSubmitting">
              <option value=""></option>
              <option v-for="e in entityOptions" :key="e.id" :value="String(e.id)">
                {{ e.title ?? e.shortCode ?? e.id }}
              </option>
            </select>
          </div>

          <!-- 备注 -->
          <div class="form-group">
            <label class="form-label">备注</label>
            <input v-model="fRemark" class="form-input" :disabled="drawerSubmitting" />
          </div>

          <!-- 通道类型 -->
          <div class="form-group">
            <label class="form-label">通道类型</label>
            <select v-model="fShareId" class="form-select" :disabled="drawerSubmitting">
              <option :value="1">收款</option>
              <option :value="2">出款</option>
            </select>
          </div>

          <!-- 结算周期 + 结算状态 -->
          <div class="form-row">
            <div class="form-group half">
              <label class="form-label">结算周期（天）</label>
              <input v-model.number="fSettleDays" type="number" min="1" class="form-input" :disabled="drawerSubmitting" />
            </div>
            <div class="form-group half">
              <label class="form-label">结算状态</label>
              <div class="toggle-row">
                <span class="toggle-switch" :class="{ on: fSettleStatus }" @click="!drawerSubmitting && (fSettleStatus = !fSettleStatus)">
                  <span class="toggle-thumb" />
                </span>
                <span class="toggle-label">{{ fSettleStatus ? 'ON' : 'OFF' }}</span>
              </div>
            </div>
          </div>

          <!-- 启用 + 单笔最小金额 -->
          <div class="form-row">
            <div class="form-group half">
              <label class="form-label">启用</label>
              <div class="toggle-row">
                <span class="toggle-switch" :class="{ on: fStatus }" @click="!drawerSubmitting && (fStatus = !fStatus)">
                  <span class="toggle-thumb" />
                </span>
                <span class="toggle-label">{{ fStatus ? 'ON' : 'OFF' }}</span>
              </div>
            </div>
            <div class="form-group half">
              <label class="form-label"><span class="req">*</span>单笔最小金额</label>
              <input v-model.number="fMinAmount" type="number" min="0" class="form-input" :disabled="drawerSubmitting" />
            </div>
          </div>

          <!-- 单笔最大金额 + 当日限额 -->
          <div class="form-row">
            <div class="form-group half">
              <label class="form-label"><span class="req">*</span>单笔最大金额</label>
              <input v-model.number="fMaxAmount" type="number" min="0" class="form-input" :disabled="drawerSubmitting" />
            </div>
            <div class="form-group half">
              <label class="form-label">当日限额</label>
              <input v-model="fDailyLimit" type="number" min="0" class="form-input" :disabled="drawerSubmitting" />
            </div>
          </div>

          <!-- 当天待结算限额 -->
          <div class="form-group">
            <label class="form-label">当天待结算限额</label>
            <input v-model="fPendingLimit" type="number" min="0" class="form-input" :disabled="drawerSubmitting" />
          </div>

          <!-- 成本配置 section header -->
          <div class="section-header">成本配置</div>

          <p v-if="drawerError" class="drawer-error">{{ drawerError }}</p>
        </div>

        <div class="drawer-footer">
          <button type="button" class="btn btn-ghost" :disabled="drawerSubmitting" @click="closeDrawer">取消</button>
          <button type="button" class="btn btn-primary" :disabled="drawerSubmitting" @click="submitDrawer">
            {{ drawerSubmitting ? '提交中...' : '提交' }}
          </button>
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

.muted  { color: rgba(255, 255, 255, 0.7); }
.error-text { color: #ff8e8e; }

.config-list-panel { display: flex; flex-direction: column; gap: 14px; }

.search-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 160px;
  max-width: 320px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  color: #fff;
  padding: 8px 14px;
  font-size: 13px;
  outline: none;
}
.search-input::placeholder { color: rgba(255, 255, 255, 0.35); }
.search-input:focus { border-color: rgba(127, 133, 249, 0.6); }

.search-select {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  color: rgba(255, 255, 255, 0.8);
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
  min-width: 130px;
}

.search-select--wide { min-width: 200px; }

.btn { padding: 8px 18px; border-radius: 10px; font-size: 13px; cursor: pointer; border: none; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary { background: rgba(100, 116, 255, 0.85); color: #fff; }
.btn-primary:hover:not(:disabled) { background: rgba(100, 116, 255, 1); }
.btn-ghost { background: rgba(255, 255, 255, 0.07); color: rgba(255, 255, 255, 0.8); border: 1px solid rgba(255, 255, 255, 0.15); }
.btn-ghost:hover:not(:disabled) { background: rgba(255, 255, 255, 0.12); }
.btn-create { background: rgba(34, 197, 94, 0.2); color: #4ade80; border: 1px solid rgba(34, 197, 94, 0.3); }
.btn-create:hover { background: rgba(34, 197, 94, 0.3); }

.table-wrapper { overflow-x: auto; }
.table-wrapper table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table-wrapper th,
.table-wrapper td {
  padding: 11px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  text-align: left;
  white-space: nowrap;
}
.table-wrapper th { color: rgba(255, 255, 255, 0.6); font-weight: 500; background: rgba(255, 255, 255, 0.02); }
.table-wrapper td { color: rgba(255, 255, 255, 0.88); }
.url-cell { max-width: 220px; overflow: hidden; text-overflow: ellipsis; }
.empty-cell { text-align: center; padding: 28px; color: rgba(255, 255, 255, 0.45); }

.currency-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(96, 165, 250, 0.15);
  color: #93c5fd;
}

.toggle-switch {
  display: inline-flex;
  position: relative;
  width: 40px;
  height: 22px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.15);
  cursor: default;
  flex-shrink: 0;
}
.toggle-switch.on { background: #3b82f6; }
.toggle-thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.2s;
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

/* ── Drawer ─────────────────────────────────────────── */
.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 200;
}

.drawer-panel {
  position: fixed;
  top: 0;
  right: 0;
  width: 360px;
  height: 100vh;
  background: #1a1b2e;
  border-left: 1px solid rgba(255, 255, 255, 0.08);
  z-index: 201;
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 32px rgba(0, 0, 0, 0.4);
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.drawer-title-text {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}

.drawer-close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  font-size: 16px;
  padding: 2px 6px;
  border-radius: 4px;
  line-height: 1;
}
.drawer-close-btn:hover { color: #fff; background: rgba(255, 255, 255, 0.08); }

.drawer-body {
  flex: 1;
  padding: 20px 20px 8px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group.half { flex: 1; min-width: 0; }

.form-row {
  display: flex;
  gap: 14px;
}

.form-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.65);
}

.req { color: #f87171; margin-right: 2px; }

.form-input,
.form-select {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  color: #fff;
  padding: 8px 11px;
  font-size: 13px;
  outline: none;
  width: 100%;
  box-sizing: border-box;
}
.form-input::placeholder { color: rgba(255, 255, 255, 0.28); }
.form-input:focus,
.form-select:focus { border-color: rgba(127, 133, 249, 0.6); }
.form-input:disabled,
.form-select:disabled { opacity: 0.5; cursor: not-allowed; }
.form-select option { background: #1a1b2e; }

.toggle-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
}

.toggle-switch { cursor: pointer; }

.toggle-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  min-width: 28px;
}

.section-header {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.5);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  padding-bottom: 6px;
  margin-top: 4px;
}

.drawer-error {
  font-size: 12px;
  color: #ff8e8e;
  margin: 0;
}

.drawer-footer {
  padding: 14px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-shrink: 0;
}
</style>
