package vn.edu.fpt.seal.modules.team.mapper;

import vn.edu.fpt.seal.modules.team.dto.TeamMemberResponse;
import vn.edu.fpt.seal.modules.team.dto.TeamResponse;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.entity.TeamMember;
import vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos;

import java.util.List;

public final class TeamMapper {

    private TeamMapper() {
    }

    public static TeamResponse toResponse(Team team, List<TeamMember> members) {
        return toResponse(team, members, true, List.of());
    }

    public static TeamResponse toResponse(Team team, List<TeamMember> members, boolean includeInviteCode) {
        return toResponse(team, members, includeInviteCode, List.of());
    }

    public static TeamResponse toResponse(Team team, List<TeamMember> members, boolean includeInviteCode,
                                          List<RecognitionDtos.Summary> recognitions) {
        return TeamResponse.builder()
                .id(team.getId())
                .teamProfileId(team.getTeamProfile() == null ? null : team.getTeamProfile().getId())
                .sourceTeamId(team.getSourceTeam() == null ? null : team.getSourceTeam().getId())
                .trackId(team.getTrack().getId())
                .eventId(team.getTrack().getEvent().getId())
                .name(team.getName())
                .inviteCode(includeInviteCode ? team.getInviteCode() : null)
                .status(team.getStatus())
                .disqualifiedReason(team.getDisqualifiedReason())
                .members(members.stream().map(TeamMapper::toMemberResponse).toList())
                .recognitions(recognitions == null ? List.of() : recognitions)
                .activatedFromProfileAt(team.getActivatedFromProfileAt())
                .rosterConfirmedAt(team.getRosterConfirmedAt())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }

    public static TeamMemberResponse toMemberResponse(TeamMember member) {
        return TeamMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .email(member.getUser().getEmail())
                .fullName(member.getUser().getFullName())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
