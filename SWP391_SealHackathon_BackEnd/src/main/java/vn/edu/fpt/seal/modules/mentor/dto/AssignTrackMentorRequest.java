package vn.edu.fpt.seal.modules.mentor.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTrackMentorRequest(@NotNull UUID trackId, @NotNull UUID userId) {
}
