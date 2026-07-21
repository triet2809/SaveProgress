package vn.edu.fpt.seal.modules.track.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.track.entity.Track;

import java.util.UUID;

@Repository
public interface TrackRepository extends JpaRepository<Track, UUID> {

    Page<Track> findByEventId(UUID eventId, Pageable pageable);

    boolean existsByEventIdAndNameIgnoreCase(UUID eventId, String name);

    long countByEventId(UUID eventId);
}
