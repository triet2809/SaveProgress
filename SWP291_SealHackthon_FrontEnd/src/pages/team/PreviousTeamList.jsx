import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Card, Col, Row, Spinner } from 'react-bootstrap';
import { History, RotateCcw, Users } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import { getMyTeamProfiles } from '../../api/hackathonApi';
import TeamRecognitionBadge from '../../components/team/TeamRecognitionBadge';

const PreviousTeamList = () => {
  const { pathname } = useLocation();
  const routeBase = pathname.startsWith('/team/') ? '/team' : '/student';
  const [profiles, setProfiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    getMyTeamProfiles()
      .then((data) => { if (active) setProfiles(Array.isArray(data) ? data : []); })
      .catch((err) => { if (active) setError(err.message || 'Failed to load previous teams'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  if (loading) return <div className="py-5 text-center"><Spinner /></div>;

  return (
    <div className="py-2">
      <div className="mb-4">
        <h1 className="h3 fw-bold mb-1">Previous Teams</h1>
        <p className="text-muted mb-0">Review immutable event rosters and reuse an eligible team identity.</p>
      </div>
      {error && <Alert variant="danger">{error}</Alert>}
      {!error && profiles.length === 0 && (
        <Alert variant="info">No historical team registrations are linked to your account.</Alert>
      )}
      <Row className="g-4">
        {profiles.map((profile) => (
          <Col lg={6} key={profile.teamProfileId}>
            <Card className="h-100 border-0 shadow-sm">
              <Card.Body className="p-4">
                <div className="d-flex justify-content-between gap-3 mb-3">
                  <div>
                    <h2 className="h5 fw-bold mb-1">{profile.canonicalName}</h2>
                    <TeamRecognitionBadge recognitions={profile.recognitions} variant="detailed" />
                    <small className="text-muted">Persistent team identity</small>
                  </div>
                  <Users className="text-primary" />
                </div>
                <div className="d-flex flex-column gap-3">
                  {profile.previousRegistrations.map((registration) => (
                    <div className="border rounded p-3" key={registration.historicalTeamId}>
                      <div className="d-flex justify-content-between align-items-start gap-2">
                        <div>
                          <div className="fw-semibold">{registration.eventName}</div>
                          <small className="text-muted">{registration.trackName}</small>
                        </div>
                        <Badge bg="secondary">{registration.eventStatus}</Badge>
                      </div>
                      <div className="small text-muted mt-2">
                        <History size={14} className="me-1" />
                        {registration.historicalRoster.length} historical member(s)
                        {registration.currentUserHistoricalRole && ` · You were ${registration.currentUserHistoricalRole}`}
                      </div>
                    </div>
                  ))}
                </div>
              </Card.Body>
              <Card.Footer className="border-0 bg-transparent px-4 pb-4">
                {profile.eligibleToInitiate ? (
                  <Button as={Link} to={`${routeBase}/previous-teams/${profile.teamProfileId}/reactivate`}>
                    <RotateCcw size={16} className="me-2" />Reactivate
                  </Button>
                ) : (
                  <small className="text-muted">Only a historical leader can initiate reactivation.</small>
                )}
              </Card.Footer>
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  );
};

export default PreviousTeamList;
