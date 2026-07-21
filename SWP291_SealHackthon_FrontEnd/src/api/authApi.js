import { API_BASE_URL } from '../config/registerConfig';
import { apiGet, apiPost } from './client';
import { clearStoredAuth } from '../utils/authSession';

export { clearStoredAuth } from '../utils/authSession';

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

export function login({ email, password }) {
  return postJson('/auth/login', { email, password });
}

export async function logout() {
  const refreshToken = localStorage.getItem('seal_refresh_token');
  try {
    return await apiPost('/auth/logout', refreshToken ? { refreshToken } : {});
  } finally {
    clearStoredAuth();
  }
}

export async function validateActivation(token) {
  const res = await apiGet(`/auth/activate/validate?token=${encodeURIComponent(token)}`);
  if (!res.ok) throw new Error(res.data?.message || 'Invalid activation link');
  return res.data;
}

export async function activateAccount(payload) {
  const res = await apiPost('/auth/activate', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Activation failed');
  return res.data;
}

export async function completeOnboarding(payload) {
  const res = await apiPost('/auth/onboarding', payload);
  if (!res.ok) throw new Error(res.data?.message || 'Onboarding failed');
  return res.data?.data || res.data;
}

export async function me() {
  const result = await apiGet('/auth/me');
  return { ...result, value: result.data ?? null };
}
