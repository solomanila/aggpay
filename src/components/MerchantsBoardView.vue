<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import http from '../services/http';

defineProps({ data: { type: Object, required: false, default: () => ({}) } });

// ── Tab ──────────────────────────────────────────────────────────
const activeTab = ref('PAYMENT');

// ── Filters ──────────────────────────────────────────────────────
const filterMerchantId = ref('');
const filterChannelId  = ref('');
const filterEnabled    = ref(true);
const filterCurrency   = ref('INR');

// ── Board data ────────────────────────────────────────────────────
const allMerchants  = ref([]);
const allChannels   = ref([]);
const allConfigs    = ref([]);
const boardLoading  = ref(false);
const boardError    = ref('');
const channelOptions = ref([]);   // for modal dropdown

const loadBoard = async () => {
  boardLoading.value = true;
  boardError.value = '';
  try {
    const params = { channelType: activeTab.value };
    if (filterMerchantId.value) params.merchantId = filterMerchantId.value;
    if (filterChannelId.value)  params.channelId  = filterChannelId.value;
    if (filterCurrency.value)   params.currency   = filterCurrency.value;
    const { data: resp } = await http.get('/admin/merchant-board/data', { params });
    console.log('[Board] raw resp:', resp);
    const d = resp?.data ?? resp;
    allMerchants.value = d?.merchants ?? [];
    allChannels.value  = d?.channels  ?? [];
    allConfigs.value   = d?.configs   ?? [];
    console.log('[Board] merchants:', allMerchants.value.length, 'channels:', allChannels.value.length, 'configs:', allConfigs.value.length);
  } catch (e) {
    console.error('[Board] load failed:', e);
    boardError.value = `加载失败: ${e?.message || e}`;
  } finally {
    boardLoading.value = false;
  }
};

const loadChannelOptions = async () => {
  try {
    const { data: resp } = await http.get('/admin/merchant-board/channel-options');
    channelOptions.value = resp?.data ?? resp ?? [];
  } catch (e) { console.error('channel options failed', e); }
};

// ── Computed display ─────────────────────────────────────────────
const displayMerchants = computed(() =>
  filterMerchantId.value
    ? allMerchants.value.filter(m => String(m.platformId) === String(filterMerchantId.value))
    : allMerchants.value
);

const displayChannels = computed(() => {
  let cols = allChannels.value;
  if (filterChannelId.value) cols = cols.filter(c => String(c.id) === String(filterChannelId.value));
  if (filterEnabled.value) {
    const ids = new Set(allConfigs.value.filter(c => c.enabled === 1).map(c => c.payConfigChannelId));
    cols = cols.filter(c => ids.has(c.id));
  }
  return cols;
});

// O(1) cell lookup
const configMap = computed(() => {
  const m = {};
  for (const c of allConfigs.value) m[`${c.platformId}_${c.payConfigChannelId}`] = c;
  return m;
});
const getConfig = (pid, cid) => configMap.value[`${pid}_${cid}`] ?? null;

const fmtLimit = (v) => {
  if (v == null) return '—';
  if (v === 0) return '0K';
  const k = v / 1000;
  return (k >= 1000 ? (k / 1000).toFixed(1) + 'M' : k + 'K');
};

// ── Tab switch ────────────────────────────────────────────────────
const switchTab = (t) => { activeTab.value = t; loadBoard(); };

// ── Enable toggle per cell ────────────────────────────────────────
const toggleConfigEnabled = async (config) => {
  const newVal = config.enabled === 1 ? 0 : 1;
  try {
    await http.patch('/admin/merchant/channel-config/enabled', null, { params: { id: config.id, enabled: newVal } });
    config.enabled = newVal;
  } catch (e) { console.error(e); }
};

// ── Delete config ─────────────────────────────────────────────────
const deleteConfig = async (config) => {
  if (!confirm(`确认删除该通道配置?`)) return;
  try {
    await http.delete('/admin/merchant/channel-config/delete', { params: { id: config.id } });
    const idx = allConfigs.value.indexOf(config);
    if (idx >= 0) allConfigs.value.splice(idx, 1);
  } catch (e) { console.error(e); }
};

// ── Column global toggle ──────────────────────────────────────────
const toggleColumnGlobal = async (col) => {
  const colConfigs = allConfigs.value.filter(c => c.payConfigChannelId === col.id);
  const anyEnabled = colConfigs.some(c => c.enabled === 1);
  const newVal = anyEnabled ? 0 : 1;
  try {
    await Promise.all(colConfigs.map(c =>
      http.patch('/admin/merchant/channel-config/enabled', null, { params: { id: c.id, enabled: newVal } })
    ));
    colConfigs.forEach(c => (c.enabled = newVal));
  } catch (e) { console.error(e); }
};

