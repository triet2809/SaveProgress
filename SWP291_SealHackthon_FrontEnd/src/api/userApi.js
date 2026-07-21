import { apiGet, apiPatch, apiPost, apiPut } from './client';

const pageItems = (data) => data?.content || data || [];
const hasRole = (user, role) => user.roles?.some((r) => r.toLowerCase() === role.toLowerCase());

export async function getMe() {
  const result = await apiGet('/users/me');
  return { ...result, value: result.data ?? null };
}

export async function updateMe(payload) {
  const result = await apiPatch('/users/me', payload);
  return { ...result, value: result.data ?? null };
}

export async function logoutServer() {
  const refreshToken = localStorage.getItem('seal_refresh_token');
  return apiPost('/auth/logout', refreshToken ? { refreshToken } : {});
}

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

export function getPendingUsers() {
  return getUsers({ status: 'pending' });
}

export async function createUser(payload) {
  const result = await apiPost('/users', payload);
  return { ...result, value: result.data ?? null };
}

export async function updateUserProfile(userId, payload) {
  const result = await apiPatch(`/users/${userId}/profile`, payload);
  return { ...result, value: result.data ?? null };
}

export async function updateUserStatus(userId, status) {
  const result = await apiPatch(`/users/${userId}/status`, { status });
  return { ...result, value: result.data ?? null };
}

export function approveUser(userId) {
  return updateUserStatus(userId, 'approved');
}

export function rejectUser(userId) {
  return updateUserStatus(userId, 'rejected');
}

export async function updateUserRoles(userId, roles) {
  const result = await apiPut(`/users/${userId}/roles`, { roles });
  return { ...result, value: result.data ?? null };
}

export async function addUserRole(user, role) {
  const roles = Array.from(new Set([...(user.roles || []), role]));
  return updateUserRoles(user.id, roles);
}

export async function removeUserRole(user, role) {
  const roles = (user.roles || []).filter((r) => r !== role);
  return updateUserRoles(user.id, roles.length ? roles : ['team_member']);
}
