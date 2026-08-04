package vn.edu.fpt.seal.modules.chat.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.chat.entity.TeamChatMessage;

import java.util.List;
import java.util.UUID;

/**
 * Repository truy xuất tin nhắn chat đội (TeamChatMessage).
 */
@Repository
public interface TeamChatMessageRepository extends JpaRepository<TeamChatMessage, UUID> {
    /**
     * Lấy 100 tin nhắn cũ nhất đầu tiên của đội, sắp xếp tăng dần theo thời gian; nạp sẵn sender/team.
     */
    @EntityGraph(attributePaths = {"sender", "team"})
    List<TeamChatMessage> findTop100ByTeamIdOrderByCreatedAtAsc(UUID teamId);
}
