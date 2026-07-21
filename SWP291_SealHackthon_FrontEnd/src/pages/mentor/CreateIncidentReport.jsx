import React from 'react';
import IncidentReportForm from '../../components/incident/IncidentReportForm';

const CreateIncidentReport = () => {
  return (
    <div className="py-2">
      <div className="mb-4">
        <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Create Incident Report</h1>
        <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Report a rule violation or unprofessional conduct</div>
      </div>
      
      <IncidentReportForm isJudge={false} />
    </div>
  );
};

export default CreateIncidentReport;
