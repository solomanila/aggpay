<script setup>
const props = defineProps({
  user: {
    type: Object,
    required: true
  },
  alerts: {
    type: Object,
    required: true
  },
  timezone: {
    type: String,
    required: true
  },
  language: {
    type: String,
    required: true
  },
  timezones: {
    type: Array,
    default: () => []
  },
  languages: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['update:timezone', 'update:language', 'logout']);

const handleTimezoneChange = (event) => {
  emit('update:timezone', event.target.value);
};

const handleLanguageChange = (event) => {
  emit('update:language', event.target.value);
};

const handlePowerClick = () => {
  emit('logout');
};
</script>

<template>
  <header class="header">
    <div class="brand">
      <div class="brand-icon">PA</div>
      <div>
        <p class="brand-title">演示 test1111</p>
        <p class="brand-caption">多通道资金 · 实时监控</p>
      </div>
    </div>
    <div class="header-actions">
      <div class="pill-group">
        <button class="pill ghost">通道下单错误数量</button>
        <button class="pill warning">
          <span class="dot" />
          异常 {{ props.alerts.openIncidents }}
        </button>
        <button class="pill solid">通知 {{ props.alerts.notifications }}</button>
      </div>
      <div class="selectors">
        <label>
          <span>时区</span>
          <select :value="props.timezone" @change="handleTimezoneChange">
            <option v-for="option in props.timezones" :key="option" :value="option">{{ option }}</option>
          </select>
        </label>
        <label>
          <span>语言</span>
          <select :value="props.language" @change="handleLanguageChange">
            <option v-for="option in props.languages" :key="option" :value="option">{{ option }}</option>
          </select>
        </label>
      </div>
      <div class="user">
        <div>
          <p class="user-name">{{ props.user.name }}</p>
          <p class="user-role">{{ props.user.role }}</p>
        </div>
        <div class="power" role="button" tabindex="0" @click="handlePowerClick">⏻</div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 32px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(7, 10, 18, 0.75);
  backdrop-filter: blur(14px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-icon {
  width: 52px;
  height: 52px;
  border-radius: 18px;
  background: linear-gradient(135deg, #7f85f9, #b291ff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-title {
  font-size: 18px;
  font-weight: 600;
}

.brand-caption {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.65);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.pill-group {
  display: flex;
  gap: 10px;
}

.pill {
  border-radius: 999px;
  padding: 8px 18px;
  border: 1px solid transparent;
  font-size: 13px;
}

.pill.ghost {
  border-color: rgba(255, 255, 255, 0.18);
  color: rgba(255, 255, 255, 0.8);
  background: transparent;
}

.pill.warning {
  background: rgba(255, 161, 117, 0.12);
  border-color: rgba(255, 161, 117, 0.4);
  color: #ffb48a;
  display: flex;
  align-items: center;
  gap: 6px;
}

.pill.solid {
  background: linear-gradient(120deg, #7887ff, #a879ff);
  color: #fff;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ff7955;
  display: inline-flex;
}

.selectors {
  display: flex;
  gap: 12px;
  align-items: center;
}

label span {
  display: block;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 4px;
}

select {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #fff;
  padding: 6px 12px;
  border-radius: 999px;
}

.user {
  display: flex;
  align-items: center;
  gap: 12px;
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  padding-left: 12px;
}

.user-name {
  font-weight: 600;
}

.user-role {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.power {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.power:hover,
.power:focus-visible {
  background: rgba(255, 255, 255, 0.2);
  outline: none;
}

@media (max-width: 960px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .header-actions {
    flex-wrap: wrap;
  }

  .selectors {
    flex-wrap: wrap;
  }
}
</style>
