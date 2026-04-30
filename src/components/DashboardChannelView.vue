<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import http from '../services/http';

// ── Chart constants ──────────────────────────────────────────────
const CHART_W = 1200;
const CHART_H = 360;
const C_ML = 72;
const C_MR = 28;
const C_MT = 40;
const C_MB = 60;
const C_PW = CHART_W - C_ML - C_MR;
const C_PH = CHART_H - C_MT - C_MB;
const Y_TICKS = [0, 20, 40, 60, 80, 100];
const COLORS = ['#f59e0b', '#60a5fa', '#34d399', '#f87171', '#a78bfa', '#fb923c', '#e879f9'];

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

const statusLabelMap = {
  green: '健康',
  amber: '波动',
  red: '异常'
};

const getSeverityClass = (severity) => severityClassMap[severity] ?? 'severity-low';
const getStatusLabel = (status) => statusLabelMap[status] ?? '未知';

// ── 5-min success rate chart ─────────────────────────────────────
const chartDate = ref('today');
const chartCurrency = ref('INR');
const chartLoading = ref(false);
const chartError = ref('');
const chartRawData = ref([]);

const dateOptions = [
  { label: 'Today', value: 'today' },
  { label: 'Yesterday', value: 'yesterday' }
];

// Number of 5-minute slots to always show on the X axis
const CHART_SLOTS = 24; // 2 hours

// Parse "2026-4-16, 17:05"  →  Date
const parseWindowTime = (wt) => {
  const [datePart, timePart] = wt.split(', ');
  const [y, mo, d] = datePart.split('-').map(Number);
  const [h, mi] = timePart.split(':').map(Number);
  return new Date(y, mo - 1, d, h, mi, 0, 0);
};

// Date  →  "2026-4-16, 17:05"
const fmtWindowTime = (dt) => {
  const h = String(dt.getHours()).padStart(2, '0');
  const mi = String(dt.getMinutes()).padStart(2, '0');
  return `${dt.getFullYear()}-${dt.getMonth() + 1}-${dt.getDate()}, ${h}:${mi}`;
};

const chartSeries = computed(() => {
  const map = new Map();
  for (const p of chartRawData.value) {
    if (!map.has(p.channelName)) map.set(p.channelName, []);
    map.get(p.channelName).push(p);
  }
  return [...map.entries()].map(([name, pts]) => ({ name, pts }));
});

// Always produce CHART_SLOTS evenly-spaced 5-min slots ending at the latest data point.
// Slots with no real data will render as 0 %.
const chartTimes = computed(() => {
  if (!chartRawData.value.length) return [];
  const maxTs = Math.max(
    ...chartRawData.value.map(p => parseWindowTime(p.windowTime).getTime())
  );
  const anchor = new Date(maxTs);
  const slots = [];
  for (let i = CHART_SLOTS - 1; i >= 0; i--) {
    slots.push(fmtWindowTime(new Date(anchor.getTime() - i * 5 * 60 * 1000)));
  }
  return slots;
});

const svgChart = computed(() => {
  const times = chartTimes.value;
  const series = chartSeries.value;
  if (!times.length || !series.length) return null;
  const n = times.length;
  const xOf = (i) => C_ML + (n === 1 ? C_PW / 2 : (i * C_PW) / (n - 1));
  const yOf = (rate) => C_MT + C_PH * (1 - rate / 100);

  return {
    yTicks: Y_TICKS.map((v) => ({ v, y: yOf(v) })),
    xLabels: times.map((t, i) => ({
      t,
      label: t.includes(', ') ? t.split(', ')[1] : t,
      x: xOf(i),
      y: C_MT + C_PH + 14
    })),
    gridX1: C_ML,
    gridX2: CHART_W - C_MR,
    series: series.map((s, idx) => {
      const rateMap = new Map(s.pts.map((p) => [p.windowTime, Number(p.successRate)]));
      // treat missing windows as 0 so the line is always continuous
      const pts = times.map((t, i) => {
        const r = rateMap.has(t) ? rateMap.get(t) : 0;
        return { x: xOf(i), y: yOf(r), r };
      });
      return {
        name: s.name,
        color: COLORS[idx % COLORS.length],
        polyline: pts.map((p) => `${p.x},${p.y}`).join(' '),
        pts
      };
    })
  };
});

