package vn.edu.fpt.seal.modules.round.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.round.entity.Round;

import java.util.UUID;

@Repository
public interface RoundRepository extends JpaRepository<Round, UUID> {

    Page<Round> findByTrackId(UUID trackId, Pageable pageable);
    Page<Round> findByTrackEventId(UUID eventId, Pageable pageable);

    boolean existsByTrackIdAndNameIgnoreCase(UUID trackId, String name);

    boolean existsByTrackIdAndSequenceNumber(UUID trackId, Integer sequenceNumber);

    java.util.Optional<Round> findTopByTrackIdOrderBySequenceNumberDesc(UUID trackId);
    java.util.Optional<Round> findByTrackIdAndSequenceNumber(UUID trackId, Integer sequenceNumber);
    java.util.List<Round> findByLogicalRoundId(UUID logicalRoundId);

    long countByTrackEventId(UUID eventId);
}
