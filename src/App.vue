<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import HeaderBar from './components/HeaderBar.vue';
import SidebarMenu from './components/SidebarMenu.vue';
import DashboardOverview from './components/DashboardOverview.vue';
import DashboardChannelView from './components/DashboardChannelView.vue';
import DashboardMerchantView from './components/DashboardMerchantView.vue';
import DashboardFundsView from './components/DashboardFundsView.vue';
import DashboardBankTradeView from './components/DashboardBankTradeView.vue';
import DashboardBankIdView from './components/DashboardBankIdView.vue';
import DashboardBankMonitorView from './components/DashboardBankMonitorView.vue';
import DashboardAgentLogView from './components/DashboardAgentLogView.vue';
import BankSuppliersView from './components/BankSuppliersView.vue';
import BankAccountsView from './components/BankAccountsView.vue';
import BankChannelSettingsView from './components/BankChannelSettingsView.vue';
import BankRealtimeBoardView from './components/BankRealtimeBoardView.vue';
import BankLedgerView from './components/BankLedgerView.vue';
import BankBookkeepingView from './components/BankBookkeepingView.vue';
import BankMappingView from './components/BankMappingView.vue';
import OrdersCollectionView from './components/OrdersCollectionView.vue';
import OrdersPayoutView from './components/OrdersPayoutView.vue';
import OrdersRollbackView from './components/OrdersRollbackView.vue';
import OrdersPayoutQueueView from './components/OrdersPayoutQueueView.vue';
import OrdersBatchView from './components/OrdersBatchView.vue';
import OrdersApprovalView from './components/OrdersApprovalView.vue';
import OrdersQueryView from './components/OrdersQueryView.vue';
import MerchantsListView from './components/MerchantsListView.vue';
import MerchantsBoardView from './components/MerchantsBoardView.vue';
import AgentsChannelsView from './components/AgentsChannelsView.vue';
import AgentsListView from './components/AgentsListView.vue';
import OpsSmsView from './components/OpsSmsView.vue';
import OpsAppForwarderView from './components/OpsAppForwarderView.vue';
import OpsBatchView from './components/OpsBatchView.vue';
import OpsVpsManageView from './components/OpsVpsManageView.vue';
import OpsVpsView from './components/OpsVpsView.vue';
import OpsTelegramView from './components/OpsTelegramView.vue';
import FinanceBillingView from './components/FinanceBillingView.vue';
import FinanceWithdrawView from './components/FinanceWithdrawView.vue';
import FinanceAgentBillingView from './components/FinanceAgentBillingView.vue';
import FinanceSystemTopupView from './components/FinanceSystemTopupView.vue';
import FinanceAgentWithdrawView from './components/FinanceAgentWithdrawView.vue';
import DownloadsHistoryView from './components/DownloadsHistoryView.vue';
import SystemBillingView from './components/SystemBillingView.vue';
import SystemUsersView from './components/SystemUsersView.vue';
import SystemSettingsView from './components/SystemSettingsView.vue';
import PaymentsEntityView from './components/PaymentsEntityView.vue';
import PaymentsChannelView from './components/PaymentsChannelView.vue';
import PaymentsErrorView from './components/PaymentsErrorView.vue';
import PaymentsRollbackView from './components/PaymentsRollbackView.vue';
import PaymentsReconcileView from './components/PaymentsReconcileView.vue';
import PaymentsThrottleView from './components/PaymentsThrottleView.vue';
import PaymentsFallbackView from './components/PaymentsFallbackView.vue';
import PaymentsIfscBlacklistView from './components/PaymentsIfscBlacklistView.vue';
import PaymentsBlacklistView from './components/PaymentsBlacklistView.vue';
import PaymentsProfitView from './components/PaymentsProfitView.vue';
import MerchantDashboardView from './components/MerchantDashboardView.vue';
import MerchantOrderPayinView from './components/MerchantOrderPayinView.vue';
import MerchantWithdrawApplyView from './components/MerchantWithdrawApplyView.vue';
import MerchantApiDocsView from './components/MerchantApiDocsView.vue';
import ChannelCards from './components/ChannelCards.vue';
import LoginPanel from './components/LoginPanel.vue';
import http, { setAuthToken, getStoredToken } from './services/http';
import {
  hero as heroMock,
  user,
  alerts,
  menuItems,
  timezones,
  languages,
  dashboardOverview as dashboardOverviewMock,
  dashboardChannelView as dashboardChannelData,
  dashboardMerchantView as dashboardMerchantData,
  dashboardFundsView as dashboardFundsData,
  dashboardBankTradeView as dashboardBankTradeData,
  dashboardBankIdView as dashboardBankIdData,
  dashboardBankMonitorView as dashboardBankMonitorData,
  dashboardAgentLogView as dashboardAgentLogData,
  bankSuppliersView as bankSuppliersData,
  bankAccountsView as bankAccountsData,
  bankChannelSettingsView as bankChannelSettingsData,
  bankRealtimeBoardView as bankRealtimeBoardData,
  bankLedgerView as bankLedgerData,
  bankBookkeepingView as bankBookkeepingData,
  bankMappingView as bankMappingData,
  ordersCollectionView as ordersCollectionData,
  ordersPayoutView as ordersPayoutData,
  ordersRollbackView as ordersRollbackData,
  ordersPayoutQueueView as ordersPayoutQueueData,
  ordersBatchView as ordersBatchData,
  ordersApprovalView as ordersApprovalData,
  ordersQueryView as ordersQueryData,
  merchantsListView as merchantsListData,
  merchantsBoardView as merchantsBoardData,
  agentsChannelsView as agentsChannelsData,
  agentsListView as agentsListData,
  opsSmsView as opsSmsData,
  opsAppForwarderView as opsAppForwarderData,
  opsBatchView as opsBatchData,
  opsVpsManageView as opsVpsManageData,
  opsVpsView as opsVpsData,
  opsTelegramView as opsTelegramData,
  financeBillingView as financeBillingData,
  financeWithdrawView as financeWithdrawData,
  financeAgentBillingView as financeAgentBillingData,
  financeSystemTopupView as financeSystemTopupData,
  financeAgentWithdrawView as financeAgentWithdrawData,
  systemBillingView as systemBillingData,
  systemUsersView as systemUsersData,
  systemSettingsView as systemSettingsData,
  downloadsHistoryView as downloadsHistoryData,
  paymentsEntityView as paymentsEntityData,
  paymentsChannelView as paymentsChannelData,
  paymentsErrorView as paymentsErrorData,
  paymentsRollbackView as paymentsRollbackData,
  paymentsReconcileView as paymentsReconcileData,
  paymentsThrottleView as paymentsThrottleData,
  paymentsFallbackView as paymentsFallbackData,
  paymentsIfscBlacklistView as paymentsIfscBlacklistData,
  paymentsBlacklistView as paymentsBlacklistData,
  paymentsProfitView as paymentsProfitData
} from './data/mock';

