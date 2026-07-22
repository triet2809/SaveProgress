package vn.edu.fpt.seal.modules.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.modules.event.dto.ChangeEventStatusRequest;
import vn.edu.fpt.seal.modules.event.dto.CreateEventRequest;
import vn.edu.fpt.seal.modules.event.dto.EventResponse;
import vn.edu.fpt.seal.modules.event.dto.SetupCompetitionRequest;
import vn.edu.fpt.seal.modules.event.dto.SetupCompetitionResponse;
import vn.edu.fpt.seal.modules.event.dto.UpdateEventRequest;
import vn.edu.fpt.seal.modules.event.service.EventService;
import vn.edu.fpt.seal.modules.round.dto.RoundResponse;
import vn.edu.fpt.seal.modules.round.service.RoundService;

import java.util.UUID;

/**
 * Controller quản lý sự kiện hackathon: tạo/sửa/xóa event, chuyển trạng thái,
 * mở/đóng đăng ký, dựng cuộc thi và điều phối vòng thi.
 * Phần lớn endpoint yêu cầu vai trò COORDINATOR.
 */
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Hackathon event management")
public class EventController {

    private final EventService eventService;
    private final RoundService roundService;

    /**
     * Công bố kết quả của một vòng thi (chỉ COORDINATOR).
     *
     * @param eventId        ID sự kiện
     * @param roundId        ID vòng thi
     * @param authentication thông tin người thực hiện
     * @return vòng thi đã công bố kết quả
     */
    @PostMapping("/{eventId}/rounds/{roundId}/publish-results")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<RoundResponse> publishResults(@PathVariable UUID eventId, @PathVariable UUID roundId,
                                                        Authentication authentication) {
        return ResponseEntity.ok(roundService.publishResults(eventId, roundId, authentication));
    }

    /**
     * Mở lại một vòng thi (chỉ COORDINATOR).
     *
     * @param eventId ID sự kiện
     * @param roundId ID vòng thi
     * @return vòng thi đã mở lại
     */
    @PostMapping("/{eventId}/rounds/{roundId}/resume")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<RoundResponse> resume(@PathVariable UUID eventId, @PathVariable UUID roundId) {
        return ResponseEntity.ok(roundService.resume(eventId, roundId));
    }

    /**
     * Chuyển cuộc thi sang vòng tiếp theo (chỉ COORDINATOR).
     *
     * @param eventId ID sự kiện
     * @param roundId ID vòng thi hiện tại
     * @return vòng thi sau khi advance
     */
    @PostMapping("/{eventId}/rounds/{roundId}/advance")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<RoundResponse> advance(@PathVariable UUID eventId, @PathVariable UUID roundId) {
        return ResponseEntity.ok(roundService.advance(eventId, roundId));
    }

    /**
     * Liệt kê sự kiện, có thể lọc theo trạng thái.
     *
     * @param status   trạng thái cần lọc (tùy chọn)
     * @param pageable thông tin phân trang
     * @return trang danh sách sự kiện
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List events (optional filter by status)")
    public ResponseEntity<Page<EventResponse>> list(
            @RequestParam(required = false) EventStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(eventService.list(status, pageable));
    }

    /**
     * Lấy chi tiết sự kiện theo ID.
     *
     * @param id ID sự kiện
     * @return chi tiết sự kiện
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get event by id")
    public ResponseEntity<EventResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.get(id));
    }

    /**
     * Tạo sự kiện mới (chỉ COORDINATOR).
     *
     * @param req dữ liệu tạo sự kiện
     * @return sự kiện vừa tạo
     */
    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Create new event (coordinator only)")
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest req) {
        return ResponseEntity.ok(eventService.create(req));
    }

    /**
     * Cập nhật thông tin sự kiện (chỉ COORDINATOR).
     *
     * @param id  ID sự kiện
     * @param req dữ liệu cập nhật
     * @return sự kiện sau cập nhật
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Update event metadata (coordinator only)")
    public ResponseEntity<EventResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateEventRequest req) {
        return ResponseEntity.ok(eventService.update(id, req));
    }

    /**
     * Thay đổi trạng thái sự kiện (chỉ COORDINATOR).
     *
     * @param id             ID sự kiện
     * @param req            trạng thái mới
     * @param authentication thông tin người thực hiện
     * @return sự kiện sau khi đổi trạng thái
     */
    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Change event status (coordinator only)")
    public ResponseEntity<EventResponse> changeStatus(@PathVariable UUID id,
                                                      @Valid @RequestBody ChangeEventStatusRequest req,
                                                      Authentication authentication) {
        return ResponseEntity.ok(eventService.changeStatus(id, req.status(), authentication));
    }

    /**
     * Mở đăng ký: chuyển draft -&gt; published; tự động tạo track "General" (chỉ COORDINATOR).
     *
     * @param id ID sự kiện
     * @return sự kiện sau khi mở đăng ký
     */
    @PostMapping("/{id}/open-registration")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Open registration: draft -> published; auto-creates a General track (coordinator only)")
    public ResponseEntity<EventResponse> openRegistration(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.openRegistration(id));
    }

    /**
     * Đóng đăng ký: chuyển published -&gt; ongoing (chỉ COORDINATOR).
     *
     * @param id   ID sự kiện
     * @param auth thông tin người thực hiện
     * @return sự kiện sau khi đóng đăng ký
     */
    @PostMapping("/{id}/close-registration")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Close registration: published -> ongoing (coordinator only)")
    public ResponseEntity<EventResponse> closeRegistration(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(eventService.closeRegistration(id, auth));
    }

    /**
     * Dựng track + vòng thi và phân bổ đội sau khi đóng đăng ký (chỉ COORDINATOR).
     *
     * @param id  ID sự kiện
     * @param req cấu hình dựng cuộc thi (tùy chọn; null dùng mặc định)
     * @return kết quả dựng cuộc thi
     */
    @PostMapping("/{id}/setup-competition")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Build tracks + rounds and distribute teams after registration closes (coordinator only)")
    public ResponseEntity<SetupCompetitionResponse> setupCompetition(@PathVariable UUID id,
                                                                     @Valid @RequestBody(required = false) SetupCompetitionRequest req) {
        return ResponseEntity.ok(eventService.setupCompetition(id, req));
    }

    /**
     * Xóa sự kiện (chỉ khi status=draft).
     *
     * @param id ID sự kiện
     * @return 204 No Content khi xóa thành công
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Delete event (only when status=draft)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
