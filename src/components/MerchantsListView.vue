<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import http from '../services/http';

defineProps({ data: { type: Object, required: false, default: () => ({}) } });

// ── 列表状态 ──────────────────────────────────────────────────────
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

// ── 搜索条件 ──────────────────────────────────────────────────────
const searchKeyword = ref('');
const filterAgent = ref('');
const filterStatus = ref('');

const formatDateTime = (value) => {
  if (!value) return '—';
  const d = new Date(value);
  return isNaN(d.getTime()) ? value : d.toLocaleString('zh-CN', { hour12: false });
};

const fetchList = async () => {
  tableLoading.value = true;
  tableError.value = '';
  try {
    const params = { pageNum: tablePage.value, pageSize: tablePageSize.value };
    if (searchKeyword.value.trim()) params.keyword = searchKeyword.value.trim();
    if (filterAgent.value !== '') params.agentId = filterAgent.value;
    if (filterStatus.value !== '') params.status = filterStatus.value;
    const { data: resp } = await http.get('/admin/merchant/page', { params });
    const payload = resp?.data ?? resp;
    tableRows.value = payload?.records ?? [];
    tableTotal.value = payload?.total ?? 0;
    tablePage.value = payload?.current ?? tablePage.value;
  } catch (e) {
    tableError.value = '加载失败';
    tableRows.value = [];
    tableTotal.value = 0;
  } finally {
    tableLoading.value = false;
  }
};

const handleSearch = () => { tablePage.value = 1; fetchList(); };
const handleReset = () => {
  searchKeyword.value = '';
  filterAgent.value = '';
  filterStatus.value = '';
  tablePage.value = 1;
  fetchList();
};
const changePage = (next) => {
  if (next < 1 || next > totalPages.value || next === tablePage.value) return;
  tablePage.value = next;
  fetchList();
};
const changePageSize = (size) => { tablePageSize.value = size; tablePage.value = 1; fetchList(); };

// ── appKey 可见性 ─────────────────────────────────────────────────
const visibleKeyIds = ref(new Set());
const toggleAppKeyVisible = (platformId) => {
  if (visibleKeyIds.value.has(platformId)) visibleKeyIds.value.delete(platformId);
  else visibleKeyIds.value.add(platformId);
  visibleKeyIds.value = new Set(visibleKeyIds.value);
};
const maskKey = (key) => key ? '•'.repeat(20) : '—';

// ── 状态切换 ──────────────────────────────────────────────────────
const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1;
  try {
    await http.patch('/admin/merchant/toggle-status', null, { params: { platformId: row.platformId, status: newStatus } });
    row.status = newStatus;
  } catch (e) {
    console.error('toggle status failed', e);
  }
};

// ── 编辑/创建 Drawer ──────────────────────────────────────────────
const drawerVisible = ref(false);
const drawerMode = ref('create');
const drawerSaving = ref(false);
const drawerError = ref('');

const form = reactive({
  platformId: null,
  title: '',
  status: 1,
  agentId: '',
  email: '',
  remark: '',
  dailyPayOrderLimit: 5,
  dailyWithdrawCountLimit: 5,
  dailyWithdrawAmountLimit: 50000,
  dailyPayLimit: '',
  dailyPayoutLimit: '',
  largePayoutRiskEnabled: 0,
  largePayoutRiskAmount: 500000,
  telegramGroupId: '',
  settlementNotify: 0,
  account: '',
  password: '',
});

const resetForm = () => {
  form.platformId = null;
  form.title = '';
  form.status = 1;
  form.agentId = '';
  form.email = '';
  form.remark = '';
  form.dailyPayOrderLimit = 5;
  form.dailyWithdrawCountLimit = 5;
  form.dailyWithdrawAmountLimit = 50000;
  form.dailyPayLimit = '';
  form.dailyPayoutLimit = '';
  form.largePayoutRiskEnabled = 0;
  form.largePayoutRiskAmount = 500000;
  form.telegramGroupId = '';
  form.settlementNotify = 0;
  form.account = '';
  form.password = '';
};

// ── 创建成功凭据弹窗 ──────────────────────────────────────────────
const credsVisible = ref(false);
const credsData = ref({ account: '', password: '', googleSecret: '', otpAuthUrl: '' });

const openCreate = () => {
  drawerMode.value = 'create';
  resetForm();
  drawerError.value = '';
  drawerVisible.value = true;
};

