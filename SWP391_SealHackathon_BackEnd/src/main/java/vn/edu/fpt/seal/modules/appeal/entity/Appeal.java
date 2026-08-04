package vn.edu.fpt.seal.modules.appeal.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersion;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thực thể Appeal — đại diện cho một đơn khiếu nại kết quả do đội thi nộp.
 * Lưu trữ lý do khiếu nại, trạng thái xử lý, quyết định cuối cùng và các mốc thời gian liên quan.
 * Ánh xạ tới bảng "appeals".
 */
@Entity
@Table(name = "appeals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appeal {
    /**
     * Khóa chính, sinh tự động dạng UUID.
     */
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;
    /**
     * Sự kiện mà khiếu nại thuộc về.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;
    /**
     * Vòng thi bị khiếu nại.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id")
    private Round round;
    /**
     * Đội nộp khiếu nại.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id")
    private Team team;
    /**
     * Người nộp (thường là đội trưởng).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;
    /**
     * Phiên bản kết quả bị khiếu nại.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_version_id")
    private RoundResultVersion resultVersion;
    /**
     * Lý do khiếu nại (bắt buộc).
     */
    @Column(nullable = false, columnDefinition = "text")
    private String reason;
    /**
     * Trạng thái xử lý: PENDING / ACCEPTED / REJECTED. Mặc định PENDING.
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";
    /**
     * Phản hồi của ban tổ chức dành cho khiếu nại.
     */
    @Column(columnDefinition = "text")
    private String response;
    /**
     * Người giải quyết khiếu nại (coordinator).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;
    /**
     * Thời điểm kết quả được công bố (snapshot tại thời điểm khiếu nại).
     */
    @Column(name = "result_published_at")
    private LocalDateTime resultPublishedAt;
    /**
     * Hạn chót được phép khiếu nại.
     */
    @Column(name = "appeal_deadline")
    private LocalDateTime appealDeadline;
    /**
     * Quyết định cuối cùng: ACCEPTED / REJECTED.
     */
    @Column(name = "decision", length = 40)
    private String decision;
    /**
     * Cờ báo hiệu cần tính lại kết quả sau khi chấp nhận khiếu nại.
     */
    @Column(name = "recalculation_required", nullable = false)
    @Builder.Default
    private boolean recalculationRequired = false;
    /**
     * Thời điểm khiếu nại được giải quyết.
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    /**
     * Thời điểm tạo (bắt buộc).
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    /**
     * Thời điểm cập nhật gần nhất.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
