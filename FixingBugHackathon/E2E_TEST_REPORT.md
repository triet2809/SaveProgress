# Báo cáo E2E Testing — SEAL Hackathon Management System

> **CẬP NHẬT 2026-07-22:** Tất cả bug đã được fix và verify. Xem mục [ĐÃ SỬA](#đã-sửa--verified) ở cuối.

**Ngày test:** 2026-07-22  
**Backend:** SWP391_SealHackathon_BackEnd (Spring Boot 3.3.4, Java 17)  
**Frontend:** SWP291_SealHackthon_FrontEnd (React + Vite)  
**DB:** PostgreSQL 17 — `seal_e2e` (fresh restore từ `db.sql`)  
**Test accounts:** `demo.ec@seal.local` (coordinator), `demo.judge@seal.local` (judge), `demo.leader@seal.local` (team_leader), `demo.member@seal.local` (team_member), `demo.mentor@seal.local` (mentor) — mật khẩu reset thành `Password123!`

---

## Tổng quan kết quả

| Hạng mục | Số lượng |
|----------|----------|
| Test cases (unit + MVC) | 195 — **tất cả PASS** (với JDK 17 + `-Dnet.bytebuddy.experimental=true`) |
| E2E API smoke tests | ~70 request |
| Bug thật tìm được | **4 bug nghiêm trọng, 2 bug trung bình, 3 vấn đề data/infra** |
| Endpoint hoạt động bình thường | ~30/33 module |

---

## BUG NGHIỆM TRỌNG (Critical)

### 🔴 Bug #1: `GET /rounds` → 500 Internal Server Error

**Nguyên nhân gốc:** DB chứa giá trị `lifecycle_state = 'FINALIZED'` (3 bản ghi) nhưng enum `RoundLifecycleState` trong code **không có hằng số `FINALIZED`**. Enum chỉ có: `SCORING, APPEAL_WINDOW_OPEN, PAUSED_FOR_APPEAL, AWAITING_RECALCULATION, READY_TO_ADVANCE, ADVANCED, READY_FOR_AWARDS`.

Khi Hibernate load entity `Round`, nó gặp string `FINALIZED` → `IllegalArgumentException: No enum constant` → 500.

```
Caused by: java.lang.IllegalArgumentException: No enum constant
vn.edu.fpt.seal.common.enums.RoundLifecycleState.FINALIZED
```

**Tác động:** Mọi API nào list/query round mà DB có data `FINALIZED` đều crash 500. Đây là bug chặn toàn bộ luồng round.

**Cách sửa (2 lựa chọn):**

- **Option A (khuyến nghị):** Thêm `FINALIZED` vào enum `RoundLifecycleState` (nếu logic domain cần trạng thái "đã chốt"). Migration batch14 mới normalize `PUBLISHED → APPEAL_WINDOW_OPEN` nhưng bỏ sót `FINALIZED`. Thêm migration batch15:
  ```sql
  -- batch15: normalize FINALIZED → READY_FOR_AWARDS
  UPDATE rounds SET lifecycle_state = 'READY_FOR_AWARDS' WHERE lifecycle_state = 'FINALIZED';
  UPDATE round_definitions SET lifecycle_state = 'READY_FOR_AWARDS' WHERE lifecycle_state = 'FINALIZED';
  ```
  Hoặc thêm enum value + cập nhật `CompetitionLifecycleService.refresh()` để xử lý `FINALIZED` như terminal state (skip refresh).

- **Option B:** Data-only fix — UPDATE 3 bản ghi `FINALIZED` → `READY_FOR_AWARDS` (hoặc `ADVANCED`). Nhanh nhưng không phòng ngừa data cũ khác.

**Files liên quan:**
- `common/enums/RoundLifecycleState.java`
- `modules/round/service/CompetitionLifecycleService.java:refresh()`
- `migration_batch14_round_lifecycle_compatibility.sql` (bổ sung)

---

### 🔴 Bug #2: `GET /scores` (judge) → 403 Forbidden

**Nguyên nhân gốc:** `ScoreService.list()` gọi `findAll(pageable)` rồi kiểm tra từng score: `canReadDetailedScore(user, submission)`. Judge `demo.judge` **chưa được assign vào round nào** trong DB test → `isAssignedJudge()` trả false cho mọi score → 403.

Test data `demo.judge` có role `judge` nhưng không có bản ghi trong `round_judges`. Đây là vấn đề seed data, không phải bug code. Tuy nhiên, message lỗi `"You are not authorized to view one or more requested scores"` gây nhầm lẫn vì judge authenticated đúng role.

**Tác động:** Judge không xem được danh sách score nếu chưa assign round — đúng logic RBAC, nhưng test data thiếu assign.

**Cách sửa:**
- Seed `round_judges` cho `demo.judge` vào ít nhất 1 round của DEMO event
- Hoặc: endpoint `/scores` nên cho judge xem score của round họ được assign — nếu không có assign nào, trả list rỗng thay vì 403 (vì `findAll()` lấy tất cả score rồi kiểm tra từng cái)
- **Góp ý thiết kế:** `list()` đang dùng `findAll()` rồi filter — nên filter theo `judgeId = currentUser.id` khi caller là judge thay vì load all rồi reject

---

### 🔴 Bug #3: Login pending/rejected user → 401 thay vì 403

**Nguyên nhân gốc:** `AuthService.login()` kiểm tra password **trước** rồi mới kiểm tra status:
```java
if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
    throw ApiException.unauthorized("Invalid email or password");  // ← 401
}
if (user.getStatus() == AccountStatus.pending) {
    throw ApiException.forbidden("Your account is pending approval");  // ← 403
}
```

Test reset tất cả user approved thành `Password123!` nhưng `e2e.pending` và `e2e.rejected` **vẫn giữ hash cũ** (không nằm trong `WHERE status='approved'`). Nên password không match → 401.

**Thực tế:** Code logic đúng — pending/rejected user nhập đúng password sẽ nhận 403. Nhưng thông báo 401 `"Invalid email or password"` cho pending/rejected user có thể leak thông tin (không tiết lộ account tồn tại). Đây là security pattern chấp nhận được.

**Cách xử lý:** Đây là false positive do test script reset password sai. Để test đúng, reset hash cho pending/rejected user nữa:
```sql
UPDATE users SET password_hash='<hash Password123!>' WHERE email IN ('e2e.pending@seal.local','e2e.rejected@seal.local');
```

---

### 🔴 Bug #4: `GET /logical-rounds` (root) → 404

**Nguyên nhân gốc:** `LogicalRoundController` chỉ có mapping `{id}` (PATCH, DELETE) và `{id}/promoted-unassigned`, `{id}/assignments` — **không có `GET` (list) endpoint**. Class-level `@PreAuthorize("hasRole('COORDINATOR')")` đúng, nhưng không có route `GET /logical-rounds`.

**Tác động:** Không có API list tất cả logical round. Coordinator phải biết trước ID.

**Cách sửa:** Thêm endpoint list:
```java
@GetMapping
@Operation(summary = "List logical rounds by event")
public ResponseEntity<List<LogicalRoundResponse>> list(@RequestParam UUID eventId) {
    return ResponseEntity.ok(service.listByEvent(eventId));
}
```
Thêm method `listByEvent(UUID eventId)` vào `LogicalRoundIntegrityService`.

---

## BUG TRUNG BÌNH (Medium)

### 🟡 Bug #5: Onboarding block toàn bộ API — kể cả `mustChangePassword=false`

**Nguyên nhân gốc:** `JwtAuthenticationFilter` kiểm tra `onboardingRequired` và `termsAcceptanceRequired` trên mọi request authenticated. Nếu `termsAcceptedAt == null` hoặc terms/privacy version mismatch → trả `"Complete password and terms onboarding before continuing"`.

User `demo.ec` có `mustChangePassword=false` nhưng `termsAcceptedAt=null` → bị block cho đến khi gọi `POST /auth/onboarding`.

**Vấn đề UX:** Login thành công trả token, nhưng mọi API call tiếp theo bị block. Frontend phải biết tự redirect đến onboarding page dựa trên `onboardingRequired: true` trong login response.

**Cách xử lý:** Đây là design đúng (bảo vệ pháp lý — phải accept terms). Nhưng nên:
- Trả status code riêng (ví dụ `451 Unavailable For Legal Reasons` hoặc `449 Upgrade Required`) thay vì generic message
- Frontend React cần check `onboardingRequired` sau login và redirect

---

### 🟡 Bug #6: `POST /events` (create) → 409 "Event title already exists"

**Nguyên nhân gốc:** DB test đã có event title `"E2E Test Event"` từ các lần test trước (smoke test tạo rồi không xóa). Không phải bug code — là test pollution.

**Cách xử lý:** Test script nên dùng unique title (timestamp) hoặc cleanup sau test:
```bash
TITLE="E2E Test Event $(date +%s)"
```

---

## VẤN ĐỀ DATA / INFRA

### ⚠️ Vấn đề #7: DB `seal_hackathon` cũ — thiếu 9 bảng + nhiều cột

DB chính `seal_hackathon` (trước khi nạp fresh) thiếu:
- 9 bảng: `account_activation_tokens`, `team_profiles`, `team_recognitions`, `round_result_versions`, `round_definitions`, `logical_round_promotions`, `event_seed_assignments`, `event_team_finishes`, `round_result_version_entries`
- Cột users: `security_version`, `must_change_password`, `terms_accepted_at`, `terms_version`, `privacy_version`

**Nguyên nhân:** DB cũ không chạy migration batch 3–14. `db.sql` là dump đầy đủ đã chạy migration.

**Cách sửa:** Chạy migration batch 1–14 theo thứ tự, hoặc restore từ `db.sql`.

### ⚠️ Vấn đề #8: Test unit fail 124/195 với JDK 25 (Byte Buddy)

**Nguyên nhân gốc:** Java 26 (runtime `node v26.3.0` dùng JDK 25) không được Mockito/Byte Buddy support. Cần `-Dnet.bytebuddy.experimental=true`.

**Cách giải quyết:**
- Chạy test với JDK 17: `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home mvn test`
- Hoặc thêm flag vào `pom.xml` surefire config:
  ```xml
  <argLine>-Dnet.bytebuddy.experimental=true</argLine>
  ```

### ⚠️ Vấn đề #9: `lifecycle_state` là `varchar` không `enum` Postgres

DB dùng `varchar(40)` cho `lifecycle_state` thay vì Postgres enum type → không có constraint ở DB level → data `FINALIZED` cũ lọt vào. Code dùng Java enum nên chỉ crash khi load.

**Cách phòng ngừa:** Thêm CHECK constraint:
```sql
ALTER TABLE rounds ADD CONSTRAINT chk_lifecycle_state
CHECK (lifecycle_state IN ('SCORING','APPEAL_WINDOW_OPEN','PAUSED_FOR_APPEAL',
  'AWAITING_RECALCULATION','READY_TO_ADVANCE','ADVANCED','READY_FOR_AWARDS'));
```

---

## LUỒNG HOẠT ĐỘNG BÌNH THƯỜNG (PASS)

| Module | Endpoint | Status | Ghi chú |
|--------|----------|--------|---------|
| Auth | `POST /auth/login` | ✅ 200 | Tất cả role |
| Auth | `POST /auth/login` (wrong pwd) | ✅ 401 | |
| Auth | `GET /auth/me` | ✅ 200 | |
| Auth | `POST /auth/refresh` | ✅ 200 | |
| Auth | `POST /auth/logout` | ✅ 200 | |
| Auth | `POST /auth/register` | ✅ 200 | Campus hợp lệ |
| Auth | `POST /auth/onboarding` | ✅ 200 | |
| Public | `GET /universities` | ✅ 200 | Không cần token |
| Public | `GET /campuses` | ✅ 200 | Không cần token |
| Event | `GET /events` | ✅ 200 | |
| Event | `GET /events/{id}` | ✅ 200 | |
| Event | `POST /events` | ✅ 200/409 | 409 = trùng title |
| Event | `POST /events/{id}/publish` | ✅ | |
| Track | `GET /events/{id}/tracks` | ✅ | |
| Round | `GET /rounds?eventId=xxx` | ✅ 200 | Có eventId |
| Round | `GET /rounds` (không param) | ❌ 500 | Bug #1 |
| Round | `GET /rounds/{id}` | ✅ | |
| Logical Round | `GET /logical-rounds` | ❌ 404 | Bug #4 |
| Team | `GET /teams?eventId=xxx` | ✅ 200 | Cần eventId |
| Team | `GET /teams/me` | ✅ | |
| Submission | `GET /submissions?eventId=xxx` | ✅ | Cần eventId |
| Score | `GET /scores` (coordinator) | ✅ 200 | |
| Score | `GET /scores` (judge) | ❌ 403 | Bug #2 |
| Ranking | `GET /round-rankings?roundId=xxx` | ✅ | Cần roundId |
| Appeal | `GET /appeals?eventId=xxx` | ✅ 200 | Cần eventId |
| Incident | `GET /incidents?eventId=xxx` | ✅ 200 | |
| Prize | `GET /prizes?eventId=xxx` | ✅ 200 | |
| Timeline | `GET /events/{id}/timeline` | ✅ 200 | Public |
| Staff | `GET /events/{id}/staff` | ✅ 200 | |
| Seeding | `GET /events/{id}/seeds` | ✅ 200 | |
| Seeding | `GET /events/{id}/seed-candidates` | ✅ | |
| Recognition | `GET /team-profiles/{id}/recognitions/evidence` | ✅ 404 | Route đúng, ID test không tồn tại |
| Criteria | `GET /criteria-templates` | ✅ 200 | |
| Notice | `GET /notices` | ✅ 200 | |
| Mentor Feedback | `GET /mentor-feedbacks` | ✅ 200 | |
| Support Ticket | `GET /support-tickets` | ✅ 200 | |
| Chat | `GET /team-chat?teamId=xxx` | ✅ | Cần teamId |
| Report | `GET /reports/rounds/{id}/judge-variance` | ✅ 400 | Round không tồn tại |
| RBAC | `POST /events` as member | ✅ 403 | Đúng |
| RBAC | Protected endpoint no token | ✅ 401 | Đúng |

---

## HƯỚNG GIẢI QUYẾT ƯU TIÊN

1. **Bug #1 (FINALIZED enum)** — nghiêm trọng nhất, chặn toàn bộ round listing. Fix ngay: thêm enum value hoặc migration normalize data
2. **Vấn đề #7 (DB cũ)** — chạy migration batch 1–14 hoặc restore `db.sql` cho mọi môi trường
3. **Bug #4 (logical-rounds list)** — thêm GET endpoint
4. **Bug #2 (score RBAC)** — seed round_judges + cải tiến filter logic
5. **Vấn đề #8 (Byte Buddy)** — thêm surefire `argLine` vào pom.xml
6. **Vấn đề #9 (CHECK constraint)** — phòng ngừa data lỗi tiếp tục

---

## ĐÃ SỬA — Verified

Tất cả bug đã fix, chạy lại **195/195 unit test PASS** + E2E smoke verify từng endpoint trả 200.

| Bug | Fix | File | Verify |
|-----|-----|------|--------|
| #1 FINALIZED 500 | Migration batch15 normalize `FINALIZED → READY_FOR_AWARDS` + CHECK constraint DB | `migration_batch15_normalize_finalized.sql` | `GET /rounds` → 200 ✅ |
| #2 Score judge 403 | `ScoreService.list()` auto-filter theo `judgeId` khi caller là judge (không còn `findAll()` rồi 403) | `modules/score/service/ScoreService.java` | `GET /scores` (judge) → 200 ✅ |
| #4 logical-rounds 404 | Thêm `GET /logical-rounds?eventId=` + method `listByEvent()` | `LogicalRoundController.java`, `LogicalRoundIntegrityService.java` | `GET /logical-rounds` → 200 ✅ |
| #5 Onboarding block | Trả **449 Upgrade Required** + `code:ONBOARDING_REQUIRED` thay 403 generic; frontend `client.js` redirect `/onboarding` | `JwtAuthenticationFilter.java`, `api/client.js` | 449 + redirect ✅ |
| #8 Byte Buddy | Thêm surefire `<argLine>-Dnet.bytebuddy.experimental=true</argLine>` | `pom.xml` | 195 test PASS ✅ |
| #9 CHECK constraint | `chk_rounds_lifecycle_state` + `chk_round_definitions_lifecycle_state` | `migration_batch15_normalize_finalized.sql` | Constraint applied ✅ |

**Ghi chú Bug #3 (login 401):** Xác nhận false positive — code logic đúng, do test reset password chỉ cho user `approved`. Không cần fix code.

**Ghi chú Bug #6 (event 409):** Test pollution, không phải bug. Fix test dùng unique title.

**Còn lại — cần chạy trên môi trường thật:**
- Vấn đề #7 (DB cũ thiếu bảng): chạy migration batch 1–15 hoặc restore `db.sql` cho mọi env

---

## SCRIPT E2E

Script test: `/tmp/e2e_smoke.sh` — chạy được khi backend đang chạy ở port 8080.

```bash
# Khởi động backend
cd SWP391_SealHackathon_BackEnd
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
  DB_URL="jdbc:postgresql://localhost:5432/seal_e2e" \
  mvn spring-boot:run -q

# Chạy E2E test (terminal khác)
bash /tmp/e2e_smoke.sh
```
