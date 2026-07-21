import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Row, Col, Form, Button, Alert, Spinner } from 'react-bootstrap';
import { Zap } from 'lucide-react';
import loginStyles from './Login.module.css';
import { useTheme } from '../../context/ThemeContext';
import { registerFpt, registerExternal } from '../../api/authApi';
import { getCampuses } from '../../api/universityApi';
import { FPT_CAMPUSES } from '../../config/registerConfig';

const Register = () => {
  const navigate = useNavigate();
  const { setForceTheme } = useTheme();
  const [studentType, setStudentType] = useState('fpt'); // 'fpt' or 'external'
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [studentId, setStudentId] = useState('');
  const [campusId, setCampusId] = useState('');
  const [universityName, setUniversityName] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [campuses, setCampuses] = useState(FPT_CAMPUSES);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setForceTheme('light');
    return () => setForceTheme(null);
  }, [setForceTheme]);

  useEffect(() => {
    getCampuses()
      .then((list) => {
        if (Array.isArray(list) && list.length) {
          setCampuses(list.map((c) => ({ id: c.id, name: c.name })));
        }
      })
      .catch(() => { /* fall back to static FPT_CAMPUSES */ });
  }, []);

  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');
    if (password.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }
    if (password !== confirm) {
      setError('Passwords do not match');
      return;
    }
    if (!acceptedTerms) {
      setError('You must accept the Terms and Conditions and Privacy Policy');
      return;
    }
    setSubmitting(true);
    try {
      const res = studentType === 'fpt'
        ? await registerFpt({ fullName, email, password, studentId, campusId, acceptedTerms })
        : await registerExternal({ fullName, email, password, universityName, acceptedTerms });
      if (!res.ok) {
        // Surface backend field-level validation errors (e.g. password too short,
        // invalid email) instead of the generic "Validation failed" message.
        const fieldErrors = res.data?.fieldErrors;
        if (fieldErrors && typeof fieldErrors === 'object') {
          setError(Object.values(fieldErrors).join('. '));
        } else {
          setError(res.data?.message || 'Registration failed');
        }
        return;
      }
      navigate('/pending-approval');
    } catch {
      setError('Could not connect to the server');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={loginStyles.loginPage}>
      <Row className="g-0 min-vh-100">
        <Col lg={5} className={loginStyles.heroSection}>
          <div className={loginStyles.heroContent}>
            <div className={loginStyles.brandBadge}>
              <Zap size={16} fill="currentColor" /> SEAL Hackathon 2026
            </div>
            <h1 className={loginStyles.heroTitle}>Join the Competition</h1>
            <p className={loginStyles.heroDescription}>
              Register your account to join a team, submit projects, and compete for $25,000 in prizes.
            </p>
          </div>
          <div className={`${loginStyles.circle} ${loginStyles.circle1}`}></div>
          <div className={`${loginStyles.circle} ${loginStyles.circle2}`}></div>
        </Col>

        <Col lg={7} className={loginStyles.formSection}>
          <div className={loginStyles.formContainer}>
            <h2 className={loginStyles.formTitle}>Create Account</h2>
            <p className={loginStyles.formSubtitle}>Join SEAL Hackathon 2026 as a participant</p>

            {error && <Alert variant="danger">{error}</Alert>}

            <div className="d-flex gap-2 mb-4">
              <Button 
                variant={studentType === 'fpt' ? 'primary' : 'outline-primary'} 
                className="flex-grow-1"
                onClick={() => setStudentType('fpt')}
              >
                FPT Student
              </Button>
              <Button 
                variant={studentType === 'external' ? 'primary' : 'outline-primary'} 
                className="flex-grow-1"
                onClick={() => setStudentType('external')}
              >
                External Student
              </Button>
            </div>

            <Form onSubmit={handleRegister} className={loginStyles.form}>
              <Form.Group className="mb-3">
                <Form.Label>Full Name</Form.Label>
                <Form.Control type="text" placeholder="John Doe" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Email</Form.Label>
                <Form.Control type="email" placeholder="you@example.com" value={email} onChange={(e) => setEmail(e.target.value)} required />
              </Form.Group>

              {studentType === 'fpt' ? (
                <Row>
                  <Col md={6}>
                    <Form.Group className="mb-3">
                      <Form.Label>FPT Student ID</Form.Label>
                      <Form.Control type="text" placeholder="SE123456" value={studentId} onChange={(e) => setStudentId(e.target.value)} required />
                    </Form.Group>
                  </Col>
                  <Col md={6}>
                    <Form.Group className="mb-3">
                      <Form.Label>Campus</Form.Label>
                      <Form.Select value={campusId} onChange={(e) => setCampusId(e.target.value)} required>
                        <option value="">Select Campus</option>
                        {campuses.map((c) => (
                          <option key={c.id} value={c.id}>{c.name}</option>
                        ))}
                      </Form.Select>
                    </Form.Group>
                  </Col>
                </Row>
              ) : (
                <Row>
                  <Col md={6}>
                    <Form.Group className="mb-3">
                      <Form.Label>Student ID</Form.Label>
                      <Form.Control type="text" placeholder="ID Number" value={studentId} onChange={(e) => setStudentId(e.target.value)} />
                    </Form.Group>
                  </Col>
                  <Col md={6}>
                    <Form.Group className="mb-3">
                      <Form.Label>University Name</Form.Label>
                      <Form.Control type="text" placeholder="University" value={universityName} onChange={(e) => setUniversityName(e.target.value)} required />
                    </Form.Group>
                  </Col>
                </Row>
              )}

              <Row>
                <Col md={6}>
                  <Form.Group className="mb-3">
                    <Form.Label>Password</Form.Label>
                    <Form.Control type="password" placeholder="••••••••" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} />
                    <Form.Text className="text-muted">At least 8 characters</Form.Text>
                  </Form.Group>
                </Col>
                <Col md={6}>
                  <Form.Group className="mb-4">
                    <Form.Label>Confirm Password</Form.Label>
                    <Form.Control type="password" placeholder="••••••••" value={confirm} onChange={(e) => setConfirm(e.target.value)} required />
                  </Form.Group>
                </Col>
              </Row>

              <Form.Check
                className="mb-3"
                type="checkbox"
                checked={acceptedTerms}
                onChange={(e) => setAcceptedTerms(e.target.checked)}
                label="I agree to the Terms and Conditions and Privacy Policy."
                required
              />

              <Button variant="primary" type="submit" className="w-100 py-2" disabled={submitting}>
                {submitting ? <><Spinner size="sm" className="me-2" />Creating...</> : 'Create Account'}
              </Button>
            </Form>

            <div className="text-center mt-4">
              <span className="text-muted">Already have an account? </span>
              <Link to="/login" className="fw-medium text-primary text-decoration-none">
                Sign in
              </Link>
            </div>
          </div>
        </Col>
      </Row>
    </div>
  );
};

export default Register;
