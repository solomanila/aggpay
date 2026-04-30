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

export default http;
