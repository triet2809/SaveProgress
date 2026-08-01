import { useEffect, useState } from 'react';
import { Modal, Form, Button, Row, Col, Alert, Spinner } from 'react-bootstrap';
import { getCampuses } from '../../api/universityApi';
import { FPT_CAMPUSES } from '../../config/registerConfig';

/**
 * GoogleProfileModal — thu thập hồ sơ bắt buộc (loại sinh viên, MSSV, campus)
 * sau khi đăng nhập Google lần đầu. Google chỉ cấp email + tên, nên MSSV/campus
 * phải nhập ở bước này trước khi tạo tài khoản pending.
 *
 * @param show           hiển thị modal hay không
 * @param email          email lấy từ Google (chỉ đọc)
 * @param fullName       tên lấy từ Google (chỉ đọc)
 * @param submitting     đang gửi pha 2 lên backend
 * @param error          thông báo lỗi (nếu có)
 * @param onSubmit       (profile) => void — gửi hồ sơ pha 2
 * @param onHide         đóng modal / hủy
 */
const GoogleProfileModal = ({ show, email, fullName, submitting, error, onSubmit, onHide }) => {
  const [studentType, setStudentType] = useState('fpt');
  const [studentId, setStudentId] = useState('');
  const [campusId, setCampusId] = useState('');
  const [universityName, setUniversityName] = useState('');
  const [campuses, setCampuses] = useState(FPT_CAMPUSES);

  useEffect(() => {
    getCampuses()
      .then((list) => {
        if (Array.isArray(list) && list.length) {
          setCampuses(list.map((c) => ({ id: c.id, name: c.name })));
        }
      })
      .catch(() => { /* fall back to static FPT_CAMPUSES */ });
  }, []);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (studentType === 'fpt') {
      onSubmit({ studentType: 'fpt', studentId, campusId: campusId || null });
    } else {
      onSubmit({ studentType: 'external', studentId, universityName });
    }
  };

  return (
    <Modal show={show} onHide={onHide} backdrop="static" centered>
      <Form onSubmit={handleSubmit}>
        <Modal.Header closeButton>
          <Modal.Title>Complete your profile</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="text-muted small mb-3">
            Signed in as <strong>{fullName || email}</strong>. Please provide your
            student details to finish creating your account.
          </p>

          {error && <Alert variant="danger">{error}</Alert>}

          <div className="d-flex gap-2 mb-3">
            <Button
              type="button"
              variant={studentType === 'fpt' ? 'primary' : 'outline-primary'}
              className="flex-grow-1"
              onClick={() => setStudentType('fpt')}
            >
              FPT Student
            </Button>
            <Button
              type="button"
              variant={studentType === 'external' ? 'primary' : 'outline-primary'}
              className="flex-grow-1"
              onClick={() => setStudentType('external')}
            >
              External Student
            </Button>
          </div>

          {studentType === 'fpt' ? (
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>FPT Student ID</Form.Label>
                  <Form.Control
                    type="text"
                    placeholder="SE123456"
                    value={studentId}
                    onChange={(e) => setStudentId(e.target.value)}
                    required
                  />
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
                  <Form.Control
                    type="text"
                    placeholder="ID Number"
                    value={studentId}
                    onChange={(e) => setStudentId(e.target.value)}
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>University Name</Form.Label>
                  <Form.Control
                    type="text"
                    placeholder="University"
                    value={universityName}
                    onChange={(e) => setUniversityName(e.target.value)}
                    required
                  />
                </Form.Group>
              </Col>
            </Row>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onHide} disabled={submitting}>
            Cancel
          </Button>
          <Button variant="primary" type="submit" disabled={submitting}>
            {submitting ? <><Spinner size="sm" className="me-2" />Submitting...</> : 'Continue'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
};

export default GoogleProfileModal;
