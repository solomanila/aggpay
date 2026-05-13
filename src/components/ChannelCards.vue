<script setup>
import { ref, computed, onMounted } from 'vue';
import http from '../services/http';

const channels = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const loading = ref(false);

const areaTypeOptions = ref([]);
const businessTypeOptions = ref([]);
const filterAreaType = ref('');
const filterBusinessType = ref('');

const activeTab = ref('market');
const selectedChannel = ref(null);
const showDetail = ref(false);

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));

const fetchOptions = async () => {
  try {
    const { data } = await http.get('/admin/channel-profile/options');
    const payload = data?.data ?? data;
    areaTypeOptions.value = payload?.areaTypes ?? [];
    businessTypeOptions.value = payload?.businessTypes ?? [];
  } catch {
    // silent fallback
  }
};

const fetchChannels = async () => {
  loading.value = true;
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    };
    if (filterAreaType.value) params.areaType = filterAreaType.value;
    if (filterBusinessType.value) params.businessType = filterBusinessType.value;

    const { data } = await http.get('/admin/channel-profile/page', { params });
    const payload = data?.data ?? data;
    channels.value = payload?.records ?? [];
    total.value = payload?.total ?? 0;
  } catch {
    channels.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

const onFilterChange = () => {
  pageNum.value = 1;
  fetchChannels();
};

const prevPage = () => {
  if (pageNum.value > 1) {
    pageNum.value--;
    fetchChannels();
  }
};

const nextPage = () => {
  if (pageNum.value < totalPages.value) {
    pageNum.value++;
    fetchChannels();
  }
};

const openDetail = (channel) => {
  selectedChannel.value = channel;
  showDetail.value = true;
};

const closeDetail = () => {
  showDetail.value = false;
  selectedChannel.value = null;
};

const AREA_TYPE_MAP = {
  1: '国内',
  2: '印度',
  4: '印尼',
  5: '非洲',
  6: '泰国',
  7: '墨西哥',
  8: '巴西',
  9: '巴基斯坦',
  10: '孟加拉国',
  11: '日本',
  12: '俄罗斯',
  13: '马来西亚',
  14: '埃及',
};

const fmtAreaType = (val) => {
  if (val == null || val === '') return '--';
  return AREA_TYPE_MAP[Number(val)] ?? AREA_TYPE_MAP[val] ?? String(val);
};

const splitTypes = (arr) => {
  if (!arr) return [];
  // API 返回的已是 string[]，兼容传入字符串的降级情况
  if (Array.isArray(arr)) return arr;
  return [];
};

const fmtRate = (val) => (val != null ? `${val}%` : '--');

const extraLabel = (key) => {
  const labels = {
    kyc: '要求KYC',
    period: '结算周期',
    deposit: '押金',
    payinrange: '代收金额范围',
    payoutrange: '代付金额范围',
    timerange: '时间段',
    complaint: '投诉处理',
    remark: '其他',
  };
  return labels[key] ?? key;
};

const fmtExtraVal = (key, val) => {
  if (key === 'kyc') return val === '1' || val === 1 ? 'Yes' : 'No';
  if (key === 'deposit') {
    const n = Number(val);
    return !isNaN(n) && n > 0 ? 'Yes' : 'No';
  }
  return val ?? '--';
};

onMounted(async () => {
  await fetchOptions();
  fetchChannels();
});
</script>

<template>
  <div class="channel-market">
    <!-- Tab + Controls -->
    <div class="market-bar">
      <div class="tabs">
        <button
          class="tab"
          :class="{ active: activeTab === 'market' }"
          @click="activeTab = 'market'"
        >通道市场</button>
        <button
          class="tab"
          :class="{ active: activeTab === 'ad' }"
          @click="activeTab = 'ad'"
        >发布广告</button>
      </div>
      <div class="controls">
        <select v-model="filterAreaType" @change="onFilterChange">
          <option value="">国家</option>
          <option v-for="t in areaTypeOptions" :key="t" :value="t">{{ fmtAreaType(t) }}</option>
        </select>
        <select v-model="filterBusinessType" @change="onFilterChange">
          <option value="">分类</option>
          <option v-for="t in businessTypeOptions" :key="t" :value="t">{{ t }}</option>
        </select>
        <div class="pagination">
          <button class="pg-btn" :disabled="pageNum <= 1" @click="prevPage">上一页</button>
          <span class="pg-info">{{ pageNum }}/{{ totalPages }}</span>
          <button class="pg-btn" :disabled="pageNum >= totalPages" @click="nextPage">下一页</button>
        </div>
      </div>
    </div>

    <!-- Channel Grid -->
    <div v-if="activeTab === 'market'" class="market-body">
      <div v-if="loading" class="loading-tip">加载中...</div>
      <div v-else-if="channels.length === 0" class="empty-tip">暂无通道数据</div>
      <div v-else class="card-grid">
        <div v-for="ch in channels" :key="ch.id" class="ch-card">
          <div class="card-head">
            <span class="card-name">{{ ch.name }}</span>
            <button class="detail-btn" @click="openDetail(ch)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="3" /><path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7z" />
              </svg>
              详情
            </button>
          </div>
          <div class="card-row">
            <span class="field-label">Country:</span>
            <span class="badge badge-country">{{ fmtAreaType(ch.areaType) }}</span>
          </div>
          <div class="card-row card-row--types">
            <span class="field-label">业务类型</span>
            <div class="badges">
              <span
                v-for="t in splitTypes(ch.businessTypes)"
                :key="t"
                class="badge badge-type"
              >{{ t }}</span>
            </div>
          </div>
          <div class="card-row">
            <span class="field-label">PayinRate:</span>
            <span class="field-value">{{ fmtRate(ch.feeRate) }}</span>
          </div>
          <div class="card-row">
            <span class="field-label">PayoutRate:</span>
            <span class="field-value">{{ fmtRate(ch.costRate) }}</span>
          </div>
          <div class="card-row">
            <span class="field-label">Period:</span>
            <span class="field-value">{{ ch.extra?.period || '--' }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="ad-placeholder">
      <p>广告发布功能即将上线</p>
    </div>

    <!-- Detail Modal -->
    <Teleport to="body">
      <div v-if="showDetail && selectedChannel" class="modal-overlay" @click.self="closeDetail">
        <div class="modal">
          <div class="modal-head">
            <h3>{{ selectedChannel.name }}</h3>
            <button class="close-btn" @click="closeDetail">×</button>
          </div>
          <div class="modal-body">
            <!-- 基础信息 -->
            <p class="section-title title-blue">基础信息</p>
            <div class="detail-row">
              <span class="detail-label">业务类型</span>
              <div class="badges">
                <span
                  v-for="t in splitTypes(selectedChannel.businessTypes)"
                  :key="t"
                  class="badge badge-type"
                >{{ t }}</span>
              </div>
            </div>
            <div class="detail-row">
              <span class="detail-label">Country:</span>
              <span class="badge badge-country">{{ fmtAreaType(selectedChannel.areaType) }}</span>
            </div>

            <!-- 费率 -->
            <p class="section-title title-yellow">费率</p>
            <div class="detail-row">
              <span class="detail-label">代收:</span>
              <span class="detail-value">{{ fmtRate(selectedChannel.feeRate) }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">金额范围:</span>
              <span class="detail-value">{{ selectedChannel.extra?.payinrange || '--' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">Payout:</span>
              <span class="detail-value">{{ fmtRate(selectedChannel.costRate) }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">代付金额:</span>
              <span class="detail-value">{{ selectedChannel.extra?.payoutrange || '--' }}</span>
            </div>

            <!-- 服务信息 -->
            <template v-if="selectedChannel.extra?.timerange || selectedChannel.extra?.period">
              <p class="section-title title-green">服务信息</p>
              <div v-if="selectedChannel.extra?.timerange" class="detail-row">
                <span class="detail-label">时间段:</span>
                <span class="detail-value">{{ selectedChannel.extra.timerange }}</span>
              </div>
              <div v-if="selectedChannel.extra?.period" class="detail-row">
                <span class="detail-label">结算周期:</span>
                <span class="detail-value">{{ selectedChannel.extra.period }}</span>
              </div>
            </template>

            <!-- 其他信息 -->
            <p class="section-title title-purple">其他信息</p>
            <template v-for="(val, key) in selectedChannel.extra" :key="key">
              <div
                v-if="!['payinrange','payoutrange','timerange','period'].includes(key)"
                class="detail-row"
              >
                <span class="detail-label">{{ extraLabel(key) }}:</span>
                <span class="detail-value">{{ fmtExtraVal(key, val) }}</span>
              </div>
            </template>
          </div>
          <div class="modal-footer">
            <button class="contact-btn">联系</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.channel-market {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
}

.market-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* Tab bar */
.market-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  padding: 8px 16px;
  gap: 12px;
  flex-wrap: wrap;
}

.tabs {
  display: flex;
  gap: 4px;
}

.tab {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.55);
  padding: 6px 18px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.tab.active {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.controls {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.controls select {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 6px;
  padding: 5px 10px;
  color: rgba(255, 255, 255, 0.75);
  font-size: 13px;
  min-width: 90px;
}

.controls select option {
  background: #1a1c2e;
  color: #fff;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pg-btn {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 5px;
  color: rgba(255, 255, 255, 0.8);
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}

.pg-btn:disabled {
  opacity: 0.4;
  cursor: default;
}

.pg-info {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  min-width: 36px;
  text-align: center;
}

/* Card grid */
.card-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 14px;
  flex: 1;
  min-height: 0;
}

@media (max-width: 1400px) {
  .card-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 1100px) {
  .card-grid {
    grid-template-columns: repeat(3, 1fr);
    grid-template-rows: none;
  }
}

@media (max-width: 760px) {
  .card-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.ch-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: clamp(10px, 1.2vw, 18px) clamp(12px, 1.4vw, 20px);
  display: flex;
  flex-direction: column;
  gap: clamp(6px, 0.7vw, 12px);
  min-height: 0;
  overflow: hidden;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-name {
  font-weight: 700;
  font-size: clamp(13px, 1vw, 16px);
  color: #fff;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: none;
  color: #4d9de0;
  font-size: clamp(11px, 0.8vw, 13px);
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}

.detail-btn:hover {
  color: #7db8e8;
}

.card-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: clamp(11px, 0.85vw, 14px);
}

.card-row--types {
  flex-direction: column;
  gap: 6px;
}

.field-label {
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
  white-space: nowrap;
  min-width: clamp(56px, 5vw, 80px);
}

.card-row--types .field-label {
  min-width: unset;
}

.field-value {
  color: #fff;
  font-weight: 600;
}

/* Badges */
.badges {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.badge {
  padding: 2px 7px;
  border-radius: 4px;
  font-size: clamp(10px, 0.75vw, 13px);
  font-weight: 500;
}

.badge-country {
  background: #1f7a45;
  color: #fff;
}

.badge-type {
  background: #8b5a12;
  color: #fff;
}

/* Placeholder */
.loading-tip,
.empty-tip,
.ad-placeholder {
  padding: 40px;
  text-align: center;
  color: rgba(255, 255, 255, 0.45);
  font-size: 14px;
}

/* Modal overlay */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: #1a1c2e;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  width: 480px;
  max-width: 95vw;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.modal-head h3 {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  margin: 0;
}

.close-btn {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.6);
  font-size: 22px;
  cursor: pointer;
  line-height: 1;
  padding: 0 4px;
}

.close-btn:hover {
  color: #fff;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-title {
  font-weight: 700;
  font-size: 14px;
  margin: 8px 0 4px;
}

.title-blue { color: #4d9de0; }
.title-yellow { color: #e0a44d; }
.title-green { color: #4de09a; }
.title-purple { color: #b44de0; }

.detail-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  font-size: 13px;
}

.detail-label {
  color: rgba(255, 255, 255, 0.6);
  font-weight: 600;
  min-width: 80px;
  white-space: nowrap;
}

.detail-value {
  color: #fff;
}

.modal-footer {
  padding: 14px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.contact-btn {
  width: 100%;
  padding: 11px;
  background: #2d7de8;
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.contact-btn:hover {
  background: #3a8ef0;
}
</style>
