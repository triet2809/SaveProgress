package vn.edu.fpt.seal.modules.round;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.seal.common.enums.RoundLifecycleState;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.appeal.repository.AppealRepository;
import vn.edu.fpt.seal.modules.criteria.repository.RoundCriterionRepository;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.judge.repository.RoundJudgeRepository;
import vn.edu.fpt.seal.modules.participant.repository.RoundParticipantRepository;
import vn.edu.fpt.seal.modules.ranking.repository.RoundRankingRepository;
import vn.edu.fpt.seal.modules.resultversion.repository.RoundResultVersionRepository;
import vn.edu.fpt.seal.modules.resultversion.repository.RoundResultVersionEntryRepository;
import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersion;
import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersionEntry;
import vn.edu.fpt.seal.modules.round.dto.LogicalRoundProgressionDtos.*;
import vn.edu.fpt.seal.modules.round.dto.UpdateLogicalRoundRequest;
import vn.edu.fpt.seal.modules.round.entity.*;
import vn.edu.fpt.seal.modules.round.repository.*;
import vn.edu.fpt.seal.modules.round.service.LogicalRoundIntegrityService;
import vn.edu.fpt.seal.modules.submission.repository.SubmissionRepository;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.track.entity.Track;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogicalRoundIntegrityServiceTest {
    @Mock RoundDefinitionRepository definitions;
    @Mock RoundRepository rounds;
    @Mock LogicalRoundPromotionRepository promotions;
    @Mock RoundParticipantRepository participants;
    @Mock SubmissionRepository submissions;
    @Mock RoundRankingRepository rankings;
    @Mock RoundResultVersionRepository resultVersions;
    @Mock RoundResultVersionEntryRepository resultVersionEntries;
    @Mock AppealRepository appeals;
    @Mock RoundCriterionRepository criteria;
    @Mock RoundJudgeRepository roundJudges;

    private LogicalRoundIntegrityService service;
    private RoundDefinition definition;
    private Round execution;

    @BeforeEach
    void setUp() {
        service = new LogicalRoundIntegrityService(definitions, rounds, promotions, participants,
                submissions, rankings, resultVersions, resultVersionEntries, appeals, criteria, roundJudges);
        Event event = Event.builder().title("Event").build();
        event.setId(UUID.randomUUID());
        definition = RoundDefinition.builder().event(event).name("Qualifier").sequenceNumber(1).build();
        definition.setId(UUID.randomUUID());
        Track track = Track.builder().event(event).name("AI").build();
        track.setId(UUID.randomUUID());
        execution = Round.builder().logicalRound(definition).track(track).name("Qualifier")
                .sequenceNumber(1).topNToPromote(2).lifecycleState(RoundLifecycleState.SCORING).build();
        execution.setId(UUID.randomUUID());
    }

    @Test
    void sharedUpdateChangesDefinitionAndEveryExecution() {
        Round second = Round.builder().logicalRound(definition).track(execution.getTrack())
                .name("Qualifier").sequenceNumber(1).topNToPromote(1).build();
        second.setId(UUID.randomUUID());
        when(definitions.findById(definition.getId())).thenReturn(Optional.of(definition));
        when(rounds.findByLogicalRoundId(definition.getId())).thenReturn(List.of(execution, second));
        when(definitions.findByEventIdAndNameIgnoreCase(any(), eq("Semifinal"))).thenReturn(Optional.empty());
        when(definitions.findByEventIdAndSequenceNumber(any(), eq(2))).thenReturn(Optional.empty());

        service.update(definition.getId(), new UpdateLogicalRoundRequest("Semifinal", 2, false, 3));

        assertEquals("Semifinal", definition.getName());
        assertEquals(2, execution.getSequenceNumber());
        assertEquals("Semifinal", second.getName());
        assertEquals(3, definition.getDefaultTopNToPromote());
        verify(definitions).flush();
    }

    @Test
    void duplicateEventSequenceIsRejectedAsConflict() {
        RoundDefinition duplicate = RoundDefinition.builder().event(definition.getEvent())
                .name("Other").sequenceNumber(2).build();
        duplicate.setId(UUID.randomUUID());
        when(definitions.findById(definition.getId())).thenReturn(Optional.of(definition));
        when(rounds.findByLogicalRoundId(definition.getId())).thenReturn(List.of(execution));
        when(definitions.findByEventIdAndNameIgnoreCase(any(), any())).thenReturn(Optional.empty());
        when(definitions.findByEventIdAndSequenceNumber(any(), eq(2))).thenReturn(Optional.of(duplicate));

        ApiException error = assertThrows(ApiException.class, () -> service.update(
                definition.getId(), new UpdateLogicalRoundRequest(null, 2, null, null)));

        assertEquals(409, error.getStatus().value());
        verify(definitions, never()).flush();
    }

    @Test
    void sequenceChangeIsRejectedAfterPromotionDependencyExists() {
        when(definitions.findById(definition.getId())).thenReturn(Optional.of(definition));
        when(rounds.findByLogicalRoundId(definition.getId())).thenReturn(List.of(execution));
        when(definitions.findByEventIdAndNameIgnoreCase(any(), any())).thenReturn(Optional.empty());
        when(definitions.findByEventIdAndSequenceNumber(any(), eq(2))).thenReturn(Optional.empty());
        when(promotions.existsBySourceLogicalRoundId(definition.getId())).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> service.update(
                definition.getId(), new UpdateLogicalRoundRequest(null, 2, null, null)));

        assertEquals(409, error.getStatus().value());
        verify(definitions, never()).flush();
    }

    @Test
    void deleteExecutionWithAssignedTeamIsRejectedWithoutMutation() {
        when(rounds.findById(execution.getId())).thenReturn(Optional.of(execution));
        when(participants.existsByRoundId(execution.getId())).thenReturn(true);

        assertThrows(ApiException.class, () -> service.deleteExecution(execution.getId()));

        verify(rounds, never()).delete(any());
    }

    @Test
    void deletingLastSafeExecutionAlsoDeletesEmptyDefinition() {
        when(rounds.findById(execution.getId())).thenReturn(Optional.of(execution));
        when(rounds.findByLogicalRoundId(definition.getId())).thenReturn(List.of());

        service.deleteExecution(execution.getId());

        verify(rounds).delete(execution);
        verify(rounds).flush();
        verify(definitions).delete(definition);
    }

    @Test
    void wholeLogicalDeleteChecksEveryExecutionBeforeDeletingAny() {
        Round unsafe = Round.builder().logicalRound(definition).track(track("Unsafe"))
                .name("Qualifier").sequenceNumber(1)
                .lifecycleState(RoundLifecycleState.SCORING).build();
        unsafe.setId(UUID.randomUUID());
        when(definitions.findById(definition.getId())).thenReturn(Optional.of(definition));
        when(rounds.findByLogicalRoundId(definition.getId())).thenReturn(List.of(execution, unsafe));
        when(submissions.existsByRoundId(any())).thenAnswer(
                invocation -> invocation.getArgument(0, UUID.class).equals(unsafe.getId()));

        assertThrows(ApiException.class, () -> service.deleteLogical(definition.getId()));

        verify(rounds, never()).deleteAll(any());
        verify(definitions, never()).delete(any());
    }

    @Test
    void nonPromotedTeamCannotBeAssigned() {
        when(definitions.findById(definition.getId())).thenReturn(Optional.of(definition));
        when(rounds.findByLogicalRoundId(definition.getId())).thenReturn(List.of(execution));
        when(promotions.findByTargetLogicalRoundIdOrderByTeamNameAsc(definition.getId()))
                .thenReturn(List.of());
        UUID teamId = UUID.randomUUID();

        assertThrows(ApiException.class, () -> service.assignManual(definition.getId(),
                new ManualAssignmentRequest(List.of(new Assignment(execution.getId(), Set.of(teamId))))));

        verify(participants, never()).save(any());
    }

    @Test
    void finalAssignmentRequiresEveryPromotedTeamInSingleOperation() {
        definition.setFinalRound(true);
        Team a = team("A");
        Team b = team("B");
        when(definitions.findById(definition.getId())).thenReturn(Optional.of(definition));
        when(rounds.findByLogicalRoundId(definition.getId())).thenReturn(List.of(execution));
        when(promotions.findByTargetLogicalRoundIdOrderByTeamNameAsc(definition.getId()))
                .thenReturn(List.of(promotion(a), promotion(b)));

        assertThrows(ApiException.class, () -> service.assignManual(definition.getId(),
                new ManualAssignmentRequest(List.of(
                        new Assignment(execution.getId(), Set.of(a.getId()))))));

        verify(participants, never()).save(any());
    }

    @Test
    void assignmentRejectsPromotionWithoutProvenance() {
        Team a = team("A");
        LogicalRoundPromotion malformed = LogicalRoundPromotion.builder()
                .sourceLogicalRound(definition).targetLogicalRound(definition).team(a).build();
        when(definitions.findById(definition.getId())).thenReturn(Optional.of(definition));
        when(rounds.findByLogicalRoundId(definition.getId())).thenReturn(List.of(execution));
        when(promotions.findByTargetLogicalRoundIdOrderByTeamNameAsc(definition.getId()))
                .thenReturn(List.of(malformed));

        assertThrows(ApiException.class, () -> service.assignManual(definition.getId(),
                new ManualAssignmentRequest(List.of(new Assignment(execution.getId(), Set.of(a.getId()))))));
        verify(participants, never()).save(any());
    }

    @Test
    void assignmentRejectsEntryForAnotherTeamOrNonPromotedStatus() {
        Team promoted = team("Promoted");
        Team other = team("Other");
        RoundResultVersion version = RoundResultVersion.builder().round(execution)
                .versionNumber(1).status("published").build();
        version.setId(UUID.randomUUID());
        RoundResultVersionEntry entry = RoundResultVersionEntry.builder()
                .resultVersion(version).team(other).promotionStatus("not_promoted").build();
        entry.setId(UUID.randomUUID());
        LogicalRoundPromotion promotion = promotion(promoted);
        promotion.setSourceResultVersionId(version.getId());
        promotion.setSourceResultEntryId(entry.getId());
        when(definitions.findById(definition.getId())).thenReturn(Optional.of(definition));
        when(rounds.findByLogicalRoundId(definition.getId())).thenReturn(List.of(execution));
        when(promotions.findByTargetLogicalRoundIdOrderByTeamNameAsc(definition.getId()))
                .thenReturn(List.of(promotion));
        when(resultVersions.findById(version.getId())).thenReturn(Optional.of(version));
        when(resultVersions.findByRoundIdAndStatus(execution.getId(), "published"))
                .thenReturn(Optional.of(version));
        when(resultVersionEntries.findById(entry.getId())).thenReturn(Optional.of(entry));

        assertThrows(ApiException.class, () -> service.assignManual(definition.getId(),
                new ManualAssignmentRequest(List.of(new Assignment(execution.getId(), Set.of(promoted.getId()))))));
        verify(participants, never()).save(any());
    }

    @Test
    void assignmentRejectsInactivePublishedVersion() {
        Team promoted = team("Promoted");
        RoundResultVersion version = RoundResultVersion.builder().round(execution)
                .versionNumber(1).status("superseded").build();
        version.setId(UUID.randomUUID());
        RoundResultVersionEntry entry = RoundResultVersionEntry.builder()
                .resultVersion(version).team(promoted).promotionStatus("promoted").build();
        entry.setId(UUID.randomUUID());
        LogicalRoundPromotion promotion = promotion(promoted);
        promotion.setSourceResultVersionId(version.getId());
        promotion.setSourceResultEntryId(entry.getId());
        when(definitions.findById(definition.getId())).thenReturn(Optional.of(definition));
        when(rounds.findByLogicalRoundId(definition.getId())).thenReturn(List.of(execution));
        when(promotions.findByTargetLogicalRoundIdOrderByTeamNameAsc(definition.getId()))
                .thenReturn(List.of(promotion));
        when(resultVersions.findById(version.getId())).thenReturn(Optional.of(version));

        assertThrows(ApiException.class, () -> service.assignManual(definition.getId(),
                new ManualAssignmentRequest(List.of(new Assignment(execution.getId(), Set.of(promoted.getId()))))));
        verify(participants, never()).save(any());
    }

    @Test
    void deterministicBalanceCountsExistingAssignmentsAndAssignsEveryPromotionOnce() {
        Round second = Round.builder().logicalRound(definition).track(track("Web"))
                .name("Qualifier").sequenceNumber(1).topNToPromote(2).build();
        second.setId(UUID.randomUUID());
        Team a = team("A");
        Team b = team("B");
        when(definitions.findById(definition.getId())).thenReturn(Optional.of(definition));
        when(rounds.findByLogicalRoundId(definition.getId())).thenReturn(List.of(execution, second));
        when(promotions.findByTargetLogicalRoundIdOrderByTeamNameAsc(definition.getId()))
                .thenReturn(List.of(promotion(a), promotion(b)));
        when(participants.countByRoundId(execution.getId())).thenReturn(1L);
        when(participants.countByRoundId(second.getId())).thenReturn(0L);

        BalancePreview first = service.previewBalanced(definition.getId(), new BalanceRequest(42L));
        BalancePreview retry = service.previewBalanced(definition.getId(), new BalanceRequest(42L));

        assertEquals(first.assignments(), retry.assignments());
        assertEquals(3, first.projectedCounts().values().stream().mapToLong(Long::longValue).sum());
        assertEquals(2, first.assignments().stream().mapToInt(plan -> plan.teamIds().size()).sum());
    }

    private Track track(String name) {
        Track track = Track.builder().event(definition.getEvent()).name(name).build();
        track.setId(UUID.randomUUID());
        return track;
    }

    private Team team(String name) {
        Team team = Team.builder().track(execution.getTrack()).name(name).build();
        team.setId(UUID.randomUUID());
        return team;
    }

    private LogicalRoundPromotion promotion(Team team) {
        return LogicalRoundPromotion.builder()
                .sourceLogicalRound(RoundDefinition.builder().event(definition.getEvent())
                        .name("Prior").sequenceNumber(0).build())
                .targetLogicalRound(definition).team(team)
                .sourceResultVersionId(UUID.randomUUID()).sourceResultEntryId(UUID.randomUUID()).build();
    }
}
