package vn.edu.fpt.seal.modules.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.edu.fpt.seal.common.entity.BaseEntity;
import vn.edu.fpt.seal.common.enums.EventStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event extends BaseEntity {

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "event_status")
    @Builder.Default
    private EventStatus status = EventStatus.draft;

    @Column(name = "term", length = 255)
    private String term;

    @Column(name = "prize_pool", length = 255)
    private String prizePool;

    @Column(name = "registration_start")
    private LocalDateTime registrationStart;

    @Column(name = "registration_end")
    private LocalDateTime registrationEnd;

    @Column(name = "event_start")
    private LocalDateTime eventStart;

    @Column(name = "event_end")
    private LocalDateTime eventEnd;
}
