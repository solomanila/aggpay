<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import http from '../services/http';

const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});

// ── PayIn Link Open Rate table ───────────────────────────────────
const orCurrency = ref('INR');
const orMinutes = ref(30);
const orMinutesInput = ref('30');
const orLoading = ref(false);
const orError = ref('');
const orRows = ref([]);

// sort state
const orSortKey = ref('orderNum');
const orSortDir = ref(-1); // -1 desc, 1 asc

const orSortedRows = computed(() => {
  const key = orSortKey.value;
  const dir = orSortDir.value;
  return [...orRows.value].sort((a, b) => {
    const av = a[key] ?? 0;
    const bv = b[key] ?? 0;
    return (av < bv ? -1 : av > bv ? 1 : 0) * dir;
  });
});

const orColumns = [
  { key: 'merchant',        label: 'merchant' },
  { key: 'orderNum',        label: 'orderNum' },
  { key: 'openNum',         label: 'openNum' },
  { key: 'openRate',        label: 'openRate' },
  { key: 'successRate',     label: 'successRate' },
  { key: 'openSuccessRate', label: 'openSuccessRate' }
];

const rateKeys = new Set(['openRate', 'successRate', 'openSuccessRate']);

const fmtCell = (row, key) => {
  const v = row[key];
  if (v === null || v === undefined) return '—';
  if (rateKeys.has(key)) return `${Number(v).toFixed(2)}%`;
  return v;
};

const setSort = (key) => {
  if (orSortKey.value === key) {
    orSortDir.value *= -1;
  } else {
    orSortKey.value = key;
    orSortDir.value = -1;
  }
};

const clearMinutes = () => {
  orMinutesInput.value = '30';
  orMinutes.value = 30;
};

const applyMinutes = () => {
  const n = parseInt(orMinutesInput.value, 10);
  if (!Number.isNaN(n) && n > 0) {
    orMinutes.value = n;
  }
};

const fetchOpenRate = async () => {
  orLoading.value = true;
  orError.value = '';
  try {
    const { data: res } = await http.get('/admin/pay/dashboard/channelOpenRate', {
      params: { minutes: orMinutes.value }
    });
    orRows.value = res?.data ?? [];
  } catch (e) {
    console.error('Failed to load channel open rate', e);
    orError.value = '无法加载数据';
    orRows.value = [];
  } finally {
    orLoading.value = false;
  }
};

watch(orMinutes, fetchOpenRate);
onMounted(fetchOpenRate);
</script>

