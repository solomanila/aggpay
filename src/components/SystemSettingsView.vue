<script setup>
const { data } = defineProps({
  data: {
    type: Object,
    required: true
  }
});

const statusClassMap = {
  生效: 'status-live',
  灰度: 'status-gray',
  待发布: 'status-pending'
};

const toggleToneMap = {
  positive: 'tone-positive',
  warning: 'tone-warning',
  info: 'tone-info'
};

const getStatusClass = (status) => statusClassMap[status] ?? 'status-default';
const getToggleClass = (tone) => toggleToneMap[tone] ?? 'tone-default';
</script>

<template>
  <div class="system-settings-view">
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

    <section class="panel overview-card">
      <header>
        <p class="eyebrow">基础信息</p>
        <h3>控制台概览</h3>
      </header>
      <div class="overview-grid">
        <article v-for="item in data.overview" :key="item.id">
          <p class="label">{{ item.label }}</p>
          <p class="value">{{ item.value }}</p>
          <p class="meta">{{ item.meta }}</p>
        </article>
      </div>
    </section>

    <section class="panel filters-card">
      <header>
        <div>
          <p class="eyebrow">配置列表筛选</p>
          <h3>状态标签</h3>
        </div>
        <p class="muted">{{ data.filters?.note }}</p>
      </header>
      <div class="chips">
        <button v-for="filter in data.filters?.options ?? []" :key="filter" type="button">
          {{ filter }}
        </button>
      </div>
    </section>

    <section class="panel configs-table">
      <header>
        <div>
          <p class="eyebrow">配置项</p>
          <h3>最新同步</h3>
        </div>
        <div class="table-actions">
          <button
            v-for="action in data.configActions ?? []"
            :key="action"
            class="ghost-btn"
            type="button"
          >
            {{ action }}
          </button>
        </div>
      </header>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>配置名</th>
            <th>版本</th>
            <th>所属</th>
            <th>状态</th>
            <th>Owner</th>
            <th>更新时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="config in data.configs ?? []" :key="config.id">
            <td>{{ config.id }}</td>
            <td>{{ config.name }}</td>
            <td>{{ config.version }}</td>
            <td>{{ config.scope }}</td>
            <td>
              <span :class="['status-pill', getStatusClass(config.status)]">
                {{ config.status }}
              </span>
            </td>
            <td>{{ config.owner }}</td>
            <td>{{ config.updated }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="forms-grid">
      <article v-for="section in data.forms ?? []" :key="section.id" class="panel form-card">
        <header>
          <div>
            <p class="eyebrow">{{ section.title }}</p>
            <h3>{{ section.description }}</h3>
          </div>
          <button class="ghost-btn" type="button">保存</button>
        </header>
        <div class="field-grid">
          <div
            v-for="field in section.fields"
            :key="field.id"
            class="field"
            :class="{ disabled: field.disabled }"
          >
            <p class="label">{{ field.label }}</p>
            <p class="value" :class="{ placeholder: !field.value }">
              {{ field.value || field.placeholder }}
            </p>
            <p v-if="field.status" class="badge">{{ field.status }}</p>
            <p v-if="field.note" class="meta">{{ field.note }}</p>
          </div>
        </div>
      </article>
    </section>

    <section class="lower-grid">
      <article class="panel toggles-card">
        <header>
          <div>
            <p class="eyebrow">安全开关</p>
            <h3>后台防护</h3>
          </div>
          <button class="ghost-btn" type="button">配置</button>
        </header>
        <ul>
          <li
            v-for="toggle in data.toggles ?? []"
            :key="toggle.id"
            :class="getToggleClass(toggle.tone)"
          >
            <div>
              <p class="title">{{ toggle.label }}</p>
              <p class="meta">{{ toggle.description }}</p>
            </div>
            <span class="status">{{ toggle.status }}</span>
          </li>
        </ul>
      </article>

      <article class="panel audit-card">
        <header>
          <div>
            <p class="eyebrow">操作记录</p>
            <h3>近 3 条</h3>
          </div>
        </header>
        <ol>
          <li v-for="audit in data.auditTrail ?? []" :key="audit.id">
            <p class="time">{{ audit.time }}</p>
            <div>
              <p class="actor">{{ audit.actor }}</p>
              <p class="meta">{{ audit.action }}</p>
            </div>
          </li>
        </ol>
      </article>

      <article class="panel quick-links">
        <header>
          <div>
            <p class="eyebrow">快捷操作</p>
            <h3>常用动作</h3>
          </div>
        </header>
        <div class="link-buttons">
          <button
            v-for="link in data.quickLinks ?? []"
            :key="link"
            class="ghost-btn secondary"
            type="button"
          >
            {{ link }}
          </button>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.system-settings-view {
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

.overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.overview-grid article {
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.02);
}

.overview-grid .label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.overview-grid .value {
  font-size: 18px;
  font-weight: 600;
}

.filters-card header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.chips {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.chips button {
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: transparent;
  color: rgba(255, 255, 255, 0.8);
  padding: 6px 16px;
}

.configs-table header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.table-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.configs-table table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 16px;
}

.configs-table th {
  text-align: left;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.6);
  font-weight: normal;
  font-size: 13px;
}

.configs-table td {
  padding: 14px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  font-size: 13px;
}

.status-pill {
  display: inline-flex;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-live {
  background: rgba(65, 255, 183, 0.2);
  color: #41ffb7;
}

.status-gray {
  background: rgba(140, 140, 251, 0.2);
  color: #8c8cfb;
}

.status-pending,
.status-default {
  background: rgba(255, 174, 109, 0.2);
  color: #ffae6d;
}

.forms-grid {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.form-card header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.field {
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.03);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field.disabled {
  opacity: 0.6;
}

.field .label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.field .value {
  font-size: 16px;
  font-weight: 600;
}

.field .value.placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.field .badge {
  align-self: flex-start;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
}

.field .meta {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.lower-grid {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.toggles-card ul {
  list-style: none;
  padding: 0;
  margin: 20px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toggles-card li {
  border-radius: 16px;
  padding: 16px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.toggles-card .title {
  font-weight: 600;
}

.toggles-card .status {
  font-size: 12px;
  font-weight: 600;
}

.tone-positive {
  background: rgba(65, 255, 183, 0.12);
}

.tone-warning {
  background: rgba(255, 174, 109, 0.12);
}

.tone-info,
.tone-default {
  background: rgba(140, 140, 251, 0.12);
}

.audit-card ol {
  list-style: none;
  padding: 0;
  margin: 20px 0 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.audit-card li {
  display: flex;
  gap: 12px;
}

.audit-card .time {
  width: 58px;
  font-weight: 600;
}

.audit-card .actor {
  font-weight: 600;
}

.quick-links .link-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.ghost-btn {
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  padding: 8px 18px;
}

.ghost-btn.secondary {
  background: rgba(255, 255, 255, 0.04);
}

@media (max-width: 720px) {
  .configs-table header,
  .form-card header,
  .filters-card header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
