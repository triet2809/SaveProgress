import { useEffect, useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Users, 
  Upload, 
  History, 
  Info, 
  Calendar, 
  User,
  LogOut,
  FolderOpen,
  MessageSquare,
  FileCheck,
  UserPlus,
  Award,
  Zap,
  Bell,
  FileText,
  AlertTriangle,
  BarChart2,
  LifeBuoy,
  Route as RouteIcon
} from 'lucide-react';
import { users } from '../../data/mockData';
import styles from './Sidebar.module.css';
import { Badge } from 'react-bootstrap';
import { logout } from '../../api/authApi';
import { getUnreadSummary } from '../../api/hackathonApi';
import { getStoredUser, getInitials } from '../../utils/authUser';
import { getDashboardRoles, getActiveRole, routeForRole, setActiveRole } from '../../utils/authSession';

// Which notification category lights up which menu path (red dot).
const CATEGORY_BY_PATH = {
  '/team/join-requests': 'join_requests',
  '/team/support': 'my_support',
  '/team/submissions': 'my_requests',
  '/team/my-team': 'mentor_feedback',
  '/student/join-team': 'my_requests',
  '/coordinator/support': 'support',
  '/judge/submissions': ['assignments', 'submissions'],
  '/mentor/review': 'submissions',
};

const Sidebar = ({ role }) => {
  const navigate = useNavigate();
  const stored = getStoredUser();
  const fallback = users[role] || {};
  const user = {
    name: stored?.fullName || fallback.name || 'User',
    email: stored?.email || fallback.email || '',
    role: fallback.role || (stored?.roles && stored.roles[0]) || role,
    initials: getInitials(stored?.fullName || fallback.name || 'User'),
  };
  const dashboardRoles = getDashboardRoles(stored);
  const activeRole = getActiveRole(stored);
  const switchRole = (event) => {
    const next = event.target.value;
    setActiveRole(next);
    navigate(routeForRole(next));
  };

  // Define links based on role

  const getLinks = () => {
    switch(role) {
      case 'team':
        return [
          { name: 'Overview', path: '/team/dashboard', icon: LayoutDashboard },
          { name: 'Track Topic', path: '/team/topic', icon: FileText },
          { name: 'My Team', path: '/team/my-team', icon: Users },
          { name: 'Previous Teams', path: '/team/previous-teams', icon: History },
          { name: 'Team Members', path: '/team/members', icon: Users },
          { name: 'Join Requests', path: '/team/join-requests', icon: UserPlus },
          { name: 'Team Chat', path: '/team/chat', icon: MessageSquare },
          { name: 'Submission Manage...', path: '/team/submissions', icon: Upload },
          { name: 'Submission History', path: '/team/history', icon: History },
          { name: 'Team Journey', path: '/team/journey', icon: RouteIcon },
          { name: 'Notice Board', path: '/team/notices', icon: Bell },
          { name: 'Deadlines & Schedule', path: '/team/schedule', icon: Calendar },
          { name: 'Support Ticket', path: '/team/support', icon: MessageSquare },
          { name: 'Report & Support', path: '/team/cases', icon: MessageSquare },
          { name: 'Profile', path: '/team/profile', icon: User },
        ];
      case 'student':
        return [
          { name: 'Overview', path: '/student/dashboard', icon: LayoutDashboard },
          { name: 'Create Team', path: '/student/create-team', icon: Users },
          { name: 'Join Team', path: '/student/join-team', icon: UserPlus },
          { name: 'Previous Teams', path: '/student/previous-teams', icon: History },
          { name: 'Profile', path: '/student/profile', icon: User },
        ];
      case 'mentor':
        return [
          { name: 'Overview', path: '/mentor/dashboard', icon: LayoutDashboard },
          { name: 'Assigned Categories', path: '/mentor/categories', icon: FolderOpen },
          { name: 'Assigned Teams', path: '/mentor/teams', icon: Users },
          { name: 'Submission Review', path: '/mentor/review', icon: FileCheck },
          { name: 'Notice Board', path: '/mentor/notices', icon: Bell },
          { name: 'Profile', path: '/mentor/profile', icon: User },
        ];
      case 'judge':
        return [
          { name: 'Overview', path: '/judge/dashboard', icon: LayoutDashboard },
          { name: 'Assigned Submissions', path: '/judge/submissions', icon: FileCheck },
          { name: 'Notice Board', path: '/judge/notices', icon: Info },
          { name: 'Profile', path: '/judge/profile', icon: User },
        ];
      case 'coordinator':
        return [
          { name: 'Overview', path: '/coordinator/dashboard', icon: LayoutDashboard },
          { name: 'Event Manage...', path: '/coordinator/events', icon: Calendar },
          { name: 'Teams', path: '/coordinator/teams', icon: Users },
          { name: 'Submissions', path: '/coordinator/submissions', icon: Upload },
          { name: 'Judge & Mentor Management', path: '/coordinator/staff', icon: Users },
          { name: 'User Approvals', path: '/coordinator/users', icon: User },
          { name: 'Criteria', path: '/coordinator/criteria', icon: FileCheck },
          { name: 'Scoring Analytics', path: '/coordinator/scoring', icon: BarChart2 },
          { name: 'Rankings', path: '/coordinator/ranking', icon: Award },
          { name: 'Appeals', path: '/coordinator/appeals', icon: AlertTriangle },
          { name: 'Seeding Review', path: '/coordinator/seeding', icon: Award },
          { name: 'Team Timeline', path: '/coordinator/timeline', icon: RouteIcon },
          { name: 'Awards', path: '/coordinator/awards', icon: Award },
          { name: 'Incident Review', path: '/coordinator/incidents', icon: AlertTriangle },
          { name: 'Reports', path: '/coordinator/reports', icon: FileText },
          { name: 'Audit Logs', path: '/coordinator/logs', icon: History },
          { name: 'Support Tickets', path: '/coordinator/support', icon: LifeBuoy },
          { name: 'Case Management', path: '/coordinator/cases', icon: LifeBuoy },
          { name: 'Profile', path: '/coordinator/profile', icon: User },
        ];
      default:
        return [];
    }
  };

  const links = getLinks();

  // Poll unread notification counts -> red dots on the matching menu items.
  const [unread, setUnread] = useState({});
  useEffect(() => {
    let active = true;
    const load = async () => {
      try {
        const summary = await getUnreadSummary();
        if (active) setUnread(summary?.byCategory || {});
      } catch { /* not logged in / transient; keep last */ }
    };
    load();
    const t = setInterval(load, 30000);
    // Refresh when the tab regains focus so dots clear promptly after reading.
    const onFocus = () => load();
    window.addEventListener('focus', onFocus);
    return () => { active = false; clearInterval(t); window.removeEventListener('focus', onFocus); };
  }, [role]);

  const handleLogout = async () => {
    try { await logout(); } catch { /* local clear still proceeds */ }
    localStorage.removeItem('seal_access_token');
    localStorage.removeItem('seal_refresh_token');
    localStorage.removeItem('seal_token_type');
    localStorage.removeItem('seal_user');
    navigate('/login', { replace: true });
  };

  return (
    <aside className={styles.sidebar}>
      <div className={styles.header}>
        <div className={styles.logo}>
          <div className={styles.logoIcon}>
            <Zap size={20} />
          </div>
          <div>
            <h5 className="mb-0 fw-bold">SEAL Hackathon 2026</h5>
            <small className="text-muted d-block" style={{fontSize: '0.75rem'}}>FPT University</small>
          </div>
        </div>
        
        <div className="mt-4 mb-2 px-3">
          <Badge bg={
            role === 'team' ? 'primary' : 
            role === 'mentor' ? 'purple' : 'success'
          } className={styles.roleBadge}>
            {user.role}
          </Badge>
          {dashboardRoles.length > 1 && (
            <select className="form-select form-select-sm mt-2" value={activeRole || role} onChange={switchRole} aria-label="Switch active role">
              {dashboardRoles.map((item) => <option key={item} value={item}>{item.replace('_', ' ')}</option>)}
            </select>
          )}
        </div>
      </div>

      <nav className={styles.nav}>
        {links.map((link, index) => {
          const cat = CATEGORY_BY_PATH[link.path];
          const cats = cat == null ? [] : (Array.isArray(cat) ? cat : [cat]);
          const count = cats.reduce((sum, c) => sum + (unread[c] || 0), 0);
          return (
            <NavLink 
              key={index} 
              to={link.path} 
              className={({ isActive }) => 
                `${styles.navItem} ${isActive ? styles.active : ''}`
              }
            >
              <link.icon className={styles.icon} size={18} />
              <span>{link.name}</span>
              {count > 0 && (
                <span
                  title={`${count} new`}
                  style={{
                    marginLeft: 'auto',
                    minWidth: '18px',
                    height: '18px',
                    padding: '0 5px',
                    borderRadius: '9px',
                    background: '#e53e3e',
                    color: '#fff',
                    fontSize: '0.7rem',
                    fontWeight: 700,
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    lineHeight: 1,
                  }}
                >
                  {count > 9 ? '9+' : count}
                </span>
              )}
            </NavLink>
          );
        })}
      </nav>

      <div className={styles.footer}>
        <div className={styles.userProfile}>
          <div className={styles.avatar}>{user.initials}</div>
          <div className={styles.userInfo}>
            <div className={styles.userName}>{user.name}</div>
            <div className={styles.userEmail}>{user.email}</div>
          </div>
          <button className={styles.logoutBtn} onClick={handleLogout} title="Logout">
            <LogOut size={18} />
          </button>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
