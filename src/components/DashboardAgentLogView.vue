<script setup>
const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});
</script>

<template>
  <div class="agent-log-view">
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

    <section class="panel filters">
      <header>
        <p class="eyebrow">筛选</p>
        <h3>操作类别</h3>
      </header>
      <div class="chips">
        <button v-for="filter in data.filters" :key="filter" type="button">{{ filter }}</button>
      </div>
    </section>

    <section class="panel operations">
      <header>
        <p class="eyebrow">最新操作</p>
        <h3>实时日志</h3>
      </header>
      <table>
        <thead>
          <tr>
            <th>代理账户</th>
            <th>类型</th>
            <th>金额 / 动作</th>
            <th>状态</th>
            <th>操作人</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="operation in data.operations" :key="operation.id">
            <td>{{ operation.agent }}</td>
            <td>{{ operation.type }}</td>
            <td>{{ operation.amount }}</td>
            <td>{{ operation.status }}</td>
            <td>{{ operation.operator }}</td>
            <td>{{ operation.time }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="approvals-insights">
      <article class="panel approvals">
        <header>
          <p class="eyebrow">审批流</p>
          <h3>执行进度</h3>
        </header>
        <div class="approval-list">
          <div v-for="approval in data.approvals" :key="approval.id" class="approval-row">
            <div>
              <p class="title">{{ approval.title }}</p>
              <p class="meta">{{ approval.stage }} · {{ approval.owner }}</p>
            </div>
            <span class="eta">{{ approval.eta }}</span>
          </div>
        </div>
      </article>

      <article class="panel insights">
        <header>
          <p class="eyebrow">洞察</p>
          <h3>趋势提示</h3>
        </header>
        <ul>
          <li v-for="insight in data.insights" :key="insight.id">{{ insight.text }}</li>
        </ul>
      </article>
    </section>

    <section class="panel timeline">
      <header>
        <p class="eyebrow">审计追踪</p>
        <h3>系统动作</h3>
      </header>
      <ul>
        <li v-for="audit in data.auditTimeline" :key="audit.id">
          <div class="time">{{ audit.time }}</div>
          <div>
            <p class="title">{{ audit.action }}</p>
            <p class="meta">{{ audit.owner }}</p>
          </div>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.agent-log-view {
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
  background: linear-gradient(135deg, rgba(83, 93, 255, 0.65), rgba(11, 16, 32, 0.95));
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

.filters .chips {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}

.filters button {
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 999px;
  background: transparent;
  color: rgba(255, 255, 255, 0.8);
  padding: 6px 16px;
}

.operations table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 12px;
}

.operations th {
  text-align: left;
  padding: 8px 0;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.operations td {
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.approvals-insights {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.approval-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.approval-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
}

.insights ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.82);
}

.timeline ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline li {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
}

.timeline .time {
  width: 64px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

@media (max-width: 960px) {
  .hero {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
