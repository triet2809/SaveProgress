import React, { useState } from 'react';
import { Modal, Form, Button, Row, Col, Alert, Badge } from 'react-bootstrap';
import { Users, Shuffle } from 'lucide-react';

const TrackGeneratorModal = ({ show, onHide, teams, onGenerate }) => {
  const [trackCount, setTrackCount] = useState(2);
  const [trackNames, setTrackNames] = useState(['Track A', 'Track B']);
  // Để trống = không giới hạn số team cho mỗi track.
  const [maxTeams, setMaxTeams] = useState('');
  
  const availableTeams = teams;

  // Đổi số lượng track thì FE tự sinh tên mặc định Track A, Track B, ...
  const handleTrackCountChange = (e) => {
    const count = parseInt(e.target.value) || 1;
    setTrackCount(count);
    
    // Tạo tên mặc định theo alphabet để user sửa nhanh.
    const newNames = Array(count).fill('').map((_, i) => {
      const letter = String.fromCharCode(65 + i); // A, B, C...
      return `Track ${letter}`;
    });
    setTrackNames(newNames);
  };

  // Sửa tên từng track ngay trên state FE.
  const handleNameChange = (index, value) => {
    const newNames = [...trackNames];
    newNames[index] = value;
    setTrackNames(newNames);
  };

  // Bấm generate: FE gom dữ liệu rồi gọi callback onGenerate từ màn cha.
  // Modal này không tự gọi API trực tiếp; EventDetails mới là nơi gọi createTrack().
  const handleGenerate = () => {
    const cap = maxTeams === '' ? null : parseInt(maxTeams, 10);
    onGenerate('All', trackNames, availableTeams, cap);
    onHide();
  };

  return (
    <Modal show={show} onHide={onHide} size="lg" centered>
      <Modal.Header closeButton className="border-bottom-0 pb-0">
        <Modal.Title className="fw-bold d-flex align-items-center gap-2">
          <Shuffle size={20} className="text-primary" /> Auto-Generate Tracks
        </Modal.Title>
      </Modal.Header>
      <Modal.Body className="pt-3">
        <p className="text-muted mb-4">
          Divide registered teams into tracks/pools for judging.
        </p>

        <Row className="g-4">
          <Col md={6}>
            <Form.Group>
              <Form.Label className="fw-medium">1. Number of Tracks</Form.Label>
              <Form.Control 
                type="number" 
                min="1" 
                max="20"
                value={trackCount}
                onChange={handleTrackCountChange}
              />
            </Form.Group>
          </Col>
          <Col md={6}>
            <Form.Group>
              <Form.Label className="fw-medium">Max teams / track</Form.Label>
              <Form.Control 
                type="number" 
                min="1"
                value={maxTeams}
                onChange={(e) => setMaxTeams(e.target.value)}
                placeholder="Unlimited"
              />
              <Form.Text className="text-muted">Leave blank = unlimited. Applies to newly created tracks.</Form.Text>
            </Form.Group>
          </Col>
        </Row>

        {/* Khối này chỉ hiển thị thống kê FE để coordinator ước lượng chia track. */}
        <Alert variant="info" className="mt-4 mb-4 d-flex justify-content-between align-items-center">
          <div>
            <strong>Available Teams: </strong> {availableTeams.length} teams
          </div>
          <Badge bg="primary" pill>
            ~{Math.ceil(availableTeams.length / trackCount)} teams per track
          </Badge>
        </Alert>

        <div className="mb-3 fw-medium">2. Name Your Tracks</div>
        {/* Danh sách input tên track được render động theo trackCount. */}
        <div className="p-3 rounded" style={{ backgroundColor: 'var(--cf-bg-main)', border: '1px solid var(--cf-border-color)' }}>
          <Row className="g-3">
            {trackNames.map((name, index) => (
              <Col md={6} key={index}>
                <Form.Group>
                  <Form.Label className="small text-muted mb-1">Track {index + 1}</Form.Label>
                  <Form.Control 
                    type="text" 
                    value={name}
                    onChange={(e) => handleNameChange(index, e.target.value)}
                    placeholder={`Track ${String.fromCharCode(65 + index)}`}
                  />
                </Form.Group>
              </Col>
            ))}
          </Row>
        </div>

      </Modal.Body>
      <Modal.Footer className="border-top-0">
        <Button variant="light" onClick={onHide}>Cancel</Button>
        <Button variant="primary" onClick={handleGenerate}>
          Create Tracks
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default TrackGeneratorModal;
