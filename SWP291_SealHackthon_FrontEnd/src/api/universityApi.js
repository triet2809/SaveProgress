import { apiGet } from './client';

export async function getCampuses(params = {}) {
  const qs = new URLSearchParams({ size: '100', ...params }).toString();
  const res = await apiGet(`/campuses${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load campuses');
  return res.data?.content || res.data || [];
}