const cloneDeep = (payload) => JSON.parse(JSON.stringify(payload ?? {}));

const isAuthenticated = ref(false);
const authProfile = ref(null);
const currentUser = computed(() => authProfile.value ?? user);
const dashboardOverviewData = ref(cloneDeep(dashboardOverviewMock));

const timezone = ref(timezones[2] ?? 'UTC+00:00');
const language = ref(languages[0] ?? '中文');

const activeParent = ref('');
const activeChild = ref('');

// ── Tab persistence ──────────────────────────────────────────────────
const TABS_KEY = 'payadmin_tabs_v1';
const ACTIVE_TAB_KEY = 'payadmin_active_tab_v1';
const tabs = ref([]);
const activeTabId = ref('');

// Build label lookup from static menu definition
const menuLabelMap = (() => {
  const m = new Map();
  menuItems.forEach((p) => {
    if (!p.children.length) m.set(p.id, p.label);
    p.children.forEach((c) => m.set(c.id, c.label));
  });
  return m;
})();

const saveTabs = () => {
  localStorage.setItem(TABS_KEY, JSON.stringify(tabs.value));
  localStorage.setItem(ACTIVE_TAB_KEY, activeTabId.value);
};

const restoreTabs = () => {
  try {
    const savedTabs = JSON.parse(localStorage.getItem(TABS_KEY) || '[]');
    const savedActive = localStorage.getItem(ACTIVE_TAB_KEY) || '';
    if (savedTabs.length) {
      tabs.value = savedTabs;
      const active = savedTabs.find((t) => t.id === savedActive) || savedTabs[0];
      activeTabId.value = active.id;
      activeParent.value = active.parentId;
      activeChild.value = active.childId;
    }
  } catch { /* ignore */ }
};

const openOrActivateTab = (parentId, childId) => {
  const id = childId || parentId;
  const existing = tabs.value.find((t) => t.id === id);
  if (existing) {
    activeTabId.value = id;
  } else {
    const label = menuLabelMap.get(id) || id;
    tabs.value.push({ id, parentId, childId: childId || '', label });
    activeTabId.value = id;
  }
  activeParent.value = parentId;
  activeChild.value = childId || '';
  saveTabs();
};

