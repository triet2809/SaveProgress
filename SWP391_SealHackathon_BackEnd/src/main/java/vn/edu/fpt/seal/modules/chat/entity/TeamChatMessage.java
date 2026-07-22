package vn.edu.fpt.seal.modules.chat.entity;
import jakarta.persistence.*; import lombok.*; import vn.edu.fpt.seal.common.entity.BaseEntity; import vn.edu.fpt.seal.modules.team.entity.Team; import vn.edu.fpt.seal.modules.user.entity.User;
/**
 * Thực thể TeamChatMessage — tin nhắn trong khung chat nội bộ của một đội.
 * Kế thừa BaseEntity (id, createdAt...). Ánh xạ tới bảng "team_chat_messages".
 */
@Entity @Table(name="team_chat_messages") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder public class TeamChatMessage extends BaseEntity{ /** Đội sở hữu khung chat. */ @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="team_id") private Team team; /** Người gửi tin nhắn. */ @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sender_id") private User sender; /** Nội dung tin nhắn. */ @Column(nullable=false,columnDefinition="text") private String message; }
