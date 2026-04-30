<script setup>
const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});
</script>

<template>
  <div class="blacklist-view">
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
        <h3>维度</h3>
      </header>
      <div class="chips">
        <button v-for="filter in data.filters" :key="filter" type="button">{{ filter }}</button>
      </div>
    </section>

    <section class="panel entries">
      <header>
        <p class="eyebrow">黑名单条目</p>
        <h3>最新记录</h3>
      </header>
      <table>
        <thead>
          <tr>
            <th>对象</th>
            <th>类型</th>
            <th>原因</th>
            <th>状态</th>
            <th>Owner</th>
            <th>更新时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="entry in data.entries" :key="entry.id">
            <td>{{ entry.target }}</td>
            <td>{{ entry.type }}</td>
            <td>{{ entry.reason }}</td>
            <td>{{ entry.status }}</td>
            <td>{{ entry.owner }}</td>
            <td>{{ entry.updated }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="actions-reviews">
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

      <article class="panel reviews">
        <header>
          <p class="eyebrow">复核队列</p>
          <h3>待处理</h3>
        </header>
        <div class="review-list">
          <div v-for="review in data.reviews" :key="review.id" class="review-row">
            <div>
              <p class="title">{{ review.title }}</p>
              <p class="meta">{{ review.owner }}</p>
            </div>
            <span class="eta">{{ review.eta }}</span>
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
.blacklist-view {
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

.entries table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 12px;
}

.entries th {
  text-align: left;
  padding: 8px 0;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.entries td {
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.actions-reviews {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.action-list,
.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.action-row,
.review-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
}

.action-row .progress {
  width: 150px;
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
