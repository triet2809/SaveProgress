package vn.edu.fpt.seal.modules.team.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** A student requesting to join a team; optional short message to the leader. */
public record CreateJoinRequestRequest(
        @NotNull UUID teamId,
        @Size(max = 500) String message
) {}
