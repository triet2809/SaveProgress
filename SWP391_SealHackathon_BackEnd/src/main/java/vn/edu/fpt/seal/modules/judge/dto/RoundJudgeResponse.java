package vn.edu.fpt.seal.modules.judge.dto;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record RoundJudgeResponse(UUID id, UUID roundId, String roundName, UUID userId, String email, String fullName, LocalDateTime assignedAt) {}
