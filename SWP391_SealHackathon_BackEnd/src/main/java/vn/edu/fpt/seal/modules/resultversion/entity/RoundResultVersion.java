package vn.edu.fpt.seal.modules.resultversion.entity;
import jakarta.persistence.*; import lombok.*; import vn.edu.fpt.seal.modules.round.entity.Round; import vn.edu.fpt.seal.modules.user.entity.User;
import java.time.LocalDateTime; import java.util.UUID;
@Entity @Table(name="round_result_versions", uniqueConstraints=@UniqueConstraint(name="uq_result_version_number",columnNames={"round_id","version_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoundResultVersion {
 @Id @GeneratedValue @Column(columnDefinition="uuid") private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="round_id") private Round round;
 @Column(name="version_number",nullable=false) private Integer versionNumber;
 @Column(nullable=false,length=20) private String status;
 @Column(name="published_at",nullable=false) private LocalDateTime publishedAt;
 @Column(name="appeal_deadline",nullable=false) private LocalDateTime appealDeadline;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="published_by") private User publishedBy;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="source_version_id") private RoundResultVersion sourceVersion;
 @Column(columnDefinition="text") private String reason;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
}
