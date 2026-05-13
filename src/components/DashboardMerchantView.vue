<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import http from '../services/http';

defineProps({
  data: { type: Object, required: true }
});

// ── PayIn Link Open Rate ─────────────────────────────────────────
const orCurrency = ref('INR');
const orMinutes = ref(30);
const orMinutesInput = ref('30');
const orLoading = ref(false);
const orError = ref('');
const orRows = ref([]);

const orSortKey = ref('orderNum');
const orSortDir = ref(-1);

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
  if (!Number.isNaN(n) && n > 0) orMinutes.value = n;
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

// ── Success Rate Chart (merchant=NOT NULL) ───────────────────────
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

const chartDate = ref('today');
const chartCurrency = ref('INR');
const chartType = ref('all');

const dateOptions = [
  { label: 'Today',     value: 'today' },
  { label: 'Yesterday', value: 'yesterday' },
  { label: '过去6小时', value: '6hours' },
  { label: '过去1小时', value: '1hour' },
  { label: '过去5分钟', value: '5mins' },
];

const typeOptions = [
  { label: '全部',   value: 'all' },
  { label: 'payin',  value: 'payin' },
  { label: 'payout', value: 'payout' },
];

const TYPE_TEST = { all: null, payin: 0, payout: 1 };

const DATE_CONFIG = {
  today:     { intervalSec: 300, slots: 24  },
  yesterday: { intervalSec: 300, slots: 24  },
  '6hours':  { intervalSec: 150, slots: 144 },
  '1hour':   { intervalSec: 25,  slots: 144 },
  '5mins':   { intervalSec: 5,   slots: 60  },
};

const currentConfig = computed(() => DATE_CONFIG[chartDate.value] ?? DATE_CONFIG.today);

const chartLoading = ref(false);
const chartError = ref('');
const chartRawData = ref([]);

const parseWindowTime = (wt) => {
  const [datePart, timePart] = wt.split(', ');
  const [y, mo, d] = datePart.split('-').map(Number);
  const [h, mi, sec = 0] = timePart.split(':').map(Number);
  return new Date(y, mo - 1, d, h, mi, sec, 0);
};

const fmtWindowTime = (dt) => {
  const h  = String(dt.getHours()).padStart(2, '0');
  const mi = String(dt.getMinutes()).padStart(2, '0');
  const s  = String(dt.getSeconds()).padStart(2, '0');
  return `${dt.getFullYear()}-${dt.getMonth() + 1}-${dt.getDate()}, ${h}:${mi}:${s}`;
};

const chartSeries = computed(() => {
  const map = new Map();
  for (const p of chartRawData.value) {
    if (!map.has(p.channelName)) map.set(p.channelName, []);
    map.get(p.channelName).push(p);
  }
  return [...map.entries()].map(([name, pts]) => ({ name, pts }));
});

const chartTimes = computed(() => {
  if (!chartRawData.value.length) return [];
  const cfg = currentConfig.value;
  const maxTs = Math.max(
    ...chartRawData.value.map(p => parseWindowTime(p.windowTime).getTime())
  );
  const anchor = new Date(maxTs);
  const slots = [];
  for (let i = cfg.slots - 1; i >= 0; i--) {
    slots.push(fmtWindowTime(new Date(anchor.getTime() - i * cfg.intervalSec * 1000)));
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
    const params = { date: chartDate.value, merchant: 'NOT NULL' };
    const testVal = TYPE_TEST[chartType.value];
    if (testVal !== null) params.test = testVal;
    const { data: response } = await http.get('/admin/pay/dashboard/channelSuccessRate', { params });
    chartRawData.value = response?.data ?? [];
  } catch (error) {
    console.error('Failed to load channel success rate', error);
    chartError.value = '无法加载成功率数据';
    chartRawData.value = [];
  } finally {
    chartLoading.value = false;
  }
};

watch([chartDate, chartType], fetchChartData);

onMounted(async () => {
  fetchOpenRate();
  await fetchChartData();
});
</script>

<template>
  <div class="merchant-view">

    <!-- ── PayIn Link Open Rate ──────────────────────────────────── -->
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

    <!-- ── 商户成功率曲线（merchant=NOT NULL）────────────────────── -->
    <div class="sr-type-tabs">
      <button
        v-for="t in typeOptions"
        :key="t.value"
        class="sr-type-tab"
        :class="{ active: chartType === t.value }"
        type="button"
        @click="chartType = t.value"
      >{{ t.label }}</button>
    </div>

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
            <text
              x="18"
              :y="C_MT + C_PH / 2"
              text-anchor="middle"
              :transform="`rotate(-90, 18, ${C_MT + C_PH / 2})`"
              class="sr-axis-label"
            >successRate</text>

            <g v-for="tick in svgChart.yTicks" :key="tick.v">
              <line
                :x1="svgChart.gridX1" :y1="tick.y"
                :x2="svgChart.gridX2" :y2="tick.y"
                class="sr-grid-line"
              />
              <text :x="C_ML - 8" :y="tick.y + 4" text-anchor="end" class="sr-tick-label">
                {{ tick.v }}%
              </text>
            </g>

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
                <text :x="pt.x" :y="pt.y - 12" text-anchor="middle" class="sr-data-label">
                  {{ pt.r }}%
                </text>
              </g>
            </g>

            <g v-for="xl in svgChart.xLabels" :key="xl.t">
              <text
                :x="xl.x" :y="xl.y"
                text-anchor="middle"
                :transform="`rotate(-45, ${xl.x}, ${xl.y})`"
                class="sr-tick-label"
              >{{ xl.label }}</text>
            </g>

            <text
              :x="C_ML + C_PW / 2" :y="CHART_H - 10"
              text-anchor="middle"
              class="sr-axis-label"
            >5mintime: Minute</text>
          </svg>
        </div>
      </div>
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

.muted {
  color: rgba(255, 255, 255, 0.7);
}

.error-text {
  color: #f87171;
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
  background: #1a1c2e;
  border: none;
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  padding: 8px 14px;
  outline: none;
  min-width: 0;
}

.or-select option {
  background: #1a1c2e;
  color: #fff;
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

.or-th:hover { color: rgba(255, 255, 255, 0.85); }

.or-th--active { color: rgba(177, 185, 249, 0.9); }

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

.or-row:last-child .or-td { border-bottom: none; }

.or-empty {
  padding: 24px;
  text-align: center;
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
}

/* ── Type tabs ──────────────────────────────────────────────────── */
.sr-type-tabs {
  display: flex;
  gap: 8px;
}

.sr-type-tab {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.65);
  padding: 6px 22px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.18s, border-color 0.18s, color 0.18s;
}

.sr-type-tab:hover {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.25);
}

.sr-type-tab.active {
  background: rgba(127, 133, 249, 0.2);
  border-color: rgba(127, 133, 249, 0.5);
  color: #fff;
}

/* ── Success Rate Chart ─────────────────────────────────────────── */
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
  background: #1a1c2e;
  border: none;
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  padding: 8px 14px;
  cursor: pointer;
  outline: none;
}

.sr-select option {
  background: #1a1c2e;
  color: #fff;
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
