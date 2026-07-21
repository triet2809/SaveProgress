package vn.edu.fpt.seal.modules.team;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.common.enums.TeamStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.participant.repository.RoundParticipantRepository;
import vn.edu.fpt.seal.modules.ranking.repository.RoundRankingRepository;
import vn.edu.fpt.seal.modules.resultversion.repository.RoundResultVersionEntryRepository;
import vn.edu.fpt.seal.modules.seeding.repository.EventSeedAssignmentRepository;
import vn.edu.fpt.seal.modules.seeding.repository.EventTeamFinishRepository;
import vn.edu.fpt.seal.modules.submission.repository.SubmissionRepository;
import vn.edu.fpt.seal.modules.team.dto.TeamTransferDtos;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.modules.team.service.TeamTransferService;
import vn.edu.fpt.seal.modules.teamprofile.entity.TeamProfile;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamTransferServiceTest {
    @Mock TeamRepository teams;
    @Mock TrackRepository tracks;
    @Mock SubmissionRepository submissions;
    @Mock RoundParticipantRepository participants;
    @Mock RoundRankingRepository rankings;
    @Mock RoundResultVersionEntryRepository results;
    @Mock EventTeamFinishRepository finishes;
    @Mock EventSeedAssignmentRepository seeds;

    TeamTransferService service;
    Event event;
    Track a;
    Track b;

    @BeforeEach
    void setUp() {
        service = new TeamTransferService(teams, tracks, submissions, participants, rankings, results, finishes, seeds);
        event = Event.builder().title("Event").status(EventStatus.published).build();
        event.setId(UUID.randomUUID());
        a = track("A");
        b = track("B");
    }

    @Test
    void bulkTransferMovesEveryValidatedTeamInOneMutation() {
        Team first = team("One", a);
        Team second = team("Two", a);
        when(tracks.findById(b.getId())).thenReturn(Optional.of(b));
        when(teams.findAllById(List.of(first.getId(), second.getId()))).thenReturn(List.of(first, second));
        when(teams.findByTrackEventId(event.getId())).thenReturn(List.of(first, second));

        var result = service.bulkTransfer(new TeamTransferDtos.BulkTransferRequest(
                List.of(first.getId(), second.getId()), b.getId()));

        assertEquals(b, first.getTrack());
        assertEquals(b, second.getTrack());
        assertEquals(2, result.moves().size());
        verify(teams).saveAll(argThat(values -> {
            int count = 0;
            for (Object ignored : values) count++;
            return count == 2;
        }));
    }

    @Test
    void unsafeSelectedTeamRejectsWholeBulkTransferBeforeMutation() {
        Team safe = team("Safe", a);
        Team locked = team("Locked", a);
        when(tracks.findById(b.getId())).thenReturn(Optional.of(b));
        when(teams.findAllById(List.of(safe.getId(), locked.getId()))).thenReturn(List.of(safe, locked));
        when(submissions.existsByTeamId(any())).thenAnswer(invocation -> locked.getId().equals(invocation.getArgument(0)));

        assertThrows(ApiException.class, () -> service.bulkTransfer(
                new TeamTransferDtos.BulkTransferRequest(List.of(safe.getId(), locked.getId()), b.getId())));
        assertEquals(a, safe.getTrack());
        verify(teams, never()).saveAll(anyList());
    }

    @Test
    void balancePreviewIsDeterministicAndDiffersByAtMostOne() {
        List<Team> eventTeams = new ArrayList<>();
        for (int i = 0; i < 5; i++) eventTeams.add(team("Team " + i, a));
        when(tracks.findAllById(List.of(a.getId(), b.getId()))).thenReturn(List.of(a, b));
        when(teams.findByTrackEventId(event.getId())).thenReturn(eventTeams);
        TeamTransferDtos.BalanceRequest request =
                new TeamTransferDtos.BalanceRequest(List.of(a.getId(), b.getId()), 42L);

        var first = service.previewBalance(event.getId(), request);
        var second = service.previewBalance(event.getId(), request);

        assertEquals(first.moves(), second.moves());
        long min = first.counts().stream().mapToLong(TeamTransferDtos.TrackCount::afterCount).min().orElseThrow();
        long max = first.counts().stream().mapToLong(TeamTransferDtos.TrackCount::afterCount).max().orElseThrow();
        assertTrue(max - min <= 1);
    }

    @Test
    void disqualifiedTeamIsExcludedFromBalance() {
        Team active = team("Active", a);
        Team disqualified = team("Disqualified", a);
        disqualified.setStatus(TeamStatus.disqualified);
        when(tracks.findAllById(List.of(a.getId(), b.getId()))).thenReturn(List.of(a, b));
        when(teams.findByTrackEventId(event.getId())).thenReturn(List.of(active, disqualified));

        var preview = service.previewBalance(event.getId(),
                new TeamTransferDtos.BalanceRequest(List.of(a.getId(), b.getId()), null));

        assertEquals(1, preview.excluded().size());
        assertEquals(disqualified.getId(), preview.excluded().get(0).teamId());
    }

    @Test
    void duplicateNameExclusionRemainsInProjectedAndAppliedTrackCounts() {
        Team first = team("Same Name", a);
        Team second = team("Same Name", b);
        Team retained = team("Same Name", a);
        List<Team> eventTeams = new ArrayList<>(List.of(first, second, retained));
        List<UUID> targetIds = List.of(a.getId(), b.getId());
        when(tracks.findAllById(targetIds)).thenReturn(List.of(a, b));
        when(teams.findByTrackEventId(event.getId())).thenReturn(eventTeams);
        when(teams.findAllById(anyList())).thenAnswer(invocation -> {
            List<UUID> ids = invocation.getArgument(0);
            return eventTeams.stream().filter(team -> ids.contains(team.getId())).toList();
        });
        TeamTransferDtos.BalanceRequest request =
                new TeamTransferDtos.BalanceRequest(targetIds, null);

        var applied = service.applyBalance(event.getId(), request);

        assertEquals(1, applied.excluded().size());
        Map<UUID, Long> actual = eventTeams.stream().collect(java.util.stream.Collectors.groupingBy(
                team -> team.getTrack().getId(), java.util.stream.Collectors.counting()));
        for (TeamTransferDtos.TrackCount count : applied.counts()) {
            assertEquals(actual.getOrDefault(count.trackId(), 0L), count.afterCount());
        }
        assertEquals(eventTeams.size(), applied.counts().stream()
                .mapToLong(TeamTransferDtos.TrackCount::afterCount).sum());
    }

    private Track track(String name) {
        Track track = Track.builder().event(event).name(name).build();
        track.setId(UUID.randomUUID());
        return track;
    }

    private Team team(String name, Track track) {
        TeamProfile profile = TeamProfile.builder().canonicalName(name).build();
        profile.setId(UUID.randomUUID());
        Team team = Team.builder().teamProfile(profile).track(track).name(name).status(TeamStatus.active).build();
        team.setId(UUID.randomUUID());
        return team;
    }
}
