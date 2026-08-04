package vn.edu.fpt.seal.modules.team.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinTeamRequest(@NotBlank String inviteCode) {
}
