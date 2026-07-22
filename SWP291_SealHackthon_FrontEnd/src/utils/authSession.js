/**
 * authSession.js — Quản lý phiên đăng nhập: lưu/xóa token, kiểm tra token còn hạn,
 * xử lý 401, chuẩn hoá role và ánh xạ role -> route dashboard tương ứng.
 */

// Các key localStorage liên quan đến phiên.
const AUTH_STORAGE_KEYS = [
  'seal_access_token',
  'seal_refresh_token',
  'seal_token_type',
  'seal_user',
];

// Xóa toàn bộ dữ liệu phiên (token + user + active role).
export function clearStoredAuth() {
  AUTH_STORAGE_KEYS.forEach((key) => localStorage.removeItem(key));
  localStorage.removeItem('seal_active_role');
}

/**
 * Kiểm tra access token (JWT) còn dùng được không: đúng định dạng 3 phần và chưa hết hạn.
 * Decode payload base64url để đọc `exp` (giây) và so với thời điểm hiện tại.
 */
export function hasUsableAccessToken(token, nowMs = Date.now()) {
  if (typeof token !== 'string') return false;
  const parts = token.split('.');
  if (parts.length !== 3 || parts.some((part) => part.length === 0)) return false;

  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
    const payload = JSON.parse(atob(padded));
    return Number.isFinite(payload.exp) && payload.exp * 1000 > nowMs;
  } catch {
    return false;
  }
}

// Xử lý khi API trả 401: xóa phiên và chuyển về trang login (nếu chưa ở đó).
export function handleUnauthorizedResponse() {
  clearStoredAuth();
  if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
    window.location.replace('/login');
  }
}

// Ánh xạ từng role -> route dashboard mặc định.
// team_member cũng vào /team/dashboard (như leader): StudentDashboard/MyTeam/Submissions
// ở route /team. Khi chưa có team, TeamDashboard hiện "No team" + có link tạo/gia nhập.
const ROLE_ROUTES = {
  coordinator: '/coordinator/dashboard',
  judge: '/judge/dashboard',
  mentor: '/mentor/dashboard',
  team_leader: '/team/dashboard',
  team_member: '/team/dashboard',
};

// Chuẩn hoá danh sách role: lowercase và bỏ tiền tố 'role_' (nếu BE trả dạng ROLE_X).
export function normalizeRoles(roles = []) {
  return roles.map((role) => String(role).toLowerCase().replace(/^role_/, ''));
}

// Lọc ra các role có dashboard tương ứng.
export function getDashboardRoles(user = null) {
  return normalizeRoles(user?.roles || []).filter((role) => ROLE_ROUTES[role]);
}

// Trả route dashboard cho một role (fallback về student).
export function routeForRole(role) {
  return ROLE_ROUTES[String(role).toLowerCase()] || '/student/dashboard';
}

// Lấy role đang kích hoạt: ưu tiên role đã lưu nếu user thực sự giữ, ngược lại lấy role đầu tiên.
export function getActiveRole(user = null) {
  const held = getDashboardRoles(user);
  const stored = localStorage.getItem('seal_active_role')?.toLowerCase();
  return held.includes(stored) ? stored : held[0] || null;
}

// Lưu role đang kích hoạt (khi user đổi vai trò ở trang chọn role).
export function setActiveRole(role) {
  localStorage.setItem('seal_active_role', String(role).toLowerCase());
}
