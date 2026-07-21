// Cau hinh API + du lieu tinh cho form dang ky.
// API base doc tu bien moi truong Vite (VITE_API_BASE_URL), mac dinh localhost:8080.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Truong FPT University (uid co dinh theo seed DB).
export const FPT_UNIVERSITY_ID = '11111111-1111-1111-1111-111111111111';

// 5 campus FPT co dinh. value = campus uid trong DB, dung cho dropdown.
// Khi user chon campus, he thong tu gan campusId (+ universityId FPT) vao payload.
export const FPT_CAMPUSES = [
  { id: '22222222-2222-2222-2222-222222222221', name: 'FPT University Ho Chi Minh City' },
  { id: '22222222-2222-2222-2222-222222222222', name: 'FPT University Ha Noi' },
  { id: '22222222-2222-2222-2222-222222222223', name: 'FPT University Da Nang' },
  { id: '22222222-2222-2222-2222-222222222224', name: 'FPT University Can Tho' },
  { id: '22222222-2222-2222-2222-222222222225', name: 'FPT University Quy Nhon' },
];
