# HƯỚNG DẪN DEMO — SEAL Hackathon Management System

> **Lưu ý về thứ tự:** Tài liệu này sắp xếp 20 bước theo luồng thực thi hợp lệ của hệ thống.  
> Màn hình **CriteriaManagement** yêu cầu Event, Track và Round đã tồn tại trước khi cho phép thêm tiêu chí.  
> Thứ tự thực tế: Tạo Event → Tạo Track → Tạo Round → Cấu hình Criteria → Phân công Staff → Đăng ký → Demo.

---

## BƯỚC 1 — Tạo Event SEAL Summer 2026

### Mục đích
Khởi tạo sự kiện hackathon là nền tảng cho toàn bộ hệ thống. Mọi Track, Round, Team, Submission và Ranking đều thuộc về một Event cụ thể.

### Điều kiện trước khi demo
- Đã đăng nhập với tài khoản có role **coordinator**.

### Cách mở màn hình
Truy cập: `/coordinator/events`

### Thao tác demo
1. Click nút **"+ New Event"** ở góc trên bên phải.
2. Điền vào modal:
   - **Event Name**: `SEAL Summer 2026`
   - **Description**: Cuộc thi hackathon học kỳ hè 2026
   - **Term**: `Summer 2026`
   - **Prize Pool**: `50000000`
   - **Registration Start / Registration End**: khoảng thời gian mở đăng ký
   - **Event Start / Event End**: ngày thi chính thức
3. Click **"Save"**.
4. Trên hàng event vừa tạo, click nút **"Open Registration"** (biểu tượng Play) để chuyển trạng thái `draft` → `published`.

### Backend xử lý gì
- `POST /api/events` → `EventController` → `EventService.create()`.
- Validate: tiêu đề không trùng (`existsByTitleIgnoreCase`), ngày hợp lệ, **không overlap thời gian** với event khác (`existsByEventStartLessThanEqualAndEventEndGreaterThanEqual`) — rule mới bổ sung.
- "Open Registration" gọi `POST /api/events/{id}/open-registration` → đổi status `draft → published`.

### Kết quả mong đợi
- Event xuất hiện trong bảng danh sách với badge **Published**.
- Các team có thể thấy event này trong dropdown khi tạo team.

### Những lỗi dễ gặp khi demo
- **"Event dates overlap with an existing event"**: Khoảng ngày trùng với event khác đã tồn tại. Chọn lại ngày không trùng.
- **"Event name already exists"**: Tiêu đề bị trùng. Thêm hậu tố ví dụ `SEAL Summer 2026 v2`.
- Quên bấm **"Open Registration"** → team không thấy event khi tạo team (dropdown chỉ hiện event status = published).

---

## BƯỚC 2 — Tạo Track

### Mục đích
Track phân nhóm team theo chủ đề thi đấu. **Phải tạo Track trước Round** vì nút "Add Round" trong EventDetails bị vô hiệu hóa khi chưa có Track nào.

### Điều kiện trước khi demo
- Event **SEAL Summer 2026** đã tồn tại (Bước 1 hoàn thành).

### Cách mở màn hình
1. Truy cập `/coordinator/events`.
2. Click biểu tượng **mắt (View)** trên hàng **SEAL Summer 2026** → chuyển đến `/coordinator/events/:id`.
3. Chọn tab **"Tracks"**.

### Thao tác demo
1. Click nút **"Create Track"** (hoặc **"Add Track"**).
2. Điền thông tin:
   - **Track Name**: `AI Innovation`
   - **Description**: Giải pháp AI ứng dụng thực tiễn
   - **Max Teams**: để trống (unlimited) hoặc nhập số giới hạn
3. Click **"Save"** / **"Create"**.
4. Tạo thêm track thứ hai **"FinTech Solutions"** theo cùng quy trình (tuỳ chọn).

### Backend xử lý gì
- `POST /api/tracks` → `TrackService.create()`.
- Validate: track gắn đúng event, tên không trùng trong cùng event.

### Kết quả mong đợi
- Track **AI Innovation** hiển thị trong tab Tracks với capacity `0/∞`.
- Nút **"Add Round"** trong tab "Event Rounds & Tracks" bây giờ **không còn bị disable**.

### Những lỗi dễ gặp khi demo
- Nút "Add Round" vẫn bị disable: chưa có Track nào. Tạo Track trước khi tạo Round.
- **Max Teams = 0**: hệ thống hiểu là unlimited, không phải 0 chỗ — không gây lỗi.

---

## BƯỚC 3 — Tạo 2 Round (Qualification và Final)

### Mục đích
Round là vòng thi. Demo cần 2 round: **Qualification** (sơ khảo) và **Final** (chung kết). Judge và Criteria đều gắn theo Round cụ thể.

### Điều kiện trước khi demo
- Đã có ít nhất một Track (Bước 2 hoàn thành).

### Cách mở màn hình
`/coordinator/events/:id` → tab **"Event Rounds & Tracks"**

### Thao tác demo

**Round 1 — Qualification:**
1. Click **"Add Round"**.
2. Điền modal:
   - **Round Name**: `Qualification`
   - **Tracks**: tích chọn **AI Innovation**
   - **Sequence Number**: `1`
   - **Submission Deadline**: ngày/giờ chốt nộp bài vòng sơ khảo
   - **Top N to Promote**: `3`
3. Click **"Save"**.

**Round 2 — Final:**
1. Click **"Add Round"** lần thứ hai.
2. Điền modal:
   - **Round Name**: `Final`
   - **Tracks**: tích chọn **AI Innovation**
   - **Sequence Number**: `2`
   - **Submission Deadline**: ngày sau Qualification
   - **Top N to Promote**: `1`
