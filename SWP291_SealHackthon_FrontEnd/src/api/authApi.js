/**
 * authApi.js — Các lời gọi API liên quan đến xác thực: đăng ký, đăng nhập,
 * đăng xuất, kích hoạt tài khoản, onboarding và lấy thông tin user hiện tại.
 * Dùng `postJson` riêng cho các endpoint không cần token (register/login).
 */
import { API_BASE_URL } from '../config/registerConfig';
import { apiGet, apiPost } from './client';
import { clearStoredAuth } from '../utils/authSession';

export { clearStoredAuth } from '../utils/authSession';

// POST JSON không gắn token (dùng cho luồng chưa đăng nhập).
async function postJson(path, body) {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  let data;
  try {
    data = await res.json();
  } catch {
    data = null;
  }

  return { ok: res.ok, status: res.status, data };
}

// Đăng ký cho sinh viên FPT (có studentId + campusId).
export function registerFpt({ fullName, email, password, studentId, campusId, acceptedTerms }) {
  return postJson('/auth/register', {
    fullName,
    email,
    password,
    studentId,
    campusId: campusId || null,
    studentType: 'fpt',
    acceptedTerms,
  });
}

// Đăng ký cho sinh viên ngoài FPT (nhập tên trường thay vì campus).
export function registerExternal({ fullName, email, password, universityName, acceptedTerms }) {
  return postJson('/auth/register', {
    fullName,
    email,
    password,
    universityName,
    studentType: 'external',
    acceptedTerms,
  });
}

// Đăng nhập bằng email + mật khẩu.
export function login({ email, password }) {
  return postJson('/auth/login', { email, password });
}

// Đăng xuất: báo server thu hồi refresh token rồi xóa phiên local (luôn chạy dù API lỗi).
export async function logout() {
  const refreshToken = localStorage.getItem('seal_refresh_token');
  try {
    return await apiPost('/auth/logout', refreshToken ? { refreshToken } : {});
  } finally {
    clearStoredAuth();
  }
}

// Kiểm tra token kích hoạt còn hợp lệ không (từ link email).
export async function validateActivation(token) {
  const res = await apiGet(`/auth/activate/validate?token=${encodeURIComponent(token)}`);
  if (!res.ok) throw new Error(res.data?.message || 'Invalid activation link');
  return res.data;
}

// Hoàn tất kích hoạt tài khoản (đặt mật khẩu...).
export async function activateAccount(payload) {
  const res = await apiPost('/auth/activate', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Activation failed');
  return res.data;
}

// Hoàn tất onboarding (bổ sung thông tin bắt buộc sau khi đăng nhập lần đầu).
export async function completeOnboarding(payload) {
  const res = await apiPost('/auth/onboarding', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Onboarding failed');
  return res.data?.data || res.data;
}

// Lấy thông tin user hiện tại qua endpoint /auth/me.
export async function me() {
  const result = await apiGet('/auth/me');
  return { ...result, value: result.data ?? null };
}
