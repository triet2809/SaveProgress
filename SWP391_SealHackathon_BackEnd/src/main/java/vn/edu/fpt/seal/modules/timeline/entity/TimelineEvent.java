package vn.edu.fpt.seal.modules.timeline.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.track.entity.Track;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "team_timeline_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimelineEvent {
    @Id @GeneratedValue @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "event_id")
    private Event event;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "team_id")
    private Team team;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "round_id")
    private Round round;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "track_id")
    private Track track;
    @Column(name = "type", nullable = false, length = 40)
    private String type;
    @Column(name = "event_type", nullable = false, length = 64)
    @Enumerated(EnumType.STRING)
    private vn.edu.fpt.seal.modules.timeline.TimelineEventType eventType;
    @Enumerated(EnumType.STRING) @Column(name = "visibility_scope", nullable = false, length = 32)
    private vn.edu.fpt.seal.modules.timeline.TimelineScope visibilityScope;
    @Column(nullable = false, length = 255) private String title;
    @Column(columnDefinition = "text") private String description;
    @Column(name = "score_snapshot") private java.math.BigDecimal scoreSnapshot;
    @Column(name = "rank_snapshot") private Integer rankSnapshot;
    @Column(name = "status_snapshot", length = 40) private String statusSnapshot;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(columnDefinition = "jsonb") private String metadata;
    @Enumerated(EnumType.STRING) @Column(name = "source_type", length = 64)
    private vn.edu.fpt.seal.modules.timeline.TimelineSourceType sourceType;
    @Column(name = "source_id") private UUID sourceId;
    @Column(name = "idempotency_key", length = 255) private String idempotencyKey;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
}
