<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import http from '../services/http';

const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});

const severityClassMap = {
  高: 'severity-high',
  中: 'severity-medium',
  低: 'severity-low'
};

const getSeverityClass = (severity) => severityClassMap[severity] ?? 'severity-low';

// ── 实时订单详情 ─────────────────────────────────────────────────
const periodOptions = [
  { label: '今天', value: 'today' },
  { label: '一周内', value: '1w' },
  { label: '两周内', value: '2w' }
];

const payConfigOptions = ref([]);
const selectedPeriod = ref('today');
const selectedPayConfigId = ref('');

const tableRows = ref([]);
const tableTotal = ref(0);
const tablePage = ref(1);
const tablePageSize = ref(10);
const tableLoading = ref(false);
const tableError = ref('');

const paginationSizes = [10, 20, 50];

const totalPages = computed(() => {
  if (!tableTotal.value) {
    return 1;
  }
  return Math.max(1, Math.ceil(tableTotal.value / tablePageSize.value));
});

const tableColumns = [
  { key: 'id', label: 'ID' },
  { key: 'orderId', label: '订单号' },
  { key: 'platformId', label: '平台ID' },
  { key: 'frontId', label: '商户订单号' },
  { key: 'payConfigId', label: '支付配置ID' },
  { key: 'userId', label: '用户ID' },
  { key: 'gameId', label: '游戏ID' },
  { key: 'status', label: '状态' },
  { key: 'reqAmount', label: '请求金额' },
  { key: 'realAmount', label: '实际金额' },
  { key: 'payTime', label: '支付时间' },
  { key: 'createTime', label: '创建时间' },
  { key: 'createStatus', label: '创建状态' },
  { key: 'createIp', label: '创建IP' },
  { key: 'noticeStatus', label: '通知状态' },
  { key: 'noticeTime', label: '通知时间' },
  { key: 'payConfigChannelId', label: '渠道ID' },
  { key: 'otherOrderId', label: '上游订单号' },
  { key: 'onLineId', label: '在线ID' },
  { key: 'remark', label: '备注' },
  { key: 'extend1', label: '扩展1' },
  { key: 'extend2', label: '扩展2' },
  { key: 'extend3', label: '扩展3' },
  { key: 'syncStatus', label: '同步状态' },
  { key: 'settleAmount', label: '结算金额' },
  { key: 'upi', label: 'UPI' }
];

const formatDateTime = (value) => {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString('zh-CN', { hour12: false });
};

const formatNumber = (value, digits = 2) => {
  if (value === null || value === undefined || value === '') {
    return '—';
  }
  const num = Number(value);
  if (Number.isNaN(num)) {
    return value;
  }
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  });
};

const displayCell = (row, key) => {
  const value = row?.[key];
  if (value === null || value === undefined || value === '') {
    return '—';
  }
  if (key.toLowerCase().includes('time')) {
    return formatDateTime(value);
  }
  if (key.toLowerCase().includes('amount')) {
    return formatNumber(value);
  }
  return value;
};

const fetchPayConfigIds = async () => {
  try {
    const { data: response } = await http.get('/admin/pay/dashboard/payConfigIds');
    payConfigOptions.value = response?.data ?? response ?? [];
  } catch (error) {
    console.error('Failed to load payConfigIds', error);
    payConfigOptions.value = [];
  }
};

const fetchChannelStats = async () => {
  tableLoading.value = true;
  tableError.value = '';
  try {
    const params = {
      period: selectedPeriod.value,
      pageNum: tablePage.value,
      pageSize: tablePageSize.value
    };
    if (selectedPayConfigId.value !== '') {
      params.payConfigId = Number(selectedPayConfigId.value);
    }
    const { data: response } = await http.get('/admin/pay/dashboard/channelStat', { params });
    const payload = response?.data ?? response;
    tableRows.value = payload?.records ?? [];
    tableTotal.value = payload?.total ?? 0;
    tablePage.value = payload?.current ?? payload?.pageNum ?? tablePage.value;
    tablePageSize.value = payload?.size ?? payload?.pageSize ?? tablePageSize.value;
  } catch (error) {
    console.error('Failed to load channel stats', error);
    tableError.value = '无法加载通道统计';
    tableRows.value = [];
    tableTotal.value = 0;
  } finally {
    tableLoading.value = false;
  }
};

const handleFilterChange = () => {
  tablePage.value = 1;
  fetchChannelStats();
};

const changePage = (nextPage) => {
  if (nextPage < 1 || nextPage > totalPages.value || nextPage === tablePage.value) {
    return;
  }
  tablePage.value = nextPage;
  fetchChannelStats();
};

