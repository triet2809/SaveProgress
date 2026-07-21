package vn.edu.fpt.seal.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.incident.entity.IncidentReport;
import vn.edu.fpt.seal.modules.judge.repository.RoundJudgeRepository;
import vn.edu.fpt.seal.modules.mentor.repository.TrackMentorRepository;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.submission.entity.Submission;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.repository.TeamMemberRepository;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {
    @Mock TeamMemberRepository teamMembers;
    @Mock RoundJudgeRepository roundJudges;
    @Mock TrackMentorRepository trackMentors;

    AuthorizationService service;
    UUID userId;
    UUID teamId;
    UUID trackId;
    UUID roundId;
    Submission submission;
    Round round;

    @BeforeEach
    void setUp() {
        service = new AuthorizationService(teamMembers, roundJudges, trackMentors);
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        trackId = UUID.randomUUID();
        roundId = UUID.randomUUID();

        Event event = Event.builder().title("Event").build();
        event.setId(UUID.randomUUID());
        Track track = Track.builder().event(event).name("Track").build();
        track.setId(trackId);
        round = Round.builder().track(track).name("Round").sequenceNumber(1)
                .submissionDeadline(LocalDateTime.now().plusDays(1)).topNToPromote(1).build();
        round.setId(roundId);
        Team team = Team.builder().track(track).name("Team").build();
        team.setId(teamId);
        submission = Submission.builder().round(round).team(team).build();
        submission.setId(UUID.randomUUID());
    }

    @Test
    void submissionScopeAllowsOwnerAssignedJudgeMentorAndCoordinator() {
        CurrentUser participant = user("team_member");
        when(teamMembers.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        assertTrue(service.canReadSubmission(participant, submission));

        reset(teamMembers);
        CurrentUser judge = user("judge");
        when(roundJudges.existsByRoundIdAndUserId(roundId, userId)).thenReturn(true);
        assertTrue(service.canReadSubmission(judge, submission));

        reset(roundJudges);
        CurrentUser mentor = user("mentor");
        when(trackMentors.existsByTrackIdAndUserId(trackId, userId)).thenReturn(true);
        assertTrue(service.canReadSubmission(mentor, submission));

        assertTrue(service.canReadSubmission(user("coordinator"), submission));
    }

    @Test
    void unrelatedUserCannotReadSubmission() {
        assertFalse(service.canReadSubmission(user("team_member"), submission));
        assertFalse(service.canReadSubmission(user("judge"), submission));
        assertFalse(service.canReadSubmission(user("mentor"), submission));
    }

    @Test
    void owningTeamSeesDetailedScoreOnlyAfterPublication() {
        CurrentUser participant = user("team_member");
        when(teamMembers.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        assertFalse(service.canReadDetailedScore(participant, submission));

        round.setResultPublishedAt(LocalDateTime.now());
        assertTrue(service.canReadDetailedScore(participant, submission));
    }

    @Test
    void assignedJudgeAndMentorSeeDetailedScoreBeforePublication() {
        when(roundJudges.existsByRoundIdAndUserId(roundId, userId)).thenReturn(true);
        assertTrue(service.canReadDetailedScore(user("judge"), submission));
        reset(roundJudges);
        when(trackMentors.existsByTrackIdAndUserId(trackId, userId)).thenReturn(true);
        assertTrue(service.canReadDetailedScore(user("mentor"), submission));
    }

    @Test
    void competitorRankingVisibilityRequiresPublication() {
        CurrentUser participant = user("team_member");
        assertFalse(service.canReadRanking(participant, round));
        round.setResultPublishedAt(LocalDateTime.now());
        when(teamMembers.existsByUserIdAndTeamTrackId(userId, trackId)).thenReturn(true);
        assertTrue(service.canReadRanking(participant, round));
    }

    @Test
    void incidentScopeIncludesReporterTeamJudgeMentorAndCoordinator() {
        User reporter = User.builder().email("reporter@example.com").fullName("Reporter").passwordHash("x").build();
        reporter.setId(UUID.randomUUID());
        IncidentReport incident = IncidentReport.builder()
                .event(submission.getRound().getTrack().getEvent())
                .round(round).team(submission.getTeam()).submission(submission).reporter(reporter)
                .build();

        CurrentUser reporterPrincipal = CurrentUser.builder().id(reporter.getId()).roles(List.of("team_member")).build();
        assertTrue(service.canReadIncident(reporterPrincipal, incident));

        when(teamMembers.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        assertTrue(service.canReadIncident(user("team_member"), incident));
        reset(teamMembers);
        when(roundJudges.existsByRoundIdAndUserId(roundId, userId)).thenReturn(true);
        assertTrue(service.canReadIncident(user("judge"), incident));
        reset(roundJudges);
        when(trackMentors.existsByTrackIdAndUserId(trackId, userId)).thenReturn(true);
        assertTrue(service.canReadIncident(user("mentor"), incident));
        assertTrue(service.canReadIncident(user("coordinator"), incident));
    }

    private CurrentUser user(String role) {
        return CurrentUser.builder().id(userId).email("user@example.com").roles(List.of(role)).build();
    }
}
