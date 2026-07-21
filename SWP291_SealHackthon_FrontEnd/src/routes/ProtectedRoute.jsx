import { Navigate, useLocation } from 'react-router-dom';
import { getStoredUser } from '../utils/authUser';
import { clearStoredAuth, hasUsableAccessToken, getActiveRole, routeForRole } from '../utils/authSession';

const normalizeRoles = (roles = []) => roles.map((role) => String(role).toLowerCase());

const firstAllowedRoute = (roles) => {
  const active = getActiveRole({ roles });
  if (active) return routeForRole(active);
  if (roles.includes('coordinator')) return '/coordinator/dashboard';
  if (roles.includes('mentor')) return '/mentor/dashboard';
  if (roles.includes('judge')) return '/judge/dashboard';
  if (roles.includes('team_leader') || roles.includes('team_member')) return '/student/dashboard';
  return '/login';
};

const ProtectedRoute = ({ allowedRoles, children }) => {
  const location = useLocation();
  const token = localStorage.getItem('seal_access_token');
  const user = getStoredUser();

  if (!hasUsableAccessToken(token) || !user) {
    clearStoredAuth();
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  const roles = normalizeRoles(user.roles);
  const authorized = allowedRoles.some((role) => roles.includes(role.toLowerCase()));
  if (!authorized) {
    return <Navigate to={firstAllowedRoute(roles)} replace />;
  }

  return children;
};

export default ProtectedRoute;
