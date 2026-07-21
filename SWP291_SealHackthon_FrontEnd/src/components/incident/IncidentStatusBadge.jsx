import React from 'react';
import { Badge } from 'react-bootstrap';

const IncidentStatusBadge = ({ status }) => {
  let bg = 'secondary';
  let text = 'light';

  switch (status) {
    case 'Pending Review':
      bg = 'warning';
      text = 'dark';
      break;
    case 'Under Review':
      bg = 'info';
      text = 'dark';
      break;
    case 'Resolved':
      bg = 'success';
      break;
    case 'Warning Issued':
      bg = 'warning';
      text = 'dark';
      break;
    case 'Disqualified':
      bg = 'danger';
      break;
    case 'Rejected':
      bg = 'secondary';
      break;
    default:
      bg = 'secondary';
  }

  return (
    <Badge bg={bg} text={text}>
      {status}
    </Badge>
  );
};

export default IncidentStatusBadge;
