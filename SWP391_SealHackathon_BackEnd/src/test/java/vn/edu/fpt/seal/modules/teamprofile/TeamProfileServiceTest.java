package vn.edu.fpt.seal.modules.teamprofile;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import vn.edu.fpt.seal.common.enums.*;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.audit.repository.AuditLogRepository;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.team.entity.*;
import vn.edu.fpt.seal.modules.team.repository.*;
import vn.edu.fpt.seal.modules.teamprofile.dto.TeamProfileDtos;
import vn.edu.fpt.seal.modules.teamprofile.entity.TeamProfile;
import vn.edu.fpt.seal.modules.teamprofile.repository.TeamProfileRepository;
import vn.edu.fpt.seal.modules.teamprofile.service.TeamProfileService;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.CurrentUser;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamProfileServiceTest {
    @Mock TeamProfileRepository profiles;
    @Mock TeamRepository teams;
    @Mock TeamMemberRepository members;
    @Mock TrackRepository tracks;
    @Mock EventRepository events;
    @Mock UserRepository users;
    @Mock AuditLogRepository audits;

    TeamProfileService service;
    TeamProfile profile;
    Team source;
    Event targetEvent;
    Track targetTrack;
    List<TeamMember> historicalRoster;
    UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        service = new TeamProfileService(profiles, teams, members, tracks, events, users, audits);
        profile = TeamProfile.builder().canonicalName("Legacy Alpha").status(TeamProfileStatus.active).build();
        profile.setId(UUID.randomUUID());

        Event historicalEvent = Event.builder().title("2025").status(EventStatus.completed).build();
        historicalEvent.setId(UUID.randomUUID());
        Track historicalTrack = Track.builder().event(historicalEvent).name("General").build();
        historicalTrack.setId(UUID.randomUUID());
        source = Team.builder().teamProfile(profile).track(historicalTrack).name("Legacy Alpha")
                .status(TeamStatus.active).inviteCode("OLD123").build();
        source.setId(UUID.randomUUID());

        targetEvent = Event.builder().title("2026").status(EventStatus.published).build();
        targetEvent.setId(UUID.randomUUID());
        targetTrack = Track.builder().event(targetEvent).name("AI").maxTeams(10).build();
        targetTrack.setId(UUID.randomUUID());

        User leader = user("Leader");
        User member2 = user("Member Two");
        User member3 = user("Member Three");
        historicalRoster = List.of(member(leader, TeamMemberRole.leader),
                member(member2, TeamMemberRole.member), member(member3, TeamMemberRole.member));
        CurrentUser current = CurrentUser.builder().id(leader.getId())
                .roles(List.of("team_leader")).build();
        authentication = new UsernamePasswordAuthenticationToken(current, null);

        lenient().when(profiles.findById(profile.getId())).thenReturn(Optional.of(profile));
        when(teams.findWithTrackById(source.getId())).thenReturn(Optional.of(source));
        lenient().when(events.findById(targetEvent.getId())).thenReturn(Optional.of(targetEvent));
        lenient().when(tracks.findById(targetTrack.getId())).thenReturn(Optional.of(targetTrack));
        when(members.findByTeamIdOrderByRoleAscJoinedAtAsc(source.getId())).thenReturn(historicalRoster);
    }

    @Test
    void threeReturningHistoricalMembersAreEligible() {
        var preview = service.preview(profile.getId(), request(allHistoricalIds(), historicalRoster.get(0).getUser().getId()),
                authentication);

        assertTrue(preview.eligible());
        assertEquals(3, preview.returningMemberCount());
        assertEquals(1, preview.proposedNewRoster().stream()
                .filter(member -> member.role() == TeamMemberRole.leader).count());
    }

    @Test
    void twoReturningMembersAreRejectedWithoutWriting() {
        Set<UUID> selected = new LinkedHashSet<>(allHistoricalIds());
        selected.remove(historicalRoster.get(2).getUser().getId());

        var preview = service.preview(profile.getId(),
                request(selected, historicalRoster.get(0).getUser().getId()), authentication);

        assertFalse(preview.eligible());
        assertTrue(preview.missingRequirements().stream().anyMatch(text -> text.contains("At least three")));
        verify(teams, never()).save(any());
    }

    @Test
    void selectedNonHistoricalMemberAndUnselectedLeaderAreRejected() {
        Set<UUID> selected = new LinkedHashSet<>(allHistoricalIds());
        selected.add(UUID.randomUUID());

        var preview = service.preview(profile.getId(), request(selected, UUID.randomUUID()), authentication);

        assertFalse(preview.eligible());
        assertTrue(preview.missingRequirements().stream().anyMatch(text -> text.contains("source historical roster")));
        assertTrue(preview.missingRequirements().stream().anyMatch(text -> text.contains("leader must be returning")));
    }

    @Test
    void targetTrackFromAnotherEventIsBadRequest() {
        Event other = Event.builder().title("Other").status(EventStatus.published).build();
        other.setId(UUID.randomUUID());
        targetTrack.setEvent(other);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.preview(profile.getId(),
                        request(allHistoricalIds(), historicalRoster.get(0).getUser().getId()), authentication));

        assertEquals(400, exception.getStatus().value());
    }

    @Test
    void existingProfileRegistrationAndMemberConflictMakePreviewIneligible() {
        when(teams.existsByTeamProfileIdAndTrackEventId(profile.getId(), targetEvent.getId())).thenReturn(true);
        Team conflictTeam = Team.builder().teamProfile(TeamProfile.builder().canonicalName("Other").build())
                .track(targetTrack).name("Other").status(TeamStatus.active).build();
        conflictTeam.setId(UUID.randomUUID());
        TeamMember conflict = member(historicalRoster.get(1).getUser(), TeamMemberRole.member);
        conflict.setTeam(conflictTeam);
        when(members.findActiveRegistrationsInEvent(any(), eq(targetEvent.getId())))
                .thenReturn(List.of(conflict));

        var preview = service.preview(profile.getId(),
                request(allHistoricalIds(), historicalRoster.get(0).getUser().getId()), authentication);

        assertFalse(preview.eligible());
        assertFalse(preview.memberConflicts().isEmpty());
        assertTrue(preview.missingRequirements().stream().anyMatch(text -> text.contains("already has")));
    }

    @Test
    void unrelatedAndHistoricalNonLeaderCannotInitiate() {
        CurrentUser unrelated = CurrentUser.builder().id(UUID.randomUUID()).roles(List.of("team_member")).build();
        var unrelatedAuth = new UsernamePasswordAuthenticationToken(unrelated, null);
        assertEquals(403, assertThrows(ApiException.class,
                () -> service.preview(profile.getId(),
                        request(allHistoricalIds(), historicalRoster.get(0).getUser().getId()), unrelatedAuth))
                .getStatus().value());

        CurrentUser nonLeader = CurrentUser.builder().id(historicalRoster.get(1).getUser().getId())
                .roles(List.of("team_member")).build();
        var nonLeaderAuth = new UsernamePasswordAuthenticationToken(nonLeader, null);
        assertEquals(403, assertThrows(ApiException.class,
                () -> service.preview(profile.getId(),
                        request(allHistoricalIds(), historicalRoster.get(0).getUser().getId()), nonLeaderAuth))
                .getStatus().value());
    }

    @Test
    void executionCreatesNewRegistrationWithSelectedExistingUsersOnly() {
        when(profiles.findByIdForUpdate(profile.getId())).thenReturn(Optional.of(profile));
        when(users.findById(any())).thenAnswer(invocation -> historicalRoster.stream()
                .map(TeamMember::getUser)
                .filter(user -> user.getId().equals(invocation.getArgument(0)))
                .findFirst());
        when(teams.saveAndFlush(any())).thenAnswer(invocation -> {
            Team created = invocation.getArgument(0);
            created.setId(UUID.randomUUID());
            return created;
        });
        when(members.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.reactivate(profile.getId(),
                request(allHistoricalIds(), historicalRoster.get(1).getUser().getId()), authentication);

        assertNotEquals(source.getId(), response.id());
        assertEquals(profile.getId(), response.teamProfileId());
        assertEquals(source.getId(), response.sourceTeamId());
        assertNotEquals(source.getInviteCode(), response.inviteCode());
        assertEquals(3, response.members().size());
        assertEquals(1, response.members().stream()
                .filter(member -> member.role() == TeamMemberRole.leader).count());
        assertEquals(allHistoricalIds(), response.members().stream()
                .map(member -> member.userId())
                .collect(java.util.stream.Collectors.toSet()));
        assertEquals(3, historicalRoster.size());
        verify(users, times(4)).findById(any());
        verify(audits).save(any());
    }

    @Test
    void concurrentDuplicateIsRejectedBeforeAnyRosterIsCopied() {
        when(profiles.findByIdForUpdate(profile.getId())).thenReturn(Optional.of(profile));
        when(users.findById(authentication.getPrincipal() instanceof CurrentUser current
                ? current.getId() : null)).thenReturn(Optional.of(historicalRoster.get(0).getUser()));
        when(teams.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate profile/event"));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.reactivate(profile.getId(),
                        request(allHistoricalIds(), historicalRoster.get(0).getUser().getId()), authentication));

        assertEquals(409, exception.getStatus().value());
        verify(members, never()).save(any());
        verify(audits, never()).save(any());
    }

    private TeamProfileDtos.ReactivationRequest request(Set<UUID> selected, UUID leaderId) {
        return new TeamProfileDtos.ReactivationRequest(source.getId(), targetEvent.getId(),
                targetTrack.getId(), selected, leaderId);
    }

    private Set<UUID> allHistoricalIds() {
        return historicalRoster.stream().map(member -> member.getUser().getId())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private User user(String name) {
        User user = User.builder().email(name.replace(" ", "").toLowerCase() + "@example.com")
                .passwordHash("x").fullName(name).status(AccountStatus.approved).build();
        user.setId(UUID.randomUUID());
        return user;
    }

    private TeamMember member(User user, TeamMemberRole role) {
        TeamMember member = TeamMember.builder().team(source).user(user).role(role).build();
        member.setId(UUID.randomUUID());
        return member;
    }
}
