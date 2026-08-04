package vn.edu.fpt.seal.modules.criteria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.criteria.entity.CriteriaTemplate;

import java.util.UUID;

/**
 * Repository truy xuất mẫu tiêu chí chấm điểm (CriteriaTemplate).
 */
@Repository
public interface CriteriaTemplateRepository extends JpaRepository<CriteriaTemplate, UUID> {
    /**
     * Kiểm tra tên mẫu đã tồn tại (không phân biệt hoa thường) để chống trùng tên.
     */
    boolean existsByNameIgnoreCase(String name);
}
