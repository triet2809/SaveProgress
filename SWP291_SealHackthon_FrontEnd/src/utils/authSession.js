const AUTH_STORAGE_KEYS = [
  'seal_access_token',
  'seal_refresh_token',
  'seal_token_type',
  'seal_user',
];

export function clearStoredAuth() {
  AUTH_STORAGE_KEYS.forEach((key) => localStorage.removeItem(key));
  localStorage.removeItem('seal_active_role');
}

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

export function handleUnauthorizedResponse() {
  clearStoredAuth();
  if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
    window.location.replace('/login');
  }
}

const ROLE_ROUTES = {
  coordinator: '/coordinator/dashboard',
  judge: '/judge/dashboard',
  mentor: '/mentor/dashboard',
  team_leader: '/team/dashboard',
  team_member: '/student/dashboard',
};

export function normalizeRoles(roles = []) {
  return roles.map((role) => String(role).toLowerCase().replace(/^role_/, ''));
}

export function getDashboardRoles(user = null) {
  return normalizeRoles(user?.roles || []).filter((role) => ROLE_ROUTES[role]);
}

export function routeForRole(role) {
  return ROLE_ROUTES[String(role).toLowerCase()] || '/student/dashboard';
}

export function getActiveRole(user = null) {
  const held = getDashboardRoles(user);
  const stored = localStorage.getItem('seal_active_role')?.toLowerCase();
  return held.includes(stored) ? stored : held[0] || null;
}

export function setActiveRole(role) {
  localStorage.setItem('seal_active_role', String(role).toLowerCase());
}