3. Click **"Save"**.

### Backend xử lý gì
- `POST /api/rounds` → `RoundService.create()`.
- Validate: sequenceNumber duy nhất trong track; `topNToPromote` ≥ 1 và ≤ 500 (annotation `@Max(500)` mới thêm để ngăn dữ liệu bất thường).

### Kết quả mong đợi
- Hai round xuất hiện trong danh sách với sequenceNumber đúng thứ tự.
- Round Qualification hiển thị `topNToPromote = 3`, Final hiển thị `1`.

### Những lỗi dễ gặp khi demo
- **"must be less than or equal to 500"**: Nhập topNToPromote quá lớn — validation mới bảo vệ hệ thống.
- Quên tích Track trong checkbox modal: round được tạo nhưng không gắn với track nào, Judge và team không thấy round này.

---

## BƯỚC 4 — Cấu hình tiêu chí chấm điểm (Criteria)

### Mục đích
Tiêu chí xác định Judge chấm theo khía cạnh nào và trọng số từng khía cạnh. **Tổng trọng số phải bằng đúng 100%** trước khi có thể tính Ranking.

### Điều kiện trước khi demo
- Event, Track và Round Qualification đã tồn tại (Bước 1–3 hoàn thành).

### Cách mở màn hình
Truy cập: `/coordinator/criteria`

### Thao tác demo
1. Chọn **Event**: `SEAL Summer 2026` từ dropdown.
2. Chọn **Round**: `Qualification`.
3. Chọn **Track**: `AI Innovation`.
4. Click **"Add Criteria"**.
5. Điền modal:
   - **Criteria Name**: `Technical Implementation`
   - **Weight (%)**: `40`
   - **Status**: `Active`
   - **Description**: Chất lượng kỹ thuật và hiệu suất giải pháp
6. Click **"Create Criteria"**.
7. Lặp lại để thêm tiêu chí thứ hai:
   - **Criteria Name**: `Innovation & Creativity`
   - **Weight (%)**: `35`
   - **Status**: `Active`
8. Lặp lại để thêm tiêu chí thứ ba:
   - **Criteria Name**: `Presentation`
   - **Weight (%)**: `25`
   - **Status**: `Active`

> Tổng: 40 + 35 + 25 = **100%** ✓

### Backend xử lý gì
- `POST /api/criteria` → `RoundCriterionService.create()`.
- Tổng weight **chưa** được kiểm tra khi tạo từng tiêu chí — được kiểm tra tại thời điểm gọi "Recalculate Rankings".
- Khi Judge nộp điểm, `weightedScore = score × (weight / 100)` được tính tự động và lưu vào cột computed trong DB.

### Kết quả mong đợi
- Ba tiêu chí xuất hiện trong danh sách với trạng thái Active.
- Judge sẽ thấy đúng ba tiêu chí này khi chấm điểm bài nộp vòng Qualification.

### Những lỗi dễ gặp khi demo
- Tổng weight ≠ 100%: khi bấm "Recalculate Rankings" sẽ báo lỗi **"Criterion weights must sum to 100 before recalculating rankings (current sum: X)"** — đây là bug MISS-03 đã được sửa (trước đây so với 1.0 thay vì 100).
- Phải chọn đúng thứ tự Event → Round → Track; nếu không chọn Track thì danh sách tiêu chí lọc không đúng.

---

## BƯỚC 5 — Phân công Mentor

### Mục đích
Mentor hỗ trợ team trong quá trình luyện tập. Mỗi Mentor được gắn với Track cụ thể và chỉ xem được dữ liệu trong phạm vi track của mình.

### Điều kiện trước khi demo
- Event đã published, Track đã tạo.
- Tài khoản Mentor đã được duyệt trong hệ thống.

### Cách mở màn hình
Truy cập: `/coordinator/staff`

### Thao tác demo
1. Điền form phân công:
   - **Full name**: Tên Mentor
   - **Email**: Email tài khoản Mentor (đã tồn tại trong hệ thống)
   - **Event**: `SEAL Summer 2026`
   - **Event Roles**: tích checkbox **Mentor**
   - **Temporary Password**: (điền nếu muốn tạo mới tài khoản; để trống nếu tài khoản đã có)
   - **Mentor Tracks**: tích checkbox **AI Innovation**
2. Click **"Save staff assignment"**.

### Backend xử lý gì
- Gắn user với event theo role mentor và liên kết với track tương ứng.
- Mentor chỉ có quyền xem thông tin thuộc track được phân công.

### Kết quả mong đợi
- Mentor xuất hiện trong danh sách staff với role **Mentor** và track **AI Innovation**.
- Nút **"Remove mentor"** hiển thị bên cạnh — click để hủy phân công nếu cần.

### Những lỗi dễ gặp khi demo
- Email không tồn tại trong hệ thống: báo lỗi "User not found". Cần tạo tài khoản trước hoặc điền Temporary Password.
- Quên tích **Mentor Tracks**: phân công thành công nhưng mentor không thấy dữ liệu track nào.

---

## BƯỚC 6 — Phân công Judge

### Mục đích
Judge chấm điểm bài nộp trong phạm vi Track và Round được giao. Authorization đảm bảo Judge không thể chấm ngoài phạm vi.

### Điều kiện trước khi demo
- Round Qualification đã tạo (Bước 3 hoàn thành).
- Tài khoản Judge đã được duyệt.

### Cách mở màn hình
Truy cập: `/coordinator/staff`