const closeTab = (tabId, event) => {
  event?.stopPropagation();
  const idx = tabs.value.findIndex((t) => t.id === tabId);
  if (idx === -1) return;
  tabs.value.splice(idx, 1);
  if (activeTabId.value === tabId) {
    const next = tabs.value[idx] ?? tabs.value[idx - 1];
    if (next) {
      activeTabId.value = next.id;
      activeParent.value = next.parentId;
      activeChild.value = next.childId;
    } else {
      activeTabId.value = '';
      activeParent.value = '';
      activeChild.value = '';
    }
  }
  saveTabs();
};

const switchTab = (tab) => {
  if (activeTabId.value === tab.id) return;
  activeTabId.value = tab.id;
  activeParent.value = tab.parentId;
  activeChild.value = tab.childId;
  saveTabs();
};

// ── 动态菜单权限 ──────────────────────────────────────────────────
// null = 未加载，fallback 到 mock 全量；[] = 已加载但无权限
const apiMenuTree = ref(null);

const filteredMenuItems = computed(() => {
  if (!apiMenuTree.value) return menuItems;

  const parentIndex = new Map(apiMenuTree.value.map((n) => [n.routePath, n]));

  return menuItems
    .filter((item) => parentIndex.has('/' + item.id))
    .map((item) => {
      const apiNode = parentIndex.get('/' + item.id);
      const dbChildren = apiNode.children;

      // 叶子节点：DB 中该父菜单无子项（商户仪表板等独立页）
      if (dbChildren.length === 0) return { ...item, children: [] };

      // 优先：DB routePath 与 mock id 精确匹配（admin 角色走此路径）
      const allowedPaths = new Set(dbChildren.map((c) => c.routePath));
      const byPath = item.children.filter((c) => allowedPaths.has('/' + c.id));
      if (byPath.length > 0) return { ...item, children: byPath };

      // 兜底：DB routePath 与 mock id 不匹配时（商户角色，DB 用 /orders/payin 等路径）
      // 改用 DB 子菜单 name 与 mock 子菜单 label 匹配
      const dbNames = new Set(dbChildren.map((c) => c.name).filter(Boolean));
      return { ...item, children: item.children.filter((c) => dbNames.has(c.label)) };
    });
});

const fetchVisibleMenus = async () => {
  try {
    const { data: resp } = await http.get('/admin/system/menu/visible');
    apiMenuTree.value = resp?.data ?? resp ?? [];
  } catch {
    apiMenuTree.value = null;
  }
};

// 权限菜单加载后，若当前激活页签的父菜单已无权限，切到第一个合法页签
watch(filteredMenuItems, (items) => {
  if (!items.length || !activeParent.value) return;
  if (items.find((i) => i.id === activeParent.value)) return;
  const validTab = tabs.value.find((t) => items.find((i) => i.id === t.parentId));
  if (validTab) {
    activeTabId.value = validTab.id;
    activeParent.value = validTab.parentId;
    activeChild.value = validTab.childId;
  } else {
    activeParent.value = items[0].id;
    activeChild.value = items[0].children?.[0]?.id ?? '';
  }
});

const heroData = ref(heroMock);
const homeMetrics = ref({
  operatingCountries: [],
  activeChannelCount: 0,
  minuteLevelSla: '99.95%'
});
const homeMetricsError = ref('');
const homeMetricsLoading = ref(false);
const dashboardSummaryError = ref('');
const dashboardSummaryLoading = ref(false);

const SUMMARY_POLL_INTERVAL = 5 * 60 * 1000;
let summaryPollTimer = null;

const findQuickStat = (id) => dashboardOverviewData.value?.quickStats?.find((stat) => stat.id === id);

const formatCurrency = (value) => {
  if (value === null || value === undefined) {
    return '--';
  }
  const num = Number(value);
  if (Number.isNaN(num)) {
    return '--';
  }
  return `¥${num.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`;
};

const formatCount = (value) => {
  if (value === null || value === undefined) {
    return '--';
  }
  const num = Number(value);
  if (Number.isNaN(num)) {
    return '--';
  }
  return num.toLocaleString('zh-CN');
};

const formatPercentChange = (value) => {
  if (value === null || value === undefined) {
    return '—';
  }
  const num = Number(value);
  if (Number.isNaN(num)) {
    return '—';
  }
  const prefix = num > 0 ? '+' : '';
  return `${prefix}${num.toFixed(2)}%`;
};

const formatRateValue = (value) => {
  if (value === null || value === undefined) {
    return '--';
  }
  const num = Number(value);
  if (Number.isNaN(num)) {
    return '--';
  }
  return `${num.toFixed(2)}%`;
};