// ── Config edit panel ─────────────────────────────────────────────
const panelVisible  = ref(false);
const panelMode     = ref('create');
const panelSaving   = ref(false);
const panelError    = ref('');

const form = reactive({
  id: null, platformId: null, payConfigChannelId: '',
  channelType: 'PAYMENT', dailyLimit: '', weight: 1,
  enabled: 0, startTime: '', endTime: '',
  settlementCycle: '', autoSettle: 0,
  minAmount: 0, maxAmount: '',
  currency: 'INR', payMode: '', payPage: '',
  feeRate: 0, feeFixed: 0, tieredRateEnabled: 0, tieredRateConfig: '',
});

const resetForm = () => {
  Object.assign(form, {
    id: null, payConfigChannelId: '', channelType: activeTab.value,
    dailyLimit: '', weight: 1, enabled: 0, startTime: '', endTime: '',
    settlementCycle: '', autoSettle: 0, minAmount: 0, maxAmount: '',
    currency: filterCurrency.value || 'INR', payMode: '', payPage: '',
    feeRate: 0, feeFixed: 0, tieredRateEnabled: 0, tieredRateConfig: '',
  });
};

const openAdd = (merchant, col) => {
  panelMode.value = 'create';
  resetForm();
  form.platformId = merchant.platformId;
  if (col) form.payConfigChannelId = col.id;
  panelError.value = '';
  panelVisible.value = true;
};

const openEdit = (config) => {
  panelMode.value = 'edit';
  Object.assign(form, {
    id: config.id, platformId: config.platformId,
    payConfigChannelId: config.payConfigChannelId,
    channelType: config.channelType ?? activeTab.value,
    dailyLimit: config.dailyLimit ?? '', weight: config.weight ?? 1,
    enabled: config.enabled ?? 0, startTime: config.startTime ?? '',
    endTime: config.endTime ?? '', settlementCycle: config.settlementCycle ?? '',
    autoSettle: config.autoSettle ?? 0, minAmount: config.minAmount ?? 0,
    maxAmount: config.maxAmount ?? '', currency: config.currency ?? 'INR',
    payMode: config.payMode ?? '', payPage: config.payPage ?? '',
    feeRate: config.feeRate ?? 0, feeFixed: config.feeFixed ?? 0,
    tieredRateEnabled: config.tieredRateEnabled ?? 0,
    tieredRateConfig: config.tieredRateConfig ?? '',
  });
  panelError.value = '';
  panelVisible.value = true;
};

const submitPanel = async () => {
  if (!form.payConfigChannelId) { panelError.value = '请选择支付通道'; return; }
  panelSaving.value = true;
  panelError.value = '';
  try {
    const payload = {
      id: form.id, platformId: form.platformId,
      payConfigChannelId: Number(form.payConfigChannelId),
      channelType: form.channelType,
      dailyLimit: form.dailyLimit !== '' ? Number(form.dailyLimit) : null,
      weight: Number(form.weight), enabled: form.enabled,
      startTime: form.startTime || null, endTime: form.endTime || null,
      settlementCycle: form.settlementCycle !== '' ? Number(form.settlementCycle) : null,
      autoSettle: form.autoSettle,
      minAmount: form.minAmount !== '' ? Number(form.minAmount) : null,
      maxAmount: form.maxAmount !== '' ? Number(form.maxAmount) : null,
      currency: form.currency, payMode: form.payMode || null,
      payPage: form.payPage || null,
      feeRate: Number(form.feeRate), feeFixed: Number(form.feeFixed),
      tieredRateEnabled: form.tieredRateEnabled,
      tieredRateConfig: form.tieredRateConfig || null,
    };
    if (panelMode.value === 'create') await http.post('/admin/merchant/channel-config/create', payload);
    else await http.put('/admin/merchant/channel-config/update', payload);
    panelVisible.value = false;
    await loadBoard();
  } catch (e) {
    panelError.value = '保存失败，请重试';
  } finally {
    panelSaving.value = false;
  }
};

const selectedChannelLabel = computed(() => {
  const opt = channelOptions.value.find(c => c.id == form.payConfigChannelId);
  return opt ? `${opt.payConfigTitle} / ${opt.title}` : '';
});

// ── Pay test modal ────────────────────────────────────────────────
const payTestVisible = ref(false);
const payTestConfig  = ref(null);
const payTestAmount  = ref(100);
const payTestSaving  = ref(false);

