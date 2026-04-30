<script setup>
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
</script>

<template>
  <div class="dashboard-overview">
    <section class="overview-hero panel gradient">
      <div>
        <p class="eyebrow">{{ data.hero.eyebrow }}</p>
        <h2>{{ data.hero.title }}</h2>
        <p class="muted">{{ data.hero.description }}</p>
      </div>
      <div class="hero-sync">
        <p>{{ data.hero.sync.label }}</p>
        <strong>{{ data.hero.sync.value }}</strong>
      </div>
    </section>

    <section class="quick-stats">
      <article v-for="stat in data.quickStats" :key="stat.id" class="panel stat-card">
        <p class="stat-label">{{ stat.label }}</p>
        <p class="stat-value">{{ stat.value }}</p>
        <p class="stat-trend">
          <span>{{ stat.trend }}</span>
          <small>{{ stat.trendLabel }}</small>
        </p>
      </article>
    </section>

    <section class="snapshot-grid">
      <article class="panel health-panel">
        <header>
          <p class="eyebrow">运行健康</p>
          <h3>实时 SLA 追踪</h3>
        </header>
        <div class="metric-list">
          <div v-for="metric in data.healthMetrics" :key="metric.id" class="metric-row">
            <div>
              <p class="metric-label">{{ metric.label }}</p>
              <p class="metric-value">{{ metric.value }} · <span>{{ metric.target }}</span></p>
            </div>
            <p class="metric-trend">{{ metric.trend }}</p>
            <div class="metric-progress">
              <span :style="{ width: `${metric.progress}%` }" />
            </div>
          </div>
        </div>
        <div class="flow-breakdown">
          <p class="eyebrow">交易贡献</p>
          <ul>
            <li v-for="flow in data.flowBreakdown" :key="flow.id">
              <span>{{ flow.label }}</span>
              <span class="flow-value">
                {{ flow.value }}
                <small :class="`status-${flow.status}`">
                  {{ flow.status === 'up' ? '↑' : flow.status === 'down' ? '↓' : '·' }}
                </small>
              </span>
            </li>
          </ul>
        </div>
      </article>

      <article class="panel incident-panel">
        <header>
          <p class="eyebrow">关键告警</p>
          <h3>运维异动</h3>
        </header>
        <div class="incident-list">
          <div v-for="incident in data.incidents" :key="incident.id" class="incident-item">
            <span class="severity" :class="getSeverityClass(incident.severity)">{{ incident.severity }}</span>
            <div>
              <p class="incident-title">{{ incident.title }}</p>
              <p class="incident-detail">{{ incident.detail }}</p>
              <p class="incident-meta">
                {{ incident.owner }} · {{ incident.eta }}
              </p>
            </div>
          </div>
        </div>
        <div class="maintenance">
          <p class="eyebrow">计划任务</p>
          <div v-for="plan in data.maintenance" :key="plan.id" class="plan-row">
            <div>
              <p class="plan-label">{{ plan.label }}</p>
              <p class="plan-window">{{ plan.window }}</p>
            </div>
            <span class="plan-owner">{{ plan.owner }}</span>
          </div>
        </div>
      </article>

      <article class="panel region-panel">
        <header>
          <p class="eyebrow">区域性能</p>
          <h3>延迟热力</h3>
        </header>
        <ul class="region-list">
          <li v-for="region in data.regionalLatencies" :key="region.id">
            <div>
              <p class="region-name">{{ region.region }}</p>
              <p class="region-latency">{{ region.latency }}</p>
            </div>
            <span class="region-diff" :class="region.status">{{ region.diff }}</span>
          </li>
        </ul>
      </article>
    </section>

    <section class="ops-grid">
      <article class="panel timeline-panel">
        <header>
          <p class="eyebrow">处理记录</p>
          <h3>最近 1 小时</h3>
        </header>
        <ul class="timeline">
          <li v-for="node in data.timeline" :key="node.id">
            <span class="time">{{ node.time }}</span>
            <span class="dot" :class="node.type" />
            <p>{{ node.title }}</p>
          </li>
        </ul>
      </article>

      <article class="panel capacity-panel">
        <header>
          <p class="eyebrow">容量使用</p>
          <h3>{{ data.capacity.utilization }}</h3>
          <p class="capacity-change">{{ data.capacity.change }} · 环比</p>
        </header>
        <div class="capacity-bars">
          <div v-for="segment in data.capacity.segments" :key="segment.id" class="capacity-row">
            <span>{{ segment.label }}</span>
            <div class="capacity-progress">
              <span :style="{ width: `${segment.value}%` }" />
            </div>
            <strong>{{ segment.value }}%</strong>
          </div>
        </div>
        <div class="action-items">
          <p class="eyebrow">行动跟进</p>
          <div v-for="action in data.actionItems" :key="action.id" class="action-row">
            <div>
              <p class="action-title">{{ action.title }}</p>
              <p class="action-owner">{{ action.owner }}</p>
            </div>
            <div class="action-progress">
              <span :style="{ width: `${action.progress}%` }" />
            </div>
            <span class="action-percent">{{ action.progress }}%</span>
          </div>
        </div>
        <div class="notices">
          <p class="eyebrow">最新动态</p>
          <ul>
            <li v-for="notice in data.notices" :key="notice.id">
              {{ notice.title }}
            </li>
          </ul>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.dashboard-overview {
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
  background: linear-gradient(135deg, rgba(90, 103, 255, 0.65), rgba(16, 19, 33, 0.9));
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.2em;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
}

