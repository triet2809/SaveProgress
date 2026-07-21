package vn.edu.fpt.seal.modules.judge.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignRoundJudgeRequest(@NotNull UUID roundId, @NotNull UUID userId) {}
