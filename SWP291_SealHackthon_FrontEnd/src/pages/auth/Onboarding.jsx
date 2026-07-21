import { useState } from 'react';
import { Alert, Button, Card, Form } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { completeOnboarding } from '../../api/authApi';
import { getStoredUser } from '../../utils/authUser';

export default function Onboarding() {
  const navigate = useNavigate();
  const user = getStoredUser() || {};
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setSaving(true);
    try {
      const auth = await completeOnboarding({
        newPassword: user.mustChangePassword ? newPassword : null,
        confirmPassword: user.mustChangePassword ? confirmPassword : null,
        acceptedTerms,
      });
      localStorage.setItem('seal_access_token', auth.accessToken);
      localStorage.setItem('seal_refresh_token', auth.refreshToken || '');
      localStorage.setItem('seal_user', JSON.stringify(auth.user || {}));
      navigate('/select-role', { replace: true });
    } catch (failure) {
      setError(failure.message || 'Onboarding failed');
    } finally {
      setSaving(false);
    }
  };

  return <div className="min-vh-100 d-flex align-items-center justify-content-center bg-light">
    <Card className="p-4 shadow-sm" style={{ width: 'min(440px, 92vw)' }}>
      <h1 className="h4">Complete account setup</h1>
      {error && <Alert variant="danger">{error}</Alert>}
      <Form onSubmit={submit}>
        {user.mustChangePassword && <>
          <Form.Group className="mb-3">
            <Form.Label>New password</Form.Label>
            <Form.Control type="password" minLength={8} required value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)} />
          </Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>Confirm new password</Form.Label>
            <Form.Control type="password" required value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)} />
          </Form.Group>
        </>}
        <Form.Check className="mb-3" type="checkbox" checked={acceptedTerms}
          onChange={(event) => setAcceptedTerms(event.target.checked)}
          label="I agree to the Terms and Conditions and Privacy Policy." required />
        <Button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Complete setup'}</Button>
      </Form>
    </Card>
  </div>;
}
