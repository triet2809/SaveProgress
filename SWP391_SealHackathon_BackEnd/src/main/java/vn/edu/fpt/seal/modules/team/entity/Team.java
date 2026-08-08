package vn.edu.fpt.seal.modules.team.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.edu.fpt.seal.common.entity.BaseEntity;
import vn.edu.fpt.seal.common.enums.TeamStatus;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.teamprofile.entity.TeamProfile;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_profile_id", nullable = false)
    private TeamProfile teamProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    /**
     * teams.event_id là NOT NULL + FK tới events(id). Team luôn thuộc 1 track,
     * mà track đã gắn event, nên cột này là denormalized của track.event.
     * Không set thủ công ở các service tạo team — {@link #syncEventFromTrack()} tự suy ra.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "team_status")
    @Builder.Default
    private TeamStatus status = TeamStatus.active;

    @Column(name = "disqualified_reason", columnDefinition = "text")
    private String disqualifiedReason;

    /**
     * Short human-friendly invite code (6 chars) used to join the team.
     */
    @Column(name = "invite_code", length = 12, unique = true)
    private String inviteCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_team_id")
    private Team sourceTeam;

    @Column(name = "activated_from_profile_at")
    private LocalDateTime activatedFromProfileAt;

    @Column(name = "roster_confirmed_at")
    private LocalDateTime rosterConfirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roster_confirmed_by")
    private User rosterConfirmedBy;

    /**
     * Tự điền event_id từ track trước khi lưu, để mọi đường tạo team (create/activate)
     * đều thỏa ràng buộc NOT NULL mà không phải set event thủ công.
     */
    @PrePersist
    @PreUpdate
    private void syncEventFromTrack() {
        if (event == null && track != null) {
            event = track.getEvent();
        }
    }
}
