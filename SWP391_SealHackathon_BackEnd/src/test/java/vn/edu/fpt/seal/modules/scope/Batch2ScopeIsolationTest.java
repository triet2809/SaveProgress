package vn.edu.fpt.seal.modules.scope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.criteria.repository.RoundCriterionRepository;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.ranking.repository.RoundRankingRepository;
import vn.edu.fpt.seal.modules.ranking.service.RoundRankingService;
import vn.edu.fpt.seal.modules.report.service.ReportService;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;
import vn.edu.fpt.seal.modules.score.repository.ScoreRepository;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.security.AuthorizationService;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Batch2ScopeIsolationTest {
    @Mock ScoreRepository scores;
    @Mock RoundRankingRepository rankings;
    @Mock RoundRepository rounds;
    @Mock TeamRepository teams;
    @Mock RoundCriterionRepository criteria;
    @Mock AuthorizationService authorization;

    @Test
    void analyticsRejectsRoundFromAnotherEventBeforeQueryingScores() {
        UUID roundId = UUID.randomUUID();
        when(rounds.findById(roundId)).thenReturn(Optional.of(round()));
        ReportService service = new ReportService(scores, rankings, rounds);
        assertThrows(ApiException.class, () -> service.judgeVariance(UUID.randomUUID(), roundId, null));
        verifyNoInteractions(scores);
    }

    @Test
    void analyticsRejectsCrossTrackFilterBeforeQueryingScores() {
        Round round = round();
        when(rounds.findById(round.getId())).thenReturn(Optional.of(round));
        ReportService service = new ReportService(scores, rankings, rounds);
        assertThrows(ApiException.class, () -> service.judgeVariance(
                round.getTrack().getEvent().getId(), round.getId(), UUID.randomUUID()));
        verifyNoInteractions(scores);
    }

    @Test
    void analyticsAllTracksUsesOnlyTheValidatedRoundQuery() {
        Round round = round();
        when(rounds.findById(round.getId())).thenReturn(Optional.of(round));
        when(scores.findRoundScoreDetails(round.getId(), null)).thenReturn(java.util.List.of());
        ReportService service = new ReportService(scores, rankings, rounds);
        service.judgeVariance(round.getTrack().getEvent().getId(), round.getId(), null);
        verify(scores).findRoundScoreDetails(round.getId(), null);
        verifyNoMoreInteractions(scores);
    }

    @Test
    void rankingRejectsCrossEventAndTrack() {
        Round round = round();
        when(rounds.findById(round.getId())).thenReturn(Optional.of(round));
        RoundRankingService service = new RoundRankingService(rankings, rounds, teams, criteria, authorization);
        assertThrows(ApiException.class, () -> service.list(UUID.randomUUID(), round.getId(), null,
                org.springframework.data.domain.Pageable.unpaged(), null));
        assertThrows(ApiException.class, () -> service.list(round.getTrack().getEvent().getId(), round.getId(),
                UUID.randomUUID(), org.springframework.data.domain.Pageable.unpaged(), null));
        verifyNoInteractions(rankings);
    }

    private static Round round() {
        Event event = Event.builder().title("Event").build();
        event.setId(UUID.randomUUID());
        Track track = Track.builder().event(event).name("Track").build();
        track.setId(UUID.randomUUID());
        Round round = Round.builder().track(track).name("Round").build();
        round.setId(UUID.randomUUID());
        return round;
    }
}