### Thao tác demo
1. Điền form phân công:
   - **Full name**: Tên Judge
   - **Email**: Email tài khoản Judge
   - **Event**: `SEAL Summer 2026`
   - **Event Roles**: tích checkbox **Judge**
   - **Judge Tracks**: tích checkbox **AI Innovation**
   - **Judge Rounds**: tích checkbox **Qualification**
2. Click **"Save staff assignment"**.
3. Tạo thêm Judge thứ hai cho round **Final** theo cùng quy trình (Judge Rounds → tích **Final**).

### Backend xử lý gì
- Gắn user với event theo role judge, liên kết track và round.
- `AuthorizationService.canReadRanking()` kiểm tra judge chỉ được xem submission trong phạm vi được phân công.

### Kết quả mong đợi
- Judge hiển thị trong danh sách với role **Judge**, track **AI Innovation**, round **Qualification**.
- Khi Judge đăng nhập, chỉ thấy submission thuộc round và track được giao.

### Những lỗi dễ gặp khi demo
- Quên tích **Judge Rounds**: Judge thấy submission trong danh sách nhưng không được phép chấm → 403 Forbidden.
- Judge Qualification không chấm được Final và ngược lại — đây là thiết kế có chủ ý.

---

## BƯỚC 7 — Sinh viên FPT đăng ký tài khoản

### Mục đích
Minh hoạ luồng đăng ký cho sinh viên FPT University với MSSV và campus. Tài khoản tạo xong ở trạng thái **pending** — chưa thể đăng nhập cho đến khi coordinator duyệt.

### Điều kiện trước khi demo
- Không cần điều kiện trước. Thực hiện từ trình duyệt ẩn danh hoặc tài khoản khác.

### Cách mở màn hình
Truy cập: `/register`

### Thao tác demo
1. Chọn loại tài khoản **FPT Student** (mặc định khi vào trang).
2. Điền form:
   - **Full Name**: `Nguyễn Văn A`
   - **Email**: `vana.se180001@fpt.edu.vn`
   - **Student ID**: `SE180001`
   - **Campus**: chọn `FPT Hà Nội` từ dropdown
   - **Password**: tối thiểu 8 ký tự
   - **Confirm Password**: nhập lại mật khẩu
3. Tích vào checkbox **"I agree to the Terms and Conditions and Privacy Policy"**.
4. Click nút **"Register"**.

### Backend xử lý gì
- `POST /api/auth/register/fpt` → validate: email hợp lệ, password ≥ 8 ký tự, studentId và campusId không rỗng.
- Tài khoản được tạo với status **pending** — chưa thể đăng nhập.
- Hệ thống redirect sang trang **"Pending Approval"**.

### Kết quả mong đợi
- Trình duyệt chuyển sang trang thông báo "Chờ duyệt tài khoản".
- Tài khoản của `Nguyễn Văn A` xuất hiện trong hàng đợi duyệt của coordinator.

### Những lỗi dễ gặp khi demo
- Password < 8 ký tự: FE báo lỗi ngay khi submit.
- Email đã tồn tại: BE trả về lỗi conflict.
- Quên tích Terms & Conditions: nút Register không hoạt động.

---

## BƯỚC 8 — Sinh viên ngoài trường đăng ký tài khoản

### Mục đích
Minh hoạ luồng đăng ký cho sinh viên đến từ trường ngoài FPT (External). Không cần MSSV, thay bằng tên trường.

### Điều kiện trước khi demo
- Không cần điều kiện trước.

### Cách mở màn hình
Truy cập: `/register`

### Thao tác demo
1. Chuyển sang loại tài khoản **External Student** (tab hoặc radio button).
2. Điền form:
   - **Full Name**: `Trần Thị B`
   - **Email**: `b.external@bachkhoa.edu.vn`
   - **University Name**: `Đại học Bách Khoa Hà Nội`
   - **Password** / **Confirm Password**: tối thiểu 8 ký tự
3. Tích **"I agree to the Terms and Conditions and Privacy Policy"**.
4. Click **"Register"**.

### Backend xử lý gì
- `POST /api/auth/register/external` → không yêu cầu định dạng email trường cụ thể, chỉ cần email hợp lệ và universityName không rỗng.
- Tài khoản tạo với status **pending** — giống sinh viên FPT.

### Kết quả mong đợi
- Chuyển về trang "Pending Approval" như Bước 7.
- Tài khoản `Trần Thị B` (loại External) xuất hiện trong hàng đợi coordinator.

### Những lỗi dễ gặp khi demo
- **University Name để trống**: validation báo lỗi required.
- Nhìn vào cột "Type" trong UserApproval: FPT Student hiển thị MSSV + campus; External hiển thị tên trường.

---

## BƯỚC 9 — Ban tổ chức duyệt tài khoản

### Mục đích
Coordinator review và phê duyệt tài khoản đăng ký, kích hoạt quyền đăng nhập cho người dùng.

### Điều kiện trước khi demo
- Ít nhất một tài khoản đang ở trạng thái **pending** (Bước 7 và 8 hoàn thành).
- Đăng nhập bằng tài khoản coordinator.

### Cách mở màn hình
Truy cập: `/coordinator/users`

### Thao tác demo
1. Bảng hiển thị danh sách tài khoản chờ duyệt: Tên, Email, Loại (FPT Student / External), MSSV, Campus/Trường.
2. Click biểu tượng **mắt (Eye)** trên tài khoản `Nguyễn Văn A` để xem chi tiết.
3. Click nút **✓ (Approve)** để duyệt tài khoản FPT.
4. Click nút **✕ (Reject)** trên tài khoản cần từ chối → hộp thoại xác nhận "Reject this request?" → OK.

