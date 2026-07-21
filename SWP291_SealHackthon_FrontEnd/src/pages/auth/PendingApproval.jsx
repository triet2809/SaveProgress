import React, { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Container, Card, Button } from 'react-bootstrap';
import { Clock } from 'lucide-react';
import { useTheme } from '../../context/ThemeContext';

const PendingApproval = () => {
  const { setForceTheme } = useTheme();

  useEffect(() => {
    setForceTheme('light');
    return () => setForceTheme(null);
  }, [setForceTheme]);
  return (
    <div style={{ backgroundColor: 'var(--cf-bg-main)', minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <Container>
        <div className="d-flex justify-content-center">
          <Card style={{ maxWidth: '480px', width: '100%', border: 'none', boxShadow: 'var(--cf-shadow-lg)' }}>
            <Card.Body className="text-center p-5">
              <div 
                className="mx-auto mb-4 d-flex align-items-center justify-content-center" 
                style={{ width: '64px', height: '64px', borderRadius: '50%', backgroundColor: 'var(--cf-status-warning-bg)', color: 'var(--cf-status-warning)' }}
              >
                <Clock size={32} />
              </div>
              
              <h2 className="mb-3 fw-bold" style={{ color: 'var(--cf-text-primary)' }}>Registration Pending</h2>
              <p className="text-muted mb-4">
                Thank you for registering for SEAL Hackathon 2026. Your application is currently under review by the organizers. We will notify you via email once your account has been approved.
              </p>
              
              <Link to="/login">
                <Button variant="outline-primary" className="w-100 py-2">
                  Return to Login
                </Button>
              </Link>
            </Card.Body>
          </Card>
        </div>
      </Container>
    </div>
  );
};

export default PendingApproval;
