package vn.edu.fpt.seal.modules.appeal.entity;
import jakarta.persistence.*; import lombok.*; import vn.edu.fpt.seal.modules.event.entity.Event; import vn.edu.fpt.seal.modules.round.entity.Round; import vn.edu.fpt.seal.modules.team.entity.Team; import vn.edu.fpt.seal.modules.user.entity.User; import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersion;
import java.time.LocalDateTime; import java.util.UUID;
@Entity @Table(name="appeals") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Appeal {
 @Id @GeneratedValue @Column(columnDefinition="uuid") private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="event_id") private Event event;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="round_id") private Round round;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="team_id") private Team team;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="submitted_by") private User submittedBy;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="result_version_id") private RoundResultVersion resultVersion;
 @Column(nullable=false,columnDefinition="text") private String reason;
 @Column(nullable=false,length=20) @Builder.Default private String status="PENDING";
 @Column(columnDefinition="text") private String response;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="resolved_by") private User resolvedBy;
 @Column(name="result_published_at") private LocalDateTime resultPublishedAt;
 @Column(name="appeal_deadline") private LocalDateTime appealDeadline;
 @Column(name="decision",length=40) private String decision;
 @Column(name="recalculation_required",nullable=false) @Builder.Default private boolean recalculationRequired=false;
 @Column(name="resolved_at") private LocalDateTime resolvedAt;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @Column(name="updated_at") private LocalDateTime updatedAt;
}
