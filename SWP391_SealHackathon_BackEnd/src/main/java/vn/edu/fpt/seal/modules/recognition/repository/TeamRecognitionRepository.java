package vn.edu.fpt.seal.modules.recognition.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.seal.modules.recognition.entity.TeamRecognition;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRecognitionRepository extends JpaRepository<TeamRecognition, UUID> {
    @EntityGraph(attributePaths = {"teamProfile"})
    Optional<TeamRecognition> findFirstByTeamProfileIdAndRecognitionCodeOrderByCreatedAtDesc(
            UUID teamProfileId, String recognitionCode);

    @EntityGraph(attributePaths = {"teamProfile"})
    List<TeamRecognition> findByTeamProfileIdInAndActiveTrue(Collection<UUID> teamProfileIds);

    @Query("""
            select t.id, r
              from Team t
              join TeamRecognition r on r.teamProfile.id = t.teamProfile.id
             where t.id in :teamIds
               and r.active = true
            """)
    List<Object[]> findActiveRowsByTeamIds(@Param("teamIds") Collection<UUID> teamIds);
}
