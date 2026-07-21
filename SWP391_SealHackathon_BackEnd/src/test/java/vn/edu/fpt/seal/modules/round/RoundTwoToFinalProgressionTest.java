package vn.edu.fpt.seal.modules.round;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.fpt.seal.common.enums.RoundLifecycleState;
import vn.edu.fpt.seal.common.enums.RoundParticipantStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.appeal.repository.AppealRepository;
import vn.edu.fpt.seal.modules.criteria.repository.RoundCriterionRepository;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.judge.repository.RoundJudgeRepository;
import vn.edu.fpt.seal.modules.participant.entity.RoundParticipant;
import vn.edu.fpt.seal.modules.participant.repository.RoundParticipantRepository;
import vn.edu.fpt.seal.modules.ranking.repository.RoundRankingRepository;
import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersion;
import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersionEntry;
import vn.edu.fpt.seal.modules.resultversion.repository.RoundResultVersionEntryRepository;
import vn.edu.fpt.seal.modules.resultversion.repository.RoundResultVersionRepository;
import vn.edu.fpt.seal.modules.round.dto.LogicalRoundProgressionDtos.Assignment;
import vn.edu.fpt.seal.modules.round.dto.LogicalRoundProgressionDtos.ManualAssignmentRequest;
import vn.edu.fpt.seal.modules.round.entity.LogicalRoundPromotion;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.entity.RoundDefinition;
import vn.edu.fpt.seal.modules.round.repository.LogicalRoundPromotionRepository;
import vn.edu.fpt.seal.modules.round.repository.RoundDefinitionRepository;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.round.service.CompetitionLifecycleService;
import vn.edu.fpt.seal.modules.round.service.LogicalRoundIntegrityService;
import vn.edu.fpt.seal.modules.submission.repository.SubmissionRepository;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.track.entity.Track;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoundTwoToFinalProgressionTest {
    @Mock RoundRepository rounds;
    @Mock AppealRepository appeals;
    @Mock RoundResultVersionRepository versions;
    @Mock RoundResultVersionEntryRepository entries;
    @Mock RoundRankingRepository rankings;
    @Mock RoundParticipantRepository participants;
    @Mock LogicalRoundPromotionRepository promotions;
    @Mock RoundDefinitionRepository definitions;
    @Mock SubmissionRepository submissions;
    @Mock RoundCriterionRepository criteria;
    @Mock RoundJudgeRepository roundJudges;

    private Event event;
    private RoundDefinition r1, r2, finalRound;
    private List<Round> r1Executions, r2Executions;
    private Round finalExecution;
    private Map<UUID, RoundResultVersion> publishedByRound;
    private Map<UUID, List<RoundResultVersionEntry>> entriesByVersion;
    private List<LogicalRoundPromotion> promotionStore;
    private List<RoundParticipant> participantStore;
    private CompetitionLifecycleService lifecycle;
    private LogicalRoundIntegrityService integrity;

    @BeforeEach
    void setUp() {
        event = Event.builder().title("Progression").build();
        event.setId(UUID.randomUUID());
        r1 = definition("Round 1", 1);
        r2 = definition("Round 2", 2);
        finalRound = definition("Final", 3);
        finalRound.setFinalRound(true);
        r1Executions = executions(r1, 5, "R1");
        r2Executions = executions(r2, 2, "R2");
        finalExecution = executions(finalRound, 1, "Final").get(0);
        publishedByRound = new HashMap<>();
        entriesByVersion = new HashMap<>();
        promotionStore = new ArrayList<>();
        participantStore = new ArrayList<>();

        when(rounds.findByLogicalRoundId(r1.getId())).thenReturn(r1Executions);
        when(rounds.findByLogicalRoundId(r2.getId())).thenReturn(r2Executions);
        when(rounds.findByLogicalRoundId(finalRound.getId())).thenReturn(List.of(finalExecution));
        when(definitions.findById(r2.getId())).thenReturn(Optional.of(r2));
        when(definitions.findById(finalRound.getId())).thenReturn(Optional.of(finalRound));
        when(definitions.findByEventIdAndSequenceNumber(event.getId(), 2)).thenReturn(Optional.of(r2));
        when(definitions.findByEventIdAndSequenceNumber(event.getId(), 3)).thenReturn(Optional.of(finalRound));
        when(appeals.existsByRoundIdAndStatus(any(), eq("PENDING"))).thenReturn(false);
        when(versions.findByRoundIdAndStatus(any(), eq("published"))).thenAnswer(invocation ->
                Optional.ofNullable(publishedByRound.get(invocation.getArgument(0, UUID.class))));
        when(versions.findById(any())).thenAnswer(invocation -> publishedByRound.values().stream()
                .filter(version -> version.getId().equals(invocation.getArgument(0, UUID.class))).findFirst());
        when(entries.findByResultVersionId(any())).thenAnswer(invocation ->
                entriesByVersion.getOrDefault(invocation.getArgument(0, UUID.class), List.of()));
        when(promotions.existsByTargetLogicalRoundIdAndTeamId(any(), any())).thenAnswer(invocation ->
                promotionStore.stream().anyMatch(p -> p.getTargetLogicalRound().getId()
                        .equals(invocation.getArgument(0, UUID.class))
                        && p.getTeam().getId().equals(invocation.getArgument(1, UUID.class))));
        when(promotions.findByTargetLogicalRoundIdOrderByTeamNameAsc(any())).thenAnswer(invocation ->
                promotionStore.stream().filter(p -> p.getTargetLogicalRound().getId()
                        .equals(invocation.getArgument(0, UUID.class))).toList());
        when(promotions.save(any())).thenAnswer(invocation -> {
            LogicalRoundPromotion promotion = invocation.getArgument(0);
            promotionStore.add(promotion);
            return promotion;
        });
        when(participants.existsByRoundLogicalRoundIdAndTeamId(any(), any())).thenAnswer(invocation ->
                participantStore.stream().anyMatch(p -> p.getRound().getLogicalRound().getId()
                        .equals(invocation.getArgument(0, UUID.class))
                        && p.getTeam().getId().equals(invocation.getArgument(1, UUID.class))));
        doAnswer(invocation -> { participantStore.add(invocation.getArgument(0)); return invocation.getArgument(0); })
                .when(participants).save(any());
        when(resultVersionEntryRepository().findById(any())).thenAnswer(invocation ->
                entriesByVersion.values().stream().flatMap(Collection::stream)
                        .filter(entry -> entry.getId().equals(invocation.getArgument(0, UUID.class))).findFirst());

        lifecycle = new CompetitionLifecycleService(rounds, appeals, versions,
                entries, rankings, participants, Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(lifecycle, "promotions", promotions);
        ReflectionTestUtils.setField(lifecycle, "definitions", definitions);
        integrity = new LogicalRoundIntegrityService(definitions, rounds, promotions, participants,
                submissions, rankings, versions, resultVersionEntryRepository(), appeals, criteria, roundJudges);
    }

    private RoundResultVersionEntryRepository resultVersionEntryRepository() {
        return entries;
    }

    @Test
    void onlyRoundTwoPublishedSubsetCanReachFinal() {
        Map<UUID, Team> roundOneTeams = new LinkedHashMap<>();
        for (int executionIndex = 0; executionIndex < r1Executions.size(); executionIndex++) {
            Round execution = r1Executions.get(executionIndex);
            RoundResultVersion version = publishedVersion(execution);
            List<RoundResultVersionEntry> promoted = new ArrayList<>();
            for (int rank = 1; rank <= 3; rank++) {
                Team team = team("R1-" + executionIndex + "-" + rank, execution.getTrack());
                roundOneTeams.put(team.getId(), team);
                promoted.add(entry(version, team, rank, "promoted"));
            }
            entriesByVersion.put(version.getId(), promoted);
        }

        lifecycle.advance(r1Executions.get(0));
        assertEquals(15, promotionStore.size());
        assertTrue(promotionStore.stream().allMatch(p -> p.getTargetLogicalRound().getId().equals(r2.getId())));

        List<Assignment> roundTwoAssignments = r2Executions.stream().map(execution ->
                new Assignment(execution.getId(), promotionStore.stream()
                        .filter(p -> p.getTargetLogicalRound().getId().equals(r2.getId()))
                        .map(LogicalRoundPromotion::getTeam).map(Team::getId).collect(Collectors.toCollection(LinkedHashSet::new))))
                .limit(1).toList();
        // Assign all Round 1 promotions into Round 2's first execution.
        integrity.assignManual(r2.getId(), new ManualAssignmentRequest(roundTwoAssignments));
        assertEquals(15, participantStore.stream().filter(p -> p.getRound().getLogicalRound().getId().equals(r2.getId())).count());

        List<Team> finalQualified = promotionStore.stream().limit(4).map(LogicalRoundPromotion::getTeam).toList();
        for (int i = 0; i < r2Executions.size(); i++) {
            final int executionIndex = i;
            RoundResultVersion version = publishedVersion(r2Executions.get(i));
            List<RoundResultVersionEntry> resultEntries = finalQualified.stream()
                    .filter(team -> (team.getName().hashCode() & 1) == executionIndex)
                    .map(team -> entry(version, team, 1, "promoted")).toList();
            entriesByVersion.put(version.getId(), resultEntries);
        }
        lifecycle.advance(r2Executions.get(0));

        List<?> finalUnassigned = integrity.promotedUnassigned(finalRound.getId());
        assertEquals(4, finalUnassigned.size());
        assertTrue(((List<?>) finalUnassigned).stream().allMatch(item ->
                ((vn.edu.fpt.seal.modules.round.dto.LogicalRoundProgressionDtos.PromotedTeam) item)
                        .sourceLogicalRoundId().equals(r2.getId())));
        Set<UUID> roundOneOnly = new HashSet<>(roundOneTeams.keySet());
        roundOneOnly.removeAll(finalQualified.stream().map(Team::getId).collect(Collectors.toSet()));
        assertThrows(ApiException.class, () -> integrity.assignManual(finalRound.getId(),
                new ManualAssignmentRequest(List.of(new Assignment(finalExecution.getId(), roundOneOnly)))));

        integrity.assignManual(finalRound.getId(), new ManualAssignmentRequest(List.of(
                new Assignment(finalExecution.getId(), finalQualified.stream().map(Team::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new))))));
        assertEquals(4, participantStore.stream()
                .filter(p -> p.getRound().getLogicalRound().getId().equals(finalRound.getId())).count());
    }

    private RoundDefinition definition(String name, int sequence) {
        RoundDefinition definition = RoundDefinition.builder().event(event).name(name)
                .sequenceNumber(sequence).build();
        definition.setId(UUID.randomUUID());
        return definition;
    }

    private List<Round> executions(RoundDefinition definition, int count, String prefix) {
        List<Round> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Track track = Track.builder().event(event).name(prefix + " Track " + i).build();
            track.setId(UUID.randomUUID());
            Round round = Round.builder().logicalRound(definition).track(track).name(definition.getName())
                    .sequenceNumber(definition.getSequenceNumber()).lifecycleState(RoundLifecycleState.READY_TO_ADVANCE)
                    .build();
            round.setId(UUID.randomUUID());
            result.add(round);
        }
        return result;
    }

    private RoundResultVersion publishedVersion(Round round) {
        RoundResultVersion version = RoundResultVersion.builder().round(round).versionNumber(1)
                .status("published").build();
        version.setId(UUID.randomUUID());
        publishedByRound.put(round.getId(), version);
        return version;
    }

    private RoundResultVersionEntry entry(RoundResultVersion version, Team team, int rank, String status) {
        RoundResultVersionEntry entry = RoundResultVersionEntry.builder().resultVersion(version)
                .team(team).rank(rank).promotionStatus(status).build();
        entry.setId(UUID.randomUUID());
        return entry;
    }

    private Team team(String name, Track track) {
        Team team = Team.builder().name(name).track(track).build();
        team.setId(UUID.randomUUID());
        return team;
    }
}
