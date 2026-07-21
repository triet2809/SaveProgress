package vn.edu.fpt.seal.modules.mentor.dto;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TrackMentorResponse(UUID id, UUID eventId, UUID trackId, String trackName, UUID userId, String email, String fullName, LocalDateTime assignedAt) {}
