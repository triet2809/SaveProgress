package vn.edu.fpt.seal.modules.feedback.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.feedback.entity.MentorFeedback;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MentorFeedbackRepository extends JpaRepository<MentorFeedback, UUID> {
    @EntityGraph(attributePaths = {"trackMentor", "trackMentor.user", "trackMentor.track", "team", "round"})
    Page<MentorFeedback> findByTrackMentorId(UUID id, Pageable p);

    @EntityGraph(attributePaths = {"trackMentor", "trackMentor.user", "trackMentor.track", "team", "round"})
    Page<MentorFeedback> findByTeamId(UUID id, Pageable p);

    @EntityGraph(attributePaths = {"trackMentor", "trackMentor.user", "trackMentor.track", "team", "round"})
    Page<MentorFeedback> findByRoundId(UUID id, Pageable p);

    @EntityGraph(attributePaths = {"trackMentor", "trackMentor.user", "trackMentor.track", "team", "round"})
    Optional<MentorFeedback> findWithRelationsById(UUID id);
}