const changePageSize = (nextSize) => {
  tablePageSize.value = nextSize;
  tablePage.value = 1;
  fetchChannelStats();
};

watch(selectedPeriod, handleFilterChange);
watch(selectedPayConfigId, handleFilterChange);

onMounted(async () => {
  await fetchPayConfigIds();
  await fetchChannelStats();
});
</script>

<template>
  <div class="funds-view">
    <section class="panel hero gradient">
      <div>
        <p class="eyebrow">{{ data.hero.eyebrow }}</p>
        <h2>{{ data.hero.title }}</h2>
        <p class="muted">{{ data.hero.description }}</p>
      </div>
      <p class="muted">{{ data.hero.sync }}</p>
    </section>

    <!-- ── 通道监控 · 实时订单详情 ────────────────────────────── -->
    <section class="panel channel-stats-panel">
      <header class="channel-stats-header">
        <div>
          <p class="eyebrow">通道监控</p>
          <h3>实时订单详情</h3>
        </div>
        <div class="channel-stat-filters">
          <label>
            <span>时间段</span>
            <select v-model="selectedPeriod">
              <option v-for="option in periodOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
          <label>
            <span>支付配置</span>
            <select v-model="selectedPayConfigId">
              <option value="">全部配置</option>
              <option v-for="id in payConfigOptions" :key="id" :value="id">
                {{ id }}
              </option>
            </select>
          </label>
        </div>
      </header>
      <div class="channel-table-wrapper">
        <p v-if="tableError" class="error-text">{{ tableError }}</p>
        <p v-else-if="tableLoading" class="muted">正在加载通道数据...</p>
        <table v-if="!tableLoading">
          <thead>
            <tr>
              <th v-for="column in tableColumns" :key="column.key">{{ column.label }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!tableRows.length">
              <td :colspan="tableColumns.length" class="empty-cell">暂无数据</td>
            </tr>
            <tr v-for="(row, index) in tableRows" :key="row.id ?? row.orderId ?? index">
              <td v-for="column in tableColumns" :key="column.key">
                {{ displayCell(row, column.key) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination-bar">
        <div class="page-info">
          共 {{ tableTotal }} 条 · 第 {{ tablePage }} / {{ totalPages }} 页
        </div>
        <div class="pagination-controls">
          <button type="button" class="ghost-btn" :disabled="tablePage <= 1" @click="changePage(tablePage - 1)">
            上一页
          </button>
          <button
            type="button"
            class="ghost-btn"
            :disabled="tablePage >= totalPages"
            @click="changePage(tablePage + 1)"
          >
            下一页
          </button>
          <label>
            <span>每页</span>
            <select :value="tablePageSize" @change="changePageSize(Number($event.target.value))">
              <option v-for="size in paginationSizes" :key="size" :value="size">{{ size }}</option>
            </select>
          </label>
        </div>
      </div>
    </section>

    <section class="stat-grid">
      <article v-for="stat in data.stats" :key="stat.id" class="panel stat-card">
        <p class="label">{{ stat.label }}</p>
        <p class="value">{{ stat.value }}</p>
        <p class="meta">{{ stat.meta }}</p>
      </article>
    </section>

    <section class="balances-streams">
      <article class="panel balances">
        <header>
          <p class="eyebrow">币种头寸</p>
          <h3>余额与占用</h3>
        </header>
        <div class="balance-list">
          <div v-for="row in data.balances" :key="row.id" class="balance-row">
            <div>
              <p class="label">{{ row.label }}</p>
              <p class="value">{{ row.value }}</p>
            </div>
            <div class="progress">
              <span :style="{ width: `${row.utilization}%` }" />
            </div>
            <strong>{{ row.utilization }}%</strong>
          </div>
        </div>
      </article>

      <article class="panel streams">
        <header>
          <p class="eyebrow">资金流</p>
          <h3>入出款趋势</h3>
        </header>
        <ul>
          <li v-for="stream in data.streams" :key="stream.id">
            <div>
              <p class="label">{{ stream.label }}</p>
              <p class="value">{{ stream.value }}</p>
            </div>
            <span class="trend" :class="stream.type">{{ stream.trend }}</span>
          </li>
        </ul>
      </article>
    </section>

    <section class="panel ledger">
      <header>
        <p class="eyebrow">流水明细</p>
        <h3>最近动作</h3>
      </header>
      <table>
        <thead>
          <tr>
            <th>时间</th>
            <th>通道</th>
            <th>方向</th>
            <th>金额</th>
            <th>账户</th>
            <th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in data.ledger" :key="item.id">
            <td>{{ item.time }}</td>
            <td>{{ item.channel }}</td>
            <td>{{ item.direction }}</td>
            <td>{{ item.amount }}</td>
            <td>{{ item.account }}</td>
            <td>{{ item.status }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="pipelines-alerts">
      <article class="panel pipelines">
        <header>
          <p class="eyebrow">资金流转</p>
          <h3>在途 & 待复核</h3>
        </header>
        <div class="pipeline-list">
          <div v-for="pipe in data.pipelines" :key="pipe.id" class="pipeline-row">
            <p class="title">{{ pipe.label }}</p>
            <ul>
              <li v-for="item in pipe.items" :key="item">{{ item }}</li>
            </ul>
          </div>
        </div>
      </article>

      <article class="panel alerts">
        <header>
          <p class="eyebrow">告警</p>
          <h3>头寸风险</h3>
        </header>
        <div class="alert-list">
          <div v-for="alert in data.alerts" :key="alert.id" class="alert-row">
            <span class="severity" :class="getSeverityClass(alert.severity)">{{ alert.severity }}</span>
            <div>
              <p class="title">{{ alert.title }}</p>
              <p class="detail">{{ alert.detail }}</p>
              <p class="action">{{ alert.action }}</p>
            </div>
          </div>
        </div>
      </article>
    </section>

    <section class="panel transfers">
      <header>
        <p class="eyebrow">调拨计划</p>
        <h3>执行进度</h3>
      </header>
      <div class="transfer-list">
        <div v-for="transfer in data.transfers" :key="transfer.id" class="transfer-row">
          <div>
            <p class="title">{{ transfer.title }}</p>
            <p class="meta">{{ transfer.owner }} · {{ transfer.eta }}</p>
          </div>
          <div class="progress">
            <span :style="{ width: `${transfer.progress}%` }" />
          </div>
          <span class="percent">{{ transfer.progress }}%</span>
        </div>
      </div>
    </section>

  </div>
</template>

<style scoped>
.funds-view {
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
  background: linear-gradient(135deg, rgba(70, 98, 255, 0.65), rgba(13, 17, 33, 0.9));
  border: 1px solid rgba(255, 255, 255, 0.1);
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

.balances-streams {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.balance-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.balance-row {
  display: grid;
  grid-template-columns: 1fr 1fr 60px;
  gap: 12px;
  align-items: center;
}

.balance-row .progress,
.transfers .progress {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.balance-row .progress span,
.transfers .progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(120deg, rgba(134, 140, 255, 0.8), rgba(92, 230, 255, 0.7));
}

.streams ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.streams li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.streams .trend.in {
  color: #4ade80;
}

.streams .trend.out {
  color: #f87171;
}

.ledger table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 12px;
}

.ledger th {
  text-align: left;
  padding: 10px 0;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.ledger td {
  padding: 14px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.pipelines-alerts {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.pipeline-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pipeline-row {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.02);
}

.pipeline-row ul {
  list-style: none;
  padding: 0;
  margin: 10px 0 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.alerts .alert-row {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.severity-high {
  background: rgba(244, 114, 182, 0.18);
  color: #f472b6;
}

.severity-medium {
  background: rgba(251, 191, 36, 0.2);
  color: #fbbf24;
}

.severity-low {
  background: rgba(34, 197, 94, 0.18);
  color: #22c55e;
}

.alerts .severity {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.transfers .transfer-row {
  display: grid;
  grid-template-columns: 1fr 1fr 60px;
  gap: 12px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.percent {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

/* ── 通道监控 · 实时订单详情 ──────────────────────────────────── */
.channel-stats-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.channel-stats-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.channel-stat-filters {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.channel-stat-filters label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.channel-stat-filters select {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #fff;
  padding: 6px 12px;
  border-radius: 12px;
  min-width: 140px;
}

.channel-table-wrapper {
  overflow-x: auto;
}

.channel-table-wrapper table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.channel-table-wrapper th,
.channel-table-wrapper td {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  text-align: left;
  white-space: nowrap;
}

.channel-table-wrapper th {
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}

.channel-table-wrapper td {
  color: rgba(255, 255, 255, 0.9);
}

.channel-table-wrapper .empty-cell {
  text-align: center;
  padding: 20px;
  color: rgba(255, 255, 255, 0.6);
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.page-info {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-controls select {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #fff;
  padding: 4px 10px;
  border-radius: 10px;
}

.ghost-btn {
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: transparent;
  color: #fff;
  padding: 6px 16px;
  border-radius: 999px;
  cursor: pointer;
}

.ghost-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.error-text {
  color: #ff8e8e;
  margin-bottom: 8px;
}

@media (max-width: 960px) {
  .balance-row,
  .transfers .transfer-row {
    grid-template-columns: 1fr;
  }
}
</style>
