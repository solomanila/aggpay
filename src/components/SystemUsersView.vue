<script setup>
import { computed, onMounted, ref } from 'vue';
import http from '../services/http';

const nameOrEmailFilter = ref('');
const createStartTime = ref('');
const createEndTime = ref('');

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
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : d.toLocaleString('zh-CN', { hour12: false });
};

const fetchUsers = async () => {
  tableLoading.value = true;
  tableError.value = '';
  try {
    const params = { pageNum: tablePage.value, pageSize: tablePageSize.value };
    if (nameOrEmailFilter.value.trim()) params.nameOrEmail = nameOrEmailFilter.value.trim();
    if (createStartTime.value) params.createStartTime = createStartTime.value;
    if (createEndTime.value) params.createEndTime = createEndTime.value;

    const { data: resp } = await http.get('/admin/system/users/page', { params });
    const payload = resp?.data ?? resp;
    tableRows.value = payload?.records ?? [];
    tableTotal.value = payload?.total ?? 0;
    tablePage.value = payload?.current ?? tablePage.value;
    tablePageSize.value = payload?.size ?? tablePageSize.value;
  } catch {
    tableError.value = '加载失败，请稍后重试';
    tableRows.value = [];
    tableTotal.value = 0;
  } finally {
    tableLoading.value = false;
  }
};

const handleSearch = () => {
  tablePage.value = 1;
  fetchUsers();
};

const handleReset = () => {
  nameOrEmailFilter.value = '';
  createStartTime.value = '';
  createEndTime.value = '';
  tablePage.value = 1;
  fetchUsers();
};

const changePage = (next) => {
  if (next < 1 || next > totalPages.value || next === tablePage.value) return;
  tablePage.value = next;
  fetchUsers();
};

const changePageSize = (size) => {
  tablePageSize.value = size;
  tablePage.value = 1;
  fetchUsers();
};

// ---- 自定义确认弹窗 ----
const confirmModal = ref({ visible: false, message: '', onYes: null });
const showConfirm = (message, onYes) => {
  confirmModal.value = { visible: true, message, onYes };
};
const onConfirmYes = () => {
  confirmModal.value.visible = false;
  confirmModal.value.onYes?.();
};

// ---- 自定义结果弹窗 ----
const resultModal = ref({ visible: false, title: '', lines: [] });
const showResult = (title, lines) => {
  resultModal.value = { visible: true, title, lines };
};

const resetingPasswordId = ref(null);
const handleResetPassword = (row) => {
  showConfirm(`确定要重置 ${row.account} 的密码?`, async () => {
    resetingPasswordId.value = row.id;
    try {
      const { data: resp } = await http.post(`/admin/system/users/${row.id}/reset-password`);
      const newPwd = resp?.data?.newPassword ?? resp?.newPassword;
      showResult('新密码', [`账号：${row.account}`, `新密码：${newPwd}`]);
    } catch {
      showResult('错误', ['重置失败，请重试']);
    } finally {
      resetingPasswordId.value = null;
    }
  });
};

const resetingGoogleId = ref(null);
const handleResetGoogleAuth = (row) => {
  showConfirm(`确定要重置 ${row.account} 的谷歌验证码？`, async () => {
    resetingGoogleId.value = row.id;
    try {
      const { data: resp } = await http.post(`/admin/system/users/${row.id}/reset-google-auth`);
      const secret = resp?.data?.googleSecret ?? resp?.googleSecret;
      const otpUrl = resp?.data?.otpAuthUrl ?? resp?.otpAuthUrl;
      showResult('新 Google 密钥', [`账号：${row.account}`, `密钥：${secret}`, `OTP URL：${otpUrl}`]);
    } catch {
      showResult('错误', ['重置失败，请重试']);
    } finally {
      resetingGoogleId.value = null;
    }
  });
};

onMounted(fetchUsers);
</script>

