<script setup>
import { onMounted, ref } from 'vue';
import http from '../services/http';

const loading = ref(false);
const saving = ref(false);
const loadError = ref('');
const saveMsg = ref('');
const saveMsgType = ref('');

const form = ref({
  iconUrl: '',
  domain: '',
  telegramcustomservice: '',
  customerServiceEmail: '',
  customerServiceWhatsapp: '',
  telegramBotToken: '',
  telegramGroupId: '',
  payoutNotifyGroupId: '',
  telegramBotTokenSupplier: '',
  telegramLoginVerification: false,
  telegramBotUsername: '',
  accountingGroupId: ''
});

const fetchSettings = async () => {
  loading.value = true;
  loadError.value = '';
  try {
    const { data: resp } = await http.get('/admin/system/settings/telegram');
    const payload = resp?.data ?? resp ?? {};
    Object.assign(form.value, {
      iconUrl: payload.iconUrl || '',
      domain: payload.domain || '',
      telegramcustomservice: payload.telegramcustomservice || '',
      customerServiceEmail: payload.customerServiceEmail || '',
      customerServiceWhatsapp: payload.customerServiceWhatsapp || '',
      telegramBotToken: payload.telegramBotToken || '',
      telegramGroupId: payload.telegramGroupId || '',
      payoutNotifyGroupId: payload.payoutNotifyGroupId || '',
      telegramBotTokenSupplier: payload.telegramBotTokenSupplier || '',
      telegramLoginVerification: !!payload.telegramLoginVerification,
      telegramBotUsername: payload.telegramBotUsername || '',
      accountingGroupId: payload.accountingGroupId || ''
    });
  } catch {
    loadError.value = '加载配置失败，请刷新重试';
  } finally {
    loading.value = false;
  }
};

const handleSubmit = async () => {
  saving.value = true;
  saveMsg.value = '';
  try {
    await http.put('/admin/system/settings/telegram', form.value);
    saveMsg.value = '保存成功';
    saveMsgType.value = 'success';
  } catch {
    saveMsg.value = '保存失败，请重试';
    saveMsgType.value = 'error';
  } finally {
    saving.value = false;
    setTimeout(() => { saveMsg.value = ''; }, 3000);
  }
};

onMounted(fetchSettings);
</script>