const updateHeroSyncTime = () => {
  if (dashboardOverviewData.value?.hero?.sync) {
    dashboardOverviewData.value.hero.sync.value = new Date().toLocaleTimeString('zh-CN', {
      hour12: false
    });
  }
};

const applyDashboardSummary = (summary) => {
  const amount = summary?.transactionAmount ?? {};
  const amountStat = findQuickStat('volume');
  if (amountStat) {
    amountStat.value = formatCurrency(amount.todayAmount);
    amountStat.trend = formatPercentChange(amount.changePercent);
    amountStat.trendLabel = '较昨日';
  }

  const count = summary?.transactionCount ?? {};
  const countStat = findQuickStat('transactions');
  if (countStat) {
    countStat.value = formatCount(count.todayCount);
    countStat.trend = formatPercentChange(count.changePercent);
    countStat.trendLabel = '较昨日';
  }

  const success = summary?.successRate ?? {};
  const successStat = findQuickStat('success');
  if (successStat) {
    successStat.value = formatRateValue(success.currentRate);
    successStat.trend = formatPercentChange(success.changePercent);
    successStat.trendLabel = '较上一窗口';
  }
};

const startSummaryPolling = () => {
  stopSummaryPolling();
  summaryPollTimer = window.setInterval(() => {
    fetchDashboardSummary();
  }, SUMMARY_POLL_INTERVAL);
};

const stopSummaryPolling = () => {
  if (summaryPollTimer) {
    clearInterval(summaryPollTimer);
    summaryPollTimer = null;
  }
};

const bootstrapAuthState = () => {
  const stored = getStoredToken();
  if (stored) {
    isAuthenticated.value = true;
    fetchVisibleMenus();
  }
};

const handleLoginSuccess = (payload) => {
  const token = payload?.token;
  if (!token) {
    return;
  }
  authProfile.value = payload?.profile ?? null;
  setAuthToken(token);
  isAuthenticated.value = true;
  fetchVisibleMenus();
};

const handleLogout = async () => {
  const token = getStoredToken();
  try {
    if (token) {
      await http.post('/auth/admin/logout', { token });
    }
  } catch (error) {
    console.error('Logout request failed', error);
  } finally {
    setAuthToken('');
    authProfile.value = null;
    isAuthenticated.value = false;
    apiMenuTree.value = null;
    stopSummaryPolling();
    tabs.value = [];
    activeTabId.value = '';
    activeParent.value = '';
    activeChild.value = '';
    localStorage.removeItem(TABS_KEY);
    localStorage.removeItem(ACTIVE_TAB_KEY);
  }
};

const applyHomeMetrics = (payload) => {
  const operatingCountries = Array.isArray(payload?.operatingCountries)
    ? payload.operatingCountries.filter((item) => item !== null)
    : [];
  const activeChannels = Number(payload?.activeChannelCount ?? 0);
  const minuteSla = payload?.minuteLevelSla ?? '99.95%';

  homeMetrics.value = {
    operatingCountries,
    activeChannelCount: activeChannels,
    minuteLevelSla: minuteSla
  };
};

const fetchHomeMetrics = async () => {
  if (!isAuthenticated.value) {
    return;
  }
  homeMetricsLoading.value = true;
  homeMetricsError.value = '';

  try {
    const { data } = await http.get('/admin/pay/home/metrics');
    const payload = data?.data ?? data;
    if (!payload) {
      throw new Error('Empty payload');
    }
    applyHomeMetrics(payload);
  } catch (error) {
    console.error('Failed to load home metrics', error);
    homeMetricsError.value = '无法加载最新的首页指标';
  } finally {
    homeMetricsLoading.value = false;
  }
};

const fetchDashboardSummary = async () => {
  if (!isAuthenticated.value) {
    return;
  }
  dashboardSummaryLoading.value = true;
  dashboardSummaryError.value = '';
  try {
    const { data } = await http.get('/admin/pay/dashboard/summary');
    const payload = data?.data ?? data;
    if (!payload) {
      throw new Error('Empty payload');
    }
    applyDashboardSummary(payload);
    updateHeroSyncTime();
  } catch (error) {
    console.error('Failed to load dashboard summary', error);
    dashboardSummaryError.value = '无法获取仪表板摘要';
  } finally {
    dashboardSummaryLoading.value = false;
  }
};

watch(isAuthenticated, (loggedIn) => {
  if (loggedIn) {
    fetchHomeMetrics();
    fetchDashboardSummary();
    startSummaryPolling();
  } else {
    stopSummaryPolling();
  }
});

onMounted(() => {
  bootstrapAuthState();
  if (getStoredToken()) {
    restoreTabs();
  }
});

onBeforeUnmount(() => {
  stopSummaryPolling();
});