### Backend xử lý gì
- **Approve**: `POST /api/users/{id}/approve` → đổi status tài khoản `pending → active`. Người dùng có thể đăng nhập ngay.
- **Reject**: `POST /api/users/{id}/reject` → đánh dấu tài khoản bị từ chối.
- **Import hàng loạt**: nút **"Import"** (biểu tượng Upload) cho phép nhập file Excel để duyệt nhiều tài khoản cùng lúc.

### Kết quả mong đợi
- Tài khoản được duyệt biến mất khỏi hàng đợi.
- `Nguyễn Văn A` và `Trần Thị B` có thể đăng nhập và được chuyển đến dashboard tương ứng.

### Những lỗi dễ gặp khi demo
- Danh sách trống: tất cả đã được duyệt hoặc chưa có ai đăng ký → mở tab ẩn danh đăng ký thêm.

---

## BƯỚC 10 — Team Leader tạo Team

### Mục đích
Team Leader tạo team và đăng ký Track để tham gia sự kiện. Chỉ thực hiện được khi Event ở trạng thái **published**.

### Điều kiện trước khi demo
- Đăng nhập bằng tài khoản sinh viên đã được duyệt (Bước 9).
- Event **SEAL Summer 2026** đang ở trạng thái **published**.

### Cách mở màn hình
Truy cập: `/team/create-team`

### Thao tác demo
1. Chọn **Event**: `SEAL Summer 2026` (dropdown chỉ hiện event đang published).
2. Chọn **Track**: `AI Innovation` (tải sau khi chọn event; hiển thị capacity `0/∞`).
3. Điền **Team Name**: `ByteForce`
4. (Tuỳ chọn) Nhập email thành viên: thêm 1 email đồng đội vào ô mặc định; click **"+"** để thêm ô nữa (tối đa 4 ô, tương đương 4 người ngoài leader).
5. Đọc thể lệ sự kiện hiển thị bên dưới, tích **"I agree"**.
6. Click **"Create Team"**.

### Backend xử lý gì
- `POST /api/teams` → `TeamService.create()`.
- Validate: event đang mở đăng ký, track chưa đầy (teamCount < maxTeams), team ≤ 5 thành viên.
- Team leader tự động có role `team_leader`.
- Thành viên được mời qua email nhận invite code riêng.

### Kết quả mong đợi
- Màn hình hiển thị **invite code** của team (ví dụ: `BFRC2026`).
- Team `ByteForce` xuất hiện trong track AI Innovation với 1 thành viên.

### Những lỗi dễ gặp khi demo
- Track hiển thị "full" (chữ đỏ): đã đạt maxTeams. Chọn track khác hoặc coordinator tăng giới hạn.
- Quên tích thể lệ: nút "Create Team" bị disable.
- Không thấy event trong dropdown: event chưa ở trạng thái published — quay lại Bước 1 bấm "Open Registration".

---

## BƯỚC 11 — Thành viên tham gia Team

### Mục đích
Thành viên gia nhập team đã tạo bằng invite code. Mỗi team tối đa 5 người (1 leader + 4 thành viên).

### Điều kiện trước khi demo
- Có invite code của team `ByteForce` (lấy từ màn hình sau Bước 10).
- Đăng nhập bằng tài khoản sinh viên thứ hai đã được duyệt (Bước 9).

### Cách mở màn hình
Truy cập: `/team/join-team`

### Thao tác demo

**Cách 1 — Dùng invite code (khuyến nghị cho demo):**
1. Chọn tab **"Have an invite code?"** (mặc định).
2. Nhập invite code của team `ByteForce` vào ô.
3. Click **"Join"**.

**Cách 2 — Browse và gửi request:**
1. Chọn tab **"Browse"**.
2. Chọn Event từ dropdown để lọc.
3. Tìm team `ByteForce` trong ô tìm kiếm.
4. Click **"Request to Join"** → trạng thái chuyển sang "Pending".
5. Team Leader vào dashboard để approve request.

### Backend xử lý gì
- **Invite code**: `POST /api/teams/join` → kiểm tra code hợp lệ, team chưa đủ 5 thành viên → thêm ngay.
- **Browse**: `POST /api/join-requests/{teamId}` → tạo join request; Team Leader phải approve.

### Kết quả mong đợi
- (Invite code) Thành viên được thêm vào team ngay lập tức; trình duyệt chuyển về dashboard.
- (Browse) Join request xuất hiện trong dashboard của Team Leader để phê duyệt.

### Những lỗi dễ gặp khi demo
- Invite code sai hoặc hết hạn: báo lỗi "Invalid invite code".
- Team đã đủ 5 thành viên: không thể join bằng cách nào cả.

---

## BƯỚC 12 — Team xem thông tin Track đã đăng ký

### Mục đích
Team xem chủ đề, yêu cầu nộp bài và tiêu chí chấm điểm của Track mình tham gia để chuẩn bị dự án.

### Điều kiện trước khi demo
- Team đã được gắn với Track AI Innovation (Bước 10 hoàn thành).
- Đăng nhập bằng tài khoản thành viên team.

### Cách mở màn hình
Truy cập: `/team/topic`

### Thao tác demo
1. Màn hình tự động load thông tin Track của team hiện tại:
   - Tên Track: **AI Innovation**
   - Mô tả Track
   - **Project Requirements** (yêu cầu nộp bài): Repository link, Demo URL, Slide/Report
   - **Tiêu chí chấm điểm** của Round đầu tiên (Qualification):
     - Technical Implementation — 40%
     - Innovation & Creativity — 35%
     - Presentation — 25%