<template>
  <div class="settings-view">
    <p v-if="loadError" class="status-msg error">{{ loadError }}</p>
    <p v-else-if="loading" class="status-msg">加载中...</p>

    <form v-else class="settings-form" @submit.prevent="handleSubmit">
      <!-- 图标 -->
      <div class="form-row">
        <label class="form-label">图标</label>
        <div class="form-field">
          <div v-if="form.iconUrl" class="icon-preview">
            <img :src="form.iconUrl" alt="图标" />
            <button type="button" class="btn-delete" @click="form.iconUrl = ''">删除</button>
          </div>
          <input
            v-else
            v-model="form.iconUrl"
            type="text"
            class="field-input"
            placeholder="请输入图标URL"
          />
        </div>
      </div>

      <!-- 域名 -->
      <div class="form-row">
        <label class="form-label">域名</label>
        <div class="form-field">
          <input v-model="form.domain" type="text" class="field-input" placeholder="请输入域名，如：example.com" />
        </div>
      </div>

      <!-- 客服telegram -->
      <div class="form-row">
        <label class="form-label">客服telegram</label>
        <div class="form-field">
          <input v-model="form.telegramcustomservice" type="text" class="field-input" placeholder="@username" />
        </div>
      </div>

      <!-- 客服邮箱 -->
      <div class="form-row">
        <label class="form-label">客服邮箱</label>
        <div class="form-field">
          <input v-model="form.customerServiceEmail" type="text" class="field-input" placeholder="请输入客服邮箱" />
        </div>
      </div>

      <!-- 客服Whatsapp -->
      <div class="form-row">
        <label class="form-label">客服Whatsapp</label>
        <div class="form-field">
          <input v-model="form.customerServiceWhatsapp" type="text" class="field-input" placeholder="请输入Whatsapp号码" />
        </div>
      </div>

      <!-- Telegram商户机器人Token -->
      <div class="form-row">
        <label class="form-label">Telegram商户机器<br/>人Token</label>
        <div class="form-field">
          <input v-model="form.telegramBotToken" type="text" class="field-input" placeholder="请输入Bot Token" />
          <p class="field-hint">Telegram机器人设置成功</p>
        </div>
      </div>

      <!-- Telegram群ID -->
      <div class="form-row">
        <label class="form-label">Telegram群ID</label>
        <div class="form-field">
          <input v-model="form.telegramGroupId" type="text" class="field-input" placeholder="请输入Telegram群ID" />
          <p class="field-hint">将向该Telegram群发送重要通知</p>
        </div>
      </div>

      <!-- 公众打款通知Telegram群ID -->
      <div class="form-row">
        <label class="form-label">公众打款通知<br/>Telegram群ID</label>
        <div class="form-field">
          <input v-model="form.payoutNotifyGroupId" type="text" class="field-input" placeholder="请输入Telegram群ID" />
          <p class="field-hint">将向该Telegram群发送公众打款相关通知</p>
        </div>
      </div>

      <!-- Telegram机器人Token(供应商) -->
      <div class="form-row">
        <label class="form-label">Telegram机器人<br/>Token(供应商)</label>
        <div class="form-field">
          <input v-model="form.telegramBotTokenSupplier" type="text" class="field-input" placeholder="请输入Bot Token" />
          <p class="field-hint warn">Telegram机器人设置失败，请检查输入的Token</p>
        </div>
      </div>

      <!-- Telegram登录验证 -->
      <div class="form-row">
        <label class="form-label">Telegram登录验<br/>证</label>
        <div class="form-field">
          <div class="toggle-row">
            <button
              type="button"
              class="toggle-btn"
              :class="{ on: form.telegramLoginVerification }"
              @click="form.telegramLoginVerification = !form.telegramLoginVerification"
            >
              <span class="toggle-knob"></span>
              <span class="toggle-label">{{ form.telegramLoginVerification ? 'ON' : 'OFF' }}</span>
            </button>
          </div>
          <p class="field-hint">
            登录时启用Telegram二次确认，请先在Telegram中设置域名。
            <a href="#" class="field-link">查看</a>
          </p>
        </div>
      </div>

      <!-- 商户机器人的Username -->
      <div class="form-row">
        <label class="form-label">商户机器人的<br/>Username</label>
        <div class="form-field">
          <input v-model="form.telegramBotUsername" type="text" class="field-input" placeholder="payzay_bot" />
        </div>
      </div>

      <!-- 记账电报群ID -->
      <div class="form-row">
        <label class="form-label">记账电报群ID</label>
        <div class="form-field">
          <input v-model="form.accountingGroupId" type="text" class="field-input" placeholder="请输入电报群ID" />
        </div>
      </div>

      <!-- 提示消息 -->
      <p v-if="saveMsg" class="save-msg" :class="saveMsgType">{{ saveMsg }}</p>

      <!-- 提交 -->
      <div class="form-submit">
        <button type="submit" class="btn-submit" :disabled="saving">
          {{ saving ? '保存中...' : '提交' }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.settings-view {
  max-width: 760px;
}

.status-msg {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  padding: 20px 0;
}
.status-msg.error { color: #f87171; }

.settings-form {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* 每一行 */
.form-row {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.form-label {
  width: 130px;
  flex-shrink: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.65);
  padding-top: 8px;
  line-height: 1.5;
  text-align: right;
}

.form-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-input {
  width: 100%;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  outline: none;
  box-sizing: border-box;
}
.field-input::placeholder { color: rgba(255, 255, 255, 0.25); }
.field-input:focus { border-color: rgba(99, 102, 241, 0.5); }

.field-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
  margin: 0;
  line-height: 1.5;
}
.field-hint.warn { color: #f87171; }

.field-link {
  color: #6366f1;
  text-decoration: none;
}
.field-link:hover { text-decoration: underline; }

/* 图标预览 */
.icon-preview {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.icon-preview img {
  width: 120px;
  height: 120px;
  object-fit: contain;
  background: #fff;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.btn-delete {
  padding: 5px 14px;
  background: #f59e0b;
  border: none;
  border-radius: 6px;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}
.btn-delete:hover { background: #d97706; }

/* 开关 */
.toggle-row {
  display: flex;
  align-items: center;
}

.toggle-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  padding: 3px 12px 3px 4px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  font-weight: 600;
}

.toggle-btn.on {
  background: rgba(99, 102, 241, 0.2);
  border-color: rgba(99, 102, 241, 0.4);
  color: #a5b4fc;
}

.toggle-knob {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.35);
  flex-shrink: 0;
  transition: background 0.2s;
}

.toggle-btn.on .toggle-knob {
  background: #6366f1;
}

/* 保存消息 */
.save-msg {
  font-size: 13px;
  padding: 10px 0 0;
  margin: 0;
}
.save-msg.success { color: #34d399; }
.save-msg.error { color: #f87171; }

/* 提交按钮区域 */
.form-submit {
  padding-top: 20px;
}

.btn-submit {
  width: 100%;
  padding: 12px;
  background: #2563eb;
  border: none;
  border-radius: 6px;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
  letter-spacing: 0.04em;
}
.btn-submit:hover:not(:disabled) { background: #1d4ed8; }
.btn-submit:disabled { opacity: 0.5; cursor: default; }
</style>
