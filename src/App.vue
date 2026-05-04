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
  channels,
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

const [firstMenu] = menuItems;
const activeParent = ref(firstMenu?.id ?? '');
const activeChild = ref(firstMenu?.children?.[0]?.id ?? '');

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

// 过滤后菜单变化时，若当前 activeParent 不在可见列表则重置
watch(filteredMenuItems, (items) => {
  if (items.length && !items.find((i) => i.id === activeParent.value)) {
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
const channelKeyword = ref('');
const statusFilter = ref('all');
const channelStatusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '平稳', value: '平稳' },
  { label: '监控', value: '监控' },
  { label: '紧急', value: '紧急' }
];

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
});

onBeforeUnmount(() => {
  stopSummaryPolling();
});

const filteredChannels = computed(() => {
  const keyword = channelKeyword.value.trim().toLowerCase();
  return channels.filter((channel) => {
    const matchesStatus = statusFilter.value === 'all' || channel.status === statusFilter.value;
    if (!keyword) {
      return matchesStatus;
    }
    const haystack = [
      channel.id,
      channel.country,
      channel.countryTag,
      channel.description,
      ...(channel.businessTypes ?? [])
    ];
    const matchesKeyword = haystack.some((item) =>
      String(item ?? '')
        .toLowerCase()
        .includes(keyword)
    );
    return matchesStatus && matchesKeyword;
  });
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
  if (!parentId && !childId) {
    activeParent.value = '';
    activeChild.value = '';
    return;
  }
  if (parentId && parentId !== activeParent.value) {
    activeParent.value = parentId;
    const parent = filteredMenuItems.value.find((item) => item.id === parentId);
    activeChild.value = childId ?? parent?.children?.[0]?.id ?? '';
    return;
  }
  if (childId) {
    activeChild.value = childId;
  }
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
        <SystemUsersView v-else-if="isSystemUsersActive" :data="systemUsersData" />
        <SystemSettingsView
          v-else-if="isSystemSettingsActive"
          :data="systemSettingsData"
        />
        <DownloadsHistoryView
          v-else-if="isDownloadsHistoryActive"
          :data="downloadsHistoryData"
        />
        <PaymentsEntityView v-else-if="isPaymentsEntityActive" :data="paymentsEntityData" />
        <PaymentsChannelView v-else-if="isPaymentsChannelActive" :data="paymentsChannelData" />
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
          <section class="hero-card">
            <div>
              <p class="hero-subtitle">{{ heroData.subtitle }}</p>
              <h1>{{ heroData.title }}</h1>
              <p class="hero-description">{{ heroData.description }}</p>
              <ul class="hero-metrics">
                <li>
                  <span class="label">在运营国家</span>
                  <span class="value">{{ homeMetrics.operatingCountries.length || 0 }}</span>
                </li>
                <li>
                  <span class="label">活跃通道</span>
                  <span class="value">{{ homeMetrics.activeChannelCount }}</span>
                </li>
                <li>
                  <span class="label">分钟级 SLA</span>
                  <span class="value">{{ homeMetrics.minuteLevelSla }}</span>
                </li>
              </ul>
            </div>
            <div class="hero-cta">
              <p class="cta-title">{{ heroData.cta.title }}</p>
              <p class="cta-description">{{ heroData.cta.description }}</p>
              <button type="button">{{ heroData.cta.action }}</button>
            </div>
          </section>
          <p v-if="homeMetricsLoading" class="metrics-hint">首页指标加载中...</p>
          <p v-else-if="homeMetricsError" class="metrics-error">{{ homeMetricsError }}</p>

          <section class="channel-section">
            <div class="section-header">
              <div>
                <p class="eyebrow">重点观察</p>
                <h2>跨国通道运行状态</h2>
                <p class="muted">共 {{ channels.length }} 条记录，当前显示 {{ filteredChannels.length }} 条</p>
              </div>
              <div class="section-actions">
                <input
                  v-model="channelKeyword"
                  type="search"
                  placeholder="搜索国家 / 通道 / 业务类型"
                />
                <select v-model="statusFilter">
                  <option v-for="option in channelStatusOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </div>
            </div>
            <ChannelCards :channels="filteredChannels" />
          </section>
        </template>
      </main>
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
}

.main-content {
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  padding: 32px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.hero-card {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 24px;
  padding: 32px;
  border-radius: 28px;
  background: linear-gradient(135deg, rgba(89, 92, 255, 0.5), rgba(18, 24, 45, 0.9));
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.metrics-hint,
.metrics-error {
  font-size: 12px;
  margin-top: -8px;
  margin-bottom: 16px;
}

.metrics-hint {
  color: rgba(255, 255, 255, 0.6);
}

.metrics-error {
  color: #ff8c8c;
}

.hero-subtitle {
  letter-spacing: 0.3em;
  font-size: 12px;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.7);
}

.hero-description {
  color: rgba(255, 255, 255, 0.7);
  margin: 16px 0;
}

.hero-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding: 0;
  list-style: none;
}

.hero-metrics li {
  background: rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  padding: 12px 18px;
}

.hero-metrics .label {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.hero-metrics .value {
  font-size: 20px;
  font-weight: 600;
}

.hero-cta {
  align-self: center;
  background: rgba(6, 6, 12, 0.35);
  border-radius: 24px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.cta-title {
  font-size: 20px;
  font-weight: 600;
}

.cta-description {
  color: rgba(255, 255, 255, 0.7);
  margin: 8px 0 16px;
}

.hero-cta button {
  padding: 10px 24px;
  border-radius: 999px;
  border: none;
  background: #ffae6d;
  color: #1d0f05;
  font-weight: 600;
  cursor: pointer;
}

.channel-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.7);
}

.muted {
  color: rgba(255, 255, 255, 0.7);
}

.section-actions {
  display: flex;
  gap: 12px;
}

.section-actions input,
.section-actions select {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 10px 14px;
  color: #fff;
}

.section-actions input::placeholder {
  color: rgba(255, 255, 255, 0.45);
}

.section-actions select {
  min-width: 140px;
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
    padding: 24px;
  }

  .section-actions {
    width: 100%;
    flex-direction: column;
  }
}
</style>
