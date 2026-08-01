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

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Hackathon event management")
public class EventController {

    private final EventService eventService;
    private final RoundService roundService;

    // Endpoint mở đầu luồng publish kết quả: FE bấm nút publish -> controller -> RoundService.publishResults().
    // Round publish flow: FE publish button -> RoundService.publishResults.
    @PostMapping("/{eventId}/rounds/{roundId}/publish-results")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<RoundResponse> publishResults(@PathVariable UUID eventId, @PathVariable UUID roundId,
                                                        Authentication authentication) {
        return ResponseEntity.ok(roundService.publishResults(eventId, roundId, authentication));
    }

    // Endpoint cho phép coordinator mở lại vòng sau khi xử lý appeal xong.
    // Round resume flow: FE resume button -> RoundService.resume.
    @PostMapping("/{eventId}/rounds/{roundId}/resume")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<RoundResponse> resume(@PathVariable UUID eventId, @PathVariable UUID roundId) {
        return ResponseEntity.ok(roundService.resume(eventId, roundId));
    }

    // Endpoint đẩy vòng sang trạng thái kế tiếp sau khi đủ điều kiện advance.
    // Round advance flow: FE advance button -> RoundService.advance.
    @PostMapping("/{eventId}/rounds/{roundId}/advance")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<RoundResponse> advance(@PathVariable UUID eventId, @PathVariable UUID roundId) {
        return ResponseEntity.ok(roundService.advance(eventId, roundId));
    }

    // Danh sách event để FE render bảng quản lý event.
    // Event list flow: FE table/list -> EventService.list.
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List events (optional filter by status)")
    public ResponseEntity<Page<EventResponse>> list(
            @RequestParam(required = false) EventStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(eventService.list(status, pageable));
    }

    // Lấy 1 event theo ID để màn chi tiết event nạp dữ liệu.
    // Event detail flow: FE event detail page -> EventService.get.
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get event by id")
    public ResponseEntity<EventResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.get(id));
    }

    // Tạo event mới từ modal bên FE.
    // Create event flow: FE modal -> EventService.create.
    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Create new event (coordinator only)")
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest req) {
        return ResponseEntity.ok(eventService.create(req));
    }

    // Cập nhật metadata của event đã có.
    // Update event flow: FE edit modal -> EventService.update.
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Update event metadata (coordinator only)")
    public ResponseEntity<EventResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateEventRequest req) {
        return ResponseEntity.ok(eventService.update(id, req));
    }

    // Đổi status event qua endpoint riêng, FE chỉ gửi target status.
    // Change status flow: FE status action -> EventService.changeStatus.
    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Change event status (coordinator only)")
    public ResponseEntity<EventResponse> changeStatus(@PathVariable UUID id,
                                                      @Valid @RequestBody ChangeEventStatusRequest req,
                                                      Authentication authentication) {
        return ResponseEntity.ok(eventService.changeStatus(id, req.status(), authentication));
    }

    // Mở đăng ký: đổi draft -> published và tạo track General nếu thiếu.
    // Open registration flow: FE action -> EventService.openRegistration.
    @PostMapping("/{id}/open-registration")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Open registration: draft -> published; auto-creates a General track (coordinator only)")
    public ResponseEntity<EventResponse> openRegistration(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.openRegistration(id));
    }

    // Đóng đăng ký: chặn team mới và chuyển event sang ongoing.
    // Close registration flow: FE action -> EventService.closeRegistration.
    @PostMapping("/{id}/close-registration")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Close registration: published -> ongoing (coordinator only)")
    public ResponseEntity<EventResponse> closeRegistration(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(eventService.closeRegistration(id, auth));
    }

    // Dựng competition từ roundPlan do FE gửi lên.
    // Setup competition flow: FE wizard -> EventService.setupCompetition.
    @PostMapping("/{id}/setup-competition")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Build tracks + rounds and distribute teams after registration closes (coordinator only)")
    public ResponseEntity<SetupCompetitionResponse> setupCompetition(@PathVariable UUID id,
                                                                     @Valid @RequestBody(required = false) SetupCompetitionRequest req) {
        return ResponseEntity.ok(eventService.setupCompetition(id, req));
    }

    // Xóa event nháp.
    // Delete draft event flow: FE delete -> EventService.delete.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Delete event (only when status=draft)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
