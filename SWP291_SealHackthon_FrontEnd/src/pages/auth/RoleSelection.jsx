import { useMemo } from 'react';
import { Card, Button } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { getStoredUser } from '../../utils/authUser';
import { getDashboardRoles, routeForRole, setActiveRole } from '../../utils/authSession';

const labels = { coordinator: 'Coordinator', judge: 'Judge', mentor: 'Mentor', team_leader: 'Team Leader', team_member: 'Team Member' };

export default function RoleSelection() {
  const navigate = useNavigate();
  const roles = useMemo(() => getDashboardRoles(getStoredUser()), []);
  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center bg-light">
      <Card className="p-4 shadow-sm" style={{ minWidth: 320 }}>
        <h2 className="h4 mb-3">Choose a workspace</h2>
        {roles.map((role) => (
          <Button key={role} className="mb-2" variant="outline-primary" onClick={() => {
            setActiveRole(role); navigate(routeForRole(role), { replace: true });
          }}>{labels[role] || role}</Button>
        ))}
      </Card>
    </div>
  );
}