const openEdit = async (row) => {
  drawerMode.value = 'edit';
  resetForm();
  drawerError.value = '';
  form.platformId = row.platformId;
  try {
    const { data: resp } = await http.get(`/admin/merchant/detail/${row.platformId}`);
    const d = resp?.data ?? resp;
    form.title = d.title ?? '';
    form.status = d.status ?? 1;
    form.agentId = d.agentId ?? '';
    form.email = d.email ?? '';
    form.remark = d.remark ?? '';
    form.dailyPayOrderLimit = d.dailyPayOrderLimit ?? 5;
    form.dailyWithdrawCountLimit = d.dailyWithdrawCountLimit ?? 5;
    form.dailyWithdrawAmountLimit = d.dailyWithdrawAmountLimit ?? 50000;
    form.dailyPayLimit = d.dailyPayLimit ?? '';
    form.dailyPayoutLimit = d.dailyPayoutLimit ?? '';
    form.largePayoutRiskEnabled = d.largePayoutRiskEnabled ?? 0;
    form.largePayoutRiskAmount = d.largePayoutRiskAmount ?? 500000;
    form.telegramGroupId = d.telegramGroupId ?? '';
    form.settlementNotify = d.settlementNotify ?? 0;
  } catch (e) {
    console.error('load detail failed', e);
  }
  drawerVisible.value = true;
};

const submitDrawer = async () => {
  if (!form.title.trim()) { drawerError.value = '名称不能为空'; return; }
  if (drawerMode.value === 'create') {
    if (!form.account.trim()) { drawerError.value = '登录账号不能为空'; return; }
    if (!form.password || form.password.length < 8) { drawerError.value = '密码至少 8 位'; return; }
  }
  drawerSaving.value = true;
  drawerError.value = '';
  try {
    const payload = {
      platformId: form.platformId,
      title: form.title,
      status: form.status,
      agentId: form.agentId !== '' ? Number(form.agentId) : null,
      email: form.email,
      remark: form.remark,
      dailyPayOrderLimit: form.dailyPayOrderLimit !== '' ? Number(form.dailyPayOrderLimit) : null,
      dailyWithdrawCountLimit: form.dailyWithdrawCountLimit !== '' ? Number(form.dailyWithdrawCountLimit) : null,
      dailyWithdrawAmountLimit: form.dailyWithdrawAmountLimit !== '' ? Number(form.dailyWithdrawAmountLimit) : null,
      dailyPayLimit: form.dailyPayLimit !== '' ? Number(form.dailyPayLimit) : null,
      dailyPayoutLimit: form.dailyPayoutLimit !== '' ? Number(form.dailyPayoutLimit) : null,
      largePayoutRiskEnabled: form.largePayoutRiskEnabled,
      largePayoutRiskAmount: form.largePayoutRiskAmount !== '' ? Number(form.largePayoutRiskAmount) : null,
      telegramGroupId: form.telegramGroupId,
      settlementNotify: form.settlementNotify,
    };
    if (drawerMode.value === 'create') {
      payload.account = form.account.trim();
      payload.password = form.password;
      const enteredPassword = form.password;
      const { data: resp } = await http.post('/admin/merchant/create', payload);
      const d = resp?.data ?? resp;
      drawerVisible.value = false;
      fetchList();
      credsData.value = {
        account: d?.account ?? payload.account,
        password: enteredPassword,
        googleSecret: d?.googleSecret ?? '',
        otpAuthUrl: d?.otpAuthUrl ?? '',
      };
      credsVisible.value = true;
    } else {
      await http.put('/admin/merchant/update', payload);
      drawerVisible.value = false;
      fetchList();
    }
  } catch (e) {
    drawerError.value = e?.response?.data?.msg || '保存失败，请重试';
  } finally {
    drawerSaving.value = false;
  }
};

// ── 删除确认 ──────────────────────────────────────────────────────
const confirmDeleteRow = ref(null);
const openDelete = (row) => { confirmDeleteRow.value = row; };
const doDelete = async () => {
  if (!confirmDeleteRow.value) return;
  try {
    await http.delete(`/admin/merchant/${confirmDeleteRow.value.platformId}`);
    tableRows.value = tableRows.value.filter(r => r.platformId !== confirmDeleteRow.value.platformId);
    tableTotal.value = Math.max(0, tableTotal.value - 1);
  } catch (e) {
    console.error('delete failed', e);
  } finally {
    confirmDeleteRow.value = null;
  }
};

// ── 重置 API Key 确认 ─────────────────────────────────────────────
const confirmResetRow = ref(null);
const openResetKey = (row) => { confirmResetRow.value = row; };
const doResetKey = async () => {
  if (!confirmResetRow.value) return;
  try {
    await http.post(`/admin/merchant/reset-key/${confirmResetRow.value.platformId}`);
  } catch (e) {
    console.error('reset key failed', e);
  } finally {
    confirmResetRow.value = null;
  }
};