const isDashboardOverviewActive = computed(
  () => activeParent.value === 'dashboard' && activeChild.value === 'dashboard-overview'
);

const isDashboardChannelViewActive = computed(
  () => activeParent.value === 'dashboard' && activeChild.value === 'dashboard-channel'
);

const isDashboardMerchantViewActive = computed(
  () => activeParent.value === 'dashboard' && activeChild.value === 'dashboard-merchant'
);

const isDashboardFundsViewActive = computed(
  () => activeParent.value === 'dashboard' && activeChild.value === 'dashboard-funds'
);

const isDashboardBankTradeViewActive = computed(
  () => activeParent.value === 'dashboard' && activeChild.value === 'dashboard-banktrade'
);

const isDashboardBankIdViewActive = computed(
  () => activeParent.value === 'dashboard' && activeChild.value === 'dashboard-bankid'
);

const isDashboardBankMonitorActive = computed(
  () => activeParent.value === 'dashboard' && activeChild.value === 'dashboard-bankmonitor'
);

const isDashboardAgentLogActive = computed(
  () => activeParent.value === 'dashboard' && activeChild.value === 'dashboard-agentlog'
);

const isBankSuppliersActive = computed(
  () => activeParent.value === 'bank-accounts' && activeChild.value === 'bank-suppliers'
);

const isBankAccountsActive = computed(
  () => activeParent.value === 'bank-accounts' && activeChild.value === 'bank-accounts-list'
);

const isBankChannelSettingsActive = computed(
  () => activeParent.value === 'bank-accounts' && activeChild.value === 'bank-channel-settings'
);

const isBankRealtimeBoardActive = computed(
  () => activeParent.value === 'bank-accounts' && activeChild.value === 'bank-realtime-dashboard'
);

const isBankLedgerActive = computed(
  () => activeParent.value === 'bank-accounts' && activeChild.value === 'bank-ledger'
);

const isBankBookkeepingActive = computed(
  () => activeParent.value === 'bank-accounts' && activeChild.value === 'bank-bookkeeping'
);

const isBankMappingActive = computed(
  () => activeParent.value === 'bank-accounts' && activeChild.value === 'bank-mapping'
);

const isOrdersCollectionActive = computed(
  () => activeParent.value === 'orders' && activeChild.value === 'orders-payin'
);

const isOrdersPayoutActive = computed(
  () => activeParent.value === 'orders' && activeChild.value === 'orders-payout'
);

const isOrdersRollbackActive = computed(
  () => activeParent.value === 'orders' && activeChild.value === 'orders-rollback'
);

const isOrdersPayoutQueueActive = computed(
  () => activeParent.value === 'orders' && activeChild.value === 'orders-queue'
);

const isOrdersBatchActive = computed(
  () => activeParent.value === 'orders' && activeChild.value === 'orders-batch'
);

const isOrdersApprovalActive = computed(
  () => activeParent.value === 'orders' && activeChild.value === 'orders-approval'
);

const isOrdersQueryActive = computed(
  () => activeParent.value === 'orders' && activeChild.value === 'orders-query'
);

const isMerchantsListActive = computed(
  () => activeParent.value === 'merchants' && activeChild.value === 'merchants-list'
);

const isMerchantsBoardActive = computed(
  () => activeParent.value === 'merchants' && activeChild.value === 'merchants-dashboard'
);

const isAgentsChannelsActive = computed(
  () => activeParent.value === 'agents' && activeChild.value === 'agents-channels'
);

const isAgentsListActive = computed(
  () => activeParent.value === 'agents' && activeChild.value === 'agents-list'
);

const isOpsSmsActive = computed(
  () => activeParent.value === 'operations' && activeChild.value === 'ops-sms'
);

const isOpsAppForwarderActive = computed(
  () => activeParent.value === 'operations' && activeChild.value === 'ops-forwarder'
);

const isOpsBatchActive = computed(
  () => activeParent.value === 'operations' && activeChild.value === 'ops-bulk'
);

const isOpsVpsManageActive = computed(
  () => activeParent.value === 'operations' && activeChild.value === 'ops-vps-manager'
);

const isOpsVpsActive = computed(
  () => activeParent.value === 'operations' && activeChild.value === 'ops-vps'
);

const isOpsTelegramActive = computed(
  () => activeParent.value === 'operations' && activeChild.value === 'ops-telegram'
);

const isFinanceBillingActive = computed(
  () => activeParent.value === 'finance' && activeChild.value === 'finance-billing'
);

const isFinanceWithdrawActive = computed(
  () => activeParent.value === 'finance' && activeChild.value === 'finance-withdraw'
);

