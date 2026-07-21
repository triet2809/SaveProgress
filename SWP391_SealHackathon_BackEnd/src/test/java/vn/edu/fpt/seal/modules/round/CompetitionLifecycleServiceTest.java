package vn.edu.fpt.seal.modules.round;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.fpt.seal.common.enums.RoundLifecycleState;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.appeal.repository.AppealRepository;
import vn.edu.fpt.seal.modules.participant.repository.RoundParticipantRepository;
import vn.edu.fpt.seal.modules.ranking.repository.RoundRankingRepository;
import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersion;
import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersionEntry;
import vn.edu.fpt.seal.modules.resultversion.repository.RoundResultVersionEntryRepository;
import vn.edu.fpt.seal.modules.resultversion.repository.RoundResultVersionRepository;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.entity.RoundDefinition;
import vn.edu.fpt.seal.modules.round.entity.LogicalRoundPromotion;
import vn.edu.fpt.seal.modules.round.repository.LogicalRoundPromotionRepository;
import vn.edu.fpt.seal.modules.round.repository.RoundDefinitionRepository;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.round.service.CompetitionLifecycleService;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.team.entity.Team;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetitionLifecycleServiceTest {
    @Mock RoundRepository rounds;
    @Mock AppealRepository appeals;
    @Mock RoundResultVersionRepository versions;
    @Mock RoundResultVersionEntryRepository entries;
    @Mock RoundRankingRepository rankings;
    @Mock RoundParticipantRepository participants;
    @Mock LogicalRoundPromotionRepository promotions;
    @Mock RoundDefinitionRepository definitions;

    private Clock clock;
    private CompetitionLifecycleService service;
    private Round round;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-07-19T10:00:00Z"), ZoneOffset.UTC);
        service = new CompetitionLifecycleService(rounds, appeals, versions, entries, rankings, participants, clock);
        ReflectionTestUtils.setField(service, "promotions", promotions);
        ReflectionTestUtils.setField(service, "definitions", definitions);
        Track track = Track.builder().name("General").build();
        track.setId(UUID.randomUUID());
        round = Round.builder().track(track).name("Qualifier").sequenceNumber(1)
                .lifecycleState(RoundLifecycleState.SCORING).build();
        round.setId(UUID.randomUUID());
    }

    @Test
    void publicationCreatesImmutableVersionWithFifteenMinuteServerDeadline() {
        when(versions.findByRoundIdAndStatus(round.getId(), "published")).thenReturn(Optional.empty());
        when(versions.findTopByRoundIdOrderByVersionNumberDesc(round.getId())).thenReturn(Optional.empty());
        when(versions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(rankings.findByRoundId(eq(round.getId()), any(Pageable.class))).thenReturn(Page.empty());

        RoundResultVersion version = service.publish(round, null, "Initial publication");

        assertEquals(1, version.getVersionNumber());
        assertEquals(LocalDateTime.of(2026, 7, 19, 10, 0), version.getPublishedAt());
        assertEquals(version.getPublishedAt().plusMinutes(15), version.getAppealDeadline());
        assertEquals(RoundLifecycleState.APPEAL_WINDOW_OPEN, round.getLifecycleState());
    }

    @Test
    void republicationSupersedesPriorVersionAndIncrementsVersionNumber() {
        RoundResultVersion prior = RoundResultVersion.builder().round(round).versionNumber(2)
                .status("published").publishedAt(LocalDateTime.now(clock))
                .appealDeadline(LocalDateTime.now(clock).plusMinutes(15)).build();
        when(versions.findByRoundIdAndStatus(round.getId(), "published")).thenReturn(Optional.of(prior));
        when(versions.findTopByRoundIdOrderByVersionNumberDesc(round.getId())).thenReturn(Optional.of(prior));
        when(versions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(rankings.findByRoundId(eq(round.getId()), any(Pageable.class))).thenReturn(Page.empty());

        RoundResultVersion next = service.publish(round, null, "Correction");

        assertEquals("superseded", prior.getStatus());
        assertEquals(3, next.getVersionNumber());
        assertSame(prior, next.getSourceVersion());
    }

    @Test
    void exactDeadlineClosesWindowAndMakesNonFinalRoundReadyToAdvance() {
        round.setLifecycleState(RoundLifecycleState.APPEAL_WINDOW_OPEN);
        round.setAppealDeadline(LocalDateTime.now(clock));
        Round later = Round.builder().track(round.getTrack()).sequenceNumber(2).build();
        later.setId(UUID.randomUUID());
        when(rounds.findTopByTrackIdOrderBySequenceNumberDesc(round.getTrack().getId()))
                .thenReturn(Optional.of(later));

        service.refresh(round);

        assertEquals(RoundLifecycleState.READY_TO_ADVANCE, round.getLifecycleState());
    }

    @Test
    void pausedAppealBlocksProgression() {
        round.setLifecycleState(RoundLifecycleState.PAUSED_FOR_APPEAL);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.requireProgressionAllowed(round));

        assertEquals(409, exception.getStatus().value());
    }

    @Test
    void acceptedResultChangeAllowsControlledRecalculationOnly() {
        round.setLifecycleState(RoundLifecycleState.AWAITING_RECALCULATION);
        assertDoesNotThrow(() -> service.requireRankingRecalculationAllowed(round));

        round.setLifecycleState(RoundLifecycleState.APPEAL_WINDOW_OPEN);
        assertThrows(ApiException.class, () -> service.requireRankingRecalculationAllowed(round));
    }

    @Test
    void coordinatorResumeReleasesResolvedAppealPause() {
        round.setLifecycleState(RoundLifecycleState.PAUSED_FOR_APPEAL);
        round.setAppealDeadline(LocalDateTime.now(clock).minusNanos(1));
        when(appeals.existsByRoundIdAndStatus(round.getId(), "PENDING")).thenReturn(false);
        when(rounds.findTopByTrackIdOrderBySequenceNumberDesc(round.getTrack().getId())).thenReturn(Optional.of(round));

        service.resume(round);

        assertEquals(RoundLifecycleState.READY_FOR_AWARDS, round.getLifecycleState());
    }

    @Test
    void eventWithoutFinalRoundCannotAwardPrizes() {
        when(rounds.findByTrackEventId(any(), any(Pageable.class))).thenReturn(Page.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> service.requireAwardsAllowed(UUID.randomUUID()));

        assertEquals(409, exception.getStatus().value());
    }

    @Test
    void fiveTrackRoundMaterializesFifteenPromotionsWithoutAssigningNextRound() {
        Event event = Event.builder().title("Event").build();
        event.setId(UUID.randomUUID());
        RoundDefinition source = RoundDefinition.builder().event(event).name("Qualifier")
                .sequenceNumber(1).lifecycleState(RoundLifecycleState.READY_TO_ADVANCE).build();
        source.setId(UUID.randomUUID());
        RoundDefinition target = RoundDefinition.builder().event(event).name("Semifinal")
                .sequenceNumber(2).build();
        target.setId(UUID.randomUUID());
        List<Round> sourceExecutions = new ArrayList<>();
        for (int trackIndex = 0; trackIndex < 5; trackIndex++) {
            Track sourceTrack = Track.builder().event(event).name("Track " + trackIndex).build();
            sourceTrack.setId(UUID.randomUUID());
            Round sourceExecution = Round.builder().logicalRound(source).track(sourceTrack)
                    .name("Qualifier").sequenceNumber(1)
                    .lifecycleState(RoundLifecycleState.READY_TO_ADVANCE).build();
            sourceExecution.setId(UUID.randomUUID());
            sourceExecutions.add(sourceExecution);
            RoundResultVersion version = RoundResultVersion.builder()
                    .round(sourceExecution).versionNumber(1).status("published").build();
            version.setId(UUID.randomUUID());
            when(versions.findByRoundIdAndStatus(sourceExecution.getId(), "published"))
                    .thenReturn(Optional.of(version));
            List<RoundResultVersionEntry> promotedEntries = new ArrayList<>();
            for (int rank = 1; rank <= 3; rank++) {
                Team promoted = Team.builder().track(sourceTrack)
                        .name("Team " + trackIndex + "-" + rank).build();
                promoted.setId(UUID.randomUUID());
                RoundResultVersionEntry entry = RoundResultVersionEntry.builder().resultVersion(version)
                        .team(promoted).rank(rank).promotionStatus("promoted").build();
                entry.setId(UUID.randomUUID());
                promotedEntries.add(entry);
            }
            when(entries.findByResultVersionId(version.getId())).thenReturn(promotedEntries);
        }
        round = sourceExecutions.get(0);
        when(definitions.findByEventIdAndSequenceNumber(event.getId(), 2)).thenReturn(Optional.of(target));
        when(rounds.findByLogicalRoundId(source.getId())).thenReturn(sourceExecutions);

        service.advance(round);

        verify(promotions, times(15)).save(any(LogicalRoundPromotion.class));
        verify(promotions, atLeastOnce()).save(argThat(promotion ->
                promotion.getSourceResultVersionId() != null
                        && promotion.getSourceResultEntryId() != null));
        verifyNoInteractions(participants);
        assertEquals(RoundLifecycleState.ADVANCED, source.getLifecycleState());
        assertTrue(sourceExecutions.stream()
                .allMatch(execution -> execution.getLifecycleState() == RoundLifecycleState.ADVANCED));
    }
}
