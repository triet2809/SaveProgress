package vn.edu.fpt.seal.modules.judge.repository;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.judge.entity.TrackJudge;

import java.util.*;

@Repository
public interface TrackJudgeRepository extends JpaRepository<TrackJudge, UUID> {
    @EntityGraph(attributePaths = {"event", "track", "user"}) Page<TrackJudge> findByTrackId(UUID trackId, Pageable pageable);
    @EntityGraph(attributePaths = {"event", "track", "user"}) Page<TrackJudge> findByUserId(UUID userId, Pageable pageable);
    @EntityGraph(attributePaths = {"event", "track", "user"}) List<TrackJudge> findAllByEventId(UUID eventId);
    boolean existsByTrackIdAndUserId(UUID trackId, UUID userId);
    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);
    Optional<TrackJudge> findByTrackIdAndUserId(UUID trackId, UUID userId);
}