const isFinanceAgentBillingActive = computed(
  () => activeParent.value === 'finance' && activeChild.value === 'finance-agent-billing'
);

const isFinanceSystemTopupActive = computed(
  () => activeParent.value === 'finance' && activeChild.value === 'finance-system-topup'
);

const isFinanceAgentWithdrawActive = computed(
  () => activeParent.value === 'finance' && activeChild.value === 'finance-agent-withdraw'
);

const isSystemBillingActive = computed(
  () => activeParent.value === 'system' && activeChild.value === 'system-billing'
);

const isSystemUsersActive = computed(
  () => activeParent.value === 'system' && activeChild.value === 'system-users'
);

const isSystemSettingsActive = computed(
  () => activeParent.value === 'system' && activeChild.value === 'system-settings'
);

const isDownloadsHistoryActive = computed(
  () => activeParent.value === 'downloads' && activeChild.value === 'downloads-history'
);
const isPaymentsEntityActive = computed(
  () => activeParent.value === 'payments' && activeChild.value === 'payments-entity'
);

const isPaymentsChannelActive = computed(
  () => activeParent.value === 'payments' && activeChild.value === 'payments-channel'
);

const isPaymentsErrorActive = computed(
  () => activeParent.value === 'payments' && activeChild.value === 'payments-errors'
);

const isPaymentsRollbackActive = computed(
  () => activeParent.value === 'payments' && activeChild.value === 'payments-rollback'
);

const isPaymentsReconcileActive = computed(
  () => activeParent.value === 'payments' && activeChild.value === 'payments-reconcile'
);

const isPaymentsThrottleActive = computed(
  () => activeParent.value === 'payments' && activeChild.value === 'payments-throttle'
);

const isPaymentsFallbackActive = computed(
  () => activeParent.value === 'payments' && activeChild.value === 'payments-fallback'
);

const isPaymentsIfscActive = computed(
  () => activeParent.value === 'payments' && activeChild.value === 'payments-ifsc-blacklist'
);

const isPaymentsBlacklistActive = computed(
  () => activeParent.value === 'payments' && activeChild.value === 'payments-blacklist'
);

const isPaymentsProfitActive = computed(
  () => activeParent.value === 'payments' && activeChild.value === 'payments-profit'
);
// 商户独有仪表板：dashboard 下无子菜单时（商户角色），直接渲染商户看板
const isMerchantDashboardActive = computed(() => {
  if (activeParent.value !== 'dashboard' || activeChild.value !== '') return false;
  const dashItem = filteredMenuItems.value.find((m) => m.id === 'dashboard');
  return (dashItem?.children?.length ?? 0) === 0;
});

const isMerchantOrderPayinActive = computed(
  () => activeParent.value === 'orders' && activeChild.value === 'merchant-orders-payin'
);

const isMerchantWithdrawApplyActive = computed(
  () => activeParent.value === 'withdraw' && activeChild.value === 'withdraw-apply'
);

const isMerchantApiDocsActive = computed(
  () => activeParent.value === 'docs' && activeChild.value === 'docs-api'
);
const handleMenuSelect = ({ parentId, childId }) => {
  if (!parentId && !childId) return;
  const parent = filteredMenuItems.value.find((i) => i.id === parentId);
  const resolvedChild = childId || parent?.children?.[0]?.id || '';
  openOrActivateTab(parentId, resolvedChild);
};
</script>

