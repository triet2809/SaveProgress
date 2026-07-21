package vn.edu.fpt.seal.modules.track.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.track.dto.CreateTrackRequest;
import vn.edu.fpt.seal.modules.track.dto.TrackResponse;
import vn.edu.fpt.seal.modules.track.dto.UpdateTrackRequest;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.mapper.TrackMapper;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackService {

    private final TrackRepository trackRepository;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public Page<TrackResponse> listByEvent(UUID eventId, Pageable pageable) {
        if (eventId == null) {
            return trackRepository.findAll(pageable).map(TrackMapper::toResponse);
        }
        if (!eventRepository.existsById(eventId)) {
            throw ApiException.notFound("Event not found: " + eventId);
        }
        return trackRepository.findByEventId(eventId, pageable).map(TrackMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TrackResponse get(UUID id) {
        return TrackMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public TrackResponse create(CreateTrackRequest req) {
        Event event = eventRepository.findById(req.eventId())
                .orElseThrow(() -> ApiException.notFound("Event not found: " + req.eventId()));

        if (event.getStatus() == EventStatus.completed || event.getStatus() == EventStatus.cancelled) {
            throw ApiException.badRequest("Cannot add tracks to event in status " + event.getStatus());
        }

        String name = req.name().trim();
        if (trackRepository.existsByEventIdAndNameIgnoreCase(event.getId(), name)) {
            throw ApiException.conflict("Track name already exists in this event");
        }

        Track t = Track.builder()
                .event(event)
                .name(name)
                .description(req.description())
                .maxTeams(req.maxTeams())
                .build();
        t = trackRepository.save(t);
        log.info("Track created: id={}, event={}, name={}", t.getId(), event.getId(), name);
        return TrackMapper.toResponse(t);
    }

    @Transactional
    public TrackResponse update(UUID id, UpdateTrackRequest req) {
        Track t = findOrThrow(id);
        EventStatus es = t.getEvent().getStatus();
        if (es == EventStatus.completed || es == EventStatus.cancelled) {
            throw ApiException.badRequest("Cannot edit track in event status " + es);
        }
        if (req.name() != null) {
            String name = req.name().trim();
            if (!name.equalsIgnoreCase(t.getName())
                    && trackRepository.existsByEventIdAndNameIgnoreCase(t.getEvent().getId(), name)) {
                throw ApiException.conflict("Track name already exists in this event");
            }
            t.setName(name);
        }
        if (req.description() != null) {
            t.setDescription(req.description());
        }
        if (req.maxTeams() != null) {
            t.setMaxTeams(req.maxTeams());
        }
        return TrackMapper.toResponse(t);
    }

    @Transactional
    public void delete(UUID id) {
        Track t = findOrThrow(id);
        EventStatus es = t.getEvent().getStatus();
        if (es != EventStatus.draft) {
            throw ApiException.badRequest("Tracks can only be deleted while event is draft (current: " + es + ")");
        }
        trackRepository.delete(t);
        log.info("Track deleted: id={}", id);
    }

    private Track findOrThrow(UUID id) {
        return trackRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Track not found: " + id));
    }
}
