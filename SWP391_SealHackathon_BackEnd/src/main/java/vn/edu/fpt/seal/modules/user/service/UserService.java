package vn.edu.fpt.seal.modules.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.AccountStatus;
import vn.edu.fpt.seal.common.enums.StudentType;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.university.entity.Campus;
import vn.edu.fpt.seal.modules.university.entity.University;
import vn.edu.fpt.seal.modules.university.repository.CampusRepository;
import vn.edu.fpt.seal.modules.university.repository.UniversityRepository;
import vn.edu.fpt.seal.modules.user.dto.*;
import vn.edu.fpt.seal.modules.user.entity.Role;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.mapper.UserMapper;
import vn.edu.fpt.seal.modules.user.repository.RoleRepository;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final CampusRepository campusRepo;
    private final UniversityRepository universityRepo;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserResponse> list(AccountStatus status, String email, String role, Pageable p) {
        String cleanEmail = email != null && !email.isBlank() ? email.trim() : null;
        String cleanRole = role != null && !role.isBlank() ? role.trim() : null;
        Page<User> page;
        if (status != null && cleanEmail != null && cleanRole != null)
            page = userRepo.findByStatusAndEmailContainingIgnoreCaseAndRolesNameIgnoreCase(status, cleanEmail, cleanRole, p);
        else if (status != null && cleanEmail != null)
            page = userRepo.findByStatusAndEmailContainingIgnoreCase(status, cleanEmail, p);
        else if (status != null && cleanRole != null)
            page = userRepo.findByStatusAndRolesNameIgnoreCase(status, cleanRole, p);
        else if (cleanEmail != null && cleanRole != null)
            page = userRepo.findByEmailContainingIgnoreCaseAndRolesNameIgnoreCase(cleanEmail, cleanRole, p);
        else if (status != null) page = userRepo.findByStatus(status, p);
        else if (cleanEmail != null) page = userRepo.findByEmailContainingIgnoreCase(cleanEmail, p);
        else if (cleanRole != null) page = userRepo.findByRolesNameIgnoreCase(cleanRole, p);
        else page = userRepo.findAll(p);
        return page.map(UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return UserMapper.toResponse(find(id));
    }

    @Transactional
    public UserResponse updateStatus(UUID id, UpdateUserStatusRequest r) {
        User u = find(id);
        u.setStatus(r.status());
        return UserMapper.toResponse(u);
    }

    @Transactional
    public UserResponse updateCurrentUser(UUID id, UpdateCurrentUserRequest r) {
        User u = find(id);
        if (r.fullName() != null && !r.fullName().isBlank()) u.setFullName(r.fullName().trim());
        if (r.studentId() != null) u.setStudentId(r.studentId().isBlank() ? null : r.studentId().trim());
        if (r.phone() != null) u.setPhone(r.phone().isBlank() ? null : r.phone().trim());
        if (r.department() != null) u.setDepartment(r.department().isBlank() ? null : r.department().trim());
        if (r.position() != null) u.setPosition(r.position().isBlank() ? null : r.position().trim());
        if (r.company() != null) u.setCompany(r.company().isBlank() ? null : r.company().trim());
        if (r.expertise() != null) u.setExpertise(r.expertise().isBlank() ? null : r.expertise().trim());
        if (r.bio() != null) u.setBio(r.bio().isBlank() ? null : r.bio().trim());
        if (r.campusId() != null) {
            Campus campus = campusRepo.findWithUniversityById(r.campusId()).orElseThrow(() -> ApiException.notFound("Campus not found"));
            u.setCampus(campus);
            u.setUniversity(campus.getUniversity());
        } else if (r.universityId() != null) {
            University university = universityRepo.findById(r.universityId()).orElseThrow(() -> ApiException.notFound("University not found"));
            u.setUniversity(university);
            u.setCampus(null);
        }
        return UserMapper.toResponse(u);
    }

    @Transactional
    public UserResponse create(CreateUserRequest r) {
        String email = r.email().toLowerCase().trim();
        if (userRepo.existsByEmail(email)) throw ApiException.conflict("Email already registered");
        Campus campus = null;
        University university = null;
        if (r.campusId() != null) {
            campus = campusRepo.findWithUniversityById(r.campusId()).orElseThrow(() -> ApiException.notFound("Campus not found"));
            university = campus.getUniversity();
        } else if (r.universityId() != null) {
            university = universityRepo.findById(r.universityId()).orElseThrow(() -> ApiException.notFound("University not found"));
        }
        Set<Role> roles = new HashSet<>();
        if (r.roles() != null) for (String name : r.roles()) {
            roles.add(roleRepo.findByName(name.trim()).orElseThrow(() -> ApiException.notFound("Role not found: " + name)));
        }
        User u = User.builder().email(email).passwordHash(passwordEncoder.encode(r.password())).fullName(r.fullName().trim()).studentType(r.studentType() != null ? r.studentType() : StudentType.none).studentId(clean(r.studentId())).phone(clean(r.phone())).department(clean(r.department())).position(clean(r.position())).company(clean(r.company())).expertise(clean(r.expertise())).bio(clean(r.bio())).university(university).campus(campus).isGuest(false).status(r.status() != null ? r.status() : AccountStatus.approved).roles(roles).build();
        return UserMapper.toResponse(userRepo.save(u));
    }

    private String clean(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    @Transactional
    public UserResponse updateProfile(UUID id, UpdateCurrentUserRequest r) {
        return updateCurrentUser(id, r);
    }

    @Transactional
    public UserResponse updateRoles(UUID id, UpdateUserRolesRequest r) {
        User u = find(id);
        Set<Role> roles = new HashSet<>();
        for (String name : r.roles()) {
            roles.add(roleRepo.findByName(name.trim()).orElseThrow(() -> ApiException.notFound("Role not found: " + name)));
        }
        u.setRoles(roles);
        return UserMapper.toResponse(u);
    }

    private User find(UUID id) {
        return userRepo.findWithRolesById(id).orElseThrow(() -> ApiException.notFound("User not found: " + id));
    }
}
