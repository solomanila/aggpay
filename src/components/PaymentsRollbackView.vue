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
        <h3>任务类型</h3>
      </header>
      <div class="chips">
        <button v-for="filter in data.filters" :key="filter" type="button">{{ filter }}</button>
      </div>
    </section>

    <section class="panel tasks">
      <header>
        <p class="eyebrow">回滚任务</p>
        <h3>进行中 & 完成</h3>
      </header>
      <table>
        <thead>
          <tr>
            <th>任务</th>
            <th>通道</th>
            <th>模式</th>
            <th>金额</th>
            <th>状态</th>
            <th>Owner</th>
            <th>更新时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="task in data.tasks" :key="task.id">
            <td>{{ task.title }}</td>
            <td>{{ task.channel }}</td>
            <td>{{ task.mode }}</td>
            <td>{{ task.amount }}</td>
            <td>{{ task.status }}</td>
            <td>{{ task.owner }}</td>
            <td>{{ task.updated }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="steps-approvals">
      <article class="panel steps">
        <header>
          <p class="eyebrow">执行指标</p>
          <h3>关键节点</h3>
        </header>
        <div class="step-cards">
          <div v-for="step in data.steps" :key="step.id" class="step-card">
            <p class="title">{{ step.title }}</p>
            <p class="value">{{ step.value }}</p>
            <p class="detail">{{ step.detail }}</p>
          </div>
        </div>
      </article>

      <article class="panel approvals">
        <header>
          <p class="eyebrow">审批流</p>
          <h3>待处理</h3>
        </header>
        <div class="approval-list">
          <div v-for="approval in data.approvals" :key="approval.id" class="approval-row">
            <div>
              <p class="title">{{ approval.title }}</p>
              <p class="meta">{{ approval.stage }} · {{ approval.owner }}</p>
            </div>
            <button type="button">查看</button>
          </div>
        </div>
      </article>
    </section>

    <section class="panel notices">
      <header>
        <p class="eyebrow">提醒</p>
        <h3>最新动态</h3>
      </header>
      <ul>
        <li v-for="notice in data.notices" :key="notice.id">{{ notice.text }}</li>
      </ul>
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
  background: linear-gradient(135deg, rgba(82, 95, 255, 0.68), rgba(12, 18, 34, 0.95));
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

.tasks table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 12px;
}

.tasks th {
  text-align: left;
  padding: 8px 0;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.tasks td {
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.steps-approvals {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.step-cards {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  margin-top: 12px;
}

.step-card {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.02);
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

.approval-row button {
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  background: transparent;
  color: #fff;
  padding: 4px 12px;
}

.notices ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.82);
}

@media (max-width: 960px) {
  .hero {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