2. Giải thích ý nghĩa từng tiêu chí cho team để định hướng chuẩn bị.

### Backend xử lý gì
- `GET /api/my-teams` → lấy trackId.
- `GET /api/tracks/{trackId}` → chi tiết track.
- `GET /api/rounds?trackId=...` → lấy round đầu tiên (sequenceNumber nhỏ nhất).
- `GET /api/criteria?roundId=...` → danh sách tiêu chí của round Qualification.

### Kết quả mong đợi
- Hiển thị đầy đủ thông tin Track và 3 tiêu chí với trọng số tương ứng.

### Những lỗi dễ gặp khi demo
- "No track assigned to your team yet": team chưa chọn track khi tạo hoặc mới tham gia qua invite code trước khi coordinator gắn track.

---

## BƯỚC 13 — Team nộp bài vòng Qualification

### Mục đích
Team nộp link dự án (GitHub, demo, slide, report) vào hệ thống trước deadline. Thời điểm nộp được ghi lại để phục vụ tie-breaker ranking.

### Điều kiện trước khi demo
- Team có ít nhất 1 thành viên, đã đăng ký Track.
- Round Qualification còn trong hạn nộp bài (deadline chưa qua).
- Đăng nhập bằng tài khoản thành viên team.

### Cách mở màn hình
Truy cập: `/team/submissions`

### Thao tác demo
1. Màn hình hiển thị thông tin: Team, Track, Round hiện tại (`Qualification`), Deadline.
2. Điền form:
   - **GitHub Repository URL**: `https://github.com/byteforce/seal2026`
   - **Demo URL**: `https://byteforce.demo.app`
   - **Slides URL**: (để trống hoặc điền link Google Slides)
   - **Report URL**: (để trống hoặc điền link Google Docs)
   - **Project Notes / Metadata**: ghi chú thêm (tuỳ chọn)
3. Click **"Save Draft"** để lưu nháp và tiếp tục chỉnh sửa sau.
4. Khi đã sẵn sàng, click **"Submit"** để nộp chính thức.

### Backend xử lý gì
- `POST /api/submissions` (upsert) → `SubmissionService`.
- **Validation mới**: phải có ít nhất **một URL** trong số repoUrl, demoUrl, slideUrl, reportUrl — không thể submit form trống.
- `submittedAt` được ghi lại tự động → dùng để **tie-breaker ranking** khi 2 team có điểm bằng nhau trên mọi tiêu chí.
- Unique constraint `uq_submissions_round_team`: mỗi team chỉ có **1 submission** mỗi round — lần submit sau sẽ update bài cũ.

### Kết quả mong đợi
- Status bài nộp chuyển từ `Draft` → `Submitted`.
- Bài nộp xuất hiện trong danh sách của Judge để chấm điểm.

### Những lỗi dễ gặp khi demo
- **"At least one URL is required"**: tất cả trường URL để trống. Nhập ít nhất GitHub Repository URL.
- Deadline đã qua: hệ thống không cho Submit (chỉ xem bài đã nộp).
- Submit nhiều lần: bài cũ được cập nhật, không tạo submission mới; `submittedAt` của lần submit ĐẦU TIÊN được giữ nguyên — quan trọng cho tie-breaker.

---

## BƯỚC 14 — Judge chấm điểm vòng Qualification

### Mục đích
Judge chấm điểm từng tiêu chí cho bài nộp của team. Điểm có trọng số được tính tự động.

### Điều kiện trước khi demo
- Team `ByteForce` đã nộp bài (Bước 13 hoàn thành).
- Đăng nhập bằng tài khoản **Judge Qualification** đã được phân công (Bước 6).

### Cách mở màn hình
1. Vào `/judge/submissions` → thấy danh sách bài nộp trong phạm vi round/track được giao.
2. Click **"Score"** (hoặc **"Evaluate"**) trên bài của team `ByteForce` → chuyển đến `/judge/score/:id`.

### Thao tác demo
1. Xem thông tin bài nộp: tên team, round, các link tài liệu (Repository, Live Demo, Slides, Report).
2. Nhập điểm cho từng tiêu chí:
   - **Technical Implementation**: `85`
   - **Innovation & Creativity**: `90`
   - **Presentation**: `80`
3. Nhập **Comment** (nhận xét chung): `Giải pháp kỹ thuật tốt, ý tưởng sáng tạo. Cần cải thiện phần trình bày.`
4. Click **"Submit Evaluation"**.

### Backend xử lý gì
- `POST /api/scores` (upsert) → lưu score cho từng criterion.
- `weightedScore` được tính tự động:
  - Technical: 85 × 40% = **34**
  - Innovation: 90 × 35% = **31.5**
  - Presentation: 80 × 25% = **20**
  - **Tổng điểm có trọng số: 85.5**
- Sau khi submit, Judge được chuyển về `/judge/submissions`.

### Kết quả mong đợi
- Điểm lưu thành công cho team `ByteForce`.
- Bài nộp hiển thị điểm trung bình trong giao diện Judge.

### Những lỗi dễ gặp khi demo
- Judge chấm bài ngoài round/track phụ trách: 403 Forbidden.
- Điểm mặc định là **0** cho mọi tiêu chí — nhớ nhập điểm trước khi Submit.
- Có thể chấm lại (upsert): điểm mới sẽ ghi đè điểm cũ.

---

## BƯỚC 15 — Hệ thống tính Ranking vòng Qualification

### Mục đích
Coordinator kích hoạt tính toán xếp hạng dựa trên điểm đã chấm. Minh hoạ logic tie-breaker theo submission time — tính năng mới bổ sung trong sprint này.

