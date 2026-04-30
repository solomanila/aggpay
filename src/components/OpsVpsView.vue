<script setup>
const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});
</script>

<template>
  <div class="ops-vps-view">
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
        <button v-for="filter in data.filters ?? []" :key="filter" type="button">
          {{ filter }}
        </button>
      </div>
    </section>

    <section class="panel vps-table">
      <header>
        <p class="eyebrow">VPS 列表</p>
        <h3>实时状态</h3>
      </header>
      <table>
        <thead>
          <tr>
            <th>名称</th>
            <th>区域</th>
            <th>负载</th>
            <th>状态</th>
            <th>IP</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="vps in data.vpsList ?? []" :key="vps.id">
            <td>{{ vps.name }}</td>
            <td>{{ vps.region }}</td>
            <td>{{ vps.load }}</td>
            <td>{{ vps.status }}</td>
            <td>{{ vps.ip }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="panel alerts" v-if="data.alerts?.length">
      <header>
        <p class="eyebrow">提醒</p>
        <h3>告警</h3>
      </header>
      <ul>
        <li v-for="alert in data.alerts" :key="alert.id">
          <div>
            <p class="title">{{ alert.title }}</p>
            <p class="action">{{ alert.action }}</p>
          </div>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.ops-vps-view {
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

.vps-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 12px;
}

.vps-table th {
  text-align: left;
  padding: 8px 0;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.vps-table td {
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.alerts ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alerts li {
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
}

@media (max-width: 960px) {
  .hero {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
