<script setup>
const props = defineProps({
  stats: {
    type: Array,
    default: () => []
  }
});

const formatTrend = (stat) => {
  if (typeof stat.trend !== 'number') {
    return stat.trend;
  }

  const raw = stat.trend.toFixed(2).replace(/\.00$/, '').replace(/(\.\d*[1-9])0+$/, '$1');
  const prefix = stat.trend > 0 ? '+' : '';
  const label = stat.trendLabel ? ` ${stat.trendLabel}` : '';
  return `${prefix}${raw}%${label}`;
};
</script>

<template>
  <section class="stats-grid">
    <article v-for="stat in props.stats" :key="stat.id" class="stat-card">
      <div class="stat-header">
        <p class="label">{{ stat.label }}</p>
        <span
          class="trend"
          :class="{ up: stat.trend > 0, down: stat.trend < 0 }"
        >{{ formatTrend(stat) }}</span>
      </div>
      <p class="value">{{ stat.value }}</p>
      <p class="meta">{{ stat.meta }}</p>
    </article>
  </section>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 20px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.stat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.label {
  font-size: 13px;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.6);
}

.trend {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
}

.trend.up {
  color: #5bf5b0;
}

.trend.down {
  color: #ff8c8c;
}

.value {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 6px;
}

.meta {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.65);
}
</style>
