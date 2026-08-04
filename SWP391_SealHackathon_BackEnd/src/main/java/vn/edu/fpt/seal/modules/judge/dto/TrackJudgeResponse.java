package vn.edu.fpt.seal.modules.judge.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TrackJudgeResponse(UUID id, UUID eventId, UUID trackId, String trackName, UUID userId, String email,
                                 String fullName, LocalDateTime assignedAt) {
}