<template>
  <div class="merchant-view">
    <!-- ── PayIn Link Open Rate ──────────────────────────────── -->
    <section class="or-section">
      <div class="or-filter-bar">
        <label class="or-filter-item">
          <span class="or-filter-label">currency *</span>
          <div class="or-filter-field">
            <select v-model="orCurrency" class="or-select">
              <option value="INR">INR</option>
            </select>
          </div>
        </label>
        <label class="or-filter-item">
          <span class="or-filter-label">Minute</span>
          <div class="or-filter-field or-filter-field--clearable">
            <input
              v-model="orMinutesInput"
              type="number"
              min="1"
              max="1440"
              class="or-input"
              @change="applyMinutes"
              @keydown.enter="applyMinutes"
            />
            <button type="button" class="or-clear-btn" @click="clearMinutes">×</button>
          </div>
        </label>
      </div>

      <div class="panel or-table-panel">
        <div class="or-table-header">
          <span class="or-table-title">PayIn Link Open Rate/{{ orMinutes }}min</span>
          <button type="button" class="or-refresh-btn" :disabled="orLoading" @click="fetchOpenRate">
            ↻
          </button>
        </div>

        <p v-if="orLoading" class="or-state muted">正在加载...</p>
        <p v-else-if="orError" class="or-state error-text">{{ orError }}</p>
        <div v-else class="or-table-wrap">
          <table class="or-table">
            <thead>
              <tr>
                <th
                  v-for="col in orColumns"
                  :key="col.key"
                  class="or-th"
                  :class="{ 'or-th--active': orSortKey === col.key }"
                  @click="setSort(col.key)"
                >
                  {{ col.label }}
                  <span class="or-sort-icon">
                    {{ orSortKey === col.key ? (orSortDir === -1 ? '↓' : '↑') : '↕' }}
                  </span>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!orSortedRows.length">
                <td :colspan="orColumns.length" class="or-empty">暂无数据</td>
              </tr>
              <tr v-for="(row, i) in orSortedRows" :key="i" class="or-row">
                <td v-for="col in orColumns" :key="col.key" class="or-td">
                  {{ fmtCell(row, col.key) }}
                </td>
              </tr>
            </tbody>
          </table>
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

    <section class="kpi-grid">
      <article v-for="kpi in data.kpis" :key="kpi.id" class="panel kpi-card">
        <p class="kpi-label">{{ kpi.label }}</p>
        <p class="kpi-value">{{ kpi.value }}</p>
        <p class="kpi-meta">{{ kpi.meta }}</p>
      </article>
    </section>

    <section class="spotlight-grid">
      <article v-for="merchant in data.spotlight" :key="merchant.id" class="panel spotlight-card">
        <header>
          <div>
            <p class="eyebrow">{{ merchant.country }}</p>
            <h3>{{ merchant.name }}</h3>
          </div>
          <span class="risk">{{ merchant.risk }}</span>
        </header>
        <dl>
          <div>
            <dt>GMV</dt>
            <dd>{{ merchant.gmvp }}</dd>
          </div>
          <div>
            <dt>成功率</dt>
            <dd>{{ merchant.success }}</dd>
          </div>
        </dl>
        <p class="alert">{{ merchant.alert }}</p>
      </article>
    </section>

    <section class="panel merchant-table">
      <header>
        <p class="eyebrow">重点商户</p>
        <h3>今日经营快照</h3>
      </header>
      <table>
        <thead>
          <tr>
            <th>商户</th>
            <th>区域</th>
            <th>今日 GMV</th>
            <th>成功率</th>
            <th>退款率</th>
            <th>等级</th>
            <th>Owner</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="merchant in data.merchantTable" :key="merchant.id">
            <td>
              <p class="name">{{ merchant.name }}</p>
              <div class="tags">
                <span v-for="tag in merchant.tags" :key="tag">{{ tag }}</span>
              </div>
            </td>
            <td>{{ merchant.region }}</td>
            <td>{{ merchant.today }}</td>
            <td>{{ merchant.success }}</td>
            <td>{{ merchant.dispute }}</td>
            <td>{{ merchant.tier }}</td>
            <td>{{ merchant.owner }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="retention-risk">
      <article class="panel retention">
        <header>
          <p class="eyebrow">留存趋势</p>
          <h3>经营稳定度</h3>
        </header>
        <div class="retention-list">
          <div v-for="item in data.retention" :key="item.id" class="retention-row">
            <div>
              <p class="label">{{ item.label }}</p>
              <p class="value">{{ item.value }}</p>
            </div>
            <span class="diff">{{ item.diff }}</span>
          </div>
        </div>
      </article>
      <article class="panel risks">
        <header>
          <p class="eyebrow">风控信号</p>
          <h3>实时关注</h3>
        </header>
        <ul>
          <li v-for="risk in data.riskSignals" :key="risk.id">
            <div>
              <p class="title">{{ risk.merchant }}</p>
              <p class="meta">{{ risk.signal }}</p>
            </div>
            <button type="button">{{ risk.action }}</button>
          </li>
        </ul>
      </article>
    </section>

    <section class="ops-grid">
      <article class="panel tickets">
        <header>
          <p class="eyebrow">处理工单</p>
          <h3>跨域进度</h3>
        </header>
        <ul>
          <li v-for="ticket in data.tickets" :key="ticket.id">
            <div>
              <p class="title">{{ ticket.title }}</p>
              <p class="meta">{{ ticket.owner }} · {{ ticket.eta }}</p>
            </div>
            <span class="status">跟进</span>
          </li>
        </ul>
      </article>
      <article class="panel campaigns">
        <header>
          <p class="eyebrow">运营活动</p>
          <h3>实时拉新</h3>
        </header>
        <div class="campaign-list">
          <div v-for="campaign in data.campaigns" :key="campaign.id" class="campaign-row">
            <div>
              <p class="title">{{ campaign.name }}</p>
              <p class="meta">{{ campaign.status }}</p>
            </div>
            <strong>{{ campaign.lift }}</strong>
          </div>
        </div>
      </article>
      <article class="panel notices">
        <header>
          <p class="eyebrow">运营提醒</p>
          <h3>同步事项</h3>
        </header>
        <ul>
          <li v-for="notice in data.notices" :key="notice.id">
            {{ notice.text }}
          </li>
        </ul>
      </article>
    </section>
  </div>
</template>

<style scoped>
.merchant-view {
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
  background: linear-gradient(135deg, rgba(83, 95, 255, 0.6), rgba(12, 18, 36, 0.9));
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

.hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.kpi-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.kpi-card {
  background: rgba(255, 255, 255, 0.02);
}

.kpi-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.kpi-value {
  font-size: 24px;
  font-weight: 600;
  margin: 8px 0 4px;
}

.kpi-meta {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.spotlight-grid {
  display: grid;
  gap: 20px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.spotlight-card header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.spotlight-card .risk {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.1);
}

.spotlight-card dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(120px, 1fr));
  gap: 12px;
  margin: 0 0 12px;
}

.spotlight-card dt {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.spotlight-card dd {
  margin: 4px 0 0;
  font-size: 20px;
  font-weight: 600;
}

.spotlight-card .alert {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

.merchant-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 12px;
}

.merchant-table th {
  text-align: left;
  padding: 10px 0;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.merchant-table td {
  padding: 14px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.merchant-table .name {
  margin: 0 0 6px;
  font-weight: 600;
}

.merchant-table .tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.merchant-table .tags span {
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  font-size: 11px;
}

.retention-risk {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.retention-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.retention-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
}

.retention-row .diff {
  color: rgba(122, 227, 129, 0.9);
  font-weight: 600;
}

.risks ul,
.tickets ul,
.notices ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.risks li,
.tickets li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.02);
}

.risks button {
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  background: transparent;
  color: #fff;
  padding: 4px 12px;
  font-size: 12px;
}

.tickets .status {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.ops-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 24px;
}

.campaign-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.campaign-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.02);
}