// ── 余额面板 ──────────────────────────────────────────────────────
const balancePanelVisible = ref(false);
const balanceMerchant = ref(null);
const balanceList = ref([]);
const balanceActiveTab = ref('recharge');
const balanceLoading = ref(false);
const balanceOpSaving = ref(false);
const balanceOpError = ref('');

const balanceTabOptions = [
  { key: 'recharge', label: '充值' },
  { key: 'deduct', label: '扣减' },
  { key: 'freeze', label: '冻结' },
  { key: 'unfreeze', label: '解冻' },
  { key: 'withdraw', label: '提现' },
  { key: 'payout-test', label: '出款测试' },
];

const balanceForm = reactive({ amount: '', currency: '', remark: '' });

const openBalancePanel = async (row) => {
  balanceMerchant.value = row;
  balancePanelVisible.value = true;
  balanceActiveTab.value = 'recharge';
  balanceOpError.value = '';
  balanceForm.amount = '';
  balanceForm.currency = '';
  balanceForm.remark = '';
  await loadBalances(row.platformId);
};

const loadBalances = async (platformId) => {
  balanceLoading.value = true;
  try {
    const { data: resp } = await http.get(`/admin/merchant/balance/${platformId}`);
    balanceList.value = resp?.data ?? resp ?? [];
  } catch (e) {
    balanceList.value = [];
  } finally {
    balanceLoading.value = false;
  }
};

const submitBalanceOp = async () => {
  if (!balanceForm.amount || !balanceForm.currency) { balanceOpError.value = '金额和货币不能为空'; return; }
  balanceOpSaving.value = true;
  balanceOpError.value = '';
  try {
    const payload = {
      platformId: balanceMerchant.value.platformId,
      currency: balanceForm.currency,
      amount: Number(balanceForm.amount),
      remark: balanceForm.remark,
    };
    const tab = balanceActiveTab.value;
    if (tab === 'payout-test') {
      await http.post('/admin/merchant/balance/withdraw', payload);
    } else {
      await http.post(`/admin/merchant/balance/${tab}`, payload);
    }
    balanceForm.amount = '';
    balanceForm.remark = '';
    await loadBalances(balanceMerchant.value.platformId);
    // refresh balance summary in table row
    const tr = tableRows.value.find(r => r.platformId === balanceMerchant.value.platformId);
    if (tr) {
      const bals = balanceList.value;
      if (bals.length) {
        tr.balanceSummary = bals.map(b => `${b.currency}:${b.available}`).join(',');
      }
    }
  } catch (e) {
    balanceOpError.value = e?.response?.data?.msg || '操作失败';
  } finally {
    balanceOpSaving.value = false;
  }
};

// ── 收款测试 ──────────────────────────────────────────────────────
const payTestVisible = ref(false);
const payTestMerchant = ref(null);
const payTestForm = reactive({ amount: 100, currency: 'INR' });
const payTestSaving = ref(false);
const payTestError = ref('');

const openPayTest = (row) => {
  payTestMerchant.value = row;
  payTestForm.amount = 100;
  payTestForm.currency = 'INR';
  payTestError.value = '';
  payTestVisible.value = true;
};

const submitPayTest = async () => {
  payTestSaving.value = true;
  payTestError.value = '';
  try {
    await http.post('/admin/merchant/pay-test', {
      platformId: payTestMerchant.value.platformId,
      amount: Number(payTestForm.amount),
      currency: payTestForm.currency,
    });
    payTestVisible.value = false;
  } catch (e) {
    payTestError.value = e?.response?.data?.msg || '测试失败';
  } finally {
    payTestSaving.value = false;
  }
};

onMounted(fetchList);
</script>

