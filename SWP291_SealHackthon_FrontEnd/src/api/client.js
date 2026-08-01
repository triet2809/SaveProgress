/**
 * client.js — Lớp HTTP client cốt lõi cho toàn bộ ứng dụng.
 * Bọc `fetch` để tự động gắn base URL, header Authorization (Bearer token),
 * parse JSON, và xử lý tập trung các mã lỗi 401 (hết phiên) và 449 (chưa onboarding).
 * Mọi module *Api.js đều gọi qua các hàm apiGet/apiPost/... ở đây.
 */
import { API_BASE_URL } from '../config/registerConfig';
import { handleUnauthorizedResponse } from '../utils/authSession';

// Lấy access token đang lưu trong localStorage (null nếu chưa đăng nhập).
function getAccessToken() {
  return localStorage.getItem('seal_access_token');
}

/**
 * Hàm request lõi: gửi fetch tới `${API_BASE_URL}${path}` và trả về { ok, status, data }.
 * @param {string} path - Đường dẫn API tương đối (ví dụ '/users/me').
 * @param {object} options - Tùy chọn fetch (method, body, headers...).
 * @returns {Promise<{ok:boolean, status:number, data:any}>}
 */
async function request(path, options = {}) {
  const headers = {
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(options.headers || {}),
  };

  const accessToken = getAccessToken();
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  // Cố parse JSON; nếu body rỗng hoặc không phải JSON thì data = null.
  let data;
  try {
    data = await res.json();
  } catch {
    data = null;
  }

  // 401 = token hết hạn/không hợp lệ -> xóa phiên và điều hướng về login.
  if (res.status === 401) {
    handleUnauthorizedResponse();
  }

  // 449 Upgrade Required = onboarding pending.
  // The backend returns this instead of 403 so the frontend can
  // distinguish "you must complete onboarding" from a real RBAC denial.
  if (res.status === 449) {
    const onboardingPath = '/onboarding';
    if (window.location.pathname !== onboardingPath) {
      window.location.href = onboardingPath;
    }
  }

  return { ok: res.ok, status: res.status, data };
}

// GET request đơn giản.
export function apiGet(path) {
  return request(path);
}

// POST request; tự stringify body thành JSON nếu có.
export function apiPost(path, body) {
  return request(path, {
    method: 'POST',
    body: body ? JSON.stringify(body) : undefined,
  });
}

// PATCH request (cập nhật một phần tài nguyên).
export function apiPatch(path, body) {
  return request(path, {
    method: 'PATCH',
    body: body ? JSON.stringify(body) : undefined,
  });
}

// PUT request (thay thế toàn bộ tài nguyên).
export function apiPut(path, body) {
  return request(path, {
    method: 'PUT',
    body: body ? JSON.stringify(body) : undefined,
  });
}

// DELETE request.
export function apiDelete(path) {
  return request(path, { method: 'DELETE' });
}

/**
 * Upload file qua multipart/form-data (KHÔNG tự set Content-Type để trình duyệt
 * tự thêm boundary). Dùng cho import Excel. Trả về { ok, status, data }.
 */
export async function apiUpload(path, formData) {
  const headers = {};
  const accessToken = getAccessToken();
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  const res = await fetch(`${API_BASE_URL}${path}`, { method: 'POST', headers, body: formData });
  let data;
  try {
    data = await res.json();
  } catch {
    data = null;
  }
  if (res.status === 401) {
    handleUnauthorizedResponse();
  }
  if (res.status === 449) {
    const onboardingPath = '/onboarding';
    if (window.location.pathname !== onboardingPath) {
      window.location.href = onboardingPath;
    }
  }
  return { ok: res.ok, status: res.status, data };
}

/**
 * Tải tài nguyên dạng text (ví dụ CSV) thay vì JSON.
 * Vẫn xử lý 401/449 như request thường; trả về { ok, status, text, headers }.
 */
export async function apiDownload(path) {
  const headers = {};
  const accessToken = getAccessToken();
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  const res = await fetch(`${API_BASE_URL}${path}`, { headers });
  const text = await res.text();
  if (res.status === 401) {
    handleUnauthorizedResponse();
  }
  if (res.status === 449) {
    const onboardingPath = '/onboarding';
    if (window.location.pathname !== onboardingPath) {
      window.location.href = onboardingPath;
    }
  }
  return { ok: res.ok, status: res.status, text, headers: res.headers };
}
