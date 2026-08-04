package vn.edu.fpt.seal.modules.staff.dto;

import lombok.Builder;
import vn.edu.fpt.seal.common.enums.AccountStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record EventStaffResponse(
        UUID userId, String fullName, String email, AccountStatus accountStatus,
        List<String> globalRoles, List<String> eventRoles,
        List<UUID> mentorTrackIds, List<String> mentorTracks,
        List<UUID> judgeTrackIds, List<String> judgeTracks,
        List<UUID> judgeRoundIds, List<String> judgeRounds,
        LocalDateTime createdAt, String activationDelivery, String activationUrl) {
}
