<script setup>
const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});

const statusMap = {
  green: '正常',
  amber: '波动',
  red: '异常'
};

const severityClassMap = {
  高: 'severity-high',
  中: 'severity-medium',
  低: 'severity-low'
};

const getStatusLabel = (status) => statusMap[status] ?? '未知';
const getSeverityClass = (severity) => severityClassMap[severity] ?? 'severity-low';
</script>

<template>
  <div class="bankid-view">
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

    <section class="flows-channels">
      <article class="panel flows">
        <header>
          <p class="eyebrow">链路健康</p>
          <h3>真实认证链路</h3>
        </header>
        <table>
          <thead>
            <tr>
              <th>链路</th>
              <th>成功率</th>
              <th>延迟</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="flow in data.flows" :key="flow.id">
              <td>{{ flow.name }}</td>
              <td>{{ flow.success }}</td>
              <td>{{ flow.latency }}</td>
              <td>
                <span class="status-chip" :class="flow.status">{{ getStatusLabel(flow.status) }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </article>

      <article class="panel channels">
        <header>
          <p class="eyebrow">区段请求</p>
          <h3>区域贡献</h3>
        </header>
        <ul>
          <li v-for="channel in data.channels" :key="channel.id">
            <div>
              <p class="label">{{ channel.country }}</p>
              <p class="meta">{{ channel.operator }}</p>
            </div>
            <div class="value">
              {{ channel.requests }}
              <span>{{ channel.trend }}</span>
            </div>
          </li>
        </ul>
      </article>
    </section>

    <section class="panel sessions">
      <header>
        <p class="eyebrow">商户视角</p>
        <h3>实时会话</h3>
      </header>
      <table>
        <thead>
          <tr>
            <th>商户</th>
            <th>区域</th>
            <th>OTP</th>
            <th>Auth</th>
            <th>Fallback</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="session in data.sessions" :key="session.id">
            <td>{{ session.merchant }}</td>
            <td>{{ session.region }}</td>
            <td>{{ session.otp }}</td>
            <td>{{ session.auth }}</td>
            <td>{{ session.fallback }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="incidents-backlog">
      <article class="panel incidents">
        <header>
          <p class="eyebrow">异常事件</p>
          <h3>即刻关注</h3>
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
      <article class="panel backlog">
        <header>
          <p class="eyebrow">迭代事项</p>
          <h3>进度</h3>
        </header>
        <div class="backlog-list">
          <div v-for="task in data.backlog" :key="task.id" class="backlog-row">
            <div>
              <p class="title">{{ task.title }}</p>
              <p class="meta">{{ task.owner }} · {{ task.eta }}</p>
            </div>
            <div class="progress">
              <span :style="{ width: `${task.progress}%` }" />
            </div>
            <span class="percent">{{ task.progress }}%</span>
          </div>
        </div>
      </article>
    </section>

    <section class="panel notices">
      <header>
        <p class="eyebrow">提醒</p>
        <h3>重要同步</h3>
      </header>
      <ul>
        <li v-for="notice in data.notices" :key="notice.id">{{ notice.text }}</li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.bankid-view {
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
  background: linear-gradient(135deg, rgba(74, 95, 255, 0.65), rgba(11, 18, 34, 0.95));
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

.flows-channels {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
}

.flows table,
.sessions table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 12px;
}

.flows th,
.sessions th {
  text-align: left;
  padding: 8px 0;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.flows td,
.sessions td {
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
  background: rgba(251, 191, 36, 0.22);
  color: #fbbf24;
}

.status-chip.red {
  background: rgba(248, 113, 113, 0.2);
  color: #f87171;
}

.channels ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.channels li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.channels .value span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-left: 8px;
}

.sessions {
  overflow-x: auto;
}

.incidents-backlog {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.incident-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
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

.backlog-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.backlog-row {
  display: grid;
  grid-template-columns: 1fr 1fr 60px;
  gap: 12px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.backlog-row .progress {
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.backlog-row .progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(120deg, rgba(139, 143, 255, 0.8), rgba(97, 229, 255, 0.8));
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
