package vn.edu.fpt.seal.modules.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.edu.fpt.seal.common.entity.BaseEntity;
import vn.edu.fpt.seal.common.enums.EventStatus;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho một sự kiện (cuộc thi/hackathon).
 * Là gốc của cây dữ liệu: track, vòng thi, đội... đều gắn với một event.
 */
@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event extends BaseEntity {

    /**
     * Tiêu đề sự kiện.
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * Mô tả chi tiết sự kiện.
     */
    @Column(name = "description", columnDefinition = "text")
    private String description;

    /**
     * Trạng thái sự kiện; mặc định là draft (nháp). Lưu dưới dạng enum PostgreSQL.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "event_status")
    @Builder.Default
    private EventStatus status = EventStatus.draft;

    /**
     * Học kỳ/kỳ tổ chức.
     */
    @Column(name = "term", length = 255)
    private String term;

    /**
     * Tổng giải thưởng (dạng text để linh hoạt).
     */
    @Column(name = "prize_pool", length = 255)
    private String prizePool;

    /**
     * Thời điểm mở đăng ký.
     */
    @Column(name = "registration_start")
    private LocalDateTime registrationStart;

    /**
     * Thời điểm đóng đăng ký.
     */
    @Column(name = "registration_end")
    private LocalDateTime registrationEnd;

    /**
     * Thời điểm bắt đầu sự kiện.
     */
    @Column(name = "event_start")
    private LocalDateTime eventStart;

    /**
     * Thời điểm kết thúc sự kiện.
     */
    @Column(name = "event_end")
    private LocalDateTime eventEnd;
}