<template>
  <div v-if="!isAuthenticated" class="login-wrapper">
    <LoginPanel @success="handleLoginSuccess" />
  </div>
  <div v-else class="app-shell">
    <HeaderBar
      :alerts="alerts"
      :language="language"
      :languages="languages"
      :timezone="timezone"
      :timezones="timezones"
      :user="currentUser"
      @logout="handleLogout"
      @update:language="language = $event"
      @update:timezone="timezone = $event"
    />
    <div class="layout">
      <SidebarMenu
        :menu-items="filteredMenuItems"
        :last-synced="alerts.lastUpdated"
        :active-parent="activeParent"
        :active-child="activeChild"
        @select="handleMenuSelect"
      />
      <div class="content-area">
        <!-- Tab bar -->
        <div v-if="tabs.length" class="tab-bar">
          <div
            v-for="tab in tabs"
            :key="tab.id"
            class="tab-item"
            :class="{ active: tab.id === activeTabId }"
            @click="switchTab(tab)"
          >
            <span class="tab-label">{{ tab.label }}</span>
            <button class="tab-close" @click="closeTab(tab.id, $event)">×</button>
          </div>
        </div>

        <main class="main-content">
        <MerchantDashboardView v-if="isMerchantDashboardActive" />
        <DashboardOverview v-else-if="isDashboardOverviewActive" :data="dashboardOverviewData" />
        <DashboardChannelView
          v-else-if="isDashboardChannelViewActive"
          :data="dashboardChannelData"
        />
        <DashboardMerchantView
          v-else-if="isDashboardMerchantViewActive"
          :data="dashboardMerchantData"
        />
        <DashboardFundsView v-else-if="isDashboardFundsViewActive" :data="dashboardFundsData" />
        <DashboardBankTradeView
          v-else-if="isDashboardBankTradeViewActive"
          :data="dashboardBankTradeData"
        />
        <DashboardBankIdView v-else-if="isDashboardBankIdViewActive" :data="dashboardBankIdData" />
        <DashboardBankMonitorView
          v-else-if="isDashboardBankMonitorActive"
          :data="dashboardBankMonitorData"
        />
        <DashboardAgentLogView
          v-else-if="isDashboardAgentLogActive"
          :data="dashboardAgentLogData"
        />
        <BankSuppliersView v-else-if="isBankSuppliersActive" :data="bankSuppliersData" />
        <BankAccountsView v-else-if="isBankAccountsActive" :data="bankAccountsData" />
        <BankChannelSettingsView
          v-else-if="isBankChannelSettingsActive"
          :data="bankChannelSettingsData"
        />
        <BankRealtimeBoardView
          v-else-if="isBankRealtimeBoardActive"
          :data="bankRealtimeBoardData"
        />
        <BankLedgerView v-else-if="isBankLedgerActive" :data="bankLedgerData" />
        <BankBookkeepingView
          v-else-if="isBankBookkeepingActive"
          :data="bankBookkeepingData"
        />
        <BankMappingView v-else-if="isBankMappingActive" :data="bankMappingData" />
        <MerchantOrderPayinView v-else-if="isMerchantOrderPayinActive" />
        <MerchantWithdrawApplyView v-else-if="isMerchantWithdrawApplyActive" />
        <MerchantApiDocsView v-else-if="isMerchantApiDocsActive" />
        <OrdersCollectionView
          v-else-if="isOrdersCollectionActive"
          :data="ordersCollectionData"
        />
        <OrdersPayoutView v-else-if="isOrdersPayoutActive" :data="ordersPayoutData" />
        <OrdersRollbackView v-else-if="isOrdersRollbackActive" :data="ordersRollbackData" />
        <OrdersPayoutQueueView
          v-else-if="isOrdersPayoutQueueActive"
          :data="ordersPayoutQueueData"
        />
        <OrdersBatchView v-else-if="isOrdersBatchActive" :data="ordersBatchData" />
        <OrdersApprovalView
          v-else-if="isOrdersApprovalActive"
          :data="ordersApprovalData"
        />
        <OrdersQueryView v-else-if="isOrdersQueryActive" :data="ordersQueryData" />
        <MerchantsListView v-else-if="isMerchantsListActive" :data="merchantsListData" />
        <MerchantsBoardView v-else-if="isMerchantsBoardActive" :data="merchantsBoardData" />
        <AgentsChannelsView
          v-else-if="isAgentsChannelsActive"
          :data="agentsChannelsData"
        />
        <AgentsListView v-else-if="isAgentsListActive" :data="agentsListData" />
        <OpsSmsView v-else-if="isOpsSmsActive" :data="opsSmsData" />
        <OpsAppForwarderView
          v-else-if="isOpsAppForwarderActive"
          :data="opsAppForwarderData"
        />
        <OpsBatchView v-else-if="isOpsBatchActive" :data="opsBatchData" />
        <OpsVpsManageView v-else-if="isOpsVpsManageActive" :data="opsVpsManageData" />
        <OpsVpsView v-else-if="isOpsVpsActive" :data="opsVpsData" />
        <OpsTelegramView v-else-if="isOpsTelegramActive" :data="opsTelegramData" />
        <FinanceBillingView v-else-if="isFinanceBillingActive" :data="financeBillingData" />
        <FinanceWithdrawView v-else-if="isFinanceWithdrawActive" :data="financeWithdrawData" />
        <FinanceAgentBillingView
          v-else-if="isFinanceAgentBillingActive"
          :data="financeAgentBillingData"
        />
        <FinanceSystemTopupView
          v-else-if="isFinanceSystemTopupActive"
          :data="financeSystemTopupData"
        />
        <FinanceAgentWithdrawView
          v-else-if="isFinanceAgentWithdrawActive"
          :data="financeAgentWithdrawData"
        />
        <SystemBillingView v-else-if="isSystemBillingActive" :data="systemBillingData" />
        <SystemUsersView v-else-if="isSystemUsersActive" />
        <SystemSettingsView v-else-if="isSystemSettingsActive" />
        <DownloadsHistoryView
          v-else-if="isDownloadsHistoryActive"
          :data="downloadsHistoryData"
        />
        <PaymentsEntityView v-else-if="isPaymentsEntityActive" />
        <PaymentsChannelView v-else-if="isPaymentsChannelActive" />
        <PaymentsErrorView v-else-if="isPaymentsErrorActive" :data="paymentsErrorData" />
        <PaymentsRollbackView
          v-else-if="isPaymentsRollbackActive"
          :data="paymentsRollbackData"
        />
        <PaymentsReconcileView
          v-else-if="isPaymentsReconcileActive"
          :data="paymentsReconcileData"
        />
        <PaymentsThrottleView
          v-else-if="isPaymentsThrottleActive"
          :data="paymentsThrottleData"
        />
        <PaymentsFallbackView
          v-else-if="isPaymentsFallbackActive"
          :data="paymentsFallbackData"
        />
        <PaymentsIfscBlacklistView
          v-else-if="isPaymentsIfscActive"
          :data="paymentsIfscBlacklistData"
        />
        <PaymentsBlacklistView
          v-else-if="isPaymentsBlacklistActive"
          :data="paymentsBlacklistData"
        />
        <PaymentsProfitView v-else-if="isPaymentsProfitActive" :data="paymentsProfitData" />
        <template v-else>
          <div class="quick-links">
            <button
              class="ql-card"
              @click="activeParent = 'dashboard'; activeChild = 'dashboard-overview'"
            >
              <svg class="ql-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <path d="M12 8v4l2.5 2.5"/>
                <path d="M7.5 15.5a6 6 0 1 1 9 0"/>
              </svg>
              <span class="ql-title">Dashboard</span>
              <span class="ql-sub">查看数据报表</span>
            </button>
            <button
              class="ql-card"
              @click="activeParent = 'orders'; activeChild = 'orders-payin'"
            >
              <svg class="ql-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="7" height="7" rx="1"/>
                <rect x="14" y="3" width="7" height="7" rx="1"/>
                <rect x="3" y="14" width="7" height="7" rx="1"/>
                <rect x="14" y="14" width="7" height="7" rx="1"/>
              </svg>
              <span class="ql-title">订单</span>
              <span class="ql-sub">查看所有订单</span>
            </button>
            <button
              class="ql-card"
              @click="activeParent = 'system'; activeChild = 'system-settings'"
            >
              <svg class="ql-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="3"/>
                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
              </svg>
              <span class="ql-title">设置</span>
              <span class="ql-sub">系统设置</span>
            </button>
            <button class="ql-card ql-card--disabled" disabled>
              <svg class="ql-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
              <span class="ql-title">帮助</span>
              <span class="ql-sub">帮助文档</span>
            </button>
          </div>

          <section class="channel-section">
            <ChannelCards />
          </section>
        </template>
      </main>
      </div>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: radial-gradient(circle at top, rgba(77, 95, 255, 0.2), transparent 45%),
    #05060a;
  color: #f7f8ff;
}

