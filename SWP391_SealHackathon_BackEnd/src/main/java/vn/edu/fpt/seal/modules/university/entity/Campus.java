package vn.edu.fpt.seal.modules.university.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.fpt.seal.common.entity.BaseEntity;

@Entity
@Table(name = "campuses",
        uniqueConstraints = @UniqueConstraint(name = "uq_campuses_university_name",
                columnNames = {"university_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campus extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "city", length = 100)
    private String city;
}
