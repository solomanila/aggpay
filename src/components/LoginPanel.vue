<script setup>
import { ref } from 'vue';
import { toDataURL } from 'qrcode';
import http from '../services/http';

const emit = defineEmits(['success']);

const account = ref('');
const password = ref('');
const otpCode = ref('');
const loading = ref(false);
const bindLoading = ref(false);
const error = ref('');
const otpUrl = ref('');
const otpQr = ref('');

const normalizeResponse = (response) => response?.data?.data ?? response?.data ?? response;

const buildQrCode = async (url) => {
  try {
    return await toDataURL(url, { width: 160, margin: 1 });
  } catch (qrError) {
    console.warn('Failed to render QR code', qrError);
    throw new Error('二维码生成失败');
  }
};

const handleBind = async () => {
  error.value = '';
  otpUrl.value = '';
  otpQr.value = '';
  const trimmedAccount = account.value.trim();
  if (!trimmedAccount) {
    error.value = '请输入账号后再绑定';
    return;
  }
  try {
    bindLoading.value = true;
    const response = await http.get('/admin/system/auth/otp-auth-url', {
      params: { account: trimmedAccount }
    });
    const url = normalizeResponse(response);
    if (!url) {
      throw new Error('未获取到绑定地址');
    }
    otpUrl.value = url;
    otpQr.value = await buildQrCode(url);
    try {
      await navigator.clipboard.writeText(url);
    } catch {
      // Clipboard API may be unavailable; silently ignore.
    }
  } catch (err) {
    error.value = err?.response?.data?.msg ?? err.message ?? '绑定失败';
  } finally {
    bindLoading.value = false;
  }
};

const handleLogin = async () => {
  error.value = '';
  const payload = {
    account: account.value.trim(),
    password: password.value,
    otpCode: otpCode.value.trim()
  };
  if (!payload.account || !payload.password || !payload.otpCode) {
    error.value = '请完成所有输入项';
    return;
  }
  try {
    loading.value = true;
    const response = await http.post('/auth/admin/login', payload);
    const data = normalizeResponse(response);
    if (!data?.token) {
      throw new Error(response?.data?.msg ?? '登录失败');
    }
    emit('success', data);
  } catch (err) {
    error.value = err?.response?.data?.msg ?? err.message ?? '登录失败';
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="login-shell">
    <div class="login-card">
      <div class="mode-chip">dark</div>
      <div class="login-fields">
        <label class="field">
          <span class="field-icon">👤</span>
          <input v-model.trim="account" type="text" placeholder="账号" autocomplete="username" />
        </label>
        <label class="field">
          <span class="field-icon">🔒</span>
          <input
            v-model="password"
            type="password"
            placeholder="密码"
            autocomplete="current-password"
          />
        </label>
        <div class="otp-row">
          <label class="field otp-field">
            <span class="field-icon">🅖</span>
            <input v-model.trim="otpCode" type="text" placeholder="谷歌验证码" />
          </label>
          <button class="bind-btn" type="button" :disabled="bindLoading" @click="handleBind">
            {{ bindLoading ? '绑定中...' : '点击绑定' }}
          </button>
        </div>
      </div>
      <div class="login-actions">
        <button class="login-btn" type="button" :disabled="loading" @click="handleLogin">
          {{ loading ? '登录中...' : '登录' }}
        </button>
        <div class="qr-preview" v-if="otpQr">
          <img class="qr-image" :src="otpQr" alt="绑定二维码" />
          <p class="qr-caption">
            已生成绑定二维码
            <span class="otp-tip">（也已尝试自动复制链接）</span>
          </p>
        </div>
      </div>
      <p v-if="error" class="login-error">{{ error }}</p>
    </div>
  </div>
</template>

<style scoped>
.login-shell {
  position: relative;
  min-height: 100vh;
  width: 100vw;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
  background: linear-gradient(135deg, #0b1224, #1a2941 60%, #0f1c36);
}

.login-shell::before {
  content: '';
  position: absolute;
  inset: -30% -10%;
  background: radial-gradient(circle at 20% 20%, rgba(82, 123, 255, 0.35), transparent 55%),
    radial-gradient(circle at 80% 10%, rgba(102, 214, 255, 0.2), transparent 45%),
    radial-gradient(circle at 50% 80%, rgba(74, 98, 255, 0.25), transparent 60%);
  filter: blur(120px);
  opacity: 0.8;
}

.login-card {
  width: min(420px, 100%);
  padding: 32px;
  border-radius: 32px;
  background: linear-gradient(160deg, #0d1227, #090d1d);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 10px 60px rgba(5, 10, 30, 0.6);
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: relative;
  z-index: 1;
}

.mode-chip {
  align-self: flex-start;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  font-size: 12px;
  letter-spacing: 0.1em;
  text-transform: lowercase;
}

.login-fields {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field {
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 14px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.field input {
  flex: 1;
  border: none;
  background: transparent;
  color: #fff;
  font-size: 15px;
}

.field input::placeholder {
  color: rgba(255, 255, 255, 0.35);
}

.field-icon {
  font-size: 16px;
}

.otp-row {
  display: flex;
  gap: 12px;
}

.otp-field {
  flex: 1;
}

.bind-btn {
  border: none;
  border-radius: 12px;
  padding: 0 16px;
  min-width: 96px;
  background: linear-gradient(135deg, #4a7dff, #4e9dff);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.bind-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.login-btn {
  width: 100%;
  border: none;
  border-radius: 16px;
  padding: 14px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #3b70ff, #3576ff);
  cursor: pointer;
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.login-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
}

.qr-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.qr-image {
  padding: 12px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.qr-caption {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  text-align: center;
}

.login-error {
  margin: 0;
  color: #ff8e8e;
  font-size: 14px;
  text-align: center;
}

.otp-tip {
  margin-left: 4px;
  color: rgba(255, 255, 255, 0.4);
}
</style>
