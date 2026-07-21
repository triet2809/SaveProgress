import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Row, Col, Modal, Form, InputGroup, Spinner, Alert, Badge } from 'react-bootstrap';
import { Download, FileText, BarChart2, Search, Eye } from 'lucide-react';
import { getRounds, getJudgeVariance, getAnonymizedDataset, downloadRankingCsv } from '../../api/hackathonApi';

const REPORT_DEFS = [
  { key: 'ranking-csv', name: 'Ranking Export (CSV)', type: 'CSV', desc: 'Final weighted rankings for the selected round.' },
  { key: 'judge-variance', name: 'Judge Variance Report', type: 'JSON', desc: 'Score variance across judges per team/criterion.' },
  { key: 'anonymized-dataset', name: 'Anonymized Dataset', type: 'JSON', desc: 'De-identified scoring dataset for analysis.' },
];

const Reports = () => {
  const [rounds, setRounds] = useState([]);
  const [selectedRound, setSelectedRound] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  const [busyKey, setBusyKey] = useState('');
  const [showViewModal, setShowViewModal] = useState(false);
  const [viewTitle, setViewTitle] = useState('');
  const [viewData, setViewData] = useState(null);

  useEffect(() => {
    (async () => {
      try {
        const data = await getRounds({ size: 100 });
        const list = data.content || data || [];
        setRounds(list);
        if (list.length) setSelectedRound(list[0].id);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const filteredReports = REPORT_DEFS.filter((r) =>
    r.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleDownloadCsv = async () => {
    if (!selectedRound) { setError('Select a round first'); return; }
    setError('');
    setBusyKey('ranking-csv');
    try {
      const csv = await downloadRankingCsv(selectedRound);
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `ranking-round-${selectedRound}.csv`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyKey('');
    }
  };

  const handleView = async (report) => {
    if (!selectedRound) { setError('Select a round first'); return; }
    setError('');
    if (report.key === 'ranking-csv') { handleDownloadCsv(); return; }
    setBusyKey(report.key);
    try {
      const data = report.key === 'judge-variance'
        ? await getJudgeVariance(selectedRound)
        : await getAnonymizedDataset(selectedRound);
      setViewTitle(report.name);
      setViewData(data);
      setShowViewModal(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyKey('');
    }
  };

  return (
    <div className="py-2">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1" style={{ color: 'var(--cf-text-primary)' }}>Reports & Analytics</h1>
          <div style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Export data and view event analytics</div>
        </div>
        <Form.Select
          style={{ width: '260px' }}
          value={selectedRound}
          onChange={(e) => setSelectedRound(e.target.value)}
          disabled={loading}
        >
          {loading && <option>Loading rounds...</option>}
          {!loading && rounds.length === 0 && <option value="">No rounds available</option>}
          {rounds.map((r) => (
            <option key={r.id} value={r.id}>{r.name}</option>
          ))}
        </Form.Select>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      <Row className="g-4 mb-4">
        <Col md={6}>
          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)', height: '100%' }}>
            <Card.Body className="p-4 d-flex flex-column align-items-center justify-content-center text-center">
              <div className="mb-3 text-primary bg-primary bg-opacity-10 p-3 rounded-circle">
                <Download size={32} />
              </div>
              <h5 className="fw-bold" style={{ color: 'var(--cf-text-primary)' }}>Export Ranking Data</h5>
              <p style={{ color: 'var(--cf-text-secondary)', fontSize: '0.875rem' }}>Download the final weighted ranking CSV for the selected round.</p>
              <Button variant="outline-primary" className="mt-2 d-flex align-items-center gap-2" onClick={handleDownloadCsv} disabled={busyKey === 'ranking-csv' || !selectedRound}>
                {busyKey === 'ranking-csv' ? <Spinner size="sm" animation="border" /> : <Download size={16} />} Download Ranking CSV
              </Button>
            </Card.Body>
          </Card>
        </Col>
        <Col md={6}>
          <Card style={{ border: 'none', borderRadius: 'var(--cf-radius-lg)', backgroundColor: 'var(--cf-bg-surface)', boxShadow: '0 1px 3px rgba(0,0,0,0.05)', height: '100%' }}>
            <InputGroup className="mb-3">
              <InputGroup.Text>
                <Search size={16} />
              </InputGroup.Text>
              <Form.Control placeholder="Search reports..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
            </InputGroup>
            <Card.Body className="p-4">
              <h5 className="fw-bold mb-4" style={{ color: 'var(--cf-text-primary)' }}>Available Reports</h5>
              <div className="d-flex flex-column gap-3">
                {filteredReports.length === 0 && (
                  <div className="text-center text-muted py-3">No reports match your search.</div>
                )}
                {filteredReports.map((report) => (
                  <div key={report.key} className="d-flex align-items-center justify-content-between p-3 rounded" style={{ backgroundColor: 'var(--cf-bg-main)' }}>
                    <div className="d-flex align-items-center gap-3">
                      <FileText size={18} className="text-secondary" />
                      <div>
                        <div className="fw-medium" style={{ color: 'var(--cf-text-primary)' }}>{report.name} <Badge bg="light" text="dark" className="border ms-1">{report.type}</Badge></div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--cf-text-secondary)' }}>{report.desc}</div>
                      </div>
                    </div>
                    <div className="d-flex gap-2">
                      {report.key === 'ranking-csv' ? (
                        <Button variant="link" size="sm" className="p-0 text-success" onClick={() => handleDownloadCsv()} disabled={busyKey === report.key || !selectedRound}>
                          {busyKey === report.key ? <Spinner size="sm" animation="border" /> : <Download size={16} />}
                        </Button>
                      ) : (
                        <Button variant="link" size="sm" className="p-0 text-primary" onClick={() => handleView(report)} disabled={busyKey === report.key || !selectedRound}>
                          {busyKey === report.key ? <Spinner size="sm" animation="border" /> : <Eye size={16} />}
                        </Button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <Modal show={showViewModal} onHide={() => setShowViewModal(false)} size="lg" scrollable>
        <Modal.Header closeButton>
          <Modal.Title className="d-flex align-items-center gap-2"><BarChart2 size={20} /> {viewTitle}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {viewData == null ? (
            <div className="text-center text-muted py-4">No data available.</div>
          ) : (
            <pre style={{ maxHeight: '60vh', overflow: 'auto', fontSize: '0.8rem' }}>{JSON.stringify(viewData, null, 2)}</pre>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowViewModal(false)}>Close</Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default Reports;
