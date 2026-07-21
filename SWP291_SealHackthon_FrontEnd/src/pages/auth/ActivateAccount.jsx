import { useEffect, useState } from 'react';
import { Alert, Button, Card, Form, Spinner } from 'react-bootstrap';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { activateAccount, validateActivation } from '../../api/authApi';

export default function ActivateAccount() {
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const navigate = useNavigate();
  const [valid, setValid] = useState(null);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (!token) {
        setValid(false);
        setError('Activation link is missing.');
        return;
      }
      validateActivation(token)
        .then(() => setValid(true))
        .catch(() => {
          setValid(false);
          setError('This activation link is invalid or expired.');
        });
    }, 0);
    return () => clearTimeout(timer);
  }, [token]);

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    try {
      await activateAccount({ token, password, confirmPassword, acceptedTerms });
      setDone(true);
      setTimeout(() => navigate('/login', { replace: true }), 1200);
    } catch (failure) {
      setError(failure.message || 'Activation failed.');
    }
  };

  return <div className="min-vh-100 d-flex align-items-center justify-content-center bg-light">
    <Card className="p-4 shadow-sm" style={{ minWidth: 340 }}>
      <h1 className="h4">Activate account</h1>
      {error && <Alert variant="danger">{error}</Alert>}
      {done ? <Alert variant="success">Account activated. Redirecting to login…</Alert>
        : valid === null ? <Spinner animation="border" />
          : valid && <Form onSubmit={submit}>
            <Form.Group className="mb-3">
              <Form.Label>Password</Form.Label>
              <Form.Control type="password" minLength={8} required value={password}
                onChange={(event) => setPassword(event.target.value)} />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Confirm password</Form.Label>
              <Form.Control type="password" required value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)} />
            </Form.Group>
            <Form.Check className="mb-3" type="checkbox" checked={acceptedTerms}
              onChange={(event) => setAcceptedTerms(event.target.checked)}
              label="I agree to the Terms and Conditions and Privacy Policy." required />
            <Button type="submit">Activate</Button>
          </Form>}
    </Card>
  </div>;
}
