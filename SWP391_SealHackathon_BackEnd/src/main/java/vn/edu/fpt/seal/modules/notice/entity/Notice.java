package vn.edu.fpt.seal.modules.notice.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.fpt.seal.common.entity.BaseEntity;
import vn.edu.fpt.seal.modules.user.entity.User;

@Entity
@Table(name = "notices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notice extends BaseEntity {
    @Column(name = "title", nullable = false, length = 255) private String title;
    @Column(name = "content", nullable = false, columnDefinition = "text") private String content;
    @Column(name = "priority", nullable = false, length = 20) @Builder.Default private String priority = "normal";
    @Column(name = "target_role", length = 100) private String targetRole;
    @Column(name = "target_event_id", columnDefinition = "uuid") private java.util.UUID targetEventId;
    @Column(name = "target_track_id", columnDefinition = "uuid") private java.util.UUID targetTrackId;
    @Column(name = "target_team_id", columnDefinition = "uuid") private java.util.UUID targetTeamId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_id", nullable = false) private User author;
}