.notices ul li {
  padding: 10px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

@media (max-width: 960px) {
  .hero {
    flex-direction: column;
    gap: 12px;
  }
}

/* ── PayIn Link Open Rate ──────────────────────────────────────── */
.or-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.or-filter-bar {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.or-filter-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 160px;
}

.or-filter-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 0.05em;
}

.or-filter-field {
  border: 1.5px solid rgba(127, 133, 249, 0.5);
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
}

.or-filter-field--clearable {
  padding-right: 4px;
}

.or-select,
.or-input {
  flex: 1;
  background: transparent;
  border: none;
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  padding: 8px 14px;
  outline: none;
  min-width: 0;
}

.or-input[type='number']::-webkit-inner-spin-button {
  opacity: 0.4;
}

.or-clear-btn {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  font-size: 16px;
  padding: 0 8px;
  cursor: pointer;
  line-height: 1;
  flex-shrink: 0;
}

.or-clear-btn:hover {
  color: #fff;
}

.or-table-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.or-table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.or-table-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.or-refresh-btn {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: 4px 10px;
  font-size: 14px;
  cursor: pointer;
}

.or-refresh-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.or-state {
  padding: 12px 0;
  font-size: 13px;
}

.error-text {
  color: #f87171;
}

.or-table-wrap {
  overflow-x: auto;
}

.or-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: 560px;
}

.or-th {
  padding: 10px 16px;
  text-align: left;
  color: rgba(255, 255, 255, 0.55);
  font-weight: 500;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  white-space: nowrap;
  cursor: pointer;
  user-select: none;
}

.or-th:hover {
  color: rgba(255, 255, 255, 0.85);
}

.or-th--active {
  color: rgba(177, 185, 249, 0.9);
}

.or-sort-icon {
  margin-left: 4px;
  font-size: 11px;
  opacity: 0.7;
}

.or-td {
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.88);
  white-space: nowrap;
}

.or-row:last-child .or-td {
  border-bottom: none;
}

.or-empty {
  padding: 24px;
  text-align: center;
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
}
</style>
