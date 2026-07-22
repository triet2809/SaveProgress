/**
 * universityApi.js — API lấy dữ liệu trường/campus.
 * Dùng ở luồng đăng ký để đổ danh sách campus vào dropdown.
 */
import { apiGet } from './client';

// Lấy danh sách campus; mặc định size=100, trả về mảng đã chuẩn hoá.
export async function getCampuses(params = {}) {
  const qs = new URLSearchParams({ size: '100', ...params }).toString();
  const res = await apiGet(`/campuses${qs ? `?${qs}` : ''}`);
  if (!res.ok) throw new Error(res.data?.message || 'Failed to load campuses');
  return res.data?.content || res.data || [];
}
