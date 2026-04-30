<script setup>
const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});
</script>

<template>
  <div class="realtime-board">
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
        <h3>状态</h3>
      </header>
      <div class="chips">
        <button v-for="filter in data.filters" :key="filter" type="button">{{ filter }}</button>
      </div>
    </section>

    <section class="panel accounts-table">
      <header>
        <p class="eyebrow">公户列表</p>
        <h3>实时指标</h3>
      </header>
      <table>
        <thead>
          <tr>
            <th>账户</th>
            <th>余额</th>
            <th>状态</th>
            <th>交易速率</th>
            <th>风险</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="account in data.accounts" :key="account.id">
            <td>{{ account.name }}</td>
            <td>{{ account.balance }}</td>
            <td>{{ account.status }}</td>
            <td>{{ account.transactions }}</td>
            <td>{{ account.risk }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="alerts-timeline">
      <article class="panel alerts">
        <header>
          <p class="eyebrow">告警</p>
          <h3>实时处理</h3>
        </header>
        <ul>
          <li v-for="alert in data.alerts" :key="alert.id">
            <div>
              <p class="title">{{ alert.title }}</p>
              <p class="severity">{{ alert.severity }}</p>
            </div>
            <span class="action">{{ alert.action }}</span>
          </li>
        </ul>
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
.realtime-board {
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
  background: linear-gradient(135deg, rgba(70, 95, 255, 0.65), rgba(11, 16, 32, 0.95));
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

.accounts-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 12px;
}

.accounts-table th {
  text-align: left;
  padding: 8px 0;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.accounts-table td {
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.alerts-timeline {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.alerts ul,
.timeline ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alerts li,
.timeline li {
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
  display: flex;
  justify-content: space-between;
  align-items: center;
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