.layout {
  display: flex;
  flex: 1;
  min-height: 0;
}

.content-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Tab bar */
.tab-bar {
  display: flex;
  align-items: stretch;
  flex-wrap: nowrap;
  overflow-x: auto;
  background: rgba(8, 9, 22, 0.95);
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  scrollbar-width: none;
  flex-shrink: 0;
}
.tab-bar::-webkit-scrollbar { display: none; }

.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px 0 16px;
  height: 38px;
  cursor: pointer;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  white-space: nowrap;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  transition: background 0.15s, color 0.15s;
  position: relative;
  user-select: none;
}

.tab-item:hover {
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.75);
}

.tab-item.active {
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.92);
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: #6366f1;
  border-radius: 2px 2px 0 0;
}

.tab-label { font-size: 13px; }

.tab-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.3);
  cursor: pointer;
  font-size: 15px;
  line-height: 1;
  border-radius: 3px;
  padding: 0;
  flex-shrink: 0;
}
.tab-close:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
}

.main-content {
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  overflow-y: auto;
  padding: 32px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.ql-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 32px 20px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
  color: #fff;
  text-align: center;
}

.ql-card:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
}

.ql-card--disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.ql-icon {
  width: 48px;
  height: 48px;
  color: rgba(255, 255, 255, 0.9);
}

.ql-title {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

.ql-sub {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
}

.channel-section {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.login-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 960px) {
  .layout {
    flex-direction: column;
  }

  .main-content {
    padding: 16px;
  }
}
</style>
