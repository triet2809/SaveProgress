package vn.edu.fpt.seal.modules.resultversion.repository;
import org.springframework.data.jpa.repository.JpaRepository; import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersionEntry; import java.util.*;
public interface RoundResultVersionEntryRepository extends JpaRepository<RoundResultVersionEntry,UUID>{
 List<RoundResultVersionEntry> findByResultVersionId(UUID resultVersionId);
 boolean existsByTeamId(UUID teamId);
}
