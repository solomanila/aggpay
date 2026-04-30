<script setup>
const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});
</script>

<template>
  <div class="query-view">
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
        <h3>查单状态</h3>
      </header>
      <div class="chips">
        <button v-for="filter in data.filters" :key="filter" type="button">{{ filter }}</button>
      </div>
    </section>

    <section class="panel queries-table">
      <header>
        <p class="eyebrow">查单记录</p>
        <h3>最新请求</h3>
      </header>
      <table>
        <thead>
          <tr>
            <th>商户</th>
            <th>通道</th>
            <th>原因</th>
            <th>状态</th>
            <th>负责人</th>
            <th>更新时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="query in data.queries" :key="query.id">
            <td>{{ query.merchant }}</td>
            <td>{{ query.channel }}</td>
            <td>{{ query.reason }}</td>
            <td>{{ query.status }}</td>
            <td>{{ query.owner }}</td>
            <td>{{ query.updated }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="panel timeline">
      <header>
        <p class="eyebrow">时间线</p>
        <h3>处理进度</h3>
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
    </section>
  </div>
</template>

<style scoped>
.query-view {
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
  background: linear-gradient(135deg, rgba(74, 95, 255, 0.65), rgba(11, 16, 32, 0.95));
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

.queries-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 12px;
}

.queries-table th {
  text-align: left;
  padding: 8px 0;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.queries-table td {
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
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
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
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