<template>
  <div class="merchants-list-view">

    <!-- ── 工具栏 ─────────────────────────────────────────────── -->
    <section class="panel list-panel">
      <div class="toolbar">
        <div class="search-bar">
          <input v-model="searchKeyword" class="search-input" placeholder="输入名称" @keyup.enter="handleSearch" />
          <select v-model="filterAgent" class="search-select">
            <option value="">代理商</option>
          </select>
          <select v-model="filterStatus" class="search-select">
            <option value="">状态</option>
            <option value="1">启用</option>
            <option value="0">禁用</option>
          </select>
          <button type="button" class="btn btn-primary" @click="handleSearch">搜索</button>
          <button type="button" class="btn btn-ghost" @click="handleReset">重置</button>
        </div>
        <button type="button" class="btn btn-success" @click="openCreate">新建</button>
      </div>

      <!-- ── 错误提示 ───────────────────────────────────────── -->
      <p v-if="tableError" class="table-error">{{ tableError }}</p>

      <!-- ── 表格 ──────────────────────────────────────────── -->
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>名称</th>
              <th>代理商</th>
              <th>keyId</th>
              <th>appKey</th>
              <th>状态</th>
              <th>余额</th>
              <th>备注</th>
              <th>创建时间</th>
              <th>ACTION</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="tableLoading">
              <td colspan="9" class="loading-cell">加载中…</td>
            </tr>
            <tr v-else-if="!tableRows.length">
              <td colspan="9" class="empty-cell">暂无数据</td>
            </tr>
            <tr v-for="row in tableRows" :key="row.platformId">
              <td class="td-name">{{ row.title }}</td>
              <td>{{ row.agentId ?? '—' }}</td>
              <td class="td-mono">{{ row.keyId }}</td>
              <td class="td-appkey">
                <span class="key-text">{{ visibleKeyIds.has(row.platformId) ? row.appKey : maskKey(row.appKey) }}</span>
                <button type="button" class="icon-btn" @click="toggleAppKeyVisible(row.platformId)" title="显示/隐藏">
                  <span>{{ visibleKeyIds.has(row.platformId) ? '🙈' : '👁' }}</span>
                </button>
              </td>
              <td>
                <button
                  type="button"
                  class="toggle-btn"
                  :class="row.status === 1 ? 'toggle-on' : 'toggle-off'"
                  @click="toggleStatus(row)"
                >{{ row.status === 1 ? 'ON' : 'OFF' }}</button>
              </td>
              <td>
                <button type="button" class="balance-link" @click="openBalancePanel(row)">
                  {{ row.balanceSummary || '—' }}
                </button>
              </td>
              <td class="td-remark">{{ row.remark || '—' }}</td>
              <td class="td-time">{{ row.createdAt || '—' }}</td>
              <td class="td-actions">
                <button type="button" class="act-btn act-user" title="用户" @click="() => {}">用户</button>
                <button type="button" class="act-btn act-edit" title="编辑" @click="openEdit(row)">✏</button>
                <button type="button" class="act-btn act-link" title="收款测试" @click="openPayTest(row)">🔗</button>
                <button type="button" class="act-btn act-del" title="删除" @click="openDelete(row)">🗑</button>
                <button type="button" class="act-btn act-reset" title="重置API Key" @click="openResetKey(row)">🔄</button>
                <button type="button" class="act-btn act-bell" title="通知" @click="() => {}">🔔</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- ── 分页 ──────────────────────────────────────────── -->
      <div class="pagination">
        <span class="total-text">共 {{ tableTotal }} 条</span>
        <div class="page-btns">
          <button type="button" class="page-btn" :disabled="tablePage <= 1" @click="changePage(tablePage - 1)">‹</button>
          <template v-for="p in totalPages" :key="p">
            <button
              v-if="p === 1 || p === totalPages || Math.abs(p - tablePage) <= 2"
              type="button"
              class="page-btn"
              :class="{ active: p === tablePage }"
              @click="changePage(p)"
            >{{ p }}</button>
            <span v-else-if="p === 2 && tablePage > 4" class="page-ellipsis">…</span>
            <span v-else-if="p === totalPages - 1 && tablePage < totalPages - 3" class="page-ellipsis">…</span>
          </template>
          <button type="button" class="page-btn" :disabled="tablePage >= totalPages" @click="changePage(tablePage + 1)">›</button>
        </div>
        <select class="page-size-select" :value="tablePageSize" @change="changePageSize(Number($event.target.value))">
          <option v-for="s in paginationSizes" :key="s" :value="s">{{ s }} 条/页</option>
        </select>
      </div>
    </section>

    <!-- ── 编辑/创建弹窗 ─────────────────────────────────────── -->
    <div v-if="drawerVisible" class="modal-overlay" @click.self="drawerVisible = false">
      <div class="modal-box">
        <div class="modal-header">
          <span>{{ drawerMode === 'create' ? '新建商户' : '编辑' }}</span>
          <button type="button" class="close-btn" @click="drawerVisible = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <label class="form-label required">名称</label>
            <input v-model="form.title" class="form-input" placeholder="商户名称" />
          </div>
          <template v-if="drawerMode === 'create'">
            <div class="form-row">
              <label class="form-label required">登录账号</label>
              <input v-model="form.account" class="form-input" placeholder="如 merchant_indus" autocomplete="off" />
            </div>
            <div class="form-row">
              <label class="form-label required">登录密码（至少 8 位）</label>
              <input v-model="form.password" class="form-input" type="password" placeholder="初始密码" autocomplete="new-password" />
            </div>
          </template>
          <div class="form-row">
            <label class="form-label">备注</label>
            <input v-model="form.remark" class="form-input" placeholder="备注" />
          </div>
          <div class="form-row">
            <label class="form-label">代理商</label>
            <input v-model="form.agentId" class="form-input" placeholder="代理商 ID" type="number" />
          </div>
          <div class="form-row">
            <label class="form-label">邮箱</label>
            <input v-model="form.email" class="form-input" placeholder="邮箱" type="email" />
          </div>
          <div class="form-row-2col">
            <div class="form-col">
              <label class="form-label required">用户每日收款订单数限制</label>
              <input v-model="form.dailyPayOrderLimit" class="form-input" type="number" min="0" />
            </div>
            <div class="form-col">
              <label class="form-label required">用户每日提现次数限制</label>
              <input v-model="form.dailyWithdrawCountLimit" class="form-input" type="number" min="0" />
            </div>
          </div>
          <div class="form-row-2col">
            <div class="form-col">
              <label class="form-label required">用户每日提款金额限制</label>
              <input v-model="form.dailyWithdrawAmountLimit" class="form-input" type="number" min="0" />
            </div>
            <div class="form-col">
              <label class="form-label">大额单笔出款风控金额</label>
              <input v-model="form.largePayoutRiskAmount" class="form-input" type="number" min="0" />
            </div>
          </div>
          <div class="form-row-2col">
            <div class="form-col">
              <label class="form-label">大额单笔出款风控开关</label>
              <button
                type="button"
                class="toggle-btn"
                :class="form.largePayoutRiskEnabled ? 'toggle-on' : 'toggle-off'"
                @click="form.largePayoutRiskEnabled = form.largePayoutRiskEnabled ? 0 : 1"
              >{{ form.largePayoutRiskEnabled ? 'ON' : 'OFF' }}</button>
            </div>
            <div class="form-col">
              <label class="form-label">结算通知</label>
              <button
                type="button"
                class="toggle-btn"
                :class="form.settlementNotify ? 'toggle-on' : 'toggle-off'"
                @click="form.settlementNotify = form.settlementNotify ? 0 : 1"
              >{{ form.settlementNotify ? 'ON' : 'OFF' }}</button>
            </div>
          </div>
          <div class="form-row">
            <label class="form-label">Telegram群ID</label>
            <input v-model="form.telegramGroupId" class="form-input" placeholder="Telegram 群 ID" />
          </div>
          <p v-if="drawerError" class="form-error">{{ drawerError }}</p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-ghost" @click="drawerVisible = false">取消</button>
          <button type="button" class="btn btn-primary" :disabled="drawerSaving" @click="submitDrawer">
            {{ drawerSaving ? '保存中…' : 'Submit' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ── 创建成功凭据弹窗 ─────────────────────────────────────── -->
    <div v-if="credsVisible" class="modal-overlay">
      <div class="modal-box creds-box">
        <div class="modal-header">
          <span>商户账号凭据（请妥善保存）</span>
          <button type="button" class="close-btn" @click="credsVisible = false">✕</button>
        </div>
        <div class="modal-body creds-body">
          <div class="creds-row">
            <span class="creds-label">登录账号</span>
            <span class="creds-val">{{ credsData.account }}</span>
          </div>
          <div class="creds-row">
            <span class="creds-label">登录密码</span>
            <span class="creds-val creds-mono">{{ credsData.password }}</span>
          </div>
          <div class="creds-row">
            <span class="creds-label">Google 密钥</span>
            <span class="creds-val creds-mono">{{ credsData.googleSecret }}</span>
          </div>
          <div v-if="credsData.otpAuthUrl" class="creds-row creds-row-col">
            <span class="creds-label">OTP Auth URL</span>
            <span class="creds-val creds-mono creds-url">{{ credsData.otpAuthUrl }}</span>
          </div>
          <p class="creds-warn">此页面关闭后密码将无法再次查看，请立即记录。</p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-primary" @click="credsVisible = false">已记录，关闭</button>
        </div>
      </div>
    </div>

    <!-- ── 删除确认 ───────────────────────────────────────────── -->
    <div v-if="confirmDeleteRow" class="modal-overlay" @click.self="confirmDeleteRow = null">
      <div class="modal-confirm">
        <div class="confirm-header">
          CONFIRM
          <button type="button" class="close-btn" @click="confirmDeleteRow = null">✕</button>
        </div>
        <p class="confirm-text">确认要删除 {{ confirmDeleteRow?.title }}？</p>
        <div class="confirm-footer">
          <button type="button" class="btn btn-ghost" @click="confirmDeleteRow = null">NO</button>
          <button type="button" class="btn btn-danger" @click="doDelete">YES</button>
        </div>
      </div>
    </div>

    <!-- ── 重置 API Key 确认 ──────────────────────────────────── -->
    <div v-if="confirmResetRow" class="modal-overlay" @click.self="confirmResetRow = null">
      <div class="modal-confirm">
        <div class="confirm-header">
          CONFIRM
          <button type="button" class="close-btn" @click="confirmResetRow = null">✕</button>
        </div>
        <p class="confirm-text">确认要重置 {{ confirmResetRow?.title }} 的api key？</p>
        <div class="confirm-footer">
          <button type="button" class="btn btn-ghost" @click="confirmResetRow = null">NO</button>
          <button type="button" class="btn btn-primary" @click="doResetKey">YES</button>
        </div>
      </div>
    </div>

    <!-- ── 收款测试 ───────────────────────────────────────────── -->
    <div v-if="payTestVisible" class="modal-overlay" @click.self="payTestVisible = false">
      <div class="modal-small">
        <div class="modal-header">
          <span>收款测试</span>
          <button type="button" class="close-btn" @click="payTestVisible = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <label class="form-label required">金额</label>
            <input v-model="payTestForm.amount" class="form-input" type="number" min="1" />
          </div>
          <div class="form-row">
            <label class="form-label required">货币</label>
            <select v-model="payTestForm.currency" class="form-input">
              <option>INR</option><option>RUB</option><option>BRL</option><option>USD</option>
            </select>
          </div>
          <p v-if="payTestError" class="form-error">{{ payTestError }}</p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-primary" :disabled="payTestSaving" @click="submitPayTest">
            {{ payTestSaving ? '提交中…' : '提交' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ── 余额面板（右侧抽屉） ──────────────────────────────── -->
    <div v-if="balancePanelVisible" class="balance-overlay" @click.self="balancePanelVisible = false">
      <div class="balance-drawer">
        <div class="drawer-header">
          <span>{{ balanceMerchant?.title }}</span>
          <button type="button" class="close-btn" @click="balancePanelVisible = false">✕</button>
        </div>

        <div class="balance-section-label">余额</div>
        <div v-if="balanceLoading" class="balance-loading">加载中…</div>
        <div v-else class="balance-cards">
          <div v-if="!balanceList.length" class="balance-empty">暂无余额记录</div>
          <div v-for="b in balanceList" :key="b.currency" class="balance-card">
            <div class="balance-currency">{{ b.currency }}</div>
            <div class="balance-row">
              <div class="balance-col">
                <span class="balance-col-label">可用</span>
                <span class="balance-col-val">{{ b.available }}</span>
              </div>
              <div class="balance-col">
                <span class="balance-col-label">冻结</span>
                <span class="balance-col-val">{{ b.frozen }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="balance-tabs">
          <button
            v-for="tab in balanceTabOptions"
            :key="tab.key"
            type="button"
            class="tab-btn"
            :class="{ active: balanceActiveTab === tab.key }"
            @click="balanceActiveTab = tab.key; balanceOpError = ''"
          >{{ tab.label }}</button>
        </div>

        <div class="balance-op-form">
          <div class="form-row">
            <label class="form-label required">金额</label>
            <input v-model="balanceForm.amount" class="form-input" type="number" min="0" placeholder="100" />
          </div>
          <div class="form-row">
            <label class="form-label required">货币</label>
            <select v-model="balanceForm.currency" class="form-input">
              <option value="" disabled>请选择货币</option>
              <option v-for="b in balanceList" :key="b.currency" :value="b.currency">{{ b.currency }}</option>
              <option>INR</option><option>RUB</option><option>BRL</option><option>USD</option>
            </select>
          </div>
          <div class="form-row">
            <label class="form-label">备注</label>
            <input v-model="balanceForm.remark" class="form-input" placeholder="备注（可选）" />
          </div>
          <p v-if="balanceOpError" class="form-error">{{ balanceOpError }}</p>
          <button type="button" class="btn btn-primary btn-full" :disabled="balanceOpSaving" @click="submitBalanceOp">
            {{ balanceOpSaving ? '提交中…' : '提交' }}
          </button>
        </div>

        <div class="drawer-footer">
          <button type="button" class="btn btn-ghost" @click="balancePanelVisible = false">Cancel</button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.merchants-list-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel {
  background: rgba(18, 19, 37, 0.82);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 16px;
  padding: 20px;
}

/* ── Toolbar ── */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.search-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}
.search-input {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 6px;
  color: #e8eaf6;
  padding: 6px 12px;
  font-size: 13px;
  width: 180px;
}
.search-input::placeholder { color: rgba(255,255,255,0.35); }
.search-select {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 6px;
  color: #e8eaf6;
  padding: 6px 12px;
  font-size: 13px;
}
.search-select option { background: #1a1b2e; }

/* ── Buttons ── */
.btn { border: none; border-radius: 6px; cursor: pointer; font-size: 13px; padding: 7px 16px; transition: opacity .15s; }
.btn:disabled { opacity: .5; cursor: not-allowed; }
.btn-primary { background: #4a5fff; color: #fff; }
.btn-primary:hover:not(:disabled) { opacity: .85; }
.btn-ghost { background: rgba(255,255,255,0.08); color: #c5c7d8; }
.btn-ghost:hover:not(:disabled) { background: rgba(255,255,255,0.14); }
.btn-success { background: #27ae60; color: #fff; }
.btn-success:hover:not(:disabled) { opacity: .85; }
.btn-danger { background: #e74c3c; color: #fff; }
.btn-danger:hover:not(:disabled) { opacity: .85; }
.btn-full { width: 100%; margin-top: 8px; }

/* ── Table ── */
.table-wrap { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; font-size: 13px; }
thead tr { border-bottom: 1px solid rgba(255,255,255,0.08); }
th { text-align: left; padding: 10px 8px; color: rgba(255,255,255,0.55); font-weight: 500; white-space: nowrap; }
td { padding: 11px 8px; border-bottom: 1px solid rgba(255,255,255,0.04); color: #dde0f5; vertical-align: middle; }
tr:hover td { background: rgba(255,255,255,0.02); }
.td-name { font-weight: 500; white-space: nowrap; }
.td-mono { font-family: monospace; font-size: 12px; }
.td-appkey { display: flex; align-items: center; gap: 6px; min-width: 180px; }
.key-text { font-family: monospace; font-size: 12px; color: rgba(255,255,255,0.7); }
.td-remark { max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.td-time { white-space: nowrap; font-size: 12px; color: rgba(255,255,255,0.6); }
.loading-cell, .empty-cell { text-align: center; color: rgba(255,255,255,0.4); padding: 32px 0; }
.table-error { color: #e74c3c; font-size: 13px; margin-bottom: 8px; }

/* ── Toggle ── */
.toggle-btn { border: none; border-radius: 12px; cursor: pointer; font-size: 11px; font-weight: 600; padding: 4px 12px; min-width: 48px; }
.toggle-on { background: #27ae60; color: #fff; }
.toggle-off { background: rgba(255,255,255,0.12); color: rgba(255,255,255,0.5); }

/* ── Balance link ── */
.balance-link { background: none; border: none; cursor: pointer; color: #7b8af5; font-size: 12px; padding: 0; text-decoration: underline; }

/* ── Icon / Action buttons ── */
.icon-btn { background: none; border: none; cursor: pointer; padding: 2px; font-size: 14px; }
.td-actions { display: flex; gap: 4px; align-items: center; white-space: nowrap; }
.act-btn { background: none; border: 1px solid rgba(255,255,255,0.15); border-radius: 4px; cursor: pointer; font-size: 12px; padding: 3px 7px; color: #c5c7d8; }
.act-btn:hover { background: rgba(255,255,255,0.08); }
.act-del { color: #e74c3c; border-color: rgba(231,76,60,0.3); }
.act-del:hover { background: rgba(231,76,60,0.1); }

/* ── Pagination ── */
.pagination { display: flex; align-items: center; gap: 12px; margin-top: 16px; flex-wrap: wrap; }
.total-text { color: rgba(255,255,255,0.5); font-size: 13px; }
.page-btns { display: flex; gap: 4px; }
.page-btn { background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); border-radius: 4px; color: #c5c7d8; cursor: pointer; font-size: 13px; min-width: 30px; padding: 4px 8px; }
.page-btn:disabled { opacity: .4; cursor: not-allowed; }
.page-btn.active { background: #4a5fff; border-color: #4a5fff; color: #fff; }
.page-ellipsis { color: rgba(255,255,255,0.4); padding: 0 4px; }
.page-size-select { background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.12); border-radius: 6px; color: #e8eaf6; font-size: 13px; padding: 4px 8px; }
.page-size-select option { background: #1a1b2e; }

/* ── Modal overlay ── */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.65);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.modal-box {
  background: #1a1b2e; border: 1px solid rgba(255,255,255,0.1); border-radius: 12px;
  width: 580px; max-width: 95vw; max-height: 90vh; overflow-y: auto;
}
.modal-small {
  background: #1a1b2e; border: 1px solid rgba(255,255,255,0.1); border-radius: 12px;
  width: 360px; max-width: 95vw;
}
.modal-confirm {
  background: #1a1b2e; border: 1px solid rgba(255,255,255,0.1); border-radius: 12px;
  width: 460px; max-width: 95vw; padding: 24px;
}
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid rgba(255,255,255,0.07);
  font-size: 15px; font-weight: 600; color: #e8eaf6;
}
.modal-body { padding: 20px; display: flex; flex-direction: column; gap: 16px; }
.modal-footer {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 16px 20px; border-top: 1px solid rgba(255,255,255,0.07);
}

.confirm-header {
  font-size: 16px; font-weight: 700; color: #e8eaf6;
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; background: #4a5fff; padding: 12px 16px; border-radius: 8px 8px 0 0;
  margin: -24px -24px 16px;
}
.confirm-text { color: #dde0f5; font-size: 14px; margin: 8px 0 20px; }
.confirm-footer { display: flex; gap: 10px; justify-content: flex-end; }
.close-btn { background: none; border: none; cursor: pointer; color: rgba(255,255,255,0.6); font-size: 16px; }
.close-btn:hover { color: #fff; }

/* ── Form ── */
.form-row { display: flex; flex-direction: column; gap: 6px; }
.form-row-2col { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-col { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 12px; color: rgba(255,255,255,0.6); }
.form-label.required::before { content: '* '; color: #e74c3c; }
.form-input {
  background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.12);
  border-radius: 6px; color: #e8eaf6; font-size: 13px; padding: 8px 12px; width: 100%;
  box-sizing: border-box;
}
.form-input:focus { border-color: #4a5fff; outline: none; }
.form-input option { background: #1a1b2e; }
.form-error { color: #e74c3c; font-size: 12px; margin: 0; }

/* ── Balance Drawer ── */
.balance-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.55); z-index: 1000;
  display: flex; justify-content: flex-end;
}
.balance-drawer {
  background: #1a1b2e; border-left: 1px solid rgba(255,255,255,0.08);
  width: 420px; max-width: 95vw; height: 100%; overflow-y: auto;
  display: flex; flex-direction: column; padding: 20px; gap: 16px;
}
.drawer-header {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 16px; font-weight: 600; color: #e8eaf6;
}
.balance-section-label { font-size: 13px; color: rgba(255,255,255,0.55); text-transform: uppercase; letter-spacing: .05em; }
.balance-loading, .balance-empty { color: rgba(255,255,255,0.4); font-size: 13px; }
.balance-cards { display: flex; flex-direction: column; gap: 10px; }
.balance-card {
  background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08);
  border-radius: 10px; padding: 14px 16px;
}
.balance-currency { font-size: 14px; font-weight: 600; color: #e8eaf6; margin-bottom: 10px; }
.balance-row { display: flex; gap: 24px; }
.balance-col { display: flex; flex-direction: column; gap: 4px; }
.balance-col-label { font-size: 11px; color: rgba(255,255,255,0.5); }
.balance-col-val { font-size: 24px; font-weight: 700; color: #e8eaf6; }
.balance-tabs { display: flex; gap: 6px; flex-wrap: wrap; }
.tab-btn {
  background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1);
  border-radius: 6px; color: rgba(255,255,255,0.6); cursor: pointer; font-size: 13px; padding: 6px 14px;
}
.tab-btn.active { background: #4a5fff; border-color: #4a5fff; color: #fff; }
.balance-op-form { display: flex; flex-direction: column; gap: 14px; }
.drawer-footer { display: flex; justify-content: flex-end; margin-top: auto; padding-top: 12px; }

/* ── Credentials modal ── */
.creds-box { width: 520px; }
.creds-body { gap: 12px; }
.creds-row {
  display: flex; align-items: flex-start; gap: 12px;
  background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08);
  border-radius: 8px; padding: 12px 14px;
}
.creds-row-col { flex-direction: column; gap: 6px; }
.creds-label { font-size: 12px; color: rgba(255,255,255,0.5); white-space: nowrap; min-width: 80px; padding-top: 1px; }
.creds-val { font-size: 14px; color: #e8eaf6; word-break: break-all; }
.creds-mono { font-family: monospace; letter-spacing: .04em; }
.creds-url { font-size: 12px; color: #7b8af5; }
.creds-warn {
  background: rgba(231,76,60,0.12); border: 1px solid rgba(231,76,60,0.3);
  border-radius: 8px; color: #e74c3c; font-size: 13px; margin: 4px 0 0; padding: 10px 14px;
}
</style>
