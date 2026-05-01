import axios from 'axios';

const defaultBaseUrl = `${window.location.origin.replace(/\/$/, '')}/api`;
const baseURL = (import.meta.env.VITE_API_BASE_URL || defaultBaseUrl).replace(/\/$/, '');

export const ADMIN_TOKEN_STORAGE_KEY = 'payadmin_admin_token';

export const getStoredToken = () => {
  try {
    return localStorage.getItem(ADMIN_TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
};

export const setAuthToken = (token) => {
  if (token) {
    try {
      localStorage.setItem(ADMIN_TOKEN_STORAGE_KEY, token);
    } catch {
      // ignore storage errors
    }
  } else {
    try {
      localStorage.removeItem(ADMIN_TOKEN_STORAGE_KEY);
    } catch {
      // ignore
    }
  }
};

const http = axios.create({
  baseURL,
  timeout: 15000
});

const EXCLUDED_PATHS = ['/admin/system/auth/otp-auth-url', '/auth/admin/login'];

http.interceptors.request.use((config) => {
  const url = config.url || '';
  if (EXCLUDED_PATHS.some((path) => url.includes(path))) {
    return config;
  }
  const token = getStoredToken();
  if (token) {
    config.headers = config.headers || {};
    if (!config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const code = error.response?.data?.code;
    const url = error.config?.url || '';
    const requiresAuth = !EXCLUDED_PATHS.some((p) => url.includes(p));

    // 直接收到 401，或后端 401 未带 CORS 头导致浏览器拦截响应（error.response 为 undefined）
    const isDirectAuth = status === 401 || code === 401;
    const isCorsBlocked = !error.response && requiresAuth && !!getStoredToken();

    if (isDirectAuth || isCorsBlocked) {
      setAuthToken(null);
      window.location.href = '/';
    }
    return Promise.reject(error);
  }
);

export default http;