.muted {
  color: rgba(255, 255, 255, 0.7);
  margin-top: 12px;
}

.overview-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.overview-hero h2 {
  margin: 8px 0 0;
  font-size: 28px;
}

.hero-sync {
  text-align: right;
  color: rgba(255, 255, 255, 0.8);
}

.hero-sync strong {
  display: block;
  font-size: 18px;
  margin-top: 4px;
}

.quick-stats {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
}

.stat-card {
  background: rgba(255, 255, 255, 0.02);
}

.stat-label {
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  margin: 10px 0 4px;
}

.stat-trend {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.65);
  display: flex;
  gap: 6px;
  align-items: baseline;
}

.snapshot-grid {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.health-panel header,
.incident-panel header,
.region-panel header {
  margin-bottom: 16px;
}

.metric-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metric-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metric-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.metric-value span {
  color: rgba(255, 255, 255, 0.5);
}

.metric-trend {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.metric-progress {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.metric-progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(120deg, rgba(140, 140, 251, 0.6), rgba(104, 216, 255, 0.6));
}

.flow-breakdown {
  margin-top: 20px;
}

.flow-breakdown ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.flow-breakdown li {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.flow-value {
  display: flex;
  gap: 6px;
  align-items: center;
}

.flow-value small {
  font-size: 12px;
}

.status-up {
  color: #4ade80;
}

.status-down {
  color: #f87171;
}

.status-steady {
  color: rgba(255, 255, 255, 0.5);
}

.incident-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.incident-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.03);
}

.severity {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  text-transform: uppercase;
  align-self: flex-start;
}

.severity-high {
  background: rgba(244, 114, 182, 0.2);
  color: #f472b6;
}

.severity-medium {
  background: rgba(251, 191, 36, 0.2);
  color: #fbbf24;
}

.severity-low {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.incident-title {
  margin: 0;
  font-weight: 600;
}

.incident-detail,
.incident-meta {
  margin: 4px 0 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.65);
}

.maintenance .plan-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.plan-owner {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.region-list {
  list-style: none;
  padding: 0;
  margin: 16px 0 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.region-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.region-diff {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.region-diff.warning {
  background: rgba(251, 191, 36, 0.15);
  color: #fbbf24;
}

.region-diff.good {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.region-diff.critical {
  background: rgba(248, 113, 113, 0.15);
  color: #f87171;
}

.region-diff.steady {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
}

.ops-grid {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.timeline {
  list-style: none;
  margin: 16px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline li {
  display: grid;
  grid-template-columns: 64px 12px 1fr;
  gap: 12px;
  align-items: center;
}

.timeline .time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.timeline .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
}

.timeline .dot.success {
  background: #4ade80;
}

.timeline .dot.warning {
  background: #fbbf24;
}

.capacity-panel header {
  margin-bottom: 16px;
}

.capacity-change {
  color: rgba(255, 255, 255, 0.7);
}

.capacity-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.capacity-row {
  display: grid;
  grid-template-columns: 90px 1fr 50px;
  gap: 12px;
  align-items: center;
  font-size: 13px;
}

.capacity-progress {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.capacity-progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(120deg, rgba(127, 133, 249, 0.7), rgba(102, 212, 255, 0.7));
}

.action-items {
  margin-top: 24px;
}

.action-row {
  display: grid;
  grid-template-columns: 1fr 1fr 60px;
  gap: 12px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.action-owner {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.65);
}

.action-progress {
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.action-progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: rgba(148, 163, 255, 0.8);
}

.action-percent {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.notices {
  margin-top: 24px;
}

.notices ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

@media (max-width: 960px) {
  .overview-hero {
    flex-direction: column;
    gap: 16px;
  }

  .hero-sync {
    text-align: left;
  }
}
</style>
