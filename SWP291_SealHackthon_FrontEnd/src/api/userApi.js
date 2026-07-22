/**
 * userApi.js — Các lời gọi API quản lý người dùng: hồ sơ cá nhân (me),
 * duyệt/từ chối user đang chờ, cập nhật trạng thái và vai trò (roles).
 */
import { apiGet, apiPatch, apiPost, apiPut } from './client';

// Helper: chuẩn hoá dữ liệu phân trang -> lấy mảng items (data.content hoặc chính data).
const pageItems = (data) => data?.content || data || [];
// Helper: kiểm tra user có role nào đó không (so sánh không phân biệt hoa thường).
const hasRole = (user, role) => user.roles?.some((r) => r.toLowerCase() === role.toLowerCase());

// Lấy hồ sơ user hiện tại; gắn thêm field `value` cho tiện dùng ở UI.
export async function getMe() {
  const result = await apiGet('/users/me');
  return { ...result, value: result.data ?? null };
}

// Cập nhật hồ sơ user hiện tại.
export async function updateMe(payload) {
  const result = await apiPatch('/users/me', payload);
  return { ...result, value: result.data ?? null };
}

// Đăng xuất phía server (thu hồi refresh token đang lưu).
export async function logoutServer() {
  const refreshToken = localStorage.getItem('seal_refresh_token');
  return apiPost('/auth/logout', refreshToken ? { refreshToken } : {});
}

/**
 * Lấy danh sách user, lọc theo status/search phía server và lọc role phía client.
 * @param {{role?:string, status?:string, search?:string}} opts
 * @returns kết quả API kèm `value` là mảng user đã lọc.
 */
export async function getUsers({ role, status, search } = {}) {
  const params = new URLSearchParams();
  if (status) params.set('status', status);
  if (search) params.set('email', search);
  params.set('size', '100');
  const query = params.toString();
  const result = await apiGet(`/users${query ? `?${query}` : ''}`);
  let users = pageItems(result.data);
  if (role) users = users.filter((user) => hasRole(user, role));
  return { ...result, value: users };
}

// Lấy danh sách user đang chờ duyệt (status = pending).
export function getPendingUsers() {
  return getUsers({ status: 'pending' });
}

// Tạo user mới (dùng bởi EC khi thêm nhân sự).
export async function createUser(payload) {
  const result = await apiPost('/users', payload);
  return { ...result, value: result.data ?? null };
}

// Cập nhật thông tin hồ sơ của một user cụ thể.
export async function updateUserProfile(userId, payload) {
  const result = await apiPatch(`/users/${userId}/profile`, payload);
  return { ...result, value: result.data ?? null };
}

// Cập nhật trạng thái tài khoản (approved/rejected/...).
export async function updateUserStatus(userId, status) {
  const result = await apiPatch(`/users/${userId}/status`, { status });
  return { ...result, value: result.data ?? null };
}

// Duyệt user (đặt status = approved).
export function approveUser(userId) {
  return updateUserStatus(userId, 'approved');
}

// Từ chối user (đặt status = rejected).
export function rejectUser(userId) {
  return updateUserStatus(userId, 'rejected');
}

// Ghi đè toàn bộ danh sách role của user.
export async function updateUserRoles(userId, roles) {
  const result = await apiPut(`/users/${userId}/roles`, { roles });
  return { ...result, value: result.data ?? null };
}

// Thêm một role vào user (dùng Set để tránh trùng).
export async function addUserRole(user, role) {
  const roles = Array.from(new Set([...(user.roles || []), role]));
  return updateUserRoles(user.id, roles);
}

// Gỡ một role khỏi user; nếu rỗng thì fallback về 'team_member'.
export async function removeUserRole(user, role) {
  const roles = (user.roles || []).filter((r) => r !== role);
  return updateUserRoles(user.id, roles.length ? roles : ['team_member']);
}
