package vn.edu.fpt.seal.modules.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.seal.common.enums.AccountStatus;
import vn.edu.fpt.seal.modules.user.entity.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"roles", "campus", "university"})
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"roles", "campus", "university"})
    Optional<User> findByGoogleSub(String googleSub);

    @EntityGraph(attributePaths = {"roles", "campus", "university"})
    Page<User> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"roles", "campus", "university"})
    Page<User> findByStatus(AccountStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"roles", "campus", "university"})
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    @EntityGraph(attributePaths = {"roles", "campus", "university"})
    Page<User> findByRolesNameIgnoreCase(String role, Pageable pageable);

    @EntityGraph(attributePaths = {"roles", "campus", "university"})
    Page<User> findByStatusAndRolesNameIgnoreCase(AccountStatus status, String role, Pageable pageable);

    @EntityGraph(attributePaths = {"roles", "campus", "university"})
    Page<User> findByEmailContainingIgnoreCaseAndRolesNameIgnoreCase(String email, String role, Pageable pageable);

    @EntityGraph(attributePaths = {"roles", "campus", "university"})
    Page<User> findByStatusAndEmailContainingIgnoreCase(AccountStatus status, String email, Pageable pageable);

    @EntityGraph(attributePaths = {"roles", "campus", "university"})
    Page<User> findByStatusAndEmailContainingIgnoreCaseAndRolesNameIgnoreCase(AccountStatus status, String email, String role, Pageable pageable);

    @EntityGraph(attributePaths = {"roles", "campus", "university"})
    @Query("""
            select distinct u from User u
            left join u.roles r
            where (:status is null or u.status = :status)
              and (:email is null or lower(u.email) like lower(concat('%', :email, '%')))
              and (:role is null or lower(r.name) = lower(:role))
            """)
    Page<User> search(@Param("status") AccountStatus status, @Param("email") String email, @Param("role") String role, Pageable pageable);

    @EntityGraph(attributePaths = {"roles", "campus", "university"})
    Optional<User> findWithRolesById(UUID id);
}
