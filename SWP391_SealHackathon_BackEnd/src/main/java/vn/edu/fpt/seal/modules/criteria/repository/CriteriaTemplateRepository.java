package vn.edu.fpt.seal.modules.criteria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.criteria.entity.CriteriaTemplate;

import java.util.UUID;

@Repository
public interface CriteriaTemplateRepository extends JpaRepository<CriteriaTemplate, UUID> {
    boolean existsByNameIgnoreCase(String name);
}