const openPayTest = (config) => {
  payTestConfig.value = config; payTestAmount.value = 100; payTestVisible.value = true;
};
const submitPayTest = async () => {
  payTestSaving.value = true;
  try {
    await http.post('/admin/merchant/pay-test', {
      platformId: payTestConfig.value.platformId,
      payConfigChannelId: payTestConfig.value.payConfigChannelId,
      amount: Number(payTestAmount.value),
    });
    payTestVisible.value = false;
  } catch (e) { console.error(e); } finally { payTestSaving.value = false; }
};

// ── Merchant edit panel ───────────────────────────────────────────
const mPanelVisible = ref(false);
const mPanelSaving  = ref(false);
const mPanelError   = ref('');
const mForm = reactive({
  platformId: null, title: '', status: 1, agentId: '', email: '', remark: '',
  dailyPayOrderLimit: 5, dailyWithdrawCountLimit: 5, dailyWithdrawAmountLimit: 50000,
  dailyPayLimit: '', dailyPayoutLimit: '',
  largePayoutRiskEnabled: 0, largePayoutRiskAmount: 500000,
  telegramGroupId: '', settlementNotify: 0,
});

const openMerchantEdit = async (merchant) => {
  mPanelError.value = '';
  mForm.platformId = merchant.platformId;
  try {
    const { data: resp } = await http.get(`/admin/merchant/detail/${merchant.platformId}`);
    const d = resp?.data ?? resp;
    Object.assign(mForm, {
      title: d.title ?? '', status: d.status ?? 1, agentId: d.agentId ?? '',
      email: d.email ?? '', remark: d.remark ?? '',
      dailyPayOrderLimit: d.dailyPayOrderLimit ?? 5,
      dailyWithdrawCountLimit: d.dailyWithdrawCountLimit ?? 5,
      dailyWithdrawAmountLimit: d.dailyWithdrawAmountLimit ?? 50000,
      dailyPayLimit: d.dailyPayLimit ?? '', dailyPayoutLimit: d.dailyPayoutLimit ?? '',
      largePayoutRiskEnabled: d.largePayoutRiskEnabled ?? 0,
      largePayoutRiskAmount: d.largePayoutRiskAmount ?? 500000,
      telegramGroupId: d.telegramGroupId ?? '', settlementNotify: d.settlementNotify ?? 0,
    });
  } catch (e) { console.error(e); }
  mPanelVisible.value = true;
};

const submitMerchantEdit = async () => {
  if (!mForm.title.trim()) { mPanelError.value = '名称不能为空'; return; }
  mPanelSaving.value = true; mPanelError.value = '';
  try {
    await http.put('/admin/merchant/update', {
      platformId: mForm.platformId, title: mForm.title, status: mForm.status,
      agentId: mForm.agentId !== '' ? Number(mForm.agentId) : null,
      email: mForm.email, remark: mForm.remark,
      dailyPayOrderLimit: mForm.dailyPayOrderLimit || null,
      dailyWithdrawCountLimit: mForm.dailyWithdrawCountLimit || null,
      dailyWithdrawAmountLimit: mForm.dailyWithdrawAmountLimit || null,
      dailyPayLimit: mForm.dailyPayLimit || null, dailyPayoutLimit: mForm.dailyPayoutLimit || null,
      largePayoutRiskEnabled: mForm.largePayoutRiskEnabled,
      largePayoutRiskAmount: mForm.largePayoutRiskAmount || null,
      telegramGroupId: mForm.telegramGroupId, settlementNotify: mForm.settlementNotify,
    });
    const m = allMerchants.value.find(x => x.platformId === mForm.platformId);
    if (m) m.title = mForm.title;
    mPanelVisible.value = false;
  } catch (e) { mPanelError.value = '保存失败'; } finally { mPanelSaving.value = false; }
};

onMounted(() => { loadBoard(); loadChannelOptions(); });
</script>

