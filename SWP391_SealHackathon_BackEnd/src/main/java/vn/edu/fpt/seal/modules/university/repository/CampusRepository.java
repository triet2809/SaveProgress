package vn.edu.fpt.seal.modules.university.repository;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.university.entity.Campus;

import java.util.UUID;

@Repository
public interface CampusRepository extends JpaRepository<Campus, UUID> {
    @EntityGraph(attributePaths = {"university"}) Page<Campus> findByUniversityId(UUID universityId, Pageable pageable);
    @EntityGraph(attributePaths = {"university"}) java.util.Optional<Campus> findWithUniversityById(UUID id);
    boolean existsByUniversityIdAndNameIgnoreCase(UUID universityId, String name);
}
