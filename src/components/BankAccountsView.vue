<script setup>
const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});
</script>

<template>
  <div class="accounts-view">
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
        <p class="eyebrow">账户列表</p>
        <h3>资产与限额</h3>
      </header>
      <table>
        <thead>
          <tr>
            <th>账户</th>
            <th>区域</th>
            <th>状态</th>
            <th>余额</th>
            <th>限额</th>
            <th>币种</th>
            <th>Owner</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="account in data.accounts" :key="account.id">
            <td>{{ account.name }}</td>
            <td>{{ account.region }}</td>
            <td>{{ account.status }}</td>
            <td>{{ account.balance }}</td>
            <td>{{ account.limit }}</td>
            <td>{{ account.currency }}</td>
            <td>{{ account.owner }}</td>
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
              <p class="label">{{ item.account }}</p>
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
.accounts-view {
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
  background: linear-gradient(135deg, rgba(76, 95, 255, 0.65), rgba(11, 16, 32, 0.95));
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

@media (max-width: 960px) {
  .hero {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
