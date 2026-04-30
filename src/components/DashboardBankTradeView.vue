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

const statusLabelMap = {
  green: '正常',
  amber: '波动',
  red: '异常'
};

const getSeverityClass = (severity) => severityClassMap[severity] ?? 'severity-low';
const getStatusLabel = (status) => statusLabelMap[status] ?? '未知';
</script>

<template>
  <div class="banktrade-view">
    <section class="panel hero gradient">
      <div>
        <p class="eyebrow">{{ data.hero.eyebrow }}</p>
        <h2>{{ data.hero.title }}</h2>
        <p class="muted">{{ data.hero.description }}</p>
      </div>
        <p class="muted">{{ data.hero.sync }}</p>
    </section>

    <section class="stat-grid">
      <article v-for="stat in data.stats" :key="stat.id" class="panel stat-card">
        <p class="label">{{ stat.label }}</p>
        <p class="value">{{ stat.value }}</p>
        <p class="meta">{{ stat.meta }}</p>
      </article>
    </section>

    <section class="health-queue">
      <article class="panel service-health">
        <header>
          <p class="eyebrow">服务健康</p>
          <h3>接口监控</h3>
        </header>
        <table>
          <thead>
            <tr>
              <th>服务</th>
              <th>状态</th>
              <th>延迟</th>
              <th>成功率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="service in data.serviceHealth" :key="service.id">
              <td>{{ service.name }}</td>
              <td>
                <span class="status-chip" :class="service.status">
                  {{ getStatusLabel(service.status) }}
                </span>
              </td>
              <td>{{ service.latency }}</td>
              <td>{{ service.success }}</td>
            </tr>
          </tbody>
        </table>
      </article>

      <article class="panel queues">
        <header>
          <p class="eyebrow">调度队列</p>
          <h3>实时深度</h3>
        </header>
        <ul>
          <li v-for="queue in data.queues" :key="queue.id">
            <div>
              <p class="label">{{ queue.name }}</p>
              <p class="value">{{ queue.depth }}</p>
            </div>
            <span class="meta">{{ queue.wait }}</span>
          </li>
        </ul>
      </article>
    </section>

    <section class="panel throughput">
      <header>
        <p class="eyebrow">吞吐</p>
        <h3>5 分钟窗口</h3>
      </header>
      <div class="bar-grid">
        <div v-for="point in data.throughput" :key="point.time" class="bar-row">
          <span class="time">{{ point.time }}</span>
          <div class="bars">
            <span class="success" :style="{ width: `${(point.success / 9000) * 100}%` }" />
            <span class="failed" :style="{ width: `${(point.failed / 9000) * 100}%` }" />
          </div>
          <span class="values">{{ point.success }}/{{ point.failed }}</span>
        </div>
      </div>
    </section>

    <section class="incidents-release">
      <article class="panel incidents">
        <header>
          <p class="eyebrow">告警</p>
          <h3>关键事件</h3>
        </header>
        <div class="incident-list">
          <div v-for="incident in data.incidents" :key="incident.id" class="incident-row">
            <span class="severity" :class="getSeverityClass(incident.severity)">{{ incident.severity }}</span>
            <div>
              <p class="title">{{ incident.title }}</p>
              <p class="detail">{{ incident.detail }}</p>
              <p class="action">{{ incident.action }}</p>
            </div>
          </div>
        </div>
      </article>

      <article class="panel release">
        <header>
          <p class="eyebrow">变更时间线</p>
          <h3>今日操作</h3>
        </header>
        <ul>
          <li v-for="item in data.releaseTimeline" :key="item.id">
            <span class="time">{{ item.time }}</span>
            <div>
              <p class="title">{{ item.title }}</p>
              <p class="owner">{{ item.owner }}</p>
            </div>
          </li>
        </ul>
      </article>
    </section>

    <section class="ops-grid">
      <article class="panel integrations">
        <header>
          <p class="eyebrow">对接任务</p>
          <h3>进度跟踪</h3>
        </header>
        <div class="integration-list">
          <div v-for="integration in data.integrations" :key="integration.id" class="integration-row">
            <div>
              <p class="title">{{ integration.title }}</p>
              <p class="owner">{{ integration.owner }} · {{ integration.eta }}</p>
            </div>
            <div class="progress">
              <span :style="{ width: `${integration.progress}%` }" />
            </div>
            <span class="percent">{{ integration.progress }}%</span>
          </div>
        </div>
      </article>

      <article class="panel notices">
        <header>
          <p class="eyebrow">银行侧通知</p>
          <h3>同步提醒</h3>
        </header>
        <ul>
          <li v-for="notice in data.notices" :key="notice.id">{{ notice.text }}</li>
        </ul>
      </article>
    </section>
  </div>
</template>

<style scoped>
.banktrade-view {
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
  background: linear-gradient(135deg, rgba(64, 98, 255, 0.65), rgba(14, 16, 33, 0.9));
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

.health-queue {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
}

.service-health table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 12px;
}

.service-health th {
  text-align: left;
  padding: 8px 0;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.service-health td {
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
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

.queues ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.queues li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.throughput .bar-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.bar-row {
  display: grid;
  grid-template-columns: 60px 1fr 120px;
  gap: 12px;
  align-items: center;
}

.bars {
  display: flex;
  gap: 4px;
  height: 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.bars span {
  display: block;
  border-radius: inherit;
}

.bars .success {
  background: rgba(120, 214, 255, 0.9);
}

.bars .failed {
  background: rgba(248, 113, 113, 0.9);
}

.incidents-release {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.incident-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.incident-row {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
}

.severity {
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

.release ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.release li {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
}

.release .time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  width: 60px;
}

.ops-grid {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.integration-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.integration-row {
  display: grid;
  grid-template-columns: 1fr 1fr 60px;
  gap: 12px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.integration-row .progress {
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.integration-row .progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(120deg, rgba(130, 147, 255, 0.8), rgba(95, 228, 255, 0.8));
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
  .hero {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
