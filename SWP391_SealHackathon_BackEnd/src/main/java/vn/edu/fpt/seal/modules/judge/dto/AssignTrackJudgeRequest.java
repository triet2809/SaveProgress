package vn.edu.fpt.seal.modules.judge.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTrackJudgeRequest(@NotNull UUID trackId, @NotNull UUID userId) {
}
