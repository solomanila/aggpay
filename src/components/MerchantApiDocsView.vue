<script setup>
import { ref } from 'vue';

const lang = ref('en');

const DOC_URL = 'https://bedecked-neighbor-f5b.notion.site/25c605a959a9807ca08fddd712a7de84';
</script>

<template>
  <div class="api-docs">
    <div class="docs-header">
      <div class="lang-tabs">
        <button
          class="lang-tab"
          :class="{ active: lang === 'en' }"
          @click="lang = 'en'"
        >English</button>
        <button
          class="lang-tab"
          :class="{ active: lang === 'zh' }"
          @click="lang = 'zh'"
        >中文</button>
      </div>
      <div class="endpoint-row">
        <span class="endpoint-label">Endpoint:</span>
        <span class="endpoint-url">https://api.zyapay.com</span>
      </div>
    </div>

    <div class="doc-card">
      <div class="doc-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <div class="doc-info">
        <p class="doc-title">{{ lang === 'en' ? 'API Documentation' : 'API 接口文档' }}</p>
        <p class="doc-desc">
          {{ lang === 'en'
            ? 'Complete integration guide including payment collection, payout, signature verification, and callback specifications.'
            : '完整的接入说明，包括代收、代付、签名验证及回调规范。' }}
        </p>
      </div>
      <a
        class="open-btn"
        :href="DOC_URL"
        target="_blank"
        rel="noopener noreferrer"
      >
        <span>{{ lang === 'en' ? 'Open Docs' : '打开文档' }}</span>
        <svg viewBox="0 0 20 20" fill="currentColor">
          <path d="M11 3a1 1 0 100 2h2.586l-6.293 6.293a1 1 0 101.414 1.414L15 6.414V9a1 1 0 102 0V4a1 1 0 00-1-1h-5z"/>
          <path d="M5 5a2 2 0 00-2 2v8a2 2 0 002 2h8a2 2 0 002-2v-3a1 1 0 10-2 0v3H5V7h3a1 1 0 000-2H5z"/>
        </svg>
      </a>
    </div>
  </div>
</template>

<style scoped>
.api-docs {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 780px;
}

.docs-header {
  display: flex;
  align-items: center;
  gap: 32px;
}

.lang-tabs {
  display: flex;
  border-bottom: 2px solid rgba(255, 255, 255, 0.08);
}

.lang-tab {
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  padding: 8px 20px;
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.45);
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
  letter-spacing: 0.02em;
}

.lang-tab.active {
  color: #60a5fa;
  border-bottom-color: #60a5fa;
}

.lang-tab:hover:not(.active) {
  color: rgba(255, 255, 255, 0.75);
}

.endpoint-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.endpoint-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
}

.endpoint-url {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #34d399;
  letter-spacing: 0.02em;
}

.doc-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  background: rgba(18, 19, 37, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
}

.doc-icon {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(96, 165, 250, 0.1);
  border: 1px solid rgba(96, 165, 250, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #60a5fa;
}

.doc-icon svg {
  width: 24px;
  height: 24px;
}

.doc-info {
  flex: 1;
  min-width: 0;
}

.doc-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 6px;
}

.doc-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  margin: 0;
  line-height: 1.6;
}

.open-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: #2563eb;
  color: #fff;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  transition: background 0.15s;
}

.open-btn:hover {
  background: #1d4ed8;
}

.open-btn svg {
  width: 16px;
  height: 16px;
}
</style>