const fetchChartData = async () => {
  chartLoading.value = true;
  chartError.value = '';
  try {
    const { data: response } = await http.get('/admin/pay/dashboard/channelSuccessRate', {
      params: { date: chartDate.value }
    });
    chartRawData.value = response?.data ?? [];
  } catch (error) {
    console.error('Failed to load channel success rate', error);
    chartError.value = '无法加载成功率数据';
    chartRawData.value = [];
  } finally {
    chartLoading.value = false;
  }
};

watch(chartDate, fetchChartData);

onMounted(async () => {
  await fetchChartData();
});
</script>

<template>
  <div class="channel-view">
    <section class="panel hero gradient">
      <div>
        <p class="eyebrow">{{ data.hero.eyebrow }}</p>
        <h2>{{ data.hero.title }}</h2>
        <p class="muted">{{ data.hero.description }}</p>
      </div>
      <p class="muted">{{ data.hero.sync }}</p>
    </section>

    <section class="filters">
      <button
        v-for="filter in data.filters"
        :key="filter.id"
        class="filter-chip"
        :class="{ active: filter.active }"
        type="button"
      >
        {{ filter.label }}
      </button>
    </section>

    <section class="quick-stats">
      <article v-for="stat in data.stats" :key="stat.id" class="panel stat-card">
        <p class="stat-label">{{ stat.label }}</p>
        <p class="stat-value">{{ stat.value }}</p>
        <p class="stat-meta">{{ stat.meta }}</p>
      </article>
    </section>

    <!-- ── 5-min Success Rate Chart ───────────────────────────── -->
    <section class="panel success-rate-section">
      <div class="sr-filter-bar">
        <label class="sr-filter-item">
          <span class="sr-filter-label">currency *</span>
          <div class="sr-filter-field">
            <select v-model="chartCurrency" class="sr-select">
              <option value="INR">INR</option>
            </select>
          </div>
        </label>
        <label class="sr-filter-item">
          <span class="sr-filter-label">Date *</span>
          <div class="sr-filter-field">
            <select v-model="chartDate" class="sr-select">
              <option v-for="opt in dateOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </div>
        </label>
      </div>

      <div class="sr-chart-wrap">
        <p class="sr-chart-title">Success Rate/5 min</p>

        <div v-if="chartSeries.length" class="sr-legend">
          <span v-for="(s, i) in chartSeries" :key="s.name" class="sr-legend-item">
            <span class="sr-legend-dot" :style="{ background: COLORS[i % COLORS.length] }" />
            {{ s.name }}
          </span>
        </div>

        <p v-if="chartLoading" class="muted sr-state">正在加载数据...</p>
        <p v-else-if="chartError" class="error-text sr-state">{{ chartError }}</p>
        <p v-else-if="!chartRawData.length" class="muted sr-state">暂无数据</p>

        <div v-else-if="svgChart" class="sr-svg-container">
          <svg
            :viewBox="`0 0 ${CHART_W} ${CHART_H}`"
            class="sr-svg"
            xmlns="http://www.w3.org/2000/svg"
          >
            <!-- Y axis label (rotated) -->
            <text
              x="18"
              :y="C_MT + C_PH / 2"
              text-anchor="middle"
              :transform="`rotate(-90, 18, ${C_MT + C_PH / 2})`"
              class="sr-axis-label"
            >successRate</text>

            <!-- Grid lines + Y tick labels -->
            <g v-for="tick in svgChart.yTicks" :key="tick.v">
              <line
                :x1="svgChart.gridX1"
                :y1="tick.y"
                :x2="svgChart.gridX2"
                :y2="tick.y"
                class="sr-grid-line"
              />
              <text
                :x="C_ML - 8"
                :y="tick.y + 4"
                text-anchor="end"
                class="sr-tick-label"
              >{{ tick.v }}%</text>
            </g>

            <!-- Series -->
            <g v-for="s in svgChart.series" :key="s.name">
              <polyline
                :points="s.polyline"
                fill="none"
                :stroke="s.color"
                stroke-width="2.5"
                stroke-linejoin="round"
              />
              <g v-for="(pt, pi) in s.pts" :key="pi">
                <circle :cx="pt.x" :cy="pt.y" r="5" :fill="s.color" />
                <text
                  :x="pt.x"
                  :y="pt.y - 12"
                  text-anchor="middle"
                  class="sr-data-label"
                >{{ pt.r }}%</text>
              </g>
            </g>

            <!-- X axis tick labels (rotated -45°) -->
            <g v-for="xl in svgChart.xLabels" :key="xl.t">
              <text
                :x="xl.x"
                :y="xl.y"
                text-anchor="middle"
                :transform="`rotate(-45, ${xl.x}, ${xl.y})`"
                class="sr-tick-label"
              >{{ xl.label }}</text>
            </g>

            <!-- X axis title -->
            <text
              :x="C_ML + C_PW / 2"
              :y="CHART_H - 10"
              text-anchor="middle"
              class="sr-axis-label"
            >5mintime: Minute</text>
          </svg>
        </div>
      </div>
    </section>

    <section class="chart-table">
      <article class="panel throughput">
        <header>
          <div>
            <p class="eyebrow">{{ data.throughput.label }}</p>
            <h3>吞吐趋势</h3>
          </div>
          <div class="throughput-summary">
            <p>峰值 {{ data.throughput.peak }}</p>
            <p>{{ data.throughput.imbalance }}</p>
          </div>
        </header>
        <div class="throughput-bars">
          <div v-for="point in data.throughput.entries" :key="point.time" class="bar-row">
            <span class="time">{{ point.time }}</span>
            <div class="bar">
              <span class="payin" :style="{ width: `${(point.payin / 10000) * 100}%` }" />
              <span class="payout" :style="{ width: `${(point.payout / 10000) * 100}%` }" />
            </div>
            <span class="value">{{ point.payin.toLocaleString() }}/{{ point.payout.toLocaleString() }}</span>
          </div>
        </div>
      </article>

      <article class="panel channel-table">
        <header>
          <p class="eyebrow">重点通道</p>
          <h3>健康概览</h3>
        </header>
        <table>
          <thead>
            <tr>
              <th>通道</th>
              <th>区域</th>
              <th>延迟</th>
              <th>成功率</th>
              <th>量级</th>
              <th>趋势</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="channel in data.channels" :key="channel.id">
              <td>
                <p class="channel-name">{{ channel.name }}</p>
              </td>
              <td>{{ channel.country }}</td>
              <td>{{ channel.latency }}</td>
              <td>{{ channel.success }}</td>
              <td>{{ channel.volume }}</td>
              <td>{{ channel.trend }}</td>
              <td>
                <span class="status-chip" :class="channel.status">
                  {{ getStatusLabel(channel.status) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </article>
    </section>

    <section class="lower-grid">
      <article class="panel reroute">
        <header>
          <p class="eyebrow">路由策略</p>
          <h3>临时方案</h3>
        </header>
        <div class="reroute-list">
          <div v-for="plan in data.reroutePlans" :key="plan.id" class="reroute-row">
            <div>
              <p class="title">{{ plan.title }}</p>
              <p class="detail">{{ plan.detail }}</p>
            </div>
            <div class="meta">
              <span>{{ plan.owner }}</span>
              <span>{{ plan.eta }}</span>
            </div>
          </div>
        </div>
      </article>

      <article class="panel provider-load">
        <header>
          <p class="eyebrow">服务商负载</p>
          <h3>实时负荷</h3>
        </header>
        <div class="load-list">
          <div v-for="item in data.providerLoad" :key="item.id" class="load-row">
            <span class="provider">{{ item.provider }}</span>
            <div class="load-progress">
              <span :style="{ width: `${item.load}%` }" />
            </div>
            <span class="load-status">{{ item.status }}</span>
          </div>
        </div>
      </article>

      <article class="panel alerts">
        <header>
          <p class="eyebrow">异动告警</p>
          <h3>实时处理</h3>
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

      <article class="panel checklist">
        <header>
          <p class="eyebrow">操作清单</p>
          <h3>实时追踪</h3>
        </header>
        <ul>
          <li v-for="item in data.checklist" :key="item.id">
            <div>
              <p class="title">{{ item.title }}</p>
              <p class="meta">{{ item.time }} · {{ item.owner }}</p>
            </div>
            <button type="button">查看</button>
          </li>
        </ul>
      </article>
    </section>
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
  background: linear-gradient(135deg, rgba(73, 94, 255, 0.6), rgba(12, 18, 36, 0.9));
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

.hero h2 {
  margin: 10px 0 8px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-chip {
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  padding: 6px 16px;
  cursor: pointer;
}

.filter-chip.active {
  background: rgba(127, 133, 249, 0.2);
  color: #fff;
}

.quick-stats {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.error-text {
  color: #ff8e8e;
  margin-bottom: 8px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.02);
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  margin: 8px 0 4px;
}

.stat-meta {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.chart-table {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
}

.throughput header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.throughput-bars {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bar-row {
  display: grid;
  grid-template-columns: 60px 1fr 120px;
  gap: 12px;
  align-items: center;
}

.bar {
  position: relative;
  height: 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
  display: flex;
  gap: 4px;
}

.bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
}

.bar .payin {
  background: rgba(142, 136, 255, 0.9);
}

.bar .payout {
  background: rgba(111, 207, 255, 0.9);
}

.channel-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.channel-table th {
  text-align: left;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.6);
  padding: 8px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.channel-table td {
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.85);
}

.channel-name {
  font-weight: 600;
  margin: 0;
}

.status-chip {
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
}

.status-chip.green {
  background: rgba(34, 197, 94, 0.18);
  color: #4ade80;
}

.status-chip.amber {
  background: rgba(251, 191, 36, 0.2);
  color: #fbbf24;
}

.status-chip.red {
  background: rgba(248, 113, 113, 0.2);
  color: #f87171;
}

.lower-grid {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.reroute-row,
.alert-row {
  display: flex;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.reroute-row .meta {
  text-align: right;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.provider {
  width: 140px;
}

.load-row {
  display: grid;
  grid-template-columns: 140px 1fr 50px;
  gap: 12px;
  align-items: center;
  padding: 8px 0;
}

.load-progress {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.load-progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(120deg, rgba(141, 139, 255, 0.7), rgba(92, 226, 255, 0.7));
}

.alert-list .severity {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
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

.checklist ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.checklist li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
}

.checklist button {
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: transparent;
  color: #fff;
  border-radius: 999px;
  padding: 4px 12px;
}

@media (max-width: 960px) {
  .bar-row {
    grid-template-columns: 50px 1fr;
    grid-template-rows: auto auto;
  }

  .bar-row .value {
    grid-column: 1 / -1;
  }
}

/* ── 5-min Success Rate Chart ───────────────────────────────────── */
.success-rate-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sr-filter-bar {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.sr-filter-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 180px;
}

.sr-filter-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 0.05em;
}

.sr-filter-field {
  border: 1.5px solid rgba(127, 133, 249, 0.5);
  border-radius: 8px;
  overflow: hidden;
}

.sr-select {
  width: 100%;
  background: transparent;
  border: none;
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  padding: 8px 14px;
  cursor: pointer;
  outline: none;
}

.sr-chart-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sr-chart-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.sr-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.sr-legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
}

.sr-legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.sr-state {
  padding: 20px 0;
}

.sr-svg-container {
  width: 100%;
  overflow-x: hidden;
}

.sr-svg {
  width: 100%;
  height: auto;
  display: block;
}

.sr-grid-line {
  stroke: rgba(255, 255, 255, 0.12);
  stroke-width: 1;
  stroke-dasharray: 6 4;
}

.sr-tick-label {
  fill: rgba(255, 255, 255, 0.55);
  font-size: 11px;
}

.sr-axis-label {
  fill: rgba(255, 255, 255, 0.45);
  font-size: 11px;
}

.sr-data-label {
  fill: rgba(255, 255, 255, 0.9);
  font-size: 10px;
  font-weight: 500;
}
</style>
