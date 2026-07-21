package vn.edu.fpt.seal.modules.team.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Move a team to another track within the same event (e.g. group stage -> finals). */
public record MoveTeamTrackRequest(@NotNull UUID trackId) {}