### Điều kiện trước khi demo
- Ít nhất 2 team đã nộp bài và được Judge chấm điểm.
- Tổng weight tiêu chí = 100%.
- Đăng nhập bằng tài khoản **coordinator**.

### Cách mở màn hình
Truy cập: `/coordinator/ranking`

### Thao tác demo
1. **EventSelector** ở đầu trang: chọn `SEAL Summer 2026`.
2. Dropdown **Round**: chọn `Qualification`.
3. Dropdown **Track**: chọn `AI Innovation` (hoặc để "All Tracks").
4. Click **"Recalculate Rankings"** (nút màu xanh lá, biểu tượng RefreshCw).
5. Xem bảng ranking kết quả.

### Backend xử lý gì
- `POST /api/rankings/recalculate?roundId=...` → `RoundRankingService.recalculate()`.
- **Logic xếp hạng (áp dụng tuần tự):**
  1. **Tổng điểm có trọng số** — cao hơn xếp trước (logic gốc, không thay đổi).
  2. **Nếu bằng tổng điểm** → so điểm từng tiêu chí theo trọng số giảm dần — ưu tiên tiêu chí quan trọng nhất (logic gốc, không thay đổi).
  3. **Nếu mọi tiêu chí đều bằng nhau** → đội nộp bài **sớm hơn** xếp trên **(mới bổ sung)**.
  4. Tên team A–Z để kết quả luôn xác định.
- Validate: tổng weight = 100, nếu không → lỗi.
- Kết quả lưu vào bảng `round_ranking` với trường `tieBreakerReason` mô tả lý do phân hạng.

### Kết quả mong đợi
- Bảng ranking hiển thị: Rank, Team Name, Track, Final Score, Advancement Status.
- Cột Status = **Pending** (chưa apply promotion).
- Nếu 2 team bằng điểm và bằng mọi tiêu chí: team nộp trước xếp trên, `tieBreakerReason` ghi `"Tie on total score and all criteria equal; resolved by earliest submission time"`.

### Những lỗi dễ gặp khi demo
- **"Criterion weights must sum to 100 before recalculating rankings (current sum: X)"**: tổng weight sai. Vào `/coordinator/criteria` để sửa.
- Ranking trống: chưa team nào nộp bài hoặc chưa judge nào chấm.
- Không chọn Event trước: dropdown Round trống → không có gì để tính.

---

## BƯỚC 16 — Disqualify một Team vi phạm

### Mục đích
Coordinator loại một team vi phạm quy định. Team bị disqualify không thể nhận giải thưởng và không được promote dù điểm cao.

### Điều kiện trước khi demo
- Có ít nhất 2 team trong Track (để sau khi disqualify vẫn còn team khác để demo promotion).

### Cách mở màn hình
1. Vào `/coordinator/events/:id` → tab **"Participating Teams"**.
2. Click **"View"** trên team cần disqualify → chuyển đến `/coordinator/teams/:id`.

### Thao tác demo
1. Trên trang Team Detail, click nút **"Disqualify"**.
2. Hộp thoại `window.prompt` hiện ra: **"Enter disqualification reason:"**.
3. Nhập lý do: `Vi phạm quy chế: sử dụng code từ nguồn không được phép.`
4. Click **OK**.

### Backend xử lý gì
- `PATCH /api/teams/{id}/disqualify` → `TeamService.disqualify()`.
- Đổi `teamStatus → disqualified`, ghi nhận lý do.
- **Validation mới (MISS-10 & MISS-11):** Nếu sau này coordinator cố trao giải hoặc đổi tên giải cho team này qua PATCH `/api/prizes/{id}`:
  - `PrizeService.update()` kiểm tra `team.status == disqualified` → ném **"Cannot award a prize to a disqualified team"**.
  - Kiểm tra trùng tên giải trong cùng team-event → ném **"A prize with this name has already been awarded to this team"**.
  - Trước đây 2 validation này chỉ có trong `create()`, bỏ sót `update()` — đã được sửa.

### Kết quả mong đợi
- Team hiển thị badge **Disqualified** (đỏ).
- Nút **"Reactivate"** xuất hiện — coordinator có thể khôi phục nếu nhầm.
- Khi tính ranking lại, team disqualified vẫn xuất hiện trong bảng nhưng với status riêng.

### Những lỗi dễ gặp khi demo
- Bấm **Cancel** trong hộp thoại prompt: hành động bị huỷ hoàn toàn, team không bị disqualify.
- Nhầm team: dùng nút "Reactivate" để khôi phục ngay.

---

## BƯỚC 17 — Promote Team vào Final Round

### Mục đích
Công bố kết quả Qualification và thăng hạng `topNToPromote = 3` team đứng đầu vào Final Round.

### Điều kiện trước khi demo
- Ranking Qualification đã được tính (Bước 15).
- Round **Final** đã tạo (Bước 3).

### Cách mở màn hình
Truy cập: `/coordinator/ranking`

### Thao tác demo
1. Chọn Event `SEAL Summer 2026`, Round **Qualification**, Track **AI Innovation**.
2. Click **"Recalculate Rankings"** → bảng hiển thị ngay lập tức:
   - Top 3 team (rank ≤ topNToPromote): badge **Promoted** màu xanh.
   - Các team còn lại: badge **Eliminated** màu đỏ.
3. Click **"Publish Results"** (biểu tượng Send) → hộp thoại xác nhận → click **OK**.
   - Thông báo xanh: `"Results published. Wait a moment then click Advance Round to promote teams."`
4. Chờ vài giây → nút **"Advance Round"** (biểu tượng mũi tên) xuất hiện trên toolbar.
5. Click **"Advance Round"** → xác nhận → click **OK**.
   - Thông báo: `"Round advanced. Promoted teams are now eligible for the next round."`

