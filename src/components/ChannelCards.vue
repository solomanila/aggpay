<script setup>
const props = defineProps({
  channels: {
    type: Array,
    default: () => []
  }
});

const statusClass = (status) => {
  if (!status) {
    return '';
  }
  if (status.includes('紧急')) {
    return 'danger';
  }
  if (status.includes('监控')) {
    return 'warn';
  }
  return 'ok';
};
</script>

<template>
  <div class="channel-grid" v-if="props.channels.length">
    <article
      v-for="channel in props.channels"
      :key="channel.id"
      class="channel-card"
      :class="{ highlight: channel.priority }"
    >
      <div class="channel-head">
        <div>
          <p class="channel-id">{{ channel.id }}</p>
          <h3>{{ channel.countryTag }} · {{ channel.country }}</h3>
        </div>
        <span class="status" :class="statusClass(channel.status)">{{ channel.status }}</span>
      </div>
      <p class="channel-desc">{{ channel.description }}</p>
      <div class="tags">
        <span class="country-tag">{{ channel.country }}</span>
        <span v-for="type in channel.businessTypes" :key="type" class="type-tag">{{ type }}</span>
      </div>
      <dl class="rates">
        <div>
          <dt>PayinRate</dt>
          <dd>{{ channel.payinRate }}</dd>
        </div>
        <div>
          <dt>PayoutRate</dt>
          <dd>{{ channel.payoutRate }}</dd>
        </div>
        <div>
          <dt>Period</dt>
          <dd>{{ channel.period }}</dd>
        </div>
        <div>
          <dt>覆盖</dt>
          <dd>{{ channel.coverage }}</dd>
        </div>
        <div>
          <dt>单笔额度</dt>
          <dd>{{ channel.limit }}</dd>
        </div>
        <div>
          <dt>风险</dt>
          <dd>{{ channel.risk }}</dd>
        </div>
      </dl>
      <div class="card-footer">
        <button type="button">查看详情</button>
      </div>
    </article>
  </div>
  <div v-else class="empty">暂无符合条件的通道</div>
</template>

<style scoped>
.channel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 18px;
}

.channel-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.channel-card.highlight {
  border-color: rgba(127, 133, 249, 0.5);
  box-shadow: 0 10px 30px rgba(63, 76, 178, 0.2);
}

.channel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.channel-id {
  font-size: 12px;
  letter-spacing: 0.06em;
  color: rgba(255, 255, 255, 0.55);
}

.status {
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.06);
}

.status.ok {
  color: #5bf5b0;
}

.status.warn {
  color: #ffb84a;
}

.status.danger {
  color: #ff7d7d;
}

.channel-desc {
  min-height: 54px;
  color: rgba(255, 255, 255, 0.75);
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.country-tag,
.type-tag {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  font-size: 12px;
}

.type-tag {
  background: rgba(127, 133, 249, 0.12);
}

.rates {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
}

.rates dt {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.rates dd {
  margin: 4px 0 0;
  font-size: 15px;
  font-weight: 600;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
}

button {
  border-radius: 999px;
  border: none;
  padding: 8px 20px;
  background: rgba(127, 133, 249, 0.25);
  color: #fff;
}

.empty {
  padding: 40px;
  text-align: center;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 16px;
  color: rgba(255, 255, 255, 0.5);
}
</style>
