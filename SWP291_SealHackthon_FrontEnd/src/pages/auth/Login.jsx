import { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Row, Col, Form, Button, Alert, Spinner } from 'react-bootstrap';
import { Zap, Users, Award, Calendar } from 'lucide-react';
import { login } from '../../api/authApi';
import { getMyTeams } from '../../api/hackathonApi';
import styles from './Login.module.css';
import { useTheme } from '../../context/ThemeContext';
import { getDashboardRoles, routeForRole, setActiveRole } from '../../utils/authSession';

// Participants with a team land in the team workspace; those without a team
// land in the student area where they can create or join a team.
const resolveParticipantRoute = async () => {
  try {
    const teams = await getMyTeams();
    const list = Array.isArray(teams) ? teams : (teams?.content || []);
    return list.length > 0 ? '/team/dashboard' : '/student/dashboard';
  } catch {
    return '/student/dashboard';
  }
};

const Login = () => {
  const navigate = useNavigate();
  const { setForceTheme } = useTheme();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setForceTheme('light');
    return () => setForceTheme(null);
  }, [setForceTheme]);

  const handleStandardLogin = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      const result = await login({ email, password });
      if (!result.ok) {
        setError(result.data?.message || 'Incorrect email or password');
        return;
      }
      const auth = result.data?.data || result.data;
      if (!auth?.accessToken) {
        setError('Login response is missing the access token');
        return;
      }
      localStorage.setItem('seal_access_token', auth.accessToken);
      localStorage.setItem('seal_refresh_token', auth.refreshToken || '');
      localStorage.setItem('seal_token_type', auth.tokenType || 'Bearer');
      localStorage.setItem('seal_user', JSON.stringify(auth.user || {}));
      if (auth.user?.onboardingRequired) {
        navigate('/onboarding', { replace: true });
        return;
      }
      const dashboardRoles = getDashboardRoles(auth.user);
      const dest = dashboardRoles.length > 1
        ? '/select-role'
        : dashboardRoles.length === 1
          ? (setActiveRole(dashboardRoles[0]), routeForRole(dashboardRoles[0]))
          : await resolveParticipantRoute();
      navigate(dest, { replace: true });
    } catch {
      setError('Could not connect to the server');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.loginPage}>
      <Row className="g-0 min-vh-100">
        {/* Left Side - Hero Section */}
        <Col lg={5} className={styles.heroSection}>
          <div className={styles.heroContent}>
            <div className={styles.brandBadge}>
              <Zap size={16} fill="currentColor" /> SEAL Hackathon 2026
            </div>
            
            <h1 className={styles.heroTitle}>
              FPT University<br />
              Software Engineering<br />
              Competition
            </h1>
            
            <p className={styles.heroDescription}>
              The premier hackathon platform for building, collaborating, and competing with the best engineering talent.
            </p>
            
            <div className={styles.statsContainer}>
              <div className={styles.statItem}>
                <div className={styles.statIcon}><Users size={16} /></div>
                <span>48 registered teams · 192 participants</span>
              </div>
              <div className={styles.statItem}>
                <div className={styles.statIcon}><Award size={16} /></div>
                <span>$25,000 in prizes across 6 categories</span>
              </div>
              <div className={styles.statItem}>
                <div className={styles.statIcon}><Calendar size={16} /></div>
                <span>June 20–22, 2026 · Engineering Complex</span>
              </div>
            </div>

            <div className={styles.roleTags}>
              <span className={styles.roleTag}>Student</span>
              <span className={styles.roleTag}>Team Leader</span>
              <span className={styles.roleTag}>Mentor</span>
              <span className={styles.roleTag}>Judge</span>
            </div>
          </div>
          
          {/* Abstract circles decoration */}
          <div className={`${styles.circle} ${styles.circle1}`}></div>
          <div className={`${styles.circle} ${styles.circle2}`}></div>
        </Col>

        {/* Right Side - Login Form */}
        <Col lg={7} className={styles.formSection}>
          <div className={styles.formContainer}>
            <h2 className={styles.formTitle}>Welcome back</h2>
            <p className={styles.formSubtitle}>Sign in to access your dashboard</p>

            {error && <Alert variant="danger">{error}</Alert>}

            <Form onSubmit={handleStandardLogin} className={styles.form}>
              <Form.Group className="mb-3" controlId="email">
                <Form.Label>Email address</Form.Label>
                <Form.Control
                  type="email"
                  placeholder="you@fpt.edu.vn"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </Form.Group>

              <Form.Group className="mb-3" controlId="password">
                <Form.Label>Password</Form.Label>
                <Form.Control
                  type="password"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </Form.Group>

              <Button variant="primary" type="submit" className="w-100 py-2" disabled={submitting}>
                {submitting ? <><Spinner size="sm" className="me-2" />Signing in...</> : 'Sign In'}
              </Button>
            </Form>

            <div className="text-center mt-2 mb-3">
              <span className="text-muted">Don't have an account? </span>
              <Link to="/register" className="fw-medium text-primary text-decoration-none">
                Sign up
              </Link>
            </div>

          </div>
        </Col>
      </Row>
    </div>
  );
};

export default Login;