### Backend xử lý gì
- `POST /round-rankings/rounds/{roundId}/recalculate` với `applyPromotion: true` → gán Promoted/Eliminated ngay khi tính.
- `POST /events/{eventId}/rounds/{roundId}/publish-results` → snapshot kết quả, mở cửa sổ appeal.
- `POST /events/{eventId}/rounds/{roundId}/advance` → seed các team **Promoted** từ published version vào Round Final.
- Team disqualified không được promote dù điểm cao.

### Kết quả mong đợi
- Top 3 team có status **Promoted** (badge xanh).
- Các team còn lại có status **Eliminated** (badge đỏ).
- Promoted teams được seed vào Round Final — họ có thể nộp bài cho vòng Final.

### Những lỗi dễ gặp khi demo
- Click **"Advance Round"** ngay lập tức sau **"Publish Results"** trước khi hết appeal window → lỗi "Round is not ready to advance". Chờ vài giây rồi thử lại.
- Quên bấm **"Recalculate Rankings"** trước **"Publish Results"**: publish snapshot rỗng.
- Team Disqualified (Bước 16) không xuất hiện trong danh sách promoted dù điểm cao — đây là hành vi đúng.

---

## BƯỚC 18 — Judge chấm điểm Final Round

### Mục đích
Judge được phân công cho Final Round chấm điểm các team đã được promoted từ Qualification.

### Điều kiện trước khi demo
- Judge Final đã được phân công cho round **Final** (Bước 6).
- 3 team promoted đã nộp bài cho Round Final.
- Đăng nhập bằng tài khoản **Judge Final**.

### Cách mở màn hình
`/judge/submissions` → click **"Score"** trên bài nộp Final Round → `/judge/score/:id`

### Thao tác demo
Tương tự Bước 14 nhưng cho round **Final**:
1. Chấm điểm từng tiêu chí cho team đứng nhất (ví dụ: team `AlphaCode`):
   - **Technical Implementation**: `92`
   - **Innovation & Creativity**: `88`
   - **Presentation**: `95`
2. Nhập comment và click **"Submit Evaluation"**.
3. Thực hiện tương tự cho các team còn lại trong Final.

### Backend xử lý gì
- Giống Bước 14. `weightedScore` tính theo tiêu chí của Final Round.
- Judge Final không thể xem/chấm submission của Qualification (403) — chứng minh phân quyền theo round hoạt động đúng.

### Kết quả mong đợi
- Điểm Final được lưu cho các team promoted.
- Sẵn sàng cho bước tính ranking cuối cùng.

### Những lỗi dễ gặp khi demo
- Judge Qualification cố truy cập bài Final: 403 Forbidden — đây là hành vi đúng của authorization.
- Team promoted chưa nộp bài Final: submission không xuất hiện trong danh sách Judge — nhắc team nộp bài trước.

---

## BƯỚC 19 — Tính Ranking cuối cùng (Final Round)

### Mục đích
Xác định kết quả chung cuộc và team vô địch SEAL Summer 2026. Minh hoạ tie-breaker theo submission time khi 2 team bằng điểm tuyệt đối.

### Điều kiện trước khi demo
- Judge đã chấm điểm Final (Bước 18 hoàn thành).
- Tiêu chí Final Round có tổng weight = 100%.

### Cách mở màn hình
Truy cập: `/coordinator/ranking`

### Thao tác demo
1. Chọn Event, Round **Final**, Track **AI Innovation**.
2. Click **"Recalculate Rankings"**.
3. Xem bảng kết quả cuối cùng:
   - **Rank 1**: Team vô địch
   - Cột **Tie Breaker Reason**: giải thích rõ lý do phân hạng khi bằng điểm
4. Click **"Publish Results"** → xác nhận → kết quả được công bố.

### Backend xử lý gì
- Logic ranking giống Bước 15 nhưng cho Final Round.
- **Kịch bản tie-breaker cho demo:** Nếu 2 team có điểm bằng nhau trên mọi tiêu chí:
  - `TieBreaker.decisiveBetween(teamA, teamB)` so sánh từng tiêu chí → trả về `null` (mọi tiêu chí đều bằng).
  - Hệ thống so `earliestSubmittedAt`: team nộp bài sớm hơn xếp trên.
  - `tieBreakerReason`: `"Tie on total score and all criteria equal; resolved by earliest submission time"`
  - **Phân biệt với trường hợp thông thường**: nếu tìm được tiêu chí phân biệt, `tieBreakerReason` sẽ ghi: `"Tie on total score broken by highest-weight criterion 'Technical Implementation'"`.

### Kết quả mong đợi
- Champion rõ ràng ở Rank 1 với tổng điểm cao nhất.
- Kết quả published → team thấy kết quả cuối cùng trong dashboard.

### Những lỗi dễ gặp khi demo
- Ranking trống: không có team nào nộp bài Final hoặc chưa judge nào chấm.
- Để demo tie-breaker submission time: cần 2 team có **điểm y hệt nhau** — chuẩn bị trước bằng cách đặt Judge chấm 2 team với điểm giống nhau hoàn toàn và để team A nộp bài trước team B.

---

## BƯỚC 20 — Export Report

### Mục đích
Xuất dữ liệu kết quả ra file CSV và xem báo cáo phân tích để lưu trữ, gửi ban giám khảo hoặc phân tích chuyên sâu.

### Điều kiện trước khi demo
- Ranking Final đã được tính và published (Bước 19).
- Đăng nhập bằng tài khoản **coordinator**.

