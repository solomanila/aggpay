<script setup>
const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});
</script>

<template>
  <div class="rollback-view">
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
        <h3>任务状态</h3>
      </header>
      <div class="chips">
        <button v-for="filter in data.filters" :key="filter" type="button">{{ filter }}</button>
      </div>
    </section>

    <section class="panel tasks-table">
      <header>
        <p class="eyebrow">回滚任务</p>
        <h3>最新记录</h3>
      </header>
      <table>
        <thead>
          <tr>
            <th>商户</th>
            <th>通道</th>
            <th>金额</th>
            <th>模式</th>
            <th>状态</th>
            <th>Owner</th>
            <th>更新时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="task in data.tasks" :key="task.id">
            <td>{{ task.merchant }}</td>
            <td>{{ task.channel }}</td>
            <td>{{ task.amount }}</td>
            <td>{{ task.mode }}</td>
            <td>{{ task.status }}</td>
            <td>{{ task.owner }}</td>
            <td>{{ task.updated }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="approvals-timeline">
      <article class="panel approvals">
        <header>
          <p class="eyebrow">审批队列</p>
          <h3>待处理</h3>
        </header>
        <div class="approval-list">
          <div v-for="approval in data.approvals" :key="approval.id" class="approval-row">
            <div>
              <p class="title">{{ approval.title }}</p>
              <p class="meta">{{ approval.owner }}</p>
            </div>
            <span class="eta">{{ approval.eta }}</span>
          </div>
        </div>
      </article>

      <article class="panel timeline">
        <header>
          <p class="eyebrow">时间线</p>
          <h3>关键节点</h3>
        </header>
        <ul>
          <li v-for="item in data.timeline" :key="item.id">
            <div class="time">{{ item.time }}</div>
            <div>
              <p class="title">{{ item.title }}</p>
              <p class="meta">{{ item.owner }}</p>
            </div>
          </li>
        </ul>
      </article>
    </section>
  </div>
</template>

<style scoped>
.rollback-view {
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
  background: linear-gradient(135deg, rgba(75, 95, 255, 0.65), rgba(11, 16, 32, 0.95));
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

.tasks-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 12px;
}

.tasks-table th {
  text-align: left;
  padding: 8px 0;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.tasks-table td {
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.approvals-timeline {
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

.timeline ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline li {
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
  display: flex;
  gap: 12px;
}

.timeline .time {
  width: 60px;
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
