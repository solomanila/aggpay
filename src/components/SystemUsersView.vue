<script setup>
const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});

const statusClassMap = {
  活跃: 'status-active',
  冻结: 'status-frozen',
  禁用: 'status-disabled',
  风险: 'status-risk',
  待审核: 'status-review'
};

const getStatusClass = (status) => statusClassMap[status] ?? 'status-default';
</script>

<template>
  <div class="system-users-view">
    <section class="panel hero gradient">
      <div class="hero-copy">
        <p class="eyebrow">{{ data.hero.eyebrow }}</p>
        <h2>{{ data.hero.title }}</h2>
        <p class="muted">{{ data.hero.description }}</p>
        <div class="hero-meta">
          <span>{{ data.hero.sync }}</span>
          <div class="hero-buttons">
            <button
              v-for="action in data.hero.actions ?? []"
              :key="action.id"
              :class="['ghost-btn', { primary: action.variant === 'primary' }]"
              type="button"
            >
              {{ action.label }}
            </button>
          </div>
        </div>
      </div>
      <dl class="hero-stats">
        <div v-for="stat in data.hero.highlights ?? []" :key="stat.id">
          <dt>{{ stat.label }}</dt>
          <dd>{{ stat.value }}</dd>
        </div>
      </dl>
    </section>

    <section class="stat-grid">
      <article v-for="stat in data.stats" :key="stat.id" class="panel stat-card">
        <p class="label">{{ stat.label }}</p>
        <p class="value">{{ stat.value }}</p>
        <p class="meta">{{ stat.meta }}</p>
      </article>
    </section>

    <section class="panel filters-card">
      <header>
        <div>
          <p class="eyebrow">条件筛选</p>
          <h3>组合过滤</h3>
        </div>
        <p class="muted">{{ data.filters.note }}</p>
      </header>
      <div class="filters-grid">
        <label v-for="field in data.filters.fields" :key="field.id">
          <span>{{ field.label }}</span>
          <input :placeholder="field.placeholder" type="text" />
        </label>
        <label class="date-range">
          <span>创建时间</span>
          <div class="range-pill">
            <span>{{ data.filters.dateRange.start }}</span>
            <span class="divider">~</span>
            <span>{{ data.filters.dateRange.end }}</span>
          </div>
        </label>
      </div>
      <div class="filters-footer">
        <div class="chips">
          <button v-for="status in data.filters.statuses" :key="status" type="button">
            {{ status }}
          </button>
        </div>
        <div class="filter-actions">
          <button class="ghost-btn" type="button">重置</button>
          <button class="ghost-btn primary" type="button">查询</button>
        </div>
      </div>
    </section>

    <section class="panel table-card">
      <header>
        <div>
          <p class="eyebrow">系统用户</p>
          <h3>共 {{ data.table.total }} 位</h3>
          <p class="muted">{{ data.table.updated }}</p>
        </div>
        <div class="table-actions">
          <button
            v-for="action in data.table.batchActions"
            :key="action"
            class="ghost-btn"
            type="button"
          >
            {{ action }}
          </button>
          <button class="ghost-btn primary" type="button">导出列表</button>
        </div>
      </header>
      <table>
        <thead>
          <tr>
            <th v-for="column in data.table.columns" :key="column">{{ column }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in data.table.rows" :key="row.id">
            <td>
              <p class="user-name">{{ row.name }}</p>
              <p class="meta">{{ row.account }}</p>
              <div class="tag-list">
                <span v-for="tag in row.tags" :key="tag">{{ tag }}</span>
              </div>
            </td>
            <td>
              <p>{{ row.role }}</p>
              <p class="meta">{{ row.level }}</p>
            </td>
            <td>
              <span :class="['status-pill', getStatusClass(row.status)]">{{ row.status }}</span>
              <p class="meta">{{ row.risk }}</p>
            </td>
            <td>
              <p>{{ row.lastLogin }}</p>
              <p class="meta">{{ row.location }}</p>
            </td>
            <td>
              <p>{{ row.group }}</p>
              <p class="meta">{{ row.owner }}</p>
            </td>
            <td>
              <p>{{ row.ip }}</p>
              <p class="meta">{{ row.client }}</p>
            </td>
          </tr>
        </tbody>
      </table>
      <footer class="table-footer">
        <p class="muted">分页 {{ data.table.page }}</p>
        <p class="muted">每页 20 条 · 可在右上角切换</p>
      </footer>
    </section>

    <section class="detail-grid">
      <article class="panel focus-card">
        <header>
          <div>
            <p class="eyebrow">焦点用户</p>
            <h3>{{ data.focusUser.name }}</h3>
            <p class="muted">{{ data.focusUser.role }} · {{ data.focusUser.owner }}</p>
          </div>
          <button class="ghost-btn" type="button">重置密码</button>
        </header>
        <div class="focus-header">
          <div class="avatar">{{ data.focusUser.avatar }}</div>
          <div>
            <p>{{ data.focusUser.email }}</p>
            <p class="muted">{{ data.focusUser.meta }}</p>
            <div class="tag-list">
              <span v-for="tag in data.focusUser.tags" :key="tag">{{ tag }}</span>
            </div>
          </div>
        </div>
        <dl class="info-grid">
          <div v-for="stat in data.focusUser.stats" :key="stat.id">
            <dt>{{ stat.label }}</dt>
            <dd>{{ stat.value }}</dd>
            <p class="meta">{{ stat.meta }}</p>
          </div>
        </dl>
        <div class="permissions">
          <p class="eyebrow">权限范围</p>
          <ul>
            <li v-for="permission in data.focusUser.permissions" :key="permission">
              {{ permission }}
            </li>
          </ul>
        </div>
        <div class="quick-links">
          <button
            v-for="link in data.focusUser.quickLinks"
            :key="link"
            class="ghost-btn"
            type="button"
          >
            {{ link }}
          </button>
        </div>
      </article>
      <article class="panel timeline-card">
        <header>
          <div>
            <p class="eyebrow">操作追踪</p>
            <h3>近 24 小时</h3>
          </div>
          <p class="muted">{{ data.timeline.description }}</p>
        </header>
        <ol>
          <li v-for="item in data.timeline.items" :key="item.id">
            <p class="time">{{ item.time }}</p>
            <div>
              <p class="event">{{ item.event }}</p>
              <p class="meta">{{ item.detail }}</p>
            </div>
          </li>
        </ol>
      </article>
    </section>
  </div>
</template>

<style scoped>
.system-users-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.panel {
  border-radius: 24px;
  padding: 24px;
  background: rgba(12, 14, 35, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.gradient {
  background: linear-gradient(135deg, rgba(74, 95, 255, 0.7), rgba(11, 16, 32, 0.95));
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

.hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
}

.hero-meta {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 20px;
}

.hero-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-stats {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  margin: 0;
}

.hero-stats dt {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.hero-stats dd {
  font-size: 20px;
  font-weight: 600;
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

.filters-card header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.filters-grid label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
}

.filters-grid input {
  border-radius: 12px;
  padding: 10px 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.03);
  color: #fff;
}

.filters-grid .range-pill {
  border-radius: 12px;
  border: 1px dashed rgba(255, 255, 255, 0.2);
  padding: 10px 14px;
  display: flex;
  gap: 8px;
}

.filters-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 16px;
  margin-top: 20px;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.chips button {
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  padding: 6px 16px;
  background: transparent;
  color: rgba(255, 255, 255, 0.8);
}

.ghost-btn {
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
  padding: 8px 18px;
}

.ghost-btn.primary {
  background: #ffae6d;
  color: #1d0f05;
  border-color: rgba(255, 255, 255, 0.2);
}

.table-card header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.table-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.table-card table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
}

.table-card th {
  text-align: left;
  padding-bottom: 12px;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  font-weight: normal;
  font-size: 13px;
}

.table-card td {
  padding: 16px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  vertical-align: top;
}

.user-name {
  font-weight: 600;
}

.meta {
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  margin-top: 2px;
}

.tag-list {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 6px;
}

.tag-list span {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  padding: 2px 10px;
  font-size: 11px;
}

.status-pill {
  display: inline-flex;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-active {
  background: rgba(65, 255, 183, 0.2);
  color: #41ffb7;
}

.status-frozen {
  background: rgba(255, 174, 109, 0.2);
  color: #ffae6d;
}

.status-disabled {
  background: rgba(255, 119, 119, 0.18);
  color: #ff7777;
}

.status-risk,
.status-review,
.status-default {
  background: rgba(140, 140, 251, 0.2);
  color: #8c8cfb;
}

.table-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
  font-size: 12px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.focus-card header,
.timeline-card header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.focus-header {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.avatar {
  width: 54px;
  height: 54px;
  border-radius: 16px;
  background: rgba(127, 133, 249, 0.2);
  display: grid;
  place-items: center;
  font-weight: 600;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.info-grid dt {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.info-grid dd {
  font-size: 18px;
  font-weight: 600;
}

.permissions ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.permissions li {
  padding: 6px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
}

.quick-links {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.timeline-card ol {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.timeline-card li {
  display: flex;
  gap: 16px;
  padding-left: 12px;
  border-left: 2px solid rgba(255, 255, 255, 0.08);
}

.timeline-card .time {
  width: 60px;
  font-weight: 600;
}

.timeline-card .event {
  font-weight: 600;
}

@media (max-width: 720px) {
  .hero {
    flex-direction: column;
  }

  .filters-card header {
    flex-direction: column;
    align-items: flex-start;
  }

  .table-footer {
    flex-direction: column;
    gap: 6px;
  }
}
</style>