### Cách mở màn hình
Truy cập: `/coordinator/reports`

### Thao tác demo
1. Chọn **Round** từ dropdown: `Final`.
2. **Báo cáo 1 — Ranking Export (CSV):**
   - Click **"Download Ranking CSV"**.
   - File CSV tải về máy với cột: Rank, Team Name, Track, Total Score, Status.
   - Mở file để xem kết quả cuối cùng.
3. **Báo cáo 2 — Judge Variance Report:**
   - Click **"View"** bên cạnh "Judge Variance Report".
   - JSON hiển thị phương sai điểm giữa các Judge trên cùng một submission — giúp phát hiện Judge chấm lệch.
4. **Báo cáo 3 — Anonymized Dataset:**
   - Click **"View"** bên cạnh "Anonymized Dataset".
   - JSON chứa dữ liệu điểm đã ẩn danh (không có tên team/judge) — dùng cho nghiên cứu hoặc audit.

### Backend xử lý gì
- **CSV**: `GET /api/reports/ranking-csv?roundId=...` → stream file CSV về client.
- **Judge Variance**: `GET /api/reports/judge-variance?roundId=...` → tính độ lệch chuẩn giữa các judge.
- **Anonymized**: `GET /api/reports/anonymized?roundId=...` → mask tên, trả về điểm raw.

### Kết quả mong đợi
- File `ranking.csv` được tải về với đầy đủ thông tin xếp hạng Final.
- JSON Variance Report hiển thị được trong trình duyệt.
- JSON Anonymized Dataset hiển thị được trong trình duyệt.

### Những lỗi dễ gặp khi demo
- CSV trống: chưa có ranking hoặc chưa publish kết quả.
- **Quên chọn Round**: API gọi không có roundId → báo lỗi validation.
- Báo cáo mất thời gian tải nếu có nhiều dữ liệu — bình thường.

---

## Phụ lục A — Tài khoản demo tham khảo

| Role | Email mẫu | Ghi chú |
|------|-----------|---------|
| Coordinator | coordinator@seal.fpt.edu.vn | Quản lý toàn bộ hệ thống |
| Judge Qualification | judge.qual@seal.fpt.edu.vn | Chỉ chấm Round Qualification, Track AI Innovation |
| Judge Final | judge.final@seal.fpt.edu.vn | Chỉ chấm Round Final, Track AI Innovation |
| Mentor | mentor@seal.fpt.edu.vn | Hỗ trợ team trong Track AI Innovation |
| Team Leader | leader@fpt.edu.vn | Tạo team ByteForce |
| Team Member (FPT) | member.fpt@fpt.edu.vn | Tham gia qua invite code |
| Team Member (External) | member.ext@bachkhoa.edu.vn | Đăng ký dạng External Student |

---

## Phụ lục B — Validation quan trọng và lỗi hay gặp

| Tình huống | Lỗi hiển thị | Nguyên nhân & Ghi chú |
|-----------|-------------|----------------------|
| Tạo event trùng ngày | "Event dates overlap with an existing event" | **Rule mới**: 2 event không được có khoảng thời gian chồng chéo |
| Tính ranking chưa đủ weight | "Criterion weights must sum to 100 before recalculating rankings (current sum: X)" | **Bug MISS-03 đã sửa**: trước đây so với `1.0` thay vì `100` nên không bao giờ báo lỗi |
| Trao giải team bị disqualify (qua PATCH) | "Cannot award a prize to a disqualified team" | **Bug MISS-10 đã sửa**: check này trước đây chỉ có ở `create()`, bỏ sót `update()` |
| Trao giải trùng tên (qua PATCH) | "A prize with this name has already been awarded to this team in this event" | **Bug MISS-11 đã sửa**: duplicate check cũng bỏ sót `update()` |
| Nộp bài không có URL | "At least one URL is required" | **Validation mới**: submission phải có ít nhất 1 URL |
| topNToPromote quá lớn | "must be less than or equal to 500" | **Validation mới**: `@Max(500)` để ngăn dữ liệu bất thường |
| Nộp bài sau deadline | Không thể submit (deadline passed) | Hệ thống tự block; chỉ xem được bài đã nộp |
| Judge chấm ngoài phạm vi | 403 Forbidden | Authorization kiểm tra round và track được phân công |

---

## Phụ lục C — Luồng dữ liệu tie-breaker (cho phần kỹ thuật)

```
Khi tính Ranking, thứ tự so sánh:

1. Tổng điểm có trọng số (totalScore) — DESC
   │
   ├─ Nếu KHÁC NHAU → xếp hạng bình thường, ghi tieBreakerReason:
   │   "Ranked by total weighted score; team name used for deterministic ordering on ties"
   │
   └─ Nếu BẰNG NHAU → so từng tiêu chí (theo trọng số giảm dần)
       │
       ├─ Tìm được tiêu chí khác nhau → xếp theo tiêu chí đó, ghi:
       │   "Tie on total score broken by highest-weight criterion 'Technical Implementation'"
       │
       └─ MỌI tiêu chí đều bằng nhau → so thời gian nộp bài (earliestSubmittedAt) — ASC
           │
           └─ Team nộp sớm hơn xếp trên, ghi:
               "Tie on total score and all criteria equal; resolved by earliest submission time"
```

**Lưu ý kỹ thuật**: `earliestSubmittedAt` lấy từ `MIN(sub.submittedAt)` trong query `calculateRows`. Vì unique constraint `uq_submissions_round_team` đảm bảo mỗi team chỉ có 1 submission mỗi round, hàm MIN luôn trả về đúng 1 giá trị.
