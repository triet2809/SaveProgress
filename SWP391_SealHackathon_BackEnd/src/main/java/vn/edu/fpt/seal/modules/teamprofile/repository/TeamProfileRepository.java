package vn.edu.fpt.seal.modules.teamprofile.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.teamprofile.entity.TeamProfile;

import java.util.*;

@Repository
public interface TeamProfileRepository extends JpaRepository<TeamProfile, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TeamProfile p where p.id = :id")
    Optional<TeamProfile> findByIdForUpdate(@Param("id") UUID id);
}
