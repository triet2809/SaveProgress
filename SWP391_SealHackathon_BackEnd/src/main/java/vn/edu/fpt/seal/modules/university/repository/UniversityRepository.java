package vn.edu.fpt.seal.modules.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.modules.university.entity.University;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UniversityRepository extends JpaRepository<University, UUID> {
    boolean existsByNameIgnoreCase(String name);

    Optional<University> findByNameIgnoreCase(String name);
}
