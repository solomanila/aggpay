<script setup>
const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});
</script>

<template>
  <div class="ifsc-view">
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
        <h3>条目状态</h3>
      </header>
      <div class="chips">
        <button v-for="filter in data.filters" :key="filter" type="button">{{ filter }}</button>
      </div>
    </section>

    <section class="panel record-table">
      <header>
        <p class="eyebrow">黑名单记录</p>
        <h3>最新条目</h3>
      </header>
      <table>
        <thead>
          <tr>
            <th>账户</th>
            <th>原因</th>
            <th>状态</th>
            <th>Owner</th>
            <th>录入时间</th>
            <th>更新时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="record in data.records" :key="record.id">
            <td>{{ record.account }}</td>
            <td>{{ record.reason }}</td>
            <td>{{ record.status }}</td>
            <td>{{ record.owner }}</td>
            <td>{{ record.created }}</td>
            <td>{{ record.updated }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="actions-audits">
      <article class="panel actions">
        <header>
          <p class="eyebrow">执行动作</p>
          <h3>进度</h3>
        </header>
        <div class="action-list">
          <div v-for="action in data.actions" :key="action.id" class="action-row">
            <div>
              <p class="title">{{ action.title }}</p>
              <p class="meta">{{ action.owner }} · {{ action.eta }}</p>
            </div>
            <div class="progress">
              <span :style="{ width: `${action.progress}%` }" />
            </div>
            <span class="percent">{{ action.progress }}%</span>
          </div>
        </div>
      </article>
      <article class="panel audits">
        <header>
          <p class="eyebrow">审计追踪</p>
          <h3>系统记录</h3>
        </header>
        <ul>
          <li v-for="audit in data.audits" :key="audit.id">
            <div class="time">{{ audit.time }}</div>
            <div>
              <p class="title">{{ audit.title }}</p>
              <p class="meta">{{ audit.owner }}</p>
            </div>
          </li>
        </ul>
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
.ifsc-view {
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

.record-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 12px;
}

.record-table th {
  text-align: left;
  padding: 8px 0;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.record-table td {
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.actions-audits {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.action-row {
  display: grid;
  grid-template-columns: 1fr 1fr 60px;
  gap: 12px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.action-row .progress {
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.action-row .progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(120deg, rgba(126, 143, 255, 0.8), rgba(99, 228, 255, 0.8));
}

.audits ul,
.notices ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.audits li,
.notices li {
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
  display: flex;
  gap: 12px;
  align-items: center;
}

.audits .time {
  width: 60px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.playbooks button {
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  background: transparent;
  color: #fff;
  padding: 4px 12px;
}

@media (max-width: 960px) {
  .hero {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
