package vn.edu.fpt.seal.modules.round.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.audit.entity.AuditLog;
import vn.edu.fpt.seal.modules.audit.repository.AuditLogRepository;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.round.dto.*;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.entity.RoundDefinition;
import vn.edu.fpt.seal.modules.round.mapper.RoundMapper;
import vn.edu.fpt.seal.modules.round.repository.RoundDefinitionRepository;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.timeline.TimelineEventType;
import vn.edu.fpt.seal.modules.timeline.TimelineScope;
import vn.edu.fpt.seal.modules.timeline.TimelineSourceType;
import vn.edu.fpt.seal.modules.timeline.dto.TimelineEventRequest;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class RoundService {

    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final CompetitionLifecycleService lifecycleService;
    private final TimelineService timelineService;
    private final RoundDefinitionRepository definitionRepository;

    @Autowired
    public RoundService(RoundRepository roundRepository, TrackRepository trackRepository,
                        AuditLogRepository auditLogRepository, UserRepository userRepository,
                        CompetitionLifecycleService lifecycleService, TimelineService timelineService,
                        RoundDefinitionRepository definitionRepository) {
        this.roundRepository = roundRepository;
        this.trackRepository = trackRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.lifecycleService = lifecycleService;
        this.timelineService = timelineService;
        this.definitionRepository = definitionRepository;
    }

    public RoundService(RoundRepository roundRepository, TrackRepository trackRepository,
                        AuditLogRepository auditLogRepository, UserRepository userRepository,
                        CompetitionLifecycleService lifecycleService, TimelineService timelineService) {
        this(roundRepository, trackRepository, auditLogRepository, userRepository,
                lifecycleService, timelineService, null);
    }

    // Constructor cũ giữ lại cho test đơn lẻ, không cần lifecycle hooks.
    public RoundService(RoundRepository r, TrackRepository t, AuditLogRepository a, UserRepository u) {
        this.roundRepository = r;
        this.trackRepository = t;
        this.auditLogRepository = a;
        this.userRepository = u;
        this.lifecycleService = null;
        this.timelineService = null;
        this.definitionRepository = null;
    }

    // Luồng publish kết quả: kiểm tra đúng event, ghi version kết quả, mở appeal window.
    @Transactional
    public RoundResponse publishResults(UUID eventId, UUID roundId, Authentication auth) {
        // Luồng publish kết quả: FE bấm publish -> BE kiểm tra round đúng event -> lưu version kết quả -> mở cửa sổ appeal.
        Round round = findOrThrow(roundId);
        if (!round.getTrack().getEvent().getId().equals(eventId)) {
            throw ApiException.badRequest("Round does not belong to the selected event");
        }
        if (round.getResultPublishedAt() == null
                || round.getLifecycleState() == vn.edu.fpt.seal.common.enums.RoundLifecycleState.AWAITING_RECALCULATION
                || (lifecycleService != null && !lifecycleService.hasPublishedVersion(roundId))) {
            UUID actorId = auth != null && auth.getPrincipal() instanceof CurrentUser c ? c.getId() : null;
            boolean corrected = round.getResultPublishedAt() != null;
            vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersion version = null;
            if (lifecycleService != null)
                version = lifecycleService.publish(round, actorId == null ? null : userRepository.findById(actorId).orElse(null),
                        corrected ? "Corrected result republication" : "Initial publication");
            else round.setResultPublishedAt(java.time.LocalDateTime.now());
            auditLogRepository.save(AuditLog.builder()
                    .user(actorId == null ? null : userRepository.findById(actorId).orElse(null))
                    .action(vn.edu.fpt.seal.common.enums.AuditAction.PUBLISH_RESULTS)
                    .targetType("round").targetId(roundId)
                    .newValue("published").details("Round results published").build());
            if (timelineService != null) {
                UUID sourceId = version == null ? roundId : version.getId();
                TimelineEventType type = corrected ? TimelineEventType.CORRECTED_RESULT_REPUBLISHED : TimelineEventType.RESULT_PUBLISHED;
                timelineService.record(request(round, type, TimelineScope.EVENT_PARTICIPANTS,
                        corrected ? "Corrected results republished" : "Results published",
                        "Round results are available", TimelineSourceType.RESULT_VERSION, sourceId,
                        "result-version:" + sourceId + ":published"));
                timelineService.record(request(round, TimelineEventType.APPEAL_WINDOW_OPENED,
                        TimelineScope.EVENT_PARTICIPANTS, "Appeal window opened",
                        "The result appeal window is open", TimelineSourceType.RESULT_VERSION, sourceId,
                        "result-version:" + sourceId + ":appeal-window"));
            }
        }
        if (lifecycleService != null) lifecycleService.refresh(round);
        return response(round);
    }

    // Luồng advance: chỉ chạy khi round đã READY_TO_ADVANCE sau khi hết appeal.
    @Transactional
    public RoundResponse advance(UUID eventId, UUID roundId) {
        // Luồng advance: chỉ chạy khi round đã tới trạng thái READY_TO_ADVANCE sau khi hết appeal.
        Round round = findOrThrow(roundId);
        if (!round.getTrack().getEvent().getId().equals(eventId))
            throw ApiException.badRequest("Round does not belong to the selected event");
        lifecycleService.advance(round);
        auditLogRepository.save(AuditLog.builder().action(vn.edu.fpt.seal.common.enums.AuditAction.UPDATE)
                .targetType("round").targetId(roundId).oldValue("READY_TO_ADVANCE").newValue("ADVANCED")
                .details("Coordinator advanced round using the active published result version").build());
        if (timelineService != null) {
            timelineService.record(request(round, TimelineEventType.ROUND_READY_TO_ADVANCE,
                    TimelineScope.EVENT_PARTICIPANTS, "Round ready to advance",
                    "The appeal lifecycle completed", TimelineSourceType.ROUND, roundId,
                    "round:" + roundId + ":ready-to-advance"));
            timelineService.record(request(round, TimelineEventType.ROUND_ADVANCED,
                    TimelineScope.EVENT_PARTICIPANTS, "Round advanced",
                    "Qualified teams advanced to the next round", TimelineSourceType.ROUND, roundId,
                    "round:" + roundId + ":advanced"));
        }
        return response(round);
    }

    // Luồng resume: coordinator cho round chạy tiếp sau khi xử lý appeal xong.
    @Transactional
    public RoundResponse resume(UUID eventId, UUID roundId) {
        // Luồng resume: coordinator cho round chạy lại sau khi xử lý appeal xong.
        Round round = findOrThrow(roundId);
        if (!round.getTrack().getEvent().getId().equals(eventId))
            throw ApiException.badRequest("Round does not belong to the selected event");
        lifecycleService.resume(round);
        auditLogRepository.save(AuditLog.builder().action(vn.edu.fpt.seal.common.enums.AuditAction.UPDATE)
                .targetType("round").targetId(roundId).newValue(round.getLifecycleState().name())
                .details("Coordinator explicitly resumed round after appeal resolution").build());
        if (timelineService != null) timelineService.record(request(round, TimelineEventType.COMPETITION_RESUMED,
                TimelineScope.EVENT_PARTICIPANTS, "Competition resumed",
                "The round appeal lifecycle has resumed", TimelineSourceType.ROUND, roundId,
                "round:" + roundId + ":resumed"));
        return response(round);
    }

    // Lấy danh sách round theo track để FE render tab Rounds.
    @Transactional
    public Page<RoundResponse> listByTrack(UUID trackId, Pageable pageable) {
        // Lấy danh sách round theo track để FE hiển thị bảng và sắp xếp theo sequenceNumber.
        Pageable effectivePageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("sequenceNumber").ascending());
        if (trackId == null) {
            return roundRepository.findAll(effectivePageable).map(this::refreshAndMap);
        }
        if (!trackRepository.existsById(trackId)) {
            throw ApiException.notFound("Track not found: " + trackId);
        }
        return roundRepository.findByTrackId(trackId, effectivePageable).map(this::refreshAndMap);
    }

    // Lấy danh sách round theo event cho màn tổng quan.
    @Transactional
    public Page<RoundResponse> listByEvent(UUID eventId, Pageable pageable) {
        // Lấy toàn bộ round của event cho các màn tổng quan / analytics.
        if (eventId == null) throw ApiException.badRequest("eventId is required");
        return roundRepository.findByTrackEventId(eventId, pageable).map(this::refreshAndMap);
    }

    // Lấy 1 round theo ID.
    @Transactional
    public RoundResponse get(UUID id) {
        // Lấy 1 round theo ID.
        return refreshAndMap(findOrThrow(id));
    }

    // Tạo round đơn cho track cụ thể.
    @Transactional
    public RoundResponse create(CreateRoundRequest req) {
        // Tạo round đơn cho track cụ thể, thường dùng cho flow cũ hoặc test.
        Track track = trackRepository.findById(req.trackId())
                .orElseThrow(() -> ApiException.notFound("Track not found: " + req.trackId()));
        ensureEditable(track);
        validateSubmissionDeadline(req.submissionDeadline(), track.getEvent());

        String name = req.name().trim();
        UUID trackId = track.getId();
        if (roundRepository.existsByTrackIdAndNameIgnoreCase(trackId, name)) {
            throw ApiException.conflict("Round name already exists in this track");
        }
        int sequenceNumber = resolveSequenceNumber(trackId, req.sequenceNumber());

        Round round = Round.builder()
                .logicalRound(resolveLegacyDefinition(track, name, sequenceNumber))
                .track(track)
                .name(name)
                .sequenceNumber(sequenceNumber)
                .submissionDeadline(req.submissionDeadline())
                .topNToPromote(req.topNToPromote())
                .build();
        round = roundRepository.save(round);
        log.info("Round created: id={}, track={}, sequence={}, name={}",
                round.getId(), trackId, round.getSequenceNumber(), round.getName());
        return RoundMapper.toResponse(round);
    }

    // Tạo logical round: 1 vòng logic có thể gồm nhiều track execution.
    @Transactional
    public LogicalRoundResponse createLogical(CreateLogicalRoundRequest req) {
        // Tạo logical round: 1 vòng logic có thể gồm nhiều track execution.
        if (definitionRepository == null) throw new IllegalStateException("Logical round repository is unavailable");
        if (new HashSet<>(req.trackIds()).size() != req.trackIds().size()) {
            throw ApiException.badRequest("Duplicate track selection is not allowed");
        }
        List<Track> tracks = new ArrayList<>(trackRepository.findAllById(req.trackIds()));
        if (tracks.size() != req.trackIds().size()) throw ApiException.notFound("One or more tracks were not found");
        UUID eventId = tracks.get(0).getEvent().getId();
        if (tracks.stream().anyMatch(track -> !track.getEvent().getId().equals(eventId))) {
            throw ApiException.badRequest("All selected tracks must belong to the same event");
        }
        tracks.forEach(this::ensureEditable);
        validateSubmissionDeadline(req.submissionDeadline(), tracks.get(0).getEvent());
        String name = req.name().trim();
        if (definitionRepository.existsByEventIdAndNameIgnoreCase(eventId, name)) {
            throw ApiException.conflict("Logical round name already exists in this event");
        }
        int sequence = req.sequenceNumber() == null
                ? definitionRepository.findTopByEventIdOrderBySequenceNumberDesc(eventId)
                .map(definition -> definition.getSequenceNumber() + 1).orElse(1)
                : req.sequenceNumber();
        if (definitionRepository.existsByEventIdAndSequenceNumber(eventId, sequence)) {
            throw ApiException.conflict("Logical round sequence already exists in this event");
        }
        boolean finalRound = Boolean.TRUE.equals(req.finalRound());
        if (finalRound && tracks.size() != 1) {
            throw ApiException.badRequest("A final logical round must contain exactly one track execution");
        }
        for (Track track : tracks) {
            if (roundRepository.existsByTrackIdAndNameIgnoreCase(track.getId(), name)
                    || roundRepository.existsByTrackIdAndSequenceNumber(track.getId(), sequence)) {
                throw ApiException.conflict("Selected track already has this round name or sequence: " + track.getName());
            }
        }
        RoundDefinition definition = definitionRepository.saveAndFlush(RoundDefinition.builder()
                .event(tracks.get(0).getEvent()).name(name).sequenceNumber(sequence)
                .finalRound(finalRound)
                .defaultTopNToPromote(req.defaultTopNToPromote() == null
                        ? req.topNToPromote() : req.defaultTopNToPromote())
                .build());
        List<Round> executions = tracks.stream().map(track -> Round.builder()
                .logicalRound(definition).track(track).name(name).sequenceNumber(sequence)
                .submissionDeadline(req.submissionDeadline()).topNToPromote(req.topNToPromote()).build()).toList();
        List<Round> saved = roundRepository.saveAllAndFlush(executions);
        return new LogicalRoundResponse(definition.getId(), eventId, name, sequence,
                definition.isFinalRound(), definition.getDefaultTopNToPromote(),
                definition.getLifecycleState().name(),
                saved.stream().map(RoundMapper::toResponse).toList());
    }

    // Cập nhật round nhưng không đổi name/sequence qua endpoint này.
    @Transactional
    public RoundResponse update(UUID id, UpdateRoundRequest req) {
        // Cập nhật round, nhưng không cho đổi tên / sequence qua endpoint này vì đó là dữ liệu dùng chung cho logical round.
        Round round = findOrThrow(id);
        ensureEditable(round.getTrack());
        if (req.name() != null || req.sequenceNumber() != null) {
            throw ApiException.conflict(
                    "Shared round name and sequence must be changed through the logical-round update endpoint");
        }
        if (req.submissionDeadline() != null) {
            validateSubmissionDeadline(req.submissionDeadline(), round.getTrack().getEvent());
            round.setSubmissionDeadline(req.submissionDeadline());
        }
        if (req.topNToPromote() != null) {
            round.setTopNToPromote(req.topNToPromote());
        }
        return RoundMapper.toResponse(round);
    }

    // Xóa round chỉ khi event còn draft.
    @Transactional
    public void delete(UUID id) {
        // Xóa round chỉ khi event còn draft.
        Round round = findOrThrow(id);
        ensureDraft(round.getTrack());
        roundRepository.delete(round);
        log.info("Round deleted: id={}", id);
    }

    // Helper: lấy sequence tiếp theo nếu FE không truyền.
    private int resolveSequenceNumber(UUID trackId, Integer requestedSequenceNumber) {
        if (requestedSequenceNumber != null) {
            if (roundRepository.existsByTrackIdAndSequenceNumber(trackId, requestedSequenceNumber)) {
                throw ApiException.conflict("Round sequence number already exists in this track");
            }
            return requestedSequenceNumber;
        }
        return roundRepository.findTopByTrackIdOrderBySequenceNumberDesc(trackId)
                .map(r -> r.getSequenceNumber() + 1)
                .orElse(1);
    }

    private void validateSubmissionDeadline(LocalDateTime deadline, Event event) {
        if (event.getEventStart() != null && deadline.isBefore(event.getEventStart())) {
            throw ApiException.badRequest("submissionDeadline must not be before event start");
        }
        if (event.getEventEnd() != null && deadline.isAfter(event.getEventEnd())) {
            throw ApiException.badRequest("submissionDeadline must not be after event end");
        }
    }

    // Helper: ném lỗi nếu round không tồn tại.
    private Round findOrThrow(UUID id) {
        return roundRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Round not found: " + id));
    }

    // Helper cũ để khớp logical round khi round đơn vẫn được tạo theo kiểu legacy.
    private RoundDefinition resolveLegacyDefinition(Track track, String name, int sequence) {
        if (definitionRepository == null) return null;
        return definitionRepository.findByEventIdAndNameIgnoreCaseAndSequenceNumber(
                        track.getEvent().getId(), name, sequence)
                .orElseGet(() -> definitionRepository.save(RoundDefinition.builder()
                        .event(track.getEvent()).name(name).sequenceNumber(sequence).build()));
    }

    // Helper: refresh lifecycle rồi map sang response.
    private RoundResponse refreshAndMap(Round round) {
        if (lifecycleService != null) lifecycleService.refresh(round);
        return response(round);
    }

    // Helper: map entity round sang DTO response, kèm thời gian còn lại nếu có lifecycle service.
    private RoundResponse response(Round round) {
        return lifecycleService == null ? RoundMapper.toResponse(round)
                : RoundMapper.toResponse(round, lifecycleService.remainingSeconds(round));
    }

    // Helper: chỉ cho sửa round khi event chưa completed/cancelled.
    private void ensureEditable(Track track) {
        EventStatus status = track.getEvent().getStatus();
        if (status == EventStatus.completed || status == EventStatus.cancelled) {
            throw ApiException.badRequest("Cannot edit rounds in event status " + status);
        }
    }

    // Helper: chỉ cho xóa round khi event vẫn draft.
    private void ensureDraft(Track track) {
        EventStatus status = track.getEvent().getStatus();
        if (status != EventStatus.draft) {
            throw ApiException.badRequest("Rounds can only be deleted while event is draft (current: " + status + ")");
        }
    }

    // Helper: build request timeline cho round.
    private TimelineEventRequest request(Round round, TimelineEventType type, TimelineScope scope,
                                         String title, String description, TimelineSourceType sourceType,
                                         UUID sourceId, String key) {
        return new TimelineEventRequest(round.getTrack().getEvent().getId(), null, round.getId(),
                round.getTrack().getId(), type, sourceType, sourceId, scope, title, description,
                null, key);
    }
}