<template>
  <!-- 确认弹窗 -->
  <Teleport to="body">
    <div v-if="confirmModal.visible" class="modal-overlay" @click.self="confirmModal.visible = false">
      <div class="modal-box">
        <div class="modal-header">
          <span class="modal-title">CONFIRM</span>
          <button class="modal-close" @click="confirmModal.visible = false">×</button>
        </div>
        <div class="modal-body">
          <p>{{ confirmModal.message }}</p>
        </div>
        <div class="modal-footer">
          <button class="modal-btn-no" @click="confirmModal.visible = false">NO</button>
          <button class="modal-btn-yes" @click="onConfirmYes">YES</button>
        </div>
      </div>
    </div>

    <!-- 结果弹窗 -->
    <div v-if="resultModal.visible" class="modal-overlay" @click.self="resultModal.visible = false">
      <div class="modal-box">
        <div class="modal-header">
          <span class="modal-title">{{ resultModal.title }}</span>
          <button class="modal-close" @click="resultModal.visible = false">×</button>
        </div>
        <div class="modal-body">
          <p v-for="(line, i) in resultModal.lines" :key="i" class="result-line">{{ line }}</p>
        </div>
        <div class="modal-footer">
          <button class="modal-btn-yes" @click="resultModal.visible = false">OK</button>
        </div>
      </div>
    </div>
  </Teleport>

  <div class="users-view">
    <!-- 过滤栏 -->
    <section class="filter-bar panel">
      <div class="filter-row">
        <input
          v-model="nameOrEmailFilter"
          type="text"
          placeholder="输入名称或邮箱"
          class="filter-input name-input"
          @keyup.enter="handleSearch"
        />
        <div class="date-range">
          <input v-model="createStartTime" type="date" class="filter-input date-input" />
          <span class="date-sep">-</span>
          <input v-model="createEndTime" type="date" class="filter-input date-input" />
        </div>
        <button type="button" class="btn-search" @click="handleSearch">🔍 SEARCH</button>
        <button type="button" class="btn-reset-filter" @click="handleReset">↺ RESET</button>
      </div>
    </section>

    <!-- 表格区域 -->
    <section class="panel table-section">
      <div class="table-toolbar">
        <button type="button" class="btn-new">新建</button>
      </div>

      <p v-if="tableError" class="status-text error">{{ tableError }}</p>
      <p v-else-if="tableLoading" class="status-text">加载中...</p>

      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th class="col-check"><input type="checkbox" /></th>
              <th>用户名</th>
              <th>昵称</th>
              <th>邮箱</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>最近登录时间</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!tableLoading && !tableRows.length">
              <td colspan="8" class="empty-cell">暂无数据</td>
            </tr>
            <tr v-for="row in tableRows" :key="row.id">
              <td class="col-check"><input type="checkbox" /></td>
              <td class="cell-account">{{ row.account }}</td>
              <td>{{ row.name || '—' }}</td>
              <td>{{ row.email || '—' }}</td>
              <td>
                <span class="status-toggle" :class="{ active: row.status === 'ACTIVE' }">
                  <span class="toggle-knob"></span>
                </span>
              </td>
              <td class="cell-time">{{ formatDateTime(row.createdAt) }}</td>
              <td class="cell-time">{{ formatDateTime(row.lastLoginAt) }}</td>
              <td class="col-actions">
                <div class="action-btns">
                  <button type="button" class="action-btn btn-edit" title="编辑">✏</button>
                  <button type="button" class="action-btn btn-delete" title="删除">🗑</button>
                  <button
                    type="button"
                    class="action-btn btn-reset-pwd"
                    title="Reset Password"
                    :disabled="resetingPasswordId === row.id"
                    @click="handleResetPassword(row)"
                  >🔒</button>
                  <button
                    type="button"
                    class="action-btn btn-google"
                    title="Reset Google Auth"
                    :disabled="resetingGoogleId === row.id"
                    @click="handleResetGoogleAuth(row)"
                  >G</button>
                  <button type="button" class="action-btn btn-archive" title="还原">↩</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-bar">
        <span class="page-info">共 {{ tableTotal }} 条</span>
        <div class="page-controls">
          <button type="button" class="ghost-btn" :disabled="tablePage <= 1" @click="changePage(tablePage - 1)">
            &lt;
          </button>
          <span class="page-num">{{ tablePage }}</span>
          <button type="button" class="ghost-btn" :disabled="tablePage >= totalPages" @click="changePage(tablePage + 1)">
            &gt;
          </button>
          <label class="page-size-label">
            <select :value="tablePageSize" @change="changePageSize(Number($event.target.value))">
              <option v-for="size in paginationSizes" :key="size" :value="size">{{ size }} 条/页</option>
            </select>
          </label>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.users-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  background: rgba(18, 19, 37, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  padding: 16px 20px;
}

/* 过滤栏 */
.filter-bar { padding: 12px 16px; }

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-input {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 7px 12px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  outline: none;
  height: 34px;
}
.filter-input::placeholder { color: rgba(255, 255, 255, 0.25); }
.filter-input:focus { border-color: rgba(99, 102, 241, 0.5); }

.name-input { flex: 1; min-width: 200px; max-width: 420px; }

.date-range {
  display: flex;
  align-items: center;
  gap: 6px;
}

.date-input { width: 130px; }

.date-sep {
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
}

.btn-search {
  padding: 7px 18px;
  background: rgba(99, 102, 241, 0.85);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  height: 34px;
}
.btn-search:hover { background: rgba(99, 102, 241, 1); }

.btn-reset-filter {
  padding: 7px 16px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  height: 34px;
}
.btn-reset-filter:hover { background: rgba(255, 255, 255, 0.1); }

/* 工具栏 */
.table-toolbar { margin-bottom: 12px; }

