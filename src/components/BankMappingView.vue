<script setup>
const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});
</script>

<template>
  <div class="mapping-view">
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

    <section class="panel mappings-table">
      <header>
        <p class="eyebrow">字段映射</p>
        <h3>模板列表</h3>
      </header>
      <table>
        <thead>
          <tr>
            <th>银行</th>
            <th>版本</th>
            <th>类型</th>
            <th>状态</th>
            <th>Owner</th>
            <th>更新时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="mapping in data.mappings" :key="mapping.id">
            <td>{{ mapping.bank }}</td>
            <td>{{ mapping.version }}</td>
            <td>{{ mapping.type }}</td>
            <td>{{ mapping.status }}</td>
            <td>{{ mapping.owner }}</td>
            <td>{{ mapping.updated }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="approvals-notices">
      <article class="panel approvals">
        <header>
          <p class="eyebrow">审批队列</p>
          <h3>待上线</h3>
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
.mapping-view {
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
  background: linear-gradient(135deg, rgba(71, 95, 255, 0.65), rgba(11, 16, 32, 0.95));
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

.mappings-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 12px;
}

.mappings-table th {
  text-align: left;
  padding: 8px 0;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.mappings-table td {
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.approvals-notices {
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

.eta {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.notices ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notices li {
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
