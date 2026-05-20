<script setup>
import { computed, onMounted, ref } from 'vue';
import http from '../services/http';

const paginationSizes = [10, 20, 50];

const searchShortCode = ref('');
const tableRows = ref([]);
const tableTotal = ref(0);
const tablePage = ref(1);
const tablePageSize = ref(20);
const tableLoading = ref(false);
const tableError = ref('');

// drawer state
const drawerMode = ref(''); // 'create' | 'edit'
const drawerTitle = ref('');
const drawerUrl = ref('');
const drawerEditId = ref(null);
const drawerSubmitting = ref(false);
const drawerError = ref('');

const drawerOpen = computed(() => drawerMode.value !== '');

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

const openCreate = () => {
  drawerMode.value = 'create';
  drawerTitle.value = '';
  drawerUrl.value = '';
  drawerEditId.value = null;
  drawerError.value = '';
};

const openEdit = (row) => {
  drawerMode.value = 'edit';
  drawerTitle.value = row.title ?? row.shortCode ?? '';
  drawerUrl.value = row.url ?? '';
  drawerEditId.value = row.id;
  drawerError.value = '';
};

const closeDrawer = () => {
  drawerMode.value = '';
  drawerError.value = '';
};

const submitDrawer = async () => {
  if (!drawerTitle.value.trim()) {
    drawerError.value = '请输入名称';
    return;
  }
  if (!drawerUrl.value.trim()) {
    drawerError.value = '请输入域名';
    return;
  }
  drawerSubmitting.value = true;
  drawerError.value = '';
  try {
    if (drawerMode.value === 'create') {
      await http.post('/admin/pay/dashboard/payConfigInfoCreate', null, {
        params: { title: drawerTitle.value.trim(), url: drawerUrl.value.trim() },
      });
    } else {
      await http.put('/admin/pay/dashboard/payConfigInfoUpdate', null, {
        params: { id: drawerEditId.value, title: drawerTitle.value.trim(), url: drawerUrl.value.trim() },
      });
    }
    closeDrawer();
    fetchList();
  } catch (error) {
    console.error('Failed to submit pay config info', error);
    drawerError.value = drawerMode.value === 'create' ? '新建失败，请重试' : '编辑失败，请重试';
  } finally {
    drawerSubmitting.value = false;
  }
};

onMounted(fetchList);
</script>

<template>
  <div class="entity-view">
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
        <button type="button" class="btn btn-create" @click="openCreate">新建</button>
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
              <td>{{ row.title ?? row.shortCode ?? '—' }}</td>
              <td>{{ row.url ?? '—' }}</td>
              <td>{{ row.reqDomain ?? '—' }}</td>
              <td>{{ row.remark ?? '—' }}</td>
              <td>{{ formatDateTime(row.createTime) }}</td>
              <td class="action-cell">
                <button type="button" class="icon-btn edit-btn" title="编辑" @click="openEdit(row)">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
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

    <!-- overlay + drawer -->
    <Teleport to="body">
      <div v-if="drawerOpen" class="drawer-overlay" @click.self="closeDrawer" />
      <div v-if="drawerOpen" class="drawer-panel">
        <div class="drawer-header">
          <span class="drawer-title-text">{{ drawerMode === 'create' ? '新建' : '编辑' }}</span>
          <button type="button" class="drawer-close-btn" @click="closeDrawer">✕</button>
        </div>

        <div class="drawer-body">
          <div class="form-group">
            <label class="form-label">名称</label>
            <input
              v-model="drawerTitle"
              class="form-input"
              :placeholder="drawerMode === 'create' ? '' : ''"
              :disabled="drawerSubmitting"
            />
          </div>
          <div class="form-group">
            <label class="form-label">域名</label>
            <input
              v-model="drawerUrl"
              class="form-input"
              :placeholder="drawerMode === 'create' ? '示例 pay.sparkdancing.link' : ''"
              :disabled="drawerSubmitting"
            />
          </div>
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

.muted {
  color: rgba(255, 255, 255, 0.7);
}

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

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: rgba(100, 116, 255, 0.85);
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: rgba(100, 116, 255, 1);
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.07);
  color: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.btn-ghost:hover:not(:disabled) {
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
  width: 320px;
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

.drawer-close-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.drawer-body {
  flex: 1;
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.form-input {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  color: #fff;
  padding: 9px 12px;
  font-size: 13px;
  outline: none;
  width: 100%;
  box-sizing: border-box;
}

.form-input::placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.form-input:focus {
  border-color: rgba(127, 133, 249, 0.6);
}

.form-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.drawer-error {
  font-size: 12px;
  color: #ff8e8e;
  margin: 0;
}

.drawer-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
