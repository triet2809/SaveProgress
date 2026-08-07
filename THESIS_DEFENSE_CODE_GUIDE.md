# TÀI LIỆU CHUẨN BỊ BẢO VỆ ĐỒ ÁN — SEAL Hackathon Management System

> **Mục tiêu:** Đọc tài liệu này trước bảo vệ để có thể mở đúng file, nhìn vào đúng function và giải thích tự tin với hội đồng từng dòng code đã đóng góp trên branch `0108`.
>
> **Tác giả:** HoangND (`itzhoang@gmail.com`)  
> **Branch:** `0108` — tất cả commit phân tích đều do HoangND thực hiện từ 2026-08-04 đến 2026-08-05.

---

## Mục lục

1. [Tổng quan các commit đã thực hiện](#1-tổng-quan-các-commit-đã-thực-hiện)
2. [MISS-01 — Validate thứ tự ngày Event](#2-miss-01--validate-thứ-tự-ngày-event)
3. [Event Overlap — Hai Event không được trùng thời gian](#3-event-overlap--hai-event-không-được-trùng-thời-gian)
4. [Validate Submission Deadline nằm trong cửa sổ Event](#4-validate-submission-deadline-nằm-trong-cửa-sổ-event)
5. [Validate Trọng số Tiêu chí > 0](#5-validate-trọng-số-tiêu-chí--0)
6. [Validate Tổng Trọng số = 100 trước khi Ranking (bao gồm MISS-03)](#6-validate-tổng-trọng-số--100-trước-khi-ranking-bao-gồm-miss-03)
7. [Validate Submission phải có ít nhất một URL](#7-validate-submission-phải-có-ít-nhất-một-url)
8. [@Max(500) cho topNToPromote](#8-max500-cho-topntopromote)
9. [MISS-10 — Không trao giải cho Team bị Disqualify](#9-miss-10--không-trao-giải-cho-team-bị-disqualify)
10. [MISS-11 — Không trùng tên giải thưởng trong cùng Team/Event](#10-miss-11--không-trùng-tên-giải-thưởng-trong-cùng-teamevent)
11. [Fix MISS-10 & MISS-11 — Mở rộng check sang update()](#11-fix-miss-10--miss-11--mở-rộng-check-sang-update)
12. [Tie-breaker — Thời gian nộp bài phá hòa khi điểm bằng nhau tuyệt đối](#12-tie-breaker--thời-gian-nộp-bài-phá-hòa-khi-điểm-bằng-nhau-tuyệt-đối)
13. [Code Walkthrough — Từng file và từng function](#13-code-walkthrough--từng-file-và-từng-function)

---

## 1. Tổng quan các commit đã thực hiện

| # | Hash | Ngày | Commit Message | Files thay đổi |
|---|------|------|----------------|----------------|
| 1 | `6f0ae75` | 2026-08-04 | feat(validation): validate event date ordering (MISS-01) | `EventService.java` |
| 2 | `daeb36f` | 2026-08-04 | feat(validation): validate submission deadline within event window | `RoundService.java` |
| 3 | `eeadea9` | 2026-08-04 | feat(validation): require criterion weight > 0 | `CreateRoundCriterionRequest.java`, `UpdateRoundCriterionRequest.java` |
| 4 | `6804e64` | 2026-08-04 | feat(validation): enforce criterion weight sum = 1.0 before ranking | `RoundCriterionRepository.java`, `RoundRankingService.java` |
| 5 | `82c8cce` | 2026-08-04 | feat(validation): require at least one URL in submission | `SubmissionService.java` |
| 6 | `d55c483` | 2026-08-04 | feat(validation): add upper bound @Max(500) on topNToPromote | `CreateRoundRequest.java`, `CreateLogicalRoundRequest.java` |
| 7 | `27d4afd` | 2026-08-04 | feat(validation): block prize award for disqualified teams | `PrizeService.java` |
| 8 | `35b9549` | 2026-08-04 | feat(validation): prevent duplicate prize name per team per event | `PrizeRepository.java`, `PrizeService.java` |
| 9 | `d4dd123` | 2026-08-05 | fix(validation): correct criterion weight sum target to 100 | `RoundRankingService.java` |
| 10 | `be3e902` | 2026-08-05 | fix(validation): enforce disqualified+duplicate prize checks on update | `PrizeRepository.java`, `PrizeService.java` |
| 11 | `7b5d127` | 2026-08-05 | feat(validation): reject events whose dates overlap an existing event | `EventRepository.java`, `EventService.java` |
| 12 | `a747533` | 2026-08-05 | feat(ranking): replace criterion-weight tie-breaker *(đã revert)* | — |
| 13 | `9f6c220` | 2026-08-05 | Revert commit a747533 *(revert commit 12)* | — |
| 14 | `743eee1` | 2026-08-05 | feat(ranking): use submission time as final tie-breaker | `RoundRankingRepository.java`, `RoundRankingService.java` |
| 15 | `9216268` | 2026-08-05 | docs(ranking): add Vietnamese comments | `RoundRankingService.java` |

> **Ghi chú commit 12–13:** Lần đầu implement tie-breaker, nhóm đã nhận ra cách tiếp cận thay thế hoàn toàn criterion là sai. Đã revert và re-implement đúng (commit 14): thời gian nộp bài chỉ là tie-breaker **cuối cùng** khi mọi tiêu chí đều bằng nhau.

---

## 2. MISS-01 — Validate thứ tự ngày Event

**Commit:** `6f0ae75` | **File:** `EventService.java`  
**Method:** `validateEventDates()`

### Chức năng

Kiểm tra thứ tự logic giữa các mốc thời gian khi tạo hoặc cập nhật Event: ngày đăng ký phải hợp lệ và phải xảy ra trước ngày thi chính thức.

### Bài toán

Trước khi sửa, hệ thống không kiểm tra thứ tự ngày tháng. Coordinator có thể tạo event với `registrationEnd` sau `eventStart`, tức là vẫn còn nhận đăng ký khi cuộc thi đã bắt đầu — tạo ra dữ liệu vô nghĩa.

### Giải pháp

Thêm method `validateEventDates()` vào `EventService`, gọi trong cả `create()` và `update()` trước khi lưu vào DB.

```java
// EventService.java — lines 745–759
private void validateEventDates(LocalDateTime registrationStart, LocalDateTime registrationEnd,
                                LocalDateTime eventStart, LocalDateTime eventEnd) {
    if (registrationStart != null && registrationEnd != null
            && !registrationStart.isBefore(registrationEnd)) {
        throw ApiException.badRequest("registrationStart must be before registrationEnd");
    }
    if (registrationEnd != null && eventStart != null
            && registrationEnd.isAfter(eventStart)) {
        throw ApiException.badRequest("registrationEnd must not be after eventStart");
    }
    if (eventStart != null && eventEnd != null
            && !eventStart.isBefore(eventEnd)) {
        throw ApiException.badRequest("eventStart must be before eventEnd");
    }
}
```

### Cách hoạt động

- **Khi nào được gọi:** `create()` (line 169) và `update()` (line 196) đều gọi method này trước khi save.
- **Input:** 4 mốc thời gian (đều nullable vì là partial update).
- **Điều kiện 1:** `registrationStart < registrationEnd` — thời gian mở đăng ký phải trước đóng đăng ký.
- **Điều kiện 2:** `registrationEnd ≤ eventStart` — ngày đóng đăng ký không được sau ngày bắt đầu thi.
- **Điều kiện 3:** `eventStart < eventEnd` — ngày bắt đầu phải trước ngày kết thúc.
- Nếu field nào `null` thì bỏ qua điều kiện đó (partial update an toàn).
- **Exception:** `ApiException.badRequest()` trả về HTTP 400 với message cụ thể.

### Vì sao làm như vậy

- Validate ở **Service** thay vì Controller vì Controller chỉ deserialize JSON và gọi `@Valid` cho format. Logic nghiệp vụ (thứ tự ngày) thuộc về Service.
- Validate ở **Service** thay vì **DB** (constraint) vì DB không biết context — nó không thể trả về message thân thiện như `"registrationEnd must not be after eventStart"`.
- Không validate ở DTO với annotation vì liên quan đến so sánh **nhiều field với nhau** (cross-field validation), annotation không làm được điều này dễ dàng.

### Lợi ích

- Ngăn dữ liệu logic sai ngay từ đầu.
- Error message rõ ràng, coordinator biết sửa trường nào.

### Những câu hỏi hội đồng có thể hỏi

1. **Tại sao validate ở Service thay vì Database constraint?**  
   → DB constraint chỉ check giá trị đơn lẻ, không so sánh được 2 cột với nhau (cross-column). Dùng DB trigger thì phức tạp và khó test.

2. **Tại sao không dùng @AssertTrue trong DTO?**  
   → `@AssertTrue` trong DTO làm tăng coupling giữa DTO và business logic. Nếu logic đổi, phải sửa DTO thay vì Service.

3. **Điều gì xảy ra nếu chỉ truyền eventStart mà không có eventEnd?**  
   → Điều kiện 3 check `if (eventStart != null && eventEnd != null)` — nếu một trong hai null thì skip, không ném lỗi.

4. **Vì sao dùng `isBefore()` thay vì `!isAfter()`?**  
   → `isBefore()` loại trừ bằng nhau; `isBefore()` nghĩa là `<` còn `!isAfter()` nghĩa là `≤`. Yêu cầu là *strictly before*, nên `isBefore()` là đúng.

5. **Nếu bỏ validate này thì chuyện gì xảy ra?**  
   → Coordinator có thể tạo event mà deadline đăng ký sau ngày thi, làm vòng thi không có team nào, hoặc team đăng ký sau khi thi xong.

### Câu trả lời mẫu

> "Em thêm method `validateEventDates()` vào EventService để kiểm tra 3 ràng buộc thứ tự giữa các mốc thời gian của event. Em đặt ở Service vì đây là business logic — Controller chỉ deserialize JSON, còn DB constraint không thể so sánh hai cột với nhau. Mỗi điều kiện em đều null-check trước để partial update không bị lỗi khi coordinator chỉ cập nhật 1 trường."

---

## 3. Event Overlap — Hai Event không được trùng thời gian

**Commit:** `7b5d127` | **Files:** `EventRepository.java`, `EventService.java`  
**Methods:** `existsByEventStart...`, `validateNoOverlap()`

### Chức năng

Đảm bảo không có hai Event nào chạy song song (thời gian chồng chéo nhau). Hệ thống từ chối tạo hoặc cập nhật Event nếu khoảng thời gian của nó giao nhau với Event đã tồn tại.

### Bài toán

Trước khi sửa, coordinator có thể tạo hai event có cùng thời gian. Điều này gây mâu thuẫn tài nguyên: cùng lúc có hai cuộc thi, team không biết tham gia cái nào, judge bị phân tán.

### Giải pháp

Thêm 2 derived method vào `EventRepository` và 1 helper `validateNoOverlap()` vào `EventService`.

```java
// EventRepository.java
boolean existsByEventStartLessThanEqualAndEventEndGreaterThanEqual(
        LocalDateTime end, LocalDateTime start);

boolean existsByEventStartLessThanEqualAndEventEndGreaterThanEqualAndIdNot(
        LocalDateTime end, LocalDateTime start, UUID excludeId);
```

```java
// EventService.java — lines 737–743
private void validateNoOverlap(LocalDateTime start, LocalDateTime end, UUID excludeId) {
    if (start == null || end == null) return;
    boolean overlaps = excludeId == null
            ? eventRepository.existsByEventStartLessThanEqualAndEventEndGreaterThanEqual(end, start)
            : eventRepository.existsByEventStartLessThanEqualAndEventEndGreaterThanEqualAndIdNot(end, start, excludeId);
    if (overlaps) throw ApiException.conflict("Event dates overlap with an existing event");
}
```

### Cách hoạt động

**Điều kiện overlap (interval intersection):**
```
existing.eventStart <= new.eventEnd   AND   existing.eventEnd >= new.eventStart
```

Đây là công thức chuẩn để kiểm tra 2 khoảng thời gian có giao nhau không. Nếu **cả hai điều kiện đều đúng** thì hai event chồng chéo.

**Spring Data JPA tự sinh SQL từ tên method:**
- `existsByEventStartLessThanEqual(end)` → `WHERE event_start <= ?`
- `AndEventEndGreaterThanEqual(start)` → `AND event_end >= ?`
- `AndIdNot(excludeId)` → `AND id != ?` (dùng khi update để loại event hiện tại ra)

**Luồng gọi:**
- `create()` → `validateNoOverlap(req.eventStart(), req.eventEnd(), null)` — excludeId = null vì event chưa tồn tại
- `update()` → `validateNoOverlap(..., e.getId())` — truyền id event hiện tại để không tự chặn chính mình

### Vì sao làm như vậy

- Dùng **Spring Data JPA derived method** thay vì viết `@Query` thủ công vì tên method tự mô tả ý nghĩa, ít code hơn, và Spring Data đã test kỹ.
- Tách thành **helper method** thay vì inline trong `create()` và `update()` để tránh duplicate logic. Cả hai nơi chỉ cần gọi 1 method.
- Dùng **`ApiException.conflict()`** (HTTP 409) thay vì 400 vì đây là xung đột tài nguyên, không phải input sai format.

### Lợi ích

- Hệ thống luôn đảm bảo chỉ có tối đa một event chạy tại bất kỳ thời điểm nào.
- Logic overlap không bị duplicate giữa create và update.
- SQL do Spring Data sinh ra hiệu quả, có index hỗ trợ.

### Những câu hỏi hội đồng có thể hỏi

1. **Tại sao truyền `end` vào tham số đầu và `start` vào tham số hai trong method Repository?**  
   → Spring Data JPA đọc tên method theo thứ tự: `existsByEventStartLessThanEqual(end)` nghĩa là `event_start <= end`, và `AndEventEndGreaterThanEqual(start)` nghĩa là `event_end >= start`. Thứ tự tham số phải khớp thứ tự điều kiện trong tên method.

2. **Tại sao overlap điều kiện là `start ≤ end AND end ≥ start` chứ không phải đơn giản hơn?**  
   → Đây là toán học khoảng thời gian. Hai khoảng [A, B] và [C, D] *không* giao nhau khi `B < C` hoặc `D < A`. Phủ định lại: chúng giao khi `B >= C AND D >= A`, tức `existing.start <= new.end AND existing.end >= new.start`.

3. **Tại sao cần version `AndIdNot` riêng khi update?**  
   → Khi update event hiện có, nếu không exclude nó ra, event sẽ tự phát hiện overlap với chính nó và ném lỗi — không cho update.

4. **Nếu có nhiều event, query này có chậm không?**  
   → Nếu `event_start` và `event_end` được đánh index, DB sẽ dùng range scan. Với số lượng event thực tế (hàng chục đến hàng trăm), hiệu năng không là vấn đề.

5. **Tại sao không validate ở DB level bằng trigger?**  
   → Trigger phức tạp, khó debug và không trả về error message thân thiện cho user. Validation ở Service dễ test hơn.

### Câu trả lời mẫu

> "Em dùng Spring Data derived method để tự động sinh SQL kiểm tra overlap. Điều kiện là `existing.start <= new.end AND existing.end >= new.start` — đây là công thức chuẩn cho interval intersection. Em tách thành helper `validateNoOverlap()` để cả `create()` lẫn `update()` dùng chung. Khi update thì truyền thêm id của event đang sửa để không tự block chính nó."

---

## 4. Validate Submission Deadline nằm trong cửa sổ Event

**Commit:** `daeb36f` | **File:** `RoundService.java`  
**Method:** `validateSubmissionDeadline()`

### Chức năng

Đảm bảo deadline nộp bài của Round không nằm ngoài khoảng thời gian diễn ra Event. Nếu event kết thúc ngày 30/12, thì Round không thể đặt deadline là ngày 31/12.

### Bài toán

Round deadline không có giới hạn → coordinator có thể đặt deadline nộp bài *trước* ngày event bắt đầu hoặc *sau* ngày event kết thúc, làm team không thể nộp bài trong thực tế.

### Giải pháp

Thêm method `validateSubmissionDeadline()` vào `RoundService`, gọi trong `create()`, `createLogical()`, và `update()`.

```java
// RoundService.java — lines 322–329
private void validateSubmissionDeadline(LocalDateTime deadline, Event event) {
    if (event.getEventStart() != null && deadline.isBefore(event.getEventStart())) {
        throw ApiException.badRequest("submissionDeadline must not be before event start");
    }
    if (event.getEventEnd() != null && deadline.isAfter(event.getEventEnd())) {
        throw ApiException.badRequest("submissionDeadline must not be after event end");
    }
}
```

### Cách hoạt động

- **Input:** deadline của Round và entity Event (đã load đầy đủ từ DB qua Track).
- **Điều kiện 1:** `deadline >= eventStart` — không nộp bài trước khi sự kiện bắt đầu.
- **Điều kiện 2:** `deadline <= eventEnd` — không nộp bài sau khi sự kiện kết thúc.
- Nếu `eventStart` hoặc `eventEnd` là null (event chưa đặt ngày) thì bỏ qua — flexible với dữ liệu chưa đầy đủ.
- Gọi khi `create()` (line 203), `createLogical()` (line 241), `update()` (line 290).

### Vì sao làm như vậy

- Event có thể chưa có `eventStart`/`eventEnd` nên null-check trước là an toàn.
- Validate ở Service thay vì DB vì cần đọc entity Event để so sánh — DB không có cross-table constraint dạng này.

### Lợi ích

Tránh tình huống Round deadline nằm ngoài thực tế vận hành Event, đảm bảo team có thể nộp bài trong khung giờ hợp lệ.

### Những câu hỏi hội đồng có thể hỏi

1. **Tại sao không dùng `@Future` annotation trên DTO thay vì validate ở Service?**  
   → `@Future` chỉ kiểm tra deadline > *hiện tại*, không kiểm tra được deadline phải nằm trong khoảng `[eventStart, eventEnd]`. Cần event entity để so sánh nên phải vào Service.

2. **Nếu event chưa có `eventEnd` thì sao?**  
   → Null-check `if (event.getEventEnd() != null)` — bỏ qua điều kiện đó. Event chưa đặt ngày kết thúc thì không cản trở tạo round.

3. **Sao method nhận `Event` thay vì chỉ nhận 2 ngày?**  
   → Truyền `Event` thì tái sử dụng entity đã load, không cần query thêm. Nếu chỉ nhận 2 ngày thì caller phải tự extract — thêm boilerplate.

4. **Nếu bỏ validate này thì chuyện gì xảy ra?**  
   → Round có thể có deadline = ngày hôm qua, mọi submission đều bị block bởi `ensureSubmissionOpen()` — team không nộp được bài dù trong thời gian event.

### Câu trả lời mẫu

> "Em thêm `validateSubmissionDeadline()` để đảm bảo deadline nộp bài nằm trong khoảng `[eventStart, eventEnd]`. Em null-check cả hai ngày của event vì event có thể chưa được điền đầy đủ. Method này được gọi ở cả `create()` và `update()` của Round để không bỏ sót trường hợp nào."

---

## 5. Validate Trọng số Tiêu chí > 0

**Commit:** `eeadea9` | **Files:** `CreateRoundCriterionRequest.java`, `UpdateRoundCriterionRequest.java`

### Chức năng

Ngăn coordinator tạo tiêu chí chấm điểm với trọng số = 0 hoặc âm, vì tiêu chí như vậy không ảnh hưởng gì đến điểm và gây lãng phí tài nguyên.

### Bài toán

Không có ràng buộc → có thể nhập `weight = 0`, tiêu chí này được tạo nhưng không bao giờ ảnh hưởng đến ranking.

### Giải pháp

Thêm annotation `@DecimalMin("0.01")` vào trường `weight` của cả hai DTO.

```java
// CreateRoundCriterionRequest.java — line 19
@NotNull @DecimalMin(value = "0.01", message = "weight must be greater than 0") BigDecimal weight,

// UpdateRoundCriterionRequest.java — line 14
@DecimalMin(value = "0.01", message = "weight must be greater than 0") BigDecimal weight,
```

### Cách hoạt động

- **`@DecimalMin("0.01")`** là annotation của Bean Validation (Jakarta). Spring tự gọi validator trước khi vào Service.
- Dùng `"0.01"` thay vì `"0"` vì `@DecimalMin` mặc định là **inclusive** (`>=`). `"0.01"` là giá trị dương nhỏ nhất với 2 chữ số thập phân, đảm bảo `weight > 0`.
- **Create:** `@NotNull` đi kèm để không cho phép null.
- **Update:** không có `@NotNull` vì weight là optional (partial update).
- Message `"weight must be greater than 0"` được trả về trong response lỗi 400.

### Vì sao làm như vậy

- Dùng **DTO annotation** vì đây là format/range validation đơn giản trên một field — không cần business context.
- `@DecimalMin` thay vì `@Min` vì `@Min` chỉ dùng cho `long`/`int`, còn `weight` là `BigDecimal`.
- Validate ở **DTO** (Controller layer) thay vì Service vì không cần load entity hay query DB.

### Lợi ích

- Lỗi bị chặn sớm ngay ở Controller, không vào Service.
- Message lỗi rõ ràng, FE hiển thị được.

### Những câu hỏi hội đồng có thể hỏi

1. **Tại sao dùng `@DecimalMin("0.01")` thay vì `@Positive`?**  
   → `@Positive` yêu cầu > 0 về giá trị số học, nhưng không được tất cả version Bean Validation hỗ trợ tốt với `BigDecimal`. `@DecimalMin("0.01")` rõ ràng và chắc chắn hơn.

2. **Tại sao không validate ở Service?**  
   → Đây là range validation đơn giản không cần business context. Validate ở DTO sớm hơn, tránh tốn tài nguyên vào Service.

3. **Tại sao `UpdateRoundCriterionRequest` không có `@NotNull` trên weight?**  
   → Update là partial — FE có thể chỉ muốn đổi name, không đổi weight. Nếu thêm `@NotNull` thì FE phải luôn gửi weight dù không muốn thay đổi.

4. **Giá trị 0.01 có ý nghĩa gì không?**  
   → Đây là ngưỡng kỹ thuật, không có ý nghĩa nghiệp vụ. Thực tế weight nên là số nguyên phần trăm (như 10, 20, 40). Nhưng vì weight là `BigDecimal` để linh hoạt, 0.01 là giá trị dương nhỏ nhất hợp lệ.

### Câu trả lời mẫu

> "Em dùng `@DecimalMin('0.01')` trên field `weight` của DTO để ngăn nhập trọng số bằng 0. Validate ở DTO vì không cần context từ DB — chỉ check range đơn giản. Em dùng `@DecimalMin` thay vì `@Positive` vì `weight` là `BigDecimal`. Ở Update thì không có `@NotNull` để hỗ trợ partial update."

---

## 6. Validate Tổng Trọng số = 100 trước khi Ranking (bao gồm MISS-03)

**Commits:** `6804e64` (tạo) + `d4dd123` (bug fix)  
**Files:** `RoundCriterionRepository.java`, `RoundRankingService.java`  
**Methods:** `sumWeightByRoundId()`, `recalculate()` (đoạn kiểm tra weight)

### Chức năng

Trước khi tính ranking, kiểm tra tổng trọng số của tất cả tiêu chí trong round phải bằng đúng **100** (%). Nếu chưa đủ thì ném lỗi, không tính.

### Bài toán — 2 giai đoạn

**Giai đoạn 1 (`6804e64`):** Thêm validation nhưng so với `BigDecimal.ONE` (`1.0`). Về mặt kỹ thuật, validation tồn tại nhưng **không bao giờ bắt được lỗi** vì trọng số được lưu là phần trăm (40, 35, 25), không phải thập phân (0.40, 0.35, 0.25). Tổng là 100, không phải 1.

**Giai đoạn 2 (MISS-03, `d4dd123`):** Phát hiện bug — so sánh với 1.0 thay vì 100. Sửa lại.

### Giải pháp

**Bước 1:** Thêm JPQL query để tính tổng weight.

```java
// RoundCriterionRepository.java — lines 44–46
@Query("SELECT COALESCE(SUM(c.weight), 0) FROM RoundCriterion c WHERE c.round.id = :roundId")
BigDecimal sumWeightByRoundId(@Param("roundId") UUID roundId);
```

**Bước 2 (sau fix MISS-03):** So sánh đúng với 100.

```java
// RoundRankingService.java — lines 81–85
BigDecimal weightSum = criterionRepository.sumWeightByRoundId(roundId);
if (weightSum.compareTo(new BigDecimal("100")) != 0) {
    throw ApiException.badRequest(
            "Criterion weights must sum to 100 before recalculating rankings (current sum: " + weightSum.toPlainString() + ")");
}
```

### Cách hoạt động

- **`sumWeightByRoundId()`:** JPQL aggregate query, `COALESCE(..., 0)` để trả về 0 thay vì null khi không có tiêu chí.
- **`compareTo()` thay vì `equals()`:** `BigDecimal.equals()` so sánh cả giá trị lẫn scale. `100` và `100.0` được coi là khác nhau bởi `equals()`. Dùng `compareTo() != 0` để chỉ so sánh giá trị số học.
- **`new BigDecimal("100")`:** Dùng constructor String để tránh floating-point precision issue với `new BigDecimal(100)`.
- **Message lỗi:** In ra `currentSum` để coordinator biết hiện tại đang thiếu hay thừa bao nhiêu.
- Được gọi đầu tiên trong `recalculate()`, trước mọi xử lý khác.

### Vì sao làm như vậy

- **MISS-03 root cause:** Khi trọng số 40+35+25=100, so với `BigDecimal.ONE` (=1.0) thì `100 != 1.0` → ném lỗi *mọi lúc*, ngăn tính ranking. Đây là lỗi logic nghiêm trọng dù có commit "thêm validation".
- Dùng `compareTo()` cho `BigDecimal` là best practice trong Java — tránh bug ẩn với `equals()`.
- Validate **tại thời điểm ranking** thay vì khi lưu criteria vì coordinator có thể thêm tiêu chí dần dần — mỗi lần thêm 1 tiêu chí thì tổng đang dở dang, không nên chặn.

### Lợi ích

- Đảm bảo `weightedScore = score × (weight/100)` luôn tổng bằng `score × 1.0` → tổng điểm phản ánh đúng.
- Error message rõ ràng giúp coordinator biết cần sửa criteria nào.

### Những câu hỏi hội đồng có thể hỏi

1. **Tại sao dùng `compareTo()` thay vì `equals()` cho BigDecimal?**  
   → `BigDecimal.equals()` so sánh cả scale: `new BigDecimal("100")` và `new BigDecimal("100.0")` khác nhau về `equals()`. `compareTo()` chỉ so giá trị số học, chính xác hơn.

2. **Tại sao lỗi MISS-03 không bị phát hiện sớm hơn?**  
   → Validator đúng về mặt cú pháp nhưng sai về business logic. Unit test không cover case này. Phát hiện khi demo thực tế — bấm "Recalculate" luôn báo lỗi dù criteria đã setup đủ.

3. **Tại sao validate tổng tại thời điểm ranking, không phải khi tạo criteria?**  
   → Coordinator thêm tiêu chí từng cái một (40%, rồi 35%, rồi 25%) — sau tiêu chí đầu tổng là 40, không phải 100. Nếu block mỗi lần save thì không thể thêm tiêu chí dần.

4. **COALESCE trong query có ý nghĩa gì?**  
   → `SUM()` trả về `null` nếu không có row nào thỏa điều kiện (khi round chưa có tiêu chí). `COALESCE(SUM(...), 0)` đảm bảo trả về `0` thay vì `null` — tránh NullPointerException.

5. **Nếu tổng = 99.99 thay vì 100 thì sao?**  
   → Hệ thống vẫn từ chối vì `compareTo(new BigDecimal("100")) != 0`. Đây là thiết kế nghiêm ngặt — tổng phải chính xác là 100, không làm tròn.

### Câu trả lời mẫu

> "Em thêm JPQL query `sumWeightByRoundId()` để tính tổng weight rồi so với 100. Ban đầu em so với `BigDecimal.ONE` — đây là bug MISS-03 vì trọng số lưu theo phần trăm (40, 35, 25), không phải thập phân. Em phát hiện qua testing: bấm Recalculate luôn báo lỗi dù criteria đã đúng 100%. Em sửa lại `new BigDecimal('100')` và dùng `compareTo()` chứ không phải `equals()` vì `BigDecimal.equals()` so cả scale."

---

## 7. Validate Submission phải có ít nhất một URL

**Commit:** `82c8cce` | **File:** `SubmissionService.java`  
**Method:** `ensureAtLeastOneUrl()`

### Chức năng

Đảm bảo bài nộp của team không hoàn toàn trống — phải có ít nhất một trong bốn URL: `repoUrl`, `demoUrl`, `slideUrl`, hoặc `reportUrl`.

### Bài toán

Team có thể tạo submission với status `submitted` nhưng tất cả URL đều trống (null), nghĩa là nộp bài "không có gì" → Judge không có gì để chấm.

### Giải pháp

Thêm private method `ensureAtLeastOneUrl()` và gọi trong `submit()` và `update()`.

```java
// SubmissionService.java — lines 190–194
private void ensureAtLeastOneUrl(Submission s) {
    if (s.getRepoUrl() == null && s.getDemoUrl() == null
            && s.getSlideUrl() == null && s.getReportUrl() == null) {
        throw ApiException.badRequest(
                "At least one URL (repoUrl, demoUrl, slideUrl, or reportUrl) is required");
    }
}
```

Gọi **sau** khi `apply()` cập nhật các field vào entity, **trước** khi `save()`.

### Cách hoạt động

- **Input:** entity `Submission` sau khi đã apply các thay đổi từ request.
- **Logic:** kiểm tra cả 4 URL field đều null → ném lỗi 400.
- **Quan trọng:** check sau `apply()` vì entity có thể đã có URL từ lần submit trước (upsert pattern). Nếu check trên request thì khi update chỉ gửi `apiMetadata` mà đã có `repoUrl` từ trước thì vẫn hợp lệ.

### Vì sao làm như vậy

- Check sau `apply()` tức là check **trạng thái cuối cùng của entity**, không phải input request. Điều này đảm bảo upsert pattern hoạt động đúng.
- Không dùng `@NotNull` trên DTO vì tất cả URL đều optional — chỉ cần ít nhất 1 cái.
- Đặt trong Service vì cần đọc entity sau khi apply changes.

### Lợi ích

Tránh tình huống Judge thấy submission nhưng không có link nào để click vào chấm.

### Những câu hỏi hội đồng có thể hỏi

1. **Tại sao check sau `apply()` chứ không phải trước?**  
   → Submission là upsert — có thể team đã có `repoUrl` từ lần trước, lần này chỉ update `demoUrl`. Nếu check request thì sẽ reject dù entity vẫn có URL.

2. **Tại sao không check ở DTO với custom validator?**  
   → Cần trạng thái entity sau khi merge với dữ liệu cũ, DTO chỉ có dữ liệu mới. Cross-request validation cần context entity.

3. **apiMetadata không nằm trong check — tại sao?**  
   → `apiMetadata` là metadata phụ, không phải deliverable. Judge không cần `apiMetadata` để đánh giá; cần URL để xem code/demo.

4. **Nếu team muốn chỉ nộp slide, không có code — có hợp lệ không?**  
   → Có — chỉ cần `slideUrl != null` là pass. Business rule là "ít nhất 1 URL", không yêu cầu phải có `repoUrl`.

### Câu trả lời mẫu

> "Em thêm `ensureAtLeastOneUrl()` check sau khi `apply()` cập nhật entity, trước khi `save()`. Lý do check sau `apply()` là vì đây là upsert — entity có thể đã có URL từ lần submit trước, request lần này chỉ update metadata. Nếu check trên request thì sẽ reject oan khi team chỉ sửa ghi chú mà không gửi lại URL."

---

## 8. @Max(500) cho topNToPromote

**Commit:** `d55c483` | **Files:** `CreateRoundRequest.java`, `CreateLogicalRoundRequest.java`

### Chức năng

Giới hạn số team được promote lên round tiếp theo không quá 500, ngăn dữ liệu bất thường.

### Bài toán

Không có upper bound → coordinator có thể nhập `topNToPromote = 999999`. Khi chạy promotion, hệ thống có thể tạo hàng nghìn RoundParticipant records, gây tải DB bất thường.

### Giải pháp

```java
// CreateRoundRequest.java — line 13
@NotNull @Min(1) @Max(500) Integer topNToPromote

// CreateLogicalRoundRequest.java — line 14
@NotNull @Min(1) @Max(500) Integer topNToPromote,
```

### Cách hoạt động

- **`@Max(500)`:** Bean Validation annotation, Spring MVC gọi trước Controller method body.
- Kết hợp với `@Min(1)`: ràng buộc `1 ≤ topNToPromote ≤ 500`.
- Lỗi 400 được trả về tự động với message từ Bean Validation.

### Vì sao làm như vậy

- Validate ở DTO vì đây là constraint format đơn giản không cần business context.
- 500 là giới hạn thực tế hợp lý — không có hackathon nào promote 500+ team lên round tiếp theo.

### Những câu hỏi hội đồng có thể hỏi

1. **Tại sao chọn 500 làm giới hạn?**  
   → Giới hạn bảo vệ hệ thống khỏi dữ liệu extreme. Với một hackathon thực tế, tối đa vài chục đến vài trăm team là hợp lý.

2. **Tại sao không validate ở Service với `if (topNToPromote > 500)`?**  
   → Có thể làm ở Service, nhưng đây là constraint format đơn giản (range), phù hợp để xử lý ở DTO layer ngay khi deserialize.

### Câu trả lời mẫu

> "Em thêm `@Max(500)` kết hợp với `@Min(1)` đã có sẵn để đặt giới hạn hợp lý cho số team promoted. 500 là giá trị bảo vệ hệ thống, thực tế không hackathon nào cần promote nhiều hơn thế. Validate ở DTO vì đây là range check đơn giản."

---

## 9. MISS-10 — Không trao giải cho Team bị Disqualify

**Commit:** `27d4afd` | **File:** `PrizeService.java`  
**Method:** `create()` (đoạn check disqualified)

### Chức năng

Ngăn coordinator trao giải thưởng cho team đã bị disqualify (vi phạm quy chế).

### Bài toán

Không có check → coordinator có thể vô tình hoặc cố ý trao giải cho team vi phạm → mâu thuẫn dữ liệu.

### Giải pháp

```java
// PrizeService.java — create() method
Team tm = team(r.teamId());
if (tm != null && tm.getStatus() == TeamStatus.disqualified) {
    throw ApiException.badRequest("Cannot award a prize to a disqualified team");
}
```

### Cách hoạt động

- **`team(r.teamId())`:** load entity Team từ DB.
- Check `TeamStatus.disqualified` — enum value, so bằng `==` là đúng.
- `tm != null` vì `teamId` là optional (prize có thể không gắn team).
- Ném `ApiException.badRequest()` (HTTP 400).

### Những câu hỏi hội đồng có thể hỏi

1. **Tại sao HTTP 400 thay vì 403?**  
   → 403 là không có quyền. Ở đây coordinator có quyền trao giải, nhưng điều kiện business sai. 400 Bad Request thích hợp hơn.

2. **Nếu team bị disqualify sau khi đã nhận giải thì sao?**  
   → Hệ thống không tự thu hồi giải — đây là business decision của ban tổ chức. Validation chỉ ngăn trao *mới*.

---

## 10. MISS-11 — Không trùng tên giải thưởng trong cùng Team/Event

**Commit:** `35b9549` | **Files:** `PrizeRepository.java`, `PrizeService.java`  
**Method:** `existsByEventIdAndTeamIdAndNameIgnoreCase()`, `create()`

### Chức năng

Ngăn trao hai giải trùng tên cho cùng một team trong cùng một event. Ví dụ: không thể có 2 giải "First Prize" cho team ByteForce trong SEAL Summer 2026.

### Bài toán

Không có unique constraint ở service → tạo duplicate prizes cho cùng team → dữ liệu nhập nhằng.

### Giải pháp

```java
// PrizeRepository.java
boolean existsByEventIdAndTeamIdAndNameIgnoreCase(UUID eventId, UUID teamId, String name);

// PrizeService.java — create()
if (tm != null && repo.existsByEventIdAndTeamIdAndNameIgnoreCase(
        e.getId(), tm.getId(), r.name().trim())) {
    throw ApiException.conflict("A prize with this name has already been awarded to this team in this event");
}
```

### Cách hoạt động

- Spring Data JPA derived method: `existsByEventId AND teamId AND nameIgnoreCase` → SQL với `WHERE event_id=? AND team_id=? AND LOWER(name)=LOWER(?)`.
- `IgnoreCase` để "First Prize" và "FIRST PRIZE" coi là trùng.
- Chỉ check khi `tm != null` (prize có gắn team).

### Những câu hỏi hội đồng có thể hỏi

1. **Tại sao scope là (eventId + teamId + name) chứ không chỉ (name)?**  
   → "First Prize" có thể xuất hiện ở nhiều event khác nhau, hoặc 2 team khác nhau đều nhận "First Prize". Chỉ block khi cùng team cùng event cùng tên.

2. **Tại sao không dùng UNIQUE constraint ở DB?**  
   → DB constraint ném generic SQL exception khó parse thành message thân thiện. Service-level check trả về message rõ ràng hơn.

---

## 11. Fix MISS-10 & MISS-11 — Mở rộng check sang update()

**Commit:** `be3e902` | **Files:** `PrizeRepository.java`, `PrizeService.java`  
**Method:** `update()` (đoạn check thêm)

### Chức năng

Hai check ở mục 9 và 10 ban đầu chỉ có trong `create()`. Commit này bổ sung vào `update()` vì FE có thể gán team cho prize sau khi tạo (thông qua PATCH endpoint).

### Bài toán (Root Cause)

FE không gán team khi tạo prize ban đầu (`teamId = null`). Sau đó dùng PATCH (`update()`) để gán team. Hai validation MISS-10 và MISS-11 chỉ ở `create()` → hoàn toàn bị bypass.

```
FE flow: CREATE prize (no teamId) → PATCH prize (assign teamId) ← check bị bỏ qua!
```

### Giải pháp

Thêm check vào `update()` sau khi apply các thay đổi:

```java
// PrizeService.java — update()
if (r.trackId() != null) p.setTrack(track(r.trackId()));
if (r.teamId() != null) p.setTeam(team(r.teamId()));
validateScope(p.getEvent(), p.getTrack(), p.getTeam());
if (r.name() != null) p.setName(r.name().trim());
if (p.getTeam() != null) {
    if (p.getTeam().getStatus() == TeamStatus.disqualified) {
        throw ApiException.badRequest("Cannot award a prize to a disqualified team");
    }
    if (repo.existsByEventIdAndTeamIdAndNameIgnoreCaseAndIdNot(
            p.getEvent().getId(), p.getTeam().getId(), p.getName(), id)) {
        throw ApiException.conflict("A prize with this name has already been awarded to this team in this event");
    }
}
```

Thêm method mới vào Repository:

```java
// PrizeRepository.java
boolean existsByEventIdAndTeamIdAndNameIgnoreCaseAndIdNot(UUID eventId, UUID teamId, String name, UUID id);
```

### Cách hoạt động

- Check được đặt **sau** khi apply `teamId` và `name` vào entity — kiểm tra trạng thái cuối.
- `existsByEventIdAndTeamIdAndNameIgnoreCaseAndIdNot` thêm `AndIdNot(id)` để loại prize đang update ra — tránh tự phát hiện trùng với chính mình khi không đổi tên.
- Check trong `if (p.getTeam() != null)` vì prize có thể không gắn team.

### Vì sao làm như vậy

Khi FE dùng PATCH để assign team, `update()` là entry point duy nhất. Không có check ở `update()` = lỗ hổng hoàn toàn bị bỏ qua.

### Những câu hỏi hội đồng có thể hỏi

1. **Tại sao ban đầu chỉ có check ở `create()`?**  
   → Hiểu nhầm về FE flow — tưởng FE luôn gán team khi tạo prize. Thực tế FE tạo prize rỗng trước, sau đó PATCH để assign.

2. **`AndIdNot` trong method tên có ý nghĩa gì?**  
   → Loại record đang được update ra khỏi check. Nếu không có, prize sẽ tự phát hiện mình trùng tên với chính mình → không thể giữ nguyên tên.

3. **Tại sao check MISS-10 và MISS-11 trong cùng một `if (p.getTeam() != null)` block?**  
   → Cả hai đều chỉ có ý nghĩa khi prize gắn với team. Gom vào cùng block để tránh null check lặp lại.

4. **Nếu FE gán team và đổi tên cùng lúc thì check có đúng không?**  
   → Đúng — check được thực hiện sau khi apply cả `teamId` lẫn `name` vào entity, tức là check trên giá trị cuối cùng.

### Câu trả lời mẫu

> "Ban đầu check disqualify và duplicate name chỉ có ở `create()`. Em phát hiện FE flow thực tế là: tạo prize không có team → PATCH để gán team. Vì `update()` không có check, cả hai validation đều bị bypass. Em thêm vào `update()` với điều chỉnh: dùng `AndIdNot` cho duplicate check để prize không tự block chính mình khi không đổi tên."

---

## 12. Tie-breaker — Thời gian nộp bài phá hòa khi điểm bằng nhau tuyệt đối

**Commits:** `743eee1` (feat) + `9216268` (docs)  
**Files:** `RoundRankingRepository.java`, `RoundRankingService.java`  
**Methods:** `calculateRows()`, `recalculate()`, `TieBreaker.compareByCriterion()`, `TieBreaker.decisiveBetween()`

### Chức năng

Khi hai team có cùng tổng điểm VÀ cùng điểm trên mọi tiêu chí, team nộp bài **sớm hơn** được xếp hạng cao hơn. Đây là tie-breaker cuối cùng trong chuỗi phân hạng 4 cấp.

### Bài toán

Trước khi sửa, khi mọi tiêu chí bằng nhau, thứ hạng được quyết định bởi tên team (A-Z). Điều này không công bằng với team nỗ lực nộp bài sớm.

### Revert và Re-implement (quan trọng cho bảo vệ)

- **Commit `a747533`:** Implement tie-breaker bằng cách **thay thế hoàn toàn** criterion comparison bằng submission time. → Sai vì bỏ mất logic phân biệt điểm từng tiêu chí khi tổng điểm bằng nhau.
- **Commit `9f6c220`:** Revert lại.
- **Commit `743eee1`:** Re-implement đúng: submission time chỉ là **bước thứ 3** sau khi đã so tổng điểm và từng tiêu chí.

### Giải pháp

**Bước 1:** Thêm `earliestSubmittedAt` vào query.

```java
// RoundRankingRepository.java
@Query("""
    select
        sub.team.id as teamId,
        coalesce(sum(sc.weightedScore), 0) as totalScore,
        sub.team.name as teamName,
        min(sub.submittedAt) as earliestSubmittedAt
    from Submission sub
    left join Score sc on sc.submission.id = sub.id
    where sub.round.id = :roundId
    group by sub.team.id, sub.team.name
    order by coalesce(sum(sc.weightedScore), 0) desc, sub.team.name asc
    """)
List<RoundScoreRow> calculateRows(@Param("roundId") UUID roundId);

interface RoundScoreRow {
    UUID getTeamId();
    BigDecimal getTotalScore();
    String getTeamName();
    LocalDateTime getEarliestSubmittedAt();  // THÊM MỚI
}
```

**Bước 2:** Thêm comparator thứ 3 vào chuỗi sort.

```java
// RoundRankingService.java — recalculate()
ordered.sort(
    Comparator.<RoundRankingRepository.RoundScoreRow, BigDecimal>comparing(
            r -> nz(r.getTotalScore()), Comparator.reverseOrder())    // 1. Tổng điểm (cao → thấp)
        .thenComparing(tieBreaker::compareByCriterion)                // 2. Từng tiêu chí (nếu tổng bằng)
        .thenComparing(RoundRankingRepository.RoundScoreRow::getEarliestSubmittedAt,
                Comparator.nullsLast(Comparator.naturalOrder()))      // 3. Thời gian nộp (nếu mọi tiêu chí bằng)
        .thenComparing(r -> r.getTeamName() == null ? "" : r.getTeamName())); // 4. Tên A-Z
```

**Bước 3:** Thêm method `decisiveBetween()` thay thế `decisiveFor()` cũ.

```java
Decisive decisiveBetween(UUID teamA, UUID teamB) {
    for (CriterionRef c : criteriaByWeightDesc) {
        BigDecimal sa = scoreOf(teamA, c.id());
        BigDecimal sb = scoreOf(teamB, c.id());
        if (sa.compareTo(sb) != 0) return new Decisive(c.id(), c.name(), sb);
    }
    return null; // Mọi tiêu chí đều bằng → submission time quyết định
}
```

### Cách hoạt động toàn bộ luồng ranking

```
1. TỔNG ĐIỂM (totalScore) — cao hơn xếp trước
   │
   ├─ Khác nhau → đây là yếu tố quyết định, ghi reason "Ranked by total weighted score"
   │
   └─ Bằng nhau → compareByCriterion():
       │
       ├─ Tìm tiêu chí đầu tiên (weight cao nhất) mà hai đội khác điểm
       │    → decisiveBetween() trả về Decisive(criterionId, criterionName, score)
       │    → ghi reason "Tie on total score broken by highest-weight criterion '...'"
       │
       └─ Mọi tiêu chí đều bằng nhau (decisiveBetween trả về null)
           → So earliestSubmittedAt (tự nhiên, ASC — submit sớm hơn xếp trên)
           → ghi reason "Tie on total score and all criteria equal; resolved by earliest submission time"
```

**`min(sub.submittedAt)`:** Mỗi team chỉ có 1 submission mỗi round (unique constraint `uq_submissions_round_team`), nên `MIN` luôn trả về 1 giá trị. Dùng `MIN` để an toàn với GROUP BY.

**`Comparator.nullsLast()`:** Nếu team chưa có submission (không có `submittedAt`), sort về cuối danh sách thay vì ném NullPointerException.

### Vì sao làm như vậy

- Criterion comparison (bước 2) là **logic gốc của nhóm** — quan trọng nhất sau tổng điểm. Submission time chỉ là fallback khi không thể phân biệt qua tiêu chí.
- `decisiveBetween()` so sánh trực tiếp hai đội liền kề — chính xác hơn `decisiveFor()` cũ vốn chỉ xét một đội đơn.
- `nullsLast()` đảm bảo không crash khi dữ liệu thiếu.

### Lợi ích

- Kết quả ranking luôn xác định (deterministic) — 4 cấp so sánh đảm bảo không bao giờ có tie thực sự.
- Khuyến khích team nộp bài sớm — đây là incentive tích cực.
- `tieBreakerReason` ghi lý do cụ thể → minh bạch với team và ban giám khảo.

### Những câu hỏi hội đồng có thể hỏi

1. **Tại sao revert commit đầu tiên?**  
   → Commit đầu thay thế criterion comparison bằng submission time — điều này làm mất logic phân biệt tiêu chí. Hai team có điểm kỹ thuật 90 và 70 nhưng nộp cùng lúc → xếp hạng theo tên team dù điểm rõ ràng khác nhau. Revert và re-implement đúng: submission time chỉ là bước cuối khi mọi tiêu chí đều bằng nhau.

2. **Tại sao dùng `min(submittedAt)` trong query?**  
   → GROUP BY yêu cầu mọi cột SELECT phải có aggregate hoặc là cột GROUP BY. `submittedAt` không trong GROUP BY nên phải dùng `MIN`. Thực tế chỉ có 1 submission mỗi team mỗi round nên MIN = giá trị duy nhất đó.

3. **`decisiveBetween` khác `decisiveFor` (cũ) như thế nào?**  
   → `decisiveFor(teamId)` tìm tiêu chí đầu tiên mà team *đó* có điểm ≠ 0 — không chính xác, vì team có thể có điểm cao ở tiêu chí mà đội kia cũng cao như vậy. `decisiveBetween(teamA, teamB)` so sánh trực tiếp 2 đội → chỉ ghi nhận tiêu chí thực sự quyết định.

4. **Tại sao `Comparator.nullsLast()` thay vì `Comparator.naturalOrder()`?**  
   → `naturalOrder()` crash nếu gặp null. Team không nộp bài thì `submittedAt = null`. `nullsLast()` tự động xử lý null bằng cách đẩy về cuối.

5. **Nếu hai team nộp bài cùng giây thì sao?**  
   → Cả hai có `earliestSubmittedAt` bằng nhau → thenComparing bước 4 (tên A-Z) quyết định. Kết quả luôn deterministic.

6. **`tieBreakerReason` dùng để làm gì?**  
   → Lưu vào DB, trả về trong API response. FE hiển thị cho team biết tại sao mình xếp sau team khác dù điểm bằng nhau — minh bạch.

7. **Submission time có thể bị thao túng không (nộp sớm để giành ưu thế)?**  
   → Submission time chỉ là tie-breaker *cuối cùng*, khi điểm tuyệt đối bằng nhau trên mọi tiêu chí. Muốn xếp trên thì phải điểm cao hơn — không thể đánh lừa bằng cách nộp sớm.

### Câu trả lời mẫu

> "Ban đầu em implement bằng cách thay submission time vào chỗ criterion comparison — điều này sai vì bỏ mất logic phân biệt tiêu chí. Em revert và re-implement: submission time là bước thứ 3 trong chuỗi 4 cấp. Cụ thể: so tổng điểm → nếu bằng thì so từng tiêu chí (`compareByCriterion`) → nếu mọi tiêu chí cũng bằng thì team nộp sớm hơn xếp trên → cuối cùng là tên A-Z để kết quả luôn xác định. Em cũng thay `decisiveFor()` bằng `decisiveBetween()` để ghi lý do phân hạng chính xác hơn — so sánh trực tiếp 2 đội liền kề thay vì xét từng đội đơn lẻ."

---

## 13. Code Walkthrough — Từng file và từng function

---

### `EventService.java`

#### `validateEventDates()` *(line 745)*

| Thuộc tính | Chi tiết |
|------------|---------|
| Chức năng | Kiểm tra 3 ràng buộc thứ tự: registrationStart < registrationEnd; registrationEnd ≤ eventStart; eventStart < eventEnd |
| Được gọi bởi | `create()` (line 169), `update()` (line 196) |
| Input | 4 LocalDateTime, tất cả nullable |
| Business rule | Không cho tạo/sửa event với thứ tự ngày sai logic |
| Tại sao viết như vậy | Null-check mỗi điều kiện để partial update an toàn |
| Dòng dễ bị hỏi | `!registrationStart.isBefore(registrationEnd)` — tại sao dùng `isBefore()` thay vì `<`? → `LocalDateTime.isBefore()` là cách Java thuần túy so sánh datetime, không dùng `<` với object |

#### `validateNoOverlap()` *(line 737)*

| Thuộc tính | Chi tiết |
|------------|---------|
| Chức năng | Kiểm tra event mới không overlap với event đã có |
| Được gọi bởi | `create()` với `excludeId=null`, `update()` với `excludeId=e.getId()` |
| Input | start, end của event mới; excludeId để loại event hiện tại khi update |
| Business rule | Hai event không được chạy song song |
| Tại sao viết như vậy | Tách thành helper để create và update không duplicate logic |
| Dòng dễ bị hỏi | Tên method Repository — giải thích điều kiện overlap từ tên method |

#### `create()` *(line 164)*

| Thuộc tính | Chi tiết |
|------------|---------|
| Thứ tự validation | `existsByTitleIgnoreCase` → `validateEventDates` → `validateNoOverlap` → save |
| Tại sao thứ tự này | Check rẻ trước (title duplicate = 1 query), check đắt sau (overlap = range query) |

#### `update()` *(line 191)*

| Thuộc tính | Chi tiết |
|------------|---------|
| Đặc điểm | Partial update — merge giá trị request với giá trị entity hiện có trước khi validate |
| Dòng dễ bị hỏi | `req.eventStart() != null ? req.eventStart() : e.getEventStart()` — pattern coalesce giá trị mới/cũ |

---

### `EventRepository.java`

#### `existsByEventStartLessThanEqualAndEventEndGreaterThanEqual()`

| Thuộc tính | Chi tiết |
|------------|---------|
| Chức năng | Kiểm tra có event nào overlap với khoảng [start, end] không |
| Cơ chế | Spring Data JPA tự sinh SQL từ tên method |
| SQL tương đương | `SELECT COUNT(*) > 0 FROM event WHERE event_start <= ? AND event_end >= ?` |
| Dòng dễ bị hỏi | Tại sao tham số truyền là `(end, start)` — thứ tự tham số khớp thứ tự điều kiện trong tên method |

#### `existsByEventStartLessThanEqualAndEventEndGreaterThanEqualAndIdNot()`

| Thuộc tính | Chi tiết |
|------------|---------|
| Khác với method trên | Thêm `AND id != excludeId` để loại event đang update |
| Dùng khi | update() — không tự block mình |

---

### `RoundService.java`

#### `validateSubmissionDeadline()` *(line 322)*

| Thuộc tính | Chi tiết |
|------------|---------|
| Chức năng | Deadline phải nằm trong `[eventStart, eventEnd]` |
| Được gọi bởi | `create()` (line 203), `createLogical()` (line 241), `update()` (line 290) |
| Input | LocalDateTime deadline, Event entity |
| Business rule | Team phải có khả năng nộp bài trong thời gian event đang chạy |
| Dòng dễ bị hỏi | `if (event.getEventStart() != null)` — tại sao null-check? Vì event có thể chưa set ngày |

---

### `CreateRoundCriterionRequest.java` & `UpdateRoundCriterionRequest.java`

#### Annotation `@DecimalMin("0.01")`

| Thuộc tính | Chi tiết |
|------------|---------|
| Chức năng | Trọng số phải > 0 |
| Cơ chế | Bean Validation — Spring MVC tự gọi trước controller method |
| Tại sao 0.01 | `@DecimalMin` inclusive nên 0.01 là giá trị nhỏ nhất > 0 với 2 decimal places |
| Dòng dễ bị hỏi | Tại sao không `@Positive`? → `@Positive` ít portable hơn với `BigDecimal`, `@DecimalMin` explicit hơn |

---

### `RoundCriterionRepository.java`

#### `sumWeightByRoundId()` *(line 45)*

| Thuộc tính | Chi tiết |
|------------|---------|
| Chức năng | Tính tổng trọng số criteria trong round |
| Query | `SELECT COALESCE(SUM(c.weight), 0) FROM RoundCriterion c WHERE c.round.id = :roundId` |
| COALESCE | Trả về 0 khi không có criteria thay vì null |
| Được gọi bởi | `RoundRankingService.recalculate()` |
| Dòng dễ bị hỏi | Tại sao `COALESCE(SUM(...), 0)`? → SUM trả về null khi không có row |

---

### `SubmissionService.java`

#### `ensureAtLeastOneUrl()` *(line 190)*

| Thuộc tính | Chi tiết |
|------------|---------|
| Chức năng | Ít nhất 1 URL phải có giá trị |
| Được gọi bởi | `submit()` (line 111), `update()` (line 129) |
| Thứ tự gọi | Sau `apply()`, trước `save()` |
| Tại sao sau apply | Entity có thể đã có URL từ lần submit trước; check trạng thái entity, không check request |
| Dòng dễ bị hỏi | Tại sao check entity chứ không check request DTO? |

---

### `CreateRoundRequest.java` & `CreateLogicalRoundRequest.java`

#### Annotation `@Max(500)`

| Thuộc tính | Chi tiết |
|------------|---------|
| Chức năng | Giới hạn topNToPromote ≤ 500 |
| Kết hợp với | `@Min(1)` — ràng buộc 1 ≤ topNToPromote ≤ 500 |
| Dòng dễ bị hỏi | Tại sao 500? → Giá trị bảo vệ thực tế; không hackathon nào promote 500+ team |

---

### `PrizeRepository.java`

#### `existsByEventIdAndTeamIdAndNameIgnoreCase()`

| Thuộc tính | Chi tiết |
|------------|---------|
| Chức năng | Kiểm tra trùng tên giải trong cùng team-event |
| Scope | Chỉ trùng khi cùng eventId + teamId + name (case-insensitive) |
| Dùng trong | `PrizeService.create()` |

#### `existsByEventIdAndTeamIdAndNameIgnoreCaseAndIdNot()`

| Thuộc tính | Chi tiết |
|------------|---------|
| Khác với method trên | Thêm `AND id != excludeId` |
| Dùng trong | `PrizeService.update()` — không tự block mình khi không đổi tên |

---

### `PrizeService.java`

#### `create()` *(line 55)*

| Thuộc tính | Chi tiết |
|------------|---------|
| Thứ tự validation | load event → load team → **check disqualified** → **check duplicate name** → validateScope → save |
| Dòng dễ bị hỏi | `if (tm != null && tm.getStatus() == TeamStatus.disqualified)` — tại sao `tm != null`? Vì teamId optional |

#### `update()` *(line 75)*

| Thuộc tính | Chi tiết |
|------------|---------|
| Pattern | Apply changes → validate → check disqualified → check duplicate |
| Thứ tự quan trọng | Apply `teamId` và `name` trước khi check — kiểm tra giá trị **sau** thay đổi |
| Bug đã fix | MISS-10 & MISS-11 — các check này không có trước khi fix |
| Dòng dễ bị hỏi | `existsByEventIdAndTeamIdAndNameIgnoreCaseAndIdNot(..., id)` — id ở cuối là gì? → Id của prize đang update |

---

### `RoundRankingRepository.java`

#### `calculateRows()` *(JPQL query)*

| Thuộc tính | Chi tiết |
|------------|---------|
| Chức năng | Tính tổng điểm weighted và thời gian nộp sớm nhất của từng team |
| Thêm mới | `min(sub.submittedAt) as earliestSubmittedAt` |
| Tại sao MIN | GROUP BY yêu cầu aggregate; chỉ có 1 submission/team/round nên MIN = giá trị duy nhất |
| Dòng dễ bị hỏi | `left join Score` — tại sao LEFT JOIN? Vì team có submission nhưng chưa có điểm nào vẫn phải xuất hiện |

#### Interface `RoundScoreRow`

| Thuộc tính | Chi tiết |
|------------|---------|
| Thêm mới | `LocalDateTime getEarliestSubmittedAt()` |
| Cơ chế | Spring Data JPA Projection — interface method name khớp alias trong JPQL |

---

### `RoundRankingService.java`

#### `recalculate()` *(line 77)*

| Thuộc tính | Chi tiết |
|------------|---------|
| Luồng chính | Load round → validate weight sum = 100 → load rows → sort → delete old → save new |
| Dòng dễ bị hỏi | `weightSum.compareTo(new BigDecimal("100")) != 0` — tại sao không `equals()`? |

**Comparator chain (sort, line 97):**

```
1. comparing(r -> nz(r.getTotalScore()), reverseOrder())
   → Tổng điểm cao nhất lên đầu

2. thenComparing(tieBreaker::compareByCriterion)
   → Nếu tổng bằng: so từng tiêu chí theo trọng số giảm dần

3. thenComparing(::getEarliestSubmittedAt, nullsLast(naturalOrder()))
   → Nếu mọi tiêu chí bằng: nộp sớm hơn xếp trên

4. thenComparing(r -> teamName)
   → Tên A-Z để kết quả luôn deterministic
```

| Dòng dễ bị hỏi | Tại sao `nullsLast(naturalOrder())`? → Team không nộp bài thì null; `nullsLast` đẩy về cuối thay vì crash |

#### `TieBreaker.compareByCriterion()` *(line 196)*

| Thuộc tính | Chi tiết |
|------------|---------|
| Chức năng | So sánh 2 row theo từng tiêu chí, ưu tiên tiêu chí có weight cao nhất |
| Logic | Duyệt `criteriaByWeightDesc` (đã sắp xếp theo weight giảm dần), return khi thấy tiêu chí khác nhau |
| Return | Số âm nếu a xếp trước b; dương nếu b xếp trước; 0 nếu mọi tiêu chí bằng |
| Dòng dễ bị hỏi | `int cmp = sb.compareTo(sa)` — tại sao b so với a (ngược)? → Muốn DESC (cao hơn trước), nên đảo chiều |

#### `TieBreaker.decisiveBetween()` *(line 214)*

| Thuộc tính | Chi tiết |
|------------|---------|
| Chức năng | Tìm tiêu chí đầu tiên mà teamA và teamB khác nhau để ghi vào `tieBreakerReason` |
| Return | `Decisive(criterionId, criterionName, score)` hoặc `null` nếu mọi tiêu chí bằng nhau |
| Ý nghĩa null | Báo hiệu "submission time quyết định" → ghi reason khác vào ranking record |
| Khác `decisiveFor()` cũ | Cũ: tìm tiêu chí ≠ 0 của một team; Mới: so sánh trực tiếp 2 team → chính xác hơn |
| Dòng dễ bị hỏi | `return null` — khi nào return null và ý nghĩa là gì? |

#### `TieBreaker` inner class — cấu trúc dữ liệu

| Thuộc tính | Chi tiết |
|------------|---------|
| `criteriaByWeightDesc` | Danh sách tiêu chí sắp xếp theo weight giảm dần — duyệt từ tiêu chí quan trọng nhất |
| `byTeam` | Map: `teamId → (criterionId → weightedScore)` — tra cứu O(1) |
| Khởi tạo | Từ `criterionScores()` query — rows đã order by weight desc, seen set dedup |

---

## Tóm tắt nhanh cho ngày bảo vệ

| Feature | File | Method | Câu trả lời 1 dòng |
|---------|------|--------|--------------------|
| Event date ordering | EventService | `validateEventDates()` | 3 ràng buộc thứ tự ngày, null-safe, validate ở Service vì cross-field |
| Event overlap | EventService + EventRepository | `validateNoOverlap()` | Interval intersection, Spring Data derived method, AndIdNot khi update |
| Round deadline trong event | RoundService | `validateSubmissionDeadline()` | Deadline ∈ [eventStart, eventEnd], null-safe |
| Criteria weight > 0 | DTO | `@DecimalMin("0.01")` | DTO annotation vì range check đơn giản |
| Weight sum = 100 (MISS-03) | RoundCriterionRepository + RoundRankingService | `sumWeightByRoundId()` + check | COALESCE+SUM query; bug cũ so với 1.0, fix sang 100; dùng compareTo |
| At least one URL | SubmissionService | `ensureAtLeastOneUrl()` | Check sau apply() vì upsert — check entity state, không check request |
| topNToPromote ≤ 500 | DTO | `@Max(500)` | DTO annotation vì range check đơn giản |
| Block disqualified prize | PrizeService | `create()` + `update()` | MISS-10: check cả create lẫn update vì FE assign team qua PATCH |
| No duplicate prize name | PrizeRepository + PrizeService | `existsByEventIdAndTeamIdAndNameIgnoreCaseAndIdNot()` | MISS-11: AndIdNot để update không tự block |
| Submission time tie-breaker | RoundRankingRepository + RoundRankingService | `calculateRows()` + comparator | 4-cấp: tổng → tiêu chí → thời gian → tên; `nullsLast()`; `decisiveBetween()` |
