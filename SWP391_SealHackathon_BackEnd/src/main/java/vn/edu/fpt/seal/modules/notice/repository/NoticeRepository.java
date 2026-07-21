package vn.edu.fpt.seal.modules.notice.repository;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.notice.entity.Notice;
import java.util.UUID;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, UUID> {
    @EntityGraph(attributePaths = {"author"})
    @Query("""
            select n from Notice n
            where (cast(:targetRole as string) is null or n.targetRole is null or lower(n.targetRole) = lower(cast(:targetRole as string)))
              and (:eventId is null or n.targetEventId is null or n.targetEventId = :eventId)
              and (:trackId is null or n.targetTrackId is null or n.targetTrackId = :trackId)
            order by n.createdAt desc
            """)
    Page<Notice> search(@Param("targetRole") String targetRole, @Param("eventId") UUID eventId, @Param("trackId") UUID trackId, Pageable pageable);
}