<template>
  <div class="bv">

    <!-- ── Tabs ─────────────────────────────────────────── -->
    <div class="bv-tabs">
      <button class="bv-tab" :class="{active: activeTab==='PAYMENT'}" @click="switchTab('PAYMENT')">代收</button>
      <button class="bv-tab" :class="{active: activeTab==='PAYOUT'}"  @click="switchTab('PAYOUT')">出款</button>
    </div>

    <!-- ── Filter bar ────────────────────────────────────── -->
    <div class="bv-filters">
      <select v-model="filterMerchantId" class="bv-sel" @change="loadBoard">
        <option value="">商户</option>
        <option v-for="m in allMerchants" :key="m.platformId" :value="m.platformId">{{ m.title }}</option>
      </select>
      <select v-model="filterChannelId" class="bv-sel" @change="loadBoard">
        <option value="">通道</option>
        <option v-for="c in allChannels" :key="c.id" :value="c.id">{{ c.title }}</option>
      </select>
      <span class="bv-chip" :class="{active: filterEnabled}" @click="filterEnabled=!filterEnabled">
        启用 <span v-if="filterEnabled">×</span>
      </span>
      <span v-if="filterCurrency" class="bv-chip active" @click="filterCurrency=''; loadBoard()">
        {{ filterCurrency }} ×
      </span>
    </div>

    <!-- ── Board matrix ─────────────────────────────────── -->
    <div class="bv-board">
      <div v-if="boardError" class="bv-error">{{ boardError }}</div>
      <div v-if="boardLoading" class="bv-loading">加载中…</div>
      <div v-else class="bv-scroll">
        <table class="bv-table">
          <thead>
            <tr>
              <!-- fixed corner -->
              <th class="bv-th bv-th-fixed"></th>
              <!-- channel column headers -->
              <th v-for="col in displayChannels" :key="col.id" class="bv-th bv-th-chan">
                <div class="chan-name">{{ col.title }}</div>
                <div class="chan-icons">
                  <button class="ci-btn" title="全局切换启用" @click="toggleColumnGlobal(col)">
                    <span class="ci ci-power">⏻</span>
                  </button>
                  <button v-if="activeTab==='PAYOUT'" class="ci-btn" title="同步" @click="loadBoard">
                    <span class="ci ci-refresh">↺</span>
                  </button>
                </div>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="merchant in displayMerchants" :key="merchant.platformId" class="bv-tr">
              <!-- merchant name cell (sticky) -->
              <td class="bv-td bv-td-fixed">
                <span class="m-name">{{ merchant.title }}</span>
                <button class="m-edit-btn" @click="openMerchantEdit(merchant)">✎</button>
              </td>
              <!-- config cells -->
              <td v-for="col in displayChannels" :key="col.id" class="bv-td bv-td-cell">
                <template v-if="getConfig(merchant.platformId, col.id)">
                  <div class="cfg-card" :class="getConfig(merchant.platformId,col.id).enabled===1 ? 'card-on':'card-off'">
                    <!-- action icons row -->
                    <div class="cfg-icons">
                      <button class="cfg-ico ico-status"
                        :class="getConfig(merchant.platformId,col.id).enabled===1 ? 'ico-check':'ico-pause'"
                        @click="toggleConfigEnabled(getConfig(merchant.platformId,col.id))">
                        {{ getConfig(merchant.platformId,col.id).enabled===1 ? '✓' : '⏸' }}
                      </button>
                      <button class="cfg-ico ico-link" @click="openPayTest(getConfig(merchant.platformId,col.id))">🔗</button>
                      <button class="cfg-ico ico-edit" @click="openEdit(getConfig(merchant.platformId,col.id))">✎</button>
                      <button class="cfg-ico ico-del" @click="deleteConfig(getConfig(merchant.platformId,col.id))">🗑</button>
                    </div>
                    <!-- limit row -->
                    <div class="cfg-row">
                      <span class="cfg-lbl">日限额:</span>
                      <span class="cfg-val">{{ fmtLimit(getConfig(merchant.platformId,col.id).dailyLimit) }}</span>
                    </div>
                    <!-- weight bar -->
                    <div class="cfg-weight">
                      <span class="cfg-lbl">Weights:</span>
                      <span class="cfg-val">{{ getConfig(merchant.platformId,col.id).weight ?? 1 }}</span>
                    </div>
                  </div>
                </template>
                <template v-else>
                  <button class="add-btn" @click="openAdd(merchant, col)">+</button>
                </template>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!boardLoading && displayMerchants.length===0" class="bv-empty">暂无商户数据</div>
        <div v-if="!boardLoading && displayMerchants.length>0 && displayChannels.length===0" class="bv-empty">暂无通道数据（请先为商户添加通道配置）</div>
      </div>
    </div>

    <!-- ══════════════════════════════════════════════════════
         Config edit RIGHT-SIDE PANEL
    ══════════════════════════════════════════════════════ -->
    <transition name="slide-right">
      <div v-if="panelVisible" class="rp-overlay" @click.self="panelVisible=false">
        <div class="rp">
          <button class="rp-close" @click="panelVisible=false">×</button>

          <!-- row 1: 支付通道 / 通道类型 / 每日限额 -->
          <div class="rp-grid3">
            <div class="fp">
              <label class="fl">支付通道</label>
              <template v-if="panelMode === 'edit'">
                <input class="fi" :value="selectedChannelLabel" readonly />
              </template>
              <template v-else>
                <select v-model="form.payConfigChannelId" class="fi">
                  <option value="" disabled>请选择</option>
                  <option v-for="o in channelOptions" :key="o.id" :value="o.id">{{ o.payConfigTitle }} / {{ o.title }}</option>
                </select>
              </template>
            </div>
            <div class="fp">
              <label class="fl">通道类型</label>
              <input class="fi" :value="form.channelType==='PAYMENT'?'Payment':'Payout'" readonly />
            </div>
            <div class="fp">
              <label class="fl req">每日限额</label>
              <input v-model="form.dailyLimit" class="fi" type="number" min="0" />
            </div>
          </div>

          <!-- row 2: 权重 / 开始时间 / 结束时间 -->
          <div class="rp-grid3">
            <div class="fp">
              <label class="fl">权重</label>
              <input v-model="form.weight" class="fi" type="number" min="1" />
            </div>
            <div class="fp">
              <label class="fl">开始时间</label>
              <input v-model="form.startTime" class="fi" placeholder="example: 08:00:00" />
            </div>
            <div class="fp">
              <label class="fl">结束时间</label>
              <input v-model="form.endTime" class="fi" placeholder="example: 08:00:00" />
            </div>
          </div>

          <!-- row 3: 启用 / 结算周期 / 自动到账 -->
          <div class="rp-grid3">
            <div class="fp">
              <label class="fl">启用</label>
              <button class="tog" :class="form.enabled?'tog-on':'tog-off'" @click="form.enabled=form.enabled?0:1" />
            </div>
            <div class="fp">
              <label class="fl">结算周期（分钟）</label>
              <input v-model="form.settlementCycle" class="fi" type="number" min="0" />
            </div>
            <div class="fp">
              <label class="fl">自动到账</label>
              <button class="tog" :class="form.autoSettle?'tog-on':'tog-off'" @click="form.autoSettle=form.autoSettle?0:1" />
            </div>
          </div>

          <!-- row 4: 单笔最小 / 单笔最大 -->
          <div class="rp-grid2">
            <div class="fp">
              <label class="fl">单笔最小金额</label>
              <input v-model="form.minAmount" class="fi" type="number" min="0" />
            </div>
            <div class="fp">
              <label class="fl">单笔最大金额</label>
              <input v-model="form.maxAmount" class="fi" type="number" min="0" />
            </div>
          </div>

          <!-- 支付模式 section (PAYMENT only) -->
          <template v-if="activeTab==='PAYMENT'">
            <div class="rp-section">支付模式</div>
            <div class="rp-grid2">
              <div class="fp">
                <label class="fl"><span class="currency-prefix">{{ form.currency }}</span> 支付模式</label>
                <select v-model="form.payMode" class="fi">
                  <option value=""></option>
                  <option value="WEB">WEB</option>
                  <option value="H5">H5</option>
                  <option value="APP">APP</option>
                  <option value="QR">QR</option>
                </select>
              </div>
              <div class="fp">
                <label class="fl">支付页面</label>
                <select v-model="form.payPage" class="fi">
                  <option value=""></option>
                  <option value="INR-native page (Include GPAY)">INR-native page (Include GPAY)</option>
                  <option value="default">default</option>
                </select>
              </div>
            </div>
          </template>

          <!-- 默认费率配置 -->
          <div class="rp-section">默认费率配置</div>
          <div class="rp-grid2">
            <div class="fp">
              <label class="fl req">费率</label>
              <div class="fi-pct-wrap">
                <input v-model="form.feeRate" class="fi fi-pct" type="number" min="0" step="0.01" />
                <span class="pct-sym">%</span>
              </div>
            </div>
            <div class="fp">
              <label class="fl req">固定金额</label>
              <input v-model="form.feeFixed" class="fi" type="number" min="0" />
            </div>
          </div>
          <div class="fp">
            <label class="fl">开启分段费率配置 ℹ</label>
            <button class="tog" :class="form.tieredRateEnabled?'tog-on':'tog-off'"
              @click="form.tieredRateEnabled=form.tieredRateEnabled?0:1" />
          </div>

          <p v-if="panelError" class="err">{{ panelError }}</p>
          <div class="rp-footer">
            <button class="btn-ghost" @click="panelVisible=false">取消</button>
            <button class="btn-primary" :disabled="panelSaving" @click="submitPanel">
              {{ panelSaving ? '保存中…' : 'Submit' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ══════════════════════════════════════════════════════
         收款测试 centered modal
    ══════════════════════════════════════════════════════ -->
    <div v-if="payTestVisible" class="ct-overlay" @click.self="payTestVisible=false">
      <div class="ct-modal">
        <div class="ct-hd">
          <span>收款测试</span>
          <button class="rp-close" @click="payTestVisible=false">×</button>
        </div>
        <div class="ct-body">
          <div class="fp">
            <label class="fl req">金额</label>
            <input v-model="payTestAmount" class="fi" type="number" min="1" />
          </div>
        </div>
        <div class="ct-ft">
          <button class="btn-primary" :disabled="payTestSaving" @click="submitPayTest">
            {{ payTestSaving ? '提交中…' : '提交' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ══════════════════════════════════════════════════════
         Merchant edit RIGHT-SIDE PANEL
    ══════════════════════════════════════════════════════ -->
    <transition name="slide-right">
      <div v-if="mPanelVisible" class="rp-overlay" @click.self="mPanelVisible=false">
        <div class="rp">
          <div class="rp-title">编辑</div>
          <button class="rp-close" @click="mPanelVisible=false">×</button>
          <div class="fp">
            <label class="fl req">名称</label>
            <input v-model="mForm.title" class="fi" />
          </div>
          <div class="fp">
            <label class="fl">备注</label>
            <input v-model="mForm.remark" class="fi" />
          </div>
          <div class="fp">
            <label class="fl">代理商</label>
            <select class="fi"><option>代理商</option></select>
          </div>
          <div class="rp-grid2">
            <div class="fp">
              <label class="fl">邮箱</label>
              <input v-model="mForm.email" class="fi" type="email" />
            </div>
            <div class="fp">
              <label class="fl req">用户每日收款订单数限制</label>
              <input v-model="mForm.dailyPayOrderLimit" class="fi" type="number" />
            </div>
          </div>
          <div class="rp-grid2">
            <div class="fp">
              <label class="fl req">用户每日提现次数限制</label>
              <input v-model="mForm.dailyWithdrawCountLimit" class="fi" type="number" />
            </div>
            <div class="fp">
              <label class="fl req">用户每日提款金额限制</label>
              <input v-model="mForm.dailyWithdrawAmountLimit" class="fi" type="number" />
            </div>
          </div>
          <div class="rp-grid2">
            <div class="fp">
              <label class="fl">大额单笔出款风控开关</label>
              <button class="tog" :class="mForm.largePayoutRiskEnabled?'tog-on':'tog-off'"
                @click="mForm.largePayoutRiskEnabled=mForm.largePayoutRiskEnabled?0:1" />
            </div>
            <div class="fp">
              <label class="fl req">大额单笔出款风控金额</label>
              <input v-model="mForm.largePayoutRiskAmount" class="fi" type="number" />
            </div>
          </div>
          <div class="rp-grid2">
            <div class="fp">
              <label class="fl">Telegram群ID</label>
              <input v-model="mForm.telegramGroupId" class="fi" />
            </div>
            <div class="fp">
              <label class="fl">结算通知</label>
              <button class="tog" :class="mForm.settlementNotify?'tog-on':'tog-off'"
                @click="mForm.settlementNotify=mForm.settlementNotify?0:1" />
            </div>
          </div>
          <p v-if="mPanelError" class="err">{{ mPanelError }}</p>
          <div class="rp-footer">
            <button class="btn-ghost" @click="mPanelVisible=false">取消</button>
            <button class="btn-primary" :disabled="mPanelSaving" @click="submitMerchantEdit">
              {{ mPanelSaving ? '保存中…' : 'Submit' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

  </div>
</template>

<style scoped>
/* ══ Root ══════════════════════════════════════════════════════════ */
.bv { display: flex; flex-direction: column; height: 100%; overflow: hidden; }

/* ══ Tabs ══════════════════════════════════════════════════════════ */
.bv-tabs { display: flex; border-bottom: 1px solid rgba(255,255,255,0.08); flex-shrink: 0; }
.bv-tab { background: none; border: none; border-bottom: 2px solid transparent; color: rgba(255,255,255,0.5); cursor: pointer; font-size: 14px; padding: 10px 22px; transition: color .15s; }
.bv-tab.active { border-bottom-color: #4a5fff; color: #e8eaf6; }

/* ══ Filter bar ════════════════════════════════════════════════════ */
.bv-filters { align-items: center; display: flex; flex-shrink: 0; flex-wrap: wrap; gap: 8px; padding: 10px 0; }
.bv-sel { background: rgba(20,21,40,0.9); border: 1px solid rgba(255,255,255,0.12); border-radius: 6px; color: #e8eaf6; font-size: 13px; padding: 6px 28px 6px 10px; }
.bv-sel option { background: #14152a; }
.bv-chip { background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.15); border-radius: 16px; color: rgba(255,255,255,0.6); cursor: pointer; font-size: 12px; padding: 4px 12px; user-select: none; }
.bv-chip.active { background: rgba(74,95,255,0.18); border-color: #4a5fff; color: #a0aaff; }

/* ══ Board ═════════════════════════════════════════════════════════ */
.bv-board { flex: 1; overflow: hidden; }
.bv-loading, .bv-empty { color: rgba(255,255,255,0.35); font-size: 13px; padding: 40px 20px; text-align: center; }
.bv-error { background: rgba(231,76,60,0.12); border: 1px solid rgba(231,76,60,0.3); border-radius: 6px; color: #e74c3c; font-size: 13px; margin: 10px 0; padding: 10px 16px; }
.bv-scroll { height: 100%; overflow: auto; }

/* ══ Table ═════════════════════════════════════════════════════════ */
.bv-table { border-collapse: separate; border-spacing: 0; font-size: 13px; min-width: max-content; }
.bv-th, .bv-td { border: 1px solid rgba(255,255,255,0.05); }

/* fixed left column */
.bv-th-fixed, .bv-td-fixed {
  background: #0c0d1a;
  left: 0;
  min-width: 148px;
  position: sticky;
  z-index: 2;
  width: 148px;
}
.bv-th-fixed { z-index: 3; }

/* channel header */
.bv-th-chan { background: #10112a; min-width: 178px; padding: 8px 12px; text-align: center; vertical-align: top; }
.chan-name { color: #c5caee; font-size: 12px; font-weight: 500; margin-bottom: 6px; }
.chan-icons { align-items: center; display: flex; gap: 6px; justify-content: center; }
.ci-btn { background: none; border: none; cursor: pointer; padding: 2px; }
.ci { border-radius: 50%; color: #4a5fff; display: inline-block; font-size: 14px; line-height: 1; }
.ci-power { color: #4a9eff; }
.ci-refresh { color: #4a9eff; }

/* merchant cell */
.bv-td-fixed { padding: 10px 12px; }
.m-name { color: #e8eaf6; display: block; font-size: 13px; font-weight: 500; margin-bottom: 8px; }
.m-edit-btn { background: rgba(255,255,255,0.07); border: 1px solid rgba(255,255,255,0.14); border-radius: 5px; color: rgba(255,255,255,0.6); cursor: pointer; font-size: 12px; padding: 3px 10px; }

/* config cells */
.bv-td-cell { padding: 8px; text-align: center; vertical-align: middle; min-height: 90px; }

/* config card */
.cfg-card { border-radius: 6px; padding: 6px 8px; text-align: left; }
.card-on  { background: #1a5638; border: 1px solid #27ae60; }
.card-off { background: #2a2010; border: 1px solid #e67e22; }

/* card icons row */
.cfg-icons { display: flex; gap: 5px; margin-bottom: 7px; }
.cfg-ico { align-items: center; background: rgba(0,0,0,0.25); border: 1.5px solid rgba(255,255,255,0.25); border-radius: 50%; cursor: pointer; display: inline-flex; font-size: 11px; height: 22px; justify-content: center; width: 22px; padding: 0; }
.ico-check { border-color: #2ecc71; color: #2ecc71; }
.ico-pause { border-color: #e67e22; color: #e67e22; }
.ico-link  { border-color: #5dade2; color: #5dade2; }
.ico-edit  { border-color: #5dade2; color: #5dade2; }
.ico-del   { border-color: #e74c3c; color: #e74c3c; }

/* limit / weight rows */
.cfg-row { color: rgba(255,255,255,0.85); display: flex; font-size: 12px; justify-content: space-between; margin-bottom: 4px; }
.cfg-lbl { color: rgba(255,255,255,0.6); }
.cfg-val { font-weight: 600; }
.cfg-weight { background: rgba(0,0,0,0.2); border-radius: 3px; color: rgba(255,255,255,0.85); display: flex; font-size: 12px; justify-content: space-between; padding: 2px 4px; }

/* add (+) button */
.add-btn { align-items: center; background: #4a5fff; border: none; border-radius: 50%; color: #fff; cursor: pointer; display: inline-flex; font-size: 20px; height: 36px; justify-content: center; line-height: 1; width: 36px; }
.add-btn:hover { opacity: .85; }

/* ══ Right-side panel ══════════════════════════════════════════════ */
.rp-overlay { background: rgba(0,0,0,0.6); bottom: 0; display: flex; justify-content: flex-end; left: 0; position: fixed; right: 0; top: 0; z-index: 1000; }
.rp { background: #12132a; border-left: 1px solid rgba(255,255,255,0.08); display: flex; flex-direction: column; gap: 14px; height: 100%; overflow-y: auto; padding: 20px 28px 20px 24px; position: relative; width: 680px; max-width: 90vw; }
.rp-close { background: none; border: none; color: rgba(255,255,255,0.5); cursor: pointer; font-size: 22px; line-height: 1; position: absolute; right: 16px; top: 14px; }
.rp-close:hover { color: #fff; }
.rp-title { color: #e8eaf6; font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.rp-grid3 { display: grid; gap: 14px; grid-template-columns: 1fr 1fr 1fr; }
.rp-grid2 { display: grid; gap: 14px; grid-template-columns: 1fr 1fr; }
.rp-section { border-top: 1px solid rgba(255,255,255,0.07); color: #8892c8; font-size: 13px; font-weight: 600; margin: 4px 0 0; padding-top: 14px; text-align: center; }
.rp-footer { border-top: 1px solid rgba(255,255,255,0.07); display: flex; gap: 10px; justify-content: flex-end; margin-top: 4px; padding-top: 16px; }

/* ══ Centered modal ════════════════════════════════════════════════ */
.ct-overlay { align-items: center; background: rgba(0,0,0,0.6); bottom: 0; display: flex; justify-content: center; left: 0; position: fixed; right: 0; top: 0; z-index: 1001; }
.ct-modal { background: #12132a; border: 1px solid rgba(255,255,255,0.1); border-radius: 10px; min-width: 360px; }
.ct-hd { align-items: center; border-bottom: 1px solid rgba(255,255,255,0.07); color: #e8eaf6; display: flex; font-size: 15px; font-weight: 600; justify-content: space-between; padding: 14px 20px; }
.ct-body { display: flex; flex-direction: column; gap: 12px; padding: 18px 20px; }
.ct-ft { display: flex; justify-content: flex-end; padding: 12px 20px 16px; }

/* ══ Form elements ═════════════════════════════════════════════════ */
.fp { display: flex; flex-direction: column; gap: 5px; }
.fl { color: rgba(255,255,255,0.55); font-size: 12px; }
.fl.req::before { color: #e74c3c; content: '* '; }
.fi { background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.12); border-radius: 6px; box-sizing: border-box; color: #e8eaf6; font-size: 13px; padding: 8px 12px; width: 100%; }
.fi:focus { border-color: #4a5fff; outline: none; }
.fi option, .fi:read-only { color: rgba(255,255,255,0.5); }
.fi-pct-wrap { align-items: center; display: flex; gap: 6px; }
.fi-pct { flex: 1; width: auto; }
.pct-sym { color: rgba(255,255,255,0.55); font-size: 13px; flex-shrink: 0; }
.currency-prefix { background: rgba(255,255,255,0.08); border-radius: 4px; font-size: 11px; font-weight: 600; margin-right: 6px; padding: 2px 6px; }
.tog { border: none; border-radius: 12px; cursor: pointer; font-size: 11px; font-weight: 600; min-width: 52px; padding: 5px 14px; }
.tog-on  { background: #27ae60; color: #fff; }
.tog-off { background: rgba(255,255,255,0.1); color: rgba(255,255,255,0.45); }
.err { color: #e74c3c; font-size: 12px; margin: 0; }

/* ══ Buttons ══════════════════════════════════════════════════════= */
.btn-primary { background: #4a5fff; border: none; border-radius: 6px; color: #fff; cursor: pointer; font-size: 13px; padding: 8px 22px; }
.btn-primary:disabled { opacity: .5; cursor: not-allowed; }
.btn-primary:hover:not(:disabled) { opacity: .85; }
.btn-ghost { background: rgba(255,255,255,0.07); border: none; border-radius: 6px; color: #c5c7d8; cursor: pointer; font-size: 13px; padding: 8px 18px; }

/* ══ Transition ════════════════════════════════════════════════════ */
.slide-right-enter-active, .slide-right-leave-active { transition: transform .25s ease; }
.slide-right-enter-from, .slide-right-leave-to { transform: translateX(100%); }
</style>
