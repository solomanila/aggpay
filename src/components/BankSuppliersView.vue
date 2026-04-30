<script setup>
const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});
</script>

<template>
  <div class="suppliers-view">
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

    <section class="panel suppliers-table">
      <header>
        <p class="eyebrow">供应商列表</p>
        <h3>账户覆盖</h3>
      </header>
      <table>
        <thead>
          <tr>
            <th>名称</th>
            <th>区域</th>
            <th>状态</th>
            <th>账户数</th>
            <th>联系人</th>
            <th>备注</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="supplier in data.suppliers" :key="supplier.id">
            <td>{{ supplier.name }}</td>
            <td>{{ supplier.region }}</td>
            <td>{{ supplier.status }}</td>
            <td>{{ supplier.accounts }}</td>
            <td>{{ supplier.contact }}</td>
            <td>{{ supplier.notes }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="maintenance-notices">
      <article class="panel maintenance">
        <header>
          <p class="eyebrow">维护计划</p>
          <h3>近期窗口</h3>
        </header>
        <ul>
          <li v-for="item in data.maintenance" :key="item.id">
            <div>
              <p class="label">{{ item.supplier }}</p>
              <p class="meta">{{ item.note }}</p>
            </div>
            <span class="window">{{ item.window }}</span>
          </li>
        </ul>
      </article>

      <article class="panel notices">
        <header>
          <p class="eyebrow">提醒</p>
          <h3>最新动态</h3>
        </header>
        <ul>
          <li v-for="notice in data.notices" :key="notice.id">{{ notice.text }}</li>
        </ul>
      </article>
    </section>
  </div>
</template>

<style scoped>
.suppliers-view {
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

.suppliers-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 12px;
}

.suppliers-table th {
  text-align: left;
  padding: 8px 0;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.suppliers-table td {
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.maintenance-notices {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.maintenance ul,
.notices ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.maintenance li,
.notices li {
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notices li {
  align-items: flex-start;
}

@media (max-width: 960px) {
  .hero {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
