<script setup>
const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});

const statusLabelMap = {
  green: '正常',
  amber: '波动',
  red: '异常'
};

const severityClassMap = {
  高: 'severity-high',
  中: 'severity-medium',
  低: 'severity-low'
};

const getStatusLabel = (status) => statusLabelMap[status] ?? '未知';
const getSeverityClass = (severity) => severityClassMap[severity] ?? 'severity-low';
</script>

<template>
  <div class="bankmonitor-view">
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

    <section class="banks-thresholds">
      <article class="panel banks">
        <header>
          <p class="eyebrow">银行列表</p>
          <h3>实时概览</h3>
        </header>
        <table>
          <thead>
            <tr>
              <th>银行</th>
              <th>区域</th>
              <th>延迟</th>
              <th>状态</th>
              <th>告警</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="bank in data.banks" :key="bank.id">
              <td>{{ bank.name }}</td>
              <td>{{ bank.region }}</td>
              <td>{{ bank.latency }}</td>
              <td>
                <span class="status-chip" :class="bank.status">{{ getStatusLabel(bank.status) }}</span>
              </td>
              <td>{{ bank.incidents }}</td>
            </tr>
          </tbody>
        </table>
      </article>

      <article class="panel thresholds">
        <header>
          <p class="eyebrow">策略阈值</p>
          <h3>联机动作</h3>
        </header>
        <div class="threshold-list">
          <div v-for="threshold in data.thresholds" :key="threshold.id" class="threshold-row">
            <div>
              <p class="label">{{ threshold.label }}</p>
              <p class="value">{{ threshold.value }}</p>
            </div>
            <span class="action">{{ threshold.action }}</span>
          </div>
        </div>
      </article>
    </section>

    <section class="alerts-maintenance">
      <article class="panel alerts">
        <header>
          <p class="eyebrow">告警</p>
          <h3>高优任务</h3>
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

      <article class="panel maintenance">
        <header>
          <p class="eyebrow">维护窗口</p>
          <h3>银行计划</h3>
        </header>
        <ul>
          <li v-for="item in data.maintenance" :key="item.id">
            <div>
              <p class="label">{{ item.bank }}</p>
              <p class="meta">{{ item.note }}</p>
            </div>
            <span class="window">{{ item.window }}</span>
          </li>
        </ul>
      </article>
    </section>

    <section class="automation-notices">
      <article class="panel automation">
        <header>
          <p class="eyebrow">自动化</p>
          <h3>执行进度</h3>
        </header>
        <div class="automation-list">
          <div v-for="item in data.automation" :key="item.id" class="automation-row">
            <div>
              <p class="title">{{ item.title }}</p>
              <p class="meta">{{ item.owner }} · {{ item.eta }}</p>
            </div>
            <div class="progress">
              <span :style="{ width: `${item.progress}%` }" />
            </div>
            <span class="percent">{{ item.progress }}%</span>
          </div>
        </div>
      </article>
      <article class="panel notices">
        <header>
          <p class="eyebrow">提醒</p>
          <h3>同步消息</h3>
        </header>
        <ul>
          <li v-for="notice in data.notices" :key="notice.id">{{ notice.text }}</li>
        </ul>
      </article>
    </section>
  </div>
</template>

<style scoped>
.bankmonitor-view {
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
  background: linear-gradient(135deg, rgba(71, 96, 255, 0.65), rgba(12, 17, 33, 0.92));
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

.banks-thresholds {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.banks table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 12px;
}

.banks th {
  text-align: left;
  padding: 8px 0;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.banks td {
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

.threshold-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.threshold-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
}

.alerts-maintenance {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-row {
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

.maintenance ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.maintenance li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
}

.automation-notices {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.automation-list {
  display: flex;
  flex-direction:column;
  gap: 12px;
}

.automation-row {
  display: grid;
  grid-template-columns: 1fr 1fr 60px;
  gap: 12px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.automation-row .progress {
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.automation-row .progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(120deg, rgba(126, 142, 255, 0.8), rgba(95, 228, 255, 0.85));
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