.btn-new {
  padding: 6px 18px;
  background: rgba(99, 102, 241, 0.15);
  border: 1px solid rgba(99, 102, 241, 0.4);
  border-radius: 8px;
  color: rgba(180, 185, 255, 0.9);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.btn-new:hover { background: rgba(99, 102, 241, 0.25); }

.status-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  padding: 16px 0;
  margin: 0;
}
.status-text.error { color: #f87171; }

/* 表格 */
.table-section { padding: 16px 20px 0; }
.table-wrapper { overflow-x: auto; }

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

th {
  padding: 10px 14px;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 0.04em;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  white-space: nowrap;
}

td {
  padding: 11px 14px;
  color: rgba(255, 255, 255, 0.75);
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  white-space: nowrap;
}

tr:last-child td { border-bottom: none; }
tr:hover td { background: rgba(255, 255, 255, 0.02); }

.col-check { width: 36px; }
.col-check input[type="checkbox"] { cursor: pointer; accent-color: #6366f1; }

.cell-account { font-weight: 600; color: rgba(255, 255, 255, 0.9); }
.cell-time { font-size: 12px; color: rgba(255, 255, 255, 0.5); }

.empty-cell {
  text-align: center;
  color: rgba(255, 255, 255, 0.3);
  padding: 40px;
}

/* 状态开关 */
.status-toggle {
  display: inline-flex;
  align-items: center;
  width: 36px;
  height: 20px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  padding: 2px;
  transition: background 0.2s;
  cursor: default;
}

.status-toggle.active {
  background: #6366f1;
}

.toggle-knob {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  transition: transform 0.2s;
}

.status-toggle.active .toggle-knob {
  transform: translateX(16px);
  background: #fff;
}

/* 操作按钮 */
.col-actions { width: 160px; }

.action-btns {
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  font-size: 13px;
  background: transparent;
  transition: background 0.15s;
  position: relative;
}

.action-btn:disabled { opacity: 0.4; cursor: default; }

.btn-edit { color: #60a5fa; }
.btn-edit:hover:not(:disabled) { background: rgba(96, 165, 250, 0.15); }

.btn-delete { color: #f87171; }
.btn-delete:hover:not(:disabled) { background: rgba(248, 113, 113, 0.15); }

.btn-reset-pwd { color: #fb923c; }
.btn-reset-pwd:hover:not(:disabled) { background: rgba(251, 146, 60, 0.15); }

.btn-google {
  color: #fb923c;
  font-weight: 700;
  font-size: 14px;
  font-family: Arial, sans-serif;
}
.btn-google:hover:not(:disabled) { background: rgba(251, 146, 60, 0.15); }

.btn-archive { color: rgba(255, 255, 255, 0.45); }
.btn-archive:hover:not(:disabled) { background: rgba(255, 255, 255, 0.08); }

/* Tooltip via title attribute — enhanced with CSS */
.action-btn[title]:hover::after {
  content: attr(title);
  position: absolute;
  bottom: calc(100% + 6px);
  left: 50%;
  transform: translateX(-50%);
  background: rgba(15, 16, 32, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.85);
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
  padding: 4px 8px;
  border-radius: 6px;
  pointer-events: none;
  z-index: 10;
}

/* 分页 */
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  margin-top: 4px;
}

.page-info { font-size: 13px; color: rgba(255, 255, 255, 0.4); }

.page-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ghost-btn {
  padding: 5px 12px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
}
.ghost-btn:disabled { opacity: 0.3; cursor: default; }
.ghost-btn:not(:disabled):hover { background: rgba(255, 255, 255, 0.1); }

.page-num {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  min-width: 24px;
  text-align: center;
}

.page-size-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
}

.page-size-label select {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  padding: 4px 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  outline: none;
  cursor: pointer;
}

/* 弹窗 — scoped 不会作用于 Teleport，改用 :global */
</style>

<style>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.modal-box {
  width: 380px;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: linear-gradient(90deg, #1e3a8a, #2563eb);
}

.modal-title {
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.08em;
}

.modal-close {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  padding: 0 2px;
}
.modal-close:hover { color: #fff; }

.modal-body {
  background: #1a1c2e;
  padding: 28px 20px 20px;
  min-height: 70px;
}

.modal-body p {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.6;
}

.result-line + .result-line { margin-top: 8px; }

.modal-footer {
  background: #1a1c2e;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px;
}

.modal-btn-no {
  padding: 6px 20px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
  color: rgba(255, 255, 255, 0.75);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  letter-spacing: 0.04em;
}
.modal-btn-no:hover { background: rgba(255, 255, 255, 0.14); }

.modal-btn-yes {
  padding: 6px 20px;
  background: #2563eb;
  border: none;
  border-radius: 4px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  letter-spacing: 0.04em;
}
.modal-btn-yes:hover { background: #1d4ed8; }
</style>
