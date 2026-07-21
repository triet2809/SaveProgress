import React, { useState } from 'react';
import { Card, Form, Button, Alert } from 'react-bootstrap';
import { CheckCircle, Info } from 'lucide-react';

const DecisionPanel = ({ onDecisionMade }) => {
  const [decision, setDecision] = useState('');
  const [reason, setReason] = useState('');
  const [note, setNote] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    setSubmitted(true);
    if (onDecisionMade) {
      onDecisionMade(decision);
    }
  };

  if (submitted) {
    return (
      <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <Card.Body className="p-4 text-center">
          <CheckCircle size={48} className="text-success mb-3 mx-auto" />
          <h4 className="fw-bold mb-2" style={{ color: 'var(--cf-text-primary)' }}>Decision Recorded</h4>
          <p style={{ color: 'var(--cf-text-secondary)' }}>The incident status has been updated and an audit log entry has been created.</p>
        </Card.Body>
      </Card>
    );
  }

  return (
    <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
      <Card.Body className="p-4">
        <h5 className="fw-bold mb-4" style={{ color: 'var(--cf-text-primary)' }}>Coordinator Decision Panel</h5>
        
        <Alert variant="info" className="d-flex gap-2">
          <Info size={20} className="flex-shrink-0" />
          <div style={{ fontSize: '0.875rem' }}>
            Taking action will immediately update the status of this incident. The involved parties may be notified based on the decision.
          </div>
        </Alert>

        <Form onSubmit={handleSubmit}>
          <Form.Group className="mb-4">
            <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Select Action</Form.Label>
            <Form.Select 
              required 
              value={decision}
              onChange={(e) => setDecision(e.target.value)}
              className="form-control-lg"
            >
              <option value="">Choose an action...</option>
              <option value="Under Review">Mark as Under Review</option>
              <option value="Warning Issued">Issue Warning</option>
              <option value="Require Resubmission">Require Resubmission</option>
              <option value="Adjust Score">Adjust Score (Penalty)</option>
              <option value="Disqualified">Disqualify Team</option>
              <option value="Rejected">Reject Report (No Action)</option>
            </Form.Select>
          </Form.Group>

          <Form.Group className="mb-3">
            <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Official Reason (Visible to Team)</Form.Label>
            <Form.Control 
              as="textarea" 
              rows={3} 
              required 
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="State the official reason for this decision..." 
            />
          </Form.Group>

          <Form.Group className="mb-4">
            <Form.Label className="fw-medium" style={{ color: 'var(--cf-text-primary)', fontSize: '0.875rem' }}>Internal Note (Admins Only)</Form.Label>
            <Form.Control 
              as="textarea" 
              rows={2} 
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="Private notes for the coordinator team..." 
            />
          </Form.Group>

          <div className="d-flex justify-content-end">
            <Button 
              variant={decision === 'Disqualified' ? 'danger' : 'primary'} 
              type="submit"
              disabled={!decision}
            >
              Confirm Decision
            </Button>
          </div>
        </Form>
      </Card.Body>
    </Card>
  );
};

export default DecisionPanel;
