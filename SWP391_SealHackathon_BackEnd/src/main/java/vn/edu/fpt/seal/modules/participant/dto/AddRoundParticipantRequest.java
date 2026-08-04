package vn.edu.fpt.seal.modules.participant.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.seal.common.enums.RoundParticipantStatus;

import java.util.UUID;

public record AddRoundParticipantRequest(@NotNull UUID roundId, @NotNull UUID teamId, RoundParticipantStatus status,
                                         @Size(max = 10000) String note) {
}
