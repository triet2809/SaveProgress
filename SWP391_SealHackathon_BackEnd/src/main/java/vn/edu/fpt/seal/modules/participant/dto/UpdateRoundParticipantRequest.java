package vn.edu.fpt.seal.modules.participant.dto;

import jakarta.validation.constraints.Size;
import vn.edu.fpt.seal.common.enums.RoundParticipantStatus;

public record UpdateRoundParticipantRequest(RoundParticipantStatus status, @Size(max = 10000) String note) {
}
