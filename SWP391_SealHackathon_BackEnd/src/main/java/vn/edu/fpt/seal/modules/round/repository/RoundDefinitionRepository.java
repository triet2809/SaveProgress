package vn.edu.fpt.seal.modules.round.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.seal.modules.round.entity.RoundDefinition;

import java.util.Optional;
import java.util.UUID;

public interface RoundDefinitionRepository extends JpaRepository<RoundDefinition, UUID> {
    boolean existsByEventIdAndNameIgnoreCase(UUID eventId, String name);

    boolean existsByEventIdAndSequenceNumber(UUID eventId, Integer sequenceNumber);

    Optional<RoundDefinition> findByEventIdAndNameIgnoreCaseAndSequenceNumber(
            UUID eventId, String name, Integer sequenceNumber);

    Optional<RoundDefinition> findByEventIdAndNameIgnoreCase(UUID eventId, String name);

    Optional<RoundDefinition> findTopByEventIdOrderBySequenceNumberDesc(UUID eventId);

    Optional<RoundDefinition> findByEventIdAndSequenceNumber(UUID eventId, Integer sequenceNumber);

    java.util.List<RoundDefinition> findByEventIdOrderBySequenceNumberAsc(UUID eventId);
}
