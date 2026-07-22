/**
 * authUser.js — Tiện ích đọc/ghi thông tin user trong localStorage và sinh chữ cái đại diện.
 */

// Đọc user đã lưu; trả null nếu chưa có hoặc JSON hỏng.
export function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('seal_user') || 'null');
  } catch {
    return null;
  }
}

// Lưu user vào localStorage (bỏ qua lỗi storage nếu có).
export function saveStoredUser(user) {
  try {
    localStorage.setItem('seal_user', JSON.stringify(user));
  } catch {
    // ignore storage failures
  }
}

// Sinh chữ cái đại diện (initials) từ tên — tối đa 2 ký tự, mặc định 'U'.
export function getInitials(name = '') {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase() || 'U';
}
