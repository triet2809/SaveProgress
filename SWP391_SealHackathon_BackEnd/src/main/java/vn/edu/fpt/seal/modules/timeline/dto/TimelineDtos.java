package vn.edu.fpt.seal.modules.timeline.dto;

import vn.edu.fpt.seal.modules.timeline.TimelineEventType;
import vn.edu.fpt.seal.modules.timeline.TimelineScope;

import java.time.LocalDateTime;
import java.util.UUID;

public final class TimelineDtos {
    private TimelineDtos() {
    }

    public record Response(UUID id, UUID eventId, UUID teamId, UUID roundId, UUID trackId,
                           TimelineEventType eventType, TimelineScope visibilityScope, String title,
                           String description, String statusSnapshot, LocalDateTime occurredAt) {
    }
}
