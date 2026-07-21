package vn.edu.fpt.seal.modules.mentor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "track_mentors", uniqueConstraints = {
        @UniqueConstraint(name = "uq_track_mentors_track_user", columnNames = {"track_id", "user_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrackMentor {
    @Id @GeneratedValue @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid") private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "event_id", nullable = false) private Event event;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "track_id", nullable = false) private Track track;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @CreationTimestamp @Column(name = "assigned_at", nullable = false, updatable = false) private LocalDateTime assignedAt;
}
