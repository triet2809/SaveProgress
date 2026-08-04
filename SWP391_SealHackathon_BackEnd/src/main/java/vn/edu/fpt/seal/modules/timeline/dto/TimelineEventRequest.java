package vn.edu.fpt.seal.modules.timeline.dto;

import vn.edu.fpt.seal.modules.timeline.TimelineEventType;
import vn.edu.fpt.seal.modules.timeline.TimelineScope;
import vn.edu.fpt.seal.modules.timeline.TimelineSourceType;

import java.util.UUID;

public record TimelineEventRequest(
        UUID eventId, UUID teamId, UUID roundId, UUID trackId,
        TimelineEventType eventType, TimelineSourceType sourceType, UUID sourceId,
        TimelineScope visibility, String title, String description, String metadata,
        String idempotencyKey) {
}
