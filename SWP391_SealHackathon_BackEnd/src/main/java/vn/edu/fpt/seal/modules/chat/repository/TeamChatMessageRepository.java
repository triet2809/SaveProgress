package vn.edu.fpt.seal.modules.chat.repository; import org.springframework.data.jpa.repository.*; import org.springframework.stereotype.Repository; import vn.edu.fpt.seal.modules.chat.entity.TeamChatMessage; import java.util.*; /**
 * Repository truy xuất tin nhắn chat đội (TeamChatMessage).
 */
@Repository public interface TeamChatMessageRepository extends JpaRepository<TeamChatMessage,UUID>{ /** Lấy 100 tin nhắn cũ nhất đầu tiên của đội, sắp xếp tăng dần theo thời gian; nạp sẵn sender/team. */ @EntityGraph(attributePaths={"sender","team"}) List<TeamChatMessage> findTop100ByTeamIdOrderByCreatedAtAsc(UUID teamId); }
