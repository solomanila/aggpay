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

defineProps({
  data: { type: Object, required: true }
});

// ── Filters ──────────────────────────────────────────────────────
const chartDate = ref('today');
const chartCurrency = ref('INR');
const chartType = ref('all'); // 'all' | 'payin' | 'payout'

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

// 全部不传 test；payin=0；payout=1
const TYPE_TEST = { all: null, payin: 0, payout: 1 };

// ── Chart data ───────────────────────────────────────────────────
const chartLoading = ref(false);
const chartError = ref('');
const chartRawData = ref([]);

// 每个 date 选项对应的窗口间隔（秒）和最大槽数
const DATE_CONFIG = {
  today:     { intervalSec: 300, slots: 24  },
  yesterday: { intervalSec: 300, slots: 24  },
  '6hours':  { intervalSec: 150, slots: 144 },
  '1hour':   { intervalSec: 25,  slots: 144 },
  '5mins':   { intervalSec: 5,   slots: 60  },
};

const currentConfig = computed(() => DATE_CONFIG[chartDate.value] ?? DATE_CONFIG.today);

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
    const params = { date: chartDate.value };
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
  await fetchChartData();
});
</script>

<template>
  <div class="channel-view">

    <!-- ── Type tabs ─────────────────────────────────────────────── -->
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

    <!-- ── Success Rate Chart ─────────────────────────────────────── -->
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

            <g v-for="xl in svgChart.xLabels" :key="xl.t">
              <text
                :x="xl.x"
                :y="xl.y"
                text-anchor="middle"
                :transform="`rotate(-45, ${xl.x}, ${xl.y})`"
                class="sr-tick-label"
              >{{ xl.label }}</text>
            </g>

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

  </div>
</template>

<style scoped>
.channel-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

/* ── Panel ──────────────────────────────────────────────────────── */
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
  color: #ff8e8e;
  margin-bottom: 8px;
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
