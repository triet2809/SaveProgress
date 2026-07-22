/**
 * ProtectedRoute.jsx — Wrapper bảo vệ route theo vai trò (RBAC phía client).
 * Kiểm tra token còn hạn + user tồn tại; nếu không thì đá về /login.
 * Nếu role không nằm trong allowedRoles thì chuyển về dashboard đầu tiên hợp lệ.
 */
import { Navigate, useLocation } from 'react-router-dom';
import { getStoredUser } from '../utils/authUser';
import { clearStoredAuth, hasUsableAccessToken, getActiveRole, routeForRole } from '../utils/authSession';

// Chuẩn hoá role về lowercase để so sánh.
const normalizeRoles = (roles = []) => roles.map((role) => String(role).toLowerCase());

// Xác định route dashboard đầu tiên mà user được phép vào (theo độ ưu tiên role).
const firstAllowedRoute = (roles) => {
  const active = getActiveRole({ roles });
  if (active) return routeForRole(active);
  if (roles.includes('coordinator')) return '/coordinator/dashboard';
  if (roles.includes('mentor')) return '/mentor/dashboard';
  if (roles.includes('judge')) return '/judge/dashboard';
  if (roles.includes('team_leader') || roles.includes('team_member')) return '/student/dashboard';
  return '/login';
};

/**
 * Component bọc children; chỉ render khi user đăng nhập hợp lệ và có quyền.
 * @param {string[]} allowedRoles - Danh sách role được phép truy cập route.
 * @param {ReactNode} children - Nội dung route cần bảo vệ.
 */
const ProtectedRoute = ({ allowedRoles, children }) => {
  const location = useLocation();
  const token = localStorage.getItem('seal_access_token');
  const user = getStoredUser();

  // Chưa đăng nhập hoặc token hết hạn -> xóa phiên và về login, nhớ lại trang định vào.
  if (!hasUsableAccessToken(token) || !user) {
    clearStoredAuth();
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  // Đăng nhập rồi nhưng không đúng role -> chuyển về dashboard phù hợp.
  const roles = normalizeRoles(user.roles);
  const authorized = allowedRoles.some((role) => roles.includes(role.toLowerCase()));
  if (!authorized) {
    return <Navigate to={firstAllowedRoute(roles)} replace />;
  }

  return children;
};

export default ProtectedRoute;
