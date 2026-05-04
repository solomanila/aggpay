<script setup>
import { ref, onMounted } from 'vue';
import http from '../services/http';

const loading = ref(true);
const error = ref('');
const data = ref(null);
const appKeyVisible = ref(false);

const load = async () => {
  loading.value = true;
  error.value = '';
  try {
    const { data: resp } = await http.get('/admin/merchant/dashboard');
    data.value = resp?.data ?? resp;
  } catch (e) {
    error.value = '加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
};

onMounted(load);
</script>

<template>
  <div class="merchant-dashboard">
    <p v-if="loading" class="hint">加载中...</p>
    <p v-else-if="error" class="hint error">{{ error }}</p>

    <template v-else-if="data">
      <!-- 余额（含待结算） -->
      <section class="panel">
        <h3 class="section-title">余额</h3>
        <div class="balance-grid">
          <div
            v-for="item in data.balances"
            :key="item.currency"
            class="balance-card"
          >
            <p class="currency">{{ item.currency }}</p>
            <div class="balance-cols">
              <div class="balance-col">
                <p class="col-label">出款</p>
                <p class="col-value">{{ item.available ?? '0' }}</p>
              </div>
              <div class="balance-col">
                <p class="col-label">待结算</p>
                <p class="col-value accent">
                  {{ item.currency === 'INR' ? (data.pendingSettlement ?? item.frozen ?? '0') : (item.frozen ?? '0') }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- API Key -->
      <section class="panel">
        <h3 class="section-title">API Key</h3>
        <div class="api-key-block">
          <div class="key-row">
            <p class="key-label">keyId</p>
            <div class="key-field">{{ data.keyId }}</div>
          </div>
          <div class="key-row">
            <p class="key-label">appKey</p>
            <div class="key-field key-field--secret">
              <span>{{ appKeyVisible ? data.appKey : '••••••••••••••••••••••••••••••••••••' }}</span>
              <button
                class="eye-btn"
                type="button"
                :aria-label="appKeyVisible ? '隐藏' : '显示'"
                @click="appKeyVisible = !appKeyVisible"
              >
                <svg v-if="appKeyVisible" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                  <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                  <line x1="1" y1="1" x2="23" y2="23" />
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.merchant-dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 780px;
}

.hint {
  color: rgba(255, 255, 255, 0.45);
  font-size: 14px;
}
.hint.error { color: #f87171; }

.panel {
  border-radius: 16px;
  padding: 24px;
  background: rgba(18, 19, 37, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
  margin: 0 0 16px;
  letter-spacing: 0.02em;
}

/* 余额卡片 */
.balance-grid {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.balance-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 12px;
  padding: 16px 24px;
  min-width: 200px;
}

.currency {
  font-size: 13px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 0.06em;
  text-transform: uppercase;
  margin: 0 0 12px;
}

.balance-cols {
  display: flex;
  gap: 28px;
}

.balance-col {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.col-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  margin: 0;
  letter-spacing: 0.04em;
}

.col-value {
  font-size: 26px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
  letter-spacing: -0.02em;
}

.col-value.accent {
  color: #34d399;
}

/* API Key */
.api-key-block {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.key-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.key-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  margin: 0;
}

.key-field {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  font-family: 'Courier New', monospace;
  letter-spacing: 0.02em;
  max-width: 480px;
}

.key-field--secret {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.key-field--secret span {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.eye-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px;
  color: rgba(255, 255, 255, 0.4);
  display: flex;
  align-items: center;
  flex-shrink: 0;
  transition: color 0.15s;
}

.eye-btn:hover { color: rgba(255, 255, 255, 0.75); }

.eye-btn svg {
  width: 18px;
  height: 18px;
}
</style>
