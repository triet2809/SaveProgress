package vn.edu.fpt.seal.modules.appeal.repository;
import org.springframework.data.jpa.repository.*; import org.springframework.stereotype.Repository; import vn.edu.fpt.seal.modules.appeal.entity.Appeal; import java.util.*;
@Repository public interface AppealRepository extends JpaRepository<Appeal,UUID>{
 boolean existsByTeamIdAndResultVersionId(UUID teamId,UUID versionId);
 boolean existsByRoundIdAndStatus(UUID roundId,String status);
 boolean existsByRoundId(UUID roundId);
 List<Appeal> findByEventIdOrderByCreatedAtDesc(UUID eventId);
 List<Appeal> findByTeamIdOrderByCreatedAtDesc(UUID teamId);
 @EntityGraph(attributePaths={"event","round","team","submittedBy","resolvedBy","resultVersion"}) Optional<Appeal> findWithRelationsById(UUID id);
}
