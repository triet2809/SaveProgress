# THESIS DEFENSE GUIDE — Module Team (Be3)
# Hướng dẫn bảo vệ đồ án — Module Team

> **Cách dùng:** Đọc trực tiếp file này trong lúc bảo vệ. Code hiển thị là code project hiện tại
> (`vn.edu.fpt.seal`). Phần so sánh với bản cũ được giải thích bằng văn bản và bảng.

---

## MỤC LỤC

1. [Tổng quan Module Team](#1-tổng-quan)
2. [Entity: Team & TeamMember](#2-entities)
3. [DTOs](#3-dtos)
4. [Repository: TeamRepository](#4-teamrepository)
5. [Repository: TeamMemberRepository](#5-teammemberrepository)
6. [Service: TeamService](#6-teamservice)
7. [Service: TeamJoinRequestService](#7-teamjoinrequestservice)
8. [Controller: TeamController](#8-teamcontroller)
9. [Controller: TeamJoinRequestController](#9-teamjoinrequestcontroller)
10. [Mapper: TeamMapper](#10-teammapper)
11. [Bảng so sánh cũ vs mới](#11-so-sánh)
12. [Câu hỏi bảo vệ & gợi ý trả lời](#12-câu-hỏi-bảo-vệ)

---

## 1. Tổng quan

Module Team quản lý toàn bộ vòng đời của đội thi trong Hackathon:
tạo team, quản lý thành viên, join bằng invite code, truất quyền thi đấu.

**Package:** `vn.edu.fpt.seal.modules.team`

**Các file chính:**
```
modules/team/
├── controller/
│   ├── TeamController.java
│   └── TeamJoinRequestController.java
├── service/
│   ├── TeamService.java
│   ├── TeamJoinRequestService.java
│   └── TeamTransferService.java
├── repository/
│   ├── TeamRepository.java
│   ├── TeamMemberRepository.java
│   └── TeamJoinRequestRepository.java
├── entity/
│   ├── Team.java
│   ├── TeamMember.java
│   └── TeamJoinRequest.java
├── dto/
│   ├── CreateTeamRequest.java
│   ├── UpdateTeamRequest.java
│   ├── DisqualifyTeamRequest.java
│   ├── TeamResponse.java
│   └── TeamMemberResponse.java
└── mapper/
    └── TeamMapper.java
```

**Vòng đời trạng thái Team:**
```
active ──────────────────────► disqualified  (coordinator truất quyền)
  ▲                                │
  └──────────────────────────────── (coordinator reactivate, chỉ khi event còn editable)
```

---

## 2. Entities

### 2.1 Team.java

**File:** `modules/team/entity/Team.java`

```java
@Entity
@Table(name = "teams")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Team extends BaseEntity {   // BaseEntity: id, createdAt, updatedAt

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_profile_id", nullable = false)
    private TeamProfile teamProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;                  // Team → Track → Event

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)    // Hibernate 6: handle PostgreSQL custom enum
    @Column(name = "status", nullable = false, columnDefinition = "team_status")
    @Builder.Default
    private TeamStatus status = TeamStatus.active;   // Default: active ngay từ đầu

    @Column(name = "disqualified_reason", columnDefinition = "text")
    private String disqualifiedReason;

    @Column(name = "invite_code", length = 12, unique = true)
    private String inviteCode;            // 6-char code để join team

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_team_id")
    private Team sourceTeam;             // Trace khi team được copy từ team khác

    @Column(name = "activated_from_profile_at")
    private LocalDateTime activatedFromProfileAt;

    @Column(name = "roster_confirmed_at")
    private LocalDateTime rosterConfirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roster_confirmed_by")
    private User rosterConfirmedBy;
}
```

**Điểm quan trọng:**
- Team liên kết qua `Track` rồi mới đến `Event` — không liên kết Event trực tiếp.
- `status` có thể được update bởi JPA (`team.setStatus(...)`) — không cần native SQL.
- `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` là cách Hibernate 6 xử lý PostgreSQL custom enum type.
- Không có `lockedAt` hay `isLocked()` — tính năng lock không tồn tại trong phiên bản này.

---

### 2.2 TeamMember.java

**File:** `modules/team/entity/TeamMember.java`

```java
@Entity
@Table(name = "team_members")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeamMember {

    @Id @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "role", nullable = false, columnDefinition = "team_member_role")
    @Builder.Default
    private TeamMemberRole role = TeamMemberRole.member;   // leader | member

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
}
```

**Điểm quan trọng:**
- Entity cực kỳ đơn giản: chỉ có `role` và `joinedAt`, không có `status`.
- Không còn invite/accept/decline lifecycle — member được thêm trực tiếp khi join hoặc được accept.
- Tạo `TeamMember` = user đã chính thức là thành viên, không có trạng thái chờ.

---

## 3. DTOs

### CreateTeamRequest.java

```java
public record CreateTeamRequest(
        @NotNull UUID trackId,
        @NotBlank @Size(max = 255) String name,
        UUID leaderUserId,                          // Coordinator chỉ định leader (optional)
        List<UUID> memberUserIds,                   // Coordinator thêm members ngay (optional)
        List<@Size(max = 255) String> memberEmails  // Thêm member qua email (optional)
) {}
```

### UpdateTeamRequest.java

```java
public record UpdateTeamRequest(@Size(max = 255) String name) {}
```

### DisqualifyTeamRequest.java

```java
public record DisqualifyTeamRequest(
        @NotBlank @Size(max = 10000) String reason  // Bắt buộc có lý do
) {}
```

### TeamResponse.java

```java
@Builder
public record TeamResponse(
        UUID id,
        UUID teamProfileId,
        UUID sourceTeamId,
        UUID trackId,
        UUID eventId,                           // Lấy từ track.event.id
        String name,
        String inviteCode,                      // Chỉ trả về cho coordinator/member
        TeamStatus status,
        String disqualifiedReason,
        List<TeamMemberResponse> members,
        List<RecognitionDtos.Summary> recognitions,
        LocalDateTime activatedFromProfileAt,
        LocalDateTime rosterConfirmedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
```

### TeamMemberResponse.java

```java
@Builder
public record TeamMemberResponse(
        UUID id,
        UUID userId,
        String email,
        String fullName,
        TeamMemberRole role,
        LocalDateTime joinedAt
) {}
```

---

## 4. TeamRepository

**File:** `modules/team/repository/TeamRepository.java`

```java
@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    Page<Team> findByTrackId(UUID trackId, Pageable pageable);

    // Danh sách team theo event (không filter track)
    @EntityGraph(attributePaths = {"track", "track.event"})
    Page<Team> findByTrackEventId(UUID eventId, Pageable pageable);

    // Danh sách team theo event + track cụ thể
    @EntityGraph(attributePaths = {"track", "track.event"})
    Page<Team> findByTrackEventIdAndTrackId(UUID eventId, UUID trackId, Pageable pageable);

    // Kiểm tra trùng tên trong cùng track (case-insensitive)
    boolean existsByTrackIdAndNameIgnoreCase(UUID trackId, String name);

    // Tìm team bằng invite code (user nhập để join)
    @EntityGraph(attributePaths = {"track", "track.event"})
    Optional<Team> findByInviteCodeIgnoreCase(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    // Fetch team kèm track+event để tránh N+1
    @EntityGraph(attributePaths = {"track", "track.event"})
    Optional<Team> findWithTrackById(UUID id);

    long countByTrackEventId(UUID eventId);

    @EntityGraph(attributePaths = {"track", "track.event"})
    List<Team> findByTrackEventId(UUID eventId);

    @EntityGraph(attributePaths = {"track", "track.event", "teamProfile", "sourceTeam"})
    List<Team> findByTeamProfileIdIn(Collection<UUID> profileIds);

    boolean existsByTeamProfileIdAndTrackEventId(UUID profileId, UUID eventId);

    long countByTrackId(UUID trackId);
}
```

**Điểm quan trọng:**
- Dùng `@EntityGraph` để eager-load quan hệ trong 1 query, tránh N+1.
- Không có `lockTeam()` hay `disqualifyTeam()` native query — JPA set status trực tiếp.
- Derived method names (`findByTrackEventId`) tự động sinh SQL, ngắn gọn hơn `@Query`.

---

## 5. TeamMemberRepository

**File:** `modules/team/repository/TeamMemberRepository.java`

```java
@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    // Danh sách members, sắp xếp leader lên đầu, rồi theo joinedAt
    @EntityGraph(attributePaths = {"user"})
    List<TeamMember> findByTeamIdOrderByRoleAscJoinedAtAsc(UUID teamId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    boolean existsByUserIdAndTeamTrackId(UUID userId, UUID trackId);

    long countByTeamId(UUID teamId);

    // Kiểm tra team đã có leader chưa (để tránh tạo 2 leader)
    boolean existsByTeamIdAndRole(UUID teamId, TeamMemberRole role);

    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    @EntityGraph(attributePaths = {"team", "team.teamProfile", "team.track", "team.track.event", "user"})
    List<TeamMember> findByUserIdOrderByJoinedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"team", "team.track", "team.track.event", "user"})
    List<TeamMember> findByTeamIdIn(Collection<UUID> teamIds);

    // Tìm tất cả registrations active của các users trong một event
    @EntityGraph(attributePaths = {"team", "team.track", "team.track.event", "user"})
    @Query("""
            select tm from TeamMember tm
            where tm.user.id in :userIds
              and tm.team.track.event.id = :eventId
              and tm.team.status = :status
            """)
    List<TeamMember> findActiveRegistrationsInEvent(
            @Param("userIds") Collection<UUID> userIds,
            @Param("eventId") UUID eventId,
            @Param("status") TeamStatus status);

    // Convenience: kiểm tra 1 user có đang trong team active nào của event không
    default boolean existsActiveRegistrationInEvent(UUID userId, UUID eventId) {
        return !findActiveRegistrationsInEvent(
                List.of(userId), eventId, TeamStatus.active).isEmpty();
    }
}
```

**Điểm quan trọng:**
- JPQL được dùng trực tiếp (không cần native query) vì `TeamMember` không còn status enum riêng.
- `existsActiveRegistrationInEvent()` là default method — bọc query để check 1 user, không cần thêm method riêng trong interface.
- `existsByTeamIdAndRole()` dùng để block tạo 2 leader trong cùng 1 team.

---

## 6. TeamService

**File:** `modules/team/service/TeamService.java`

### 6.1 Constants & Dependencies

```java
@Slf4j @Service @RequiredArgsConstructor
public class TeamService {

    private static final int MIN_TEAM_SIZE = 3;
    private static final int MAX_TEAM_SIZE = 5;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final EventRepository eventRepository;
    private final TeamProfileRepository teamProfileRepository;
    private final TeamRecognitionService recognitionService;
    private final TimelineService timelineService;
}
```

---

### 6.2 listByTrack() — Danh sách team

```java
@Transactional(readOnly = true)
public Page<TeamResponse> listByTrack(UUID eventId, UUID trackId, Pageable pageable) {
    if (eventId == null) throw ApiException.badRequest("eventId is required");
    if (!eventRepository.existsById(eventId))
        throw ApiException.notFound("Event not found: " + eventId);
    if (trackId != null && !trackRepository.findById(trackId)
            .filter(t -> t.getEvent().getId().equals(eventId)).isPresent()) {
        throw ApiException.badRequest("Track does not belong to the selected event");
    }
    Page<Team> teams = trackId == null
            ? teamRepository.findByTrackEventId(eventId, pageable)
            : teamRepository.findByTrackEventIdAndTrackId(eventId, trackId, pageable);
    Map<UUID, List<RecognitionDtos.Summary>> recognitionByTeam =
            recognitionService.activeByTeamIds(
                    teams.getContent().stream().map(Team::getId).toList());
    return teams.map(t -> toResponse(t, false,
            recognitionByTeam.getOrDefault(t.getId(), List.of())));
}
```

---

### 6.3 get() — Lấy team theo ID

```java
@Transactional(readOnly = true)
public TeamResponse get(UUID id, Authentication auth) {
    Team team = findOrThrow(id);
    boolean coordinator = isCoordinator(auth);
    boolean member = auth != null && auth.getPrincipal() instanceof CurrentUser c
            && teamMemberRepository.existsByTeamIdAndUserId(team.getId(), c.getId());
    return toResponse(team, coordinator || member);  // inviteCode chỉ lộ với coordinator/member
}
```

---

### 6.4 myTeams() — Danh sách team của user hiện tại

```java
@Transactional(readOnly = true)
public List<TeamResponse> myTeams(Authentication auth) {
    UUID callerId = currentUserId(auth);
    List<Team> teams = teamMemberRepository
            .findByUserIdOrderByJoinedAtDesc(callerId).stream()
            .map(TeamMember::getTeam).distinct()
            .sorted(Comparator.comparingInt(this::currentRegistrationPriority)
                    .thenComparing(Team::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    // ...
}

// Sắp xếp ưu tiên: ongoing trước, disqualified sau cùng
private int currentRegistrationPriority(Team team) {
    if (team.getStatus() == TeamStatus.disqualified) return 10;
    return switch (team.getTrack().getEvent().getStatus()) {
        case ongoing -> 0;
        case published -> 1;
        case draft -> 2;
        case completed -> 3;
        case cancelled -> 4;
    };
}
```

---

### 6.5 create() — Tạo team

```java
@Transactional
public TeamResponse create(CreateTeamRequest req, Authentication auth) {
    Track track = trackRepository.findById(req.trackId())
            .orElseThrow(() -> ApiException.notFound("Track not found: " + req.trackId()));
    ensureEditable(track);
    ensureRegistrationOpen(track);

    String name = req.name().trim();
    if (teamRepository.existsByTrackIdAndNameIgnoreCase(track.getId(), name))
        throw ApiException.conflict("Team name already exists in this track");

    boolean coordinator = isCoordinator(auth);
    UUID actorId = currentUserIdOrNull(auth);
    User creator = actorId == null ? null : userRepository.findById(actorId).orElse(null);

    // Tạo TeamProfile là canonical identity của team (tồn tại xuyên suốt các event)
    TeamProfile profile = teamProfileRepository.save(TeamProfile.builder()
            .canonicalName(name).createdBy(creator).status(TeamProfileStatus.active).build());

    Team team = teamRepository.save(Team.builder()
            .teamProfile(profile).track(track).name(name)
            .status(TeamStatus.active)
            .inviteCode(generateInviteCode())
            .build());

    timelineService.record(track.getEvent(), team.getId(), null, track.getId(),
            "TEAM_CREATED", TimelineScope.EVENT_PARTICIPANTS, "Team created",
            "A team registration was created", "TEAM", team.getId(),
            "team:" + team.getId() + ":created");

    Set<UUID> added = new LinkedHashSet<>();
    if (coordinator) {
        // Coordinator tạo team thay cho người khác — chỉ định leader/members
        if (req.leaderUserId() != null) {
            addMemberInternal(team, req.leaderUserId(), TeamMemberRole.leader);
            added.add(req.leaderUserId());
        }
        if (req.memberUserIds() != null)
            for (UUID id : req.memberUserIds())
                if (added.add(id)) addMemberInternal(team, id, TeamMemberRole.member);
        for (UUID mid : resolveEmails(req.memberEmails()))
            if (added.add(mid)) addMemberInternal(team, mid, TeamMemberRole.member);
        if (added.size() > MAX_TEAM_SIZE)
            throw ApiException.badRequest("A team can have at most " + MAX_TEAM_SIZE + " members");
    } else {
        // User thường tạo team → tự động trở thành leader
        UUID callerId = currentUserId(auth);
        addMemberInternal(team, callerId, TeamMemberRole.leader);
        added.add(callerId);
        if (req.memberUserIds() != null)
            for (UUID id : req.memberUserIds())
                if (added.add(id)) addMemberInternal(team, id, TeamMemberRole.member);
        for (UUID mid : resolveEmails(req.memberEmails()))
            if (added.add(mid)) addMemberInternal(team, mid, TeamMemberRole.member);
        if (added.size() > MAX_TEAM_SIZE)
            throw ApiException.badRequest(
                    "A team can have at most " + MAX_TEAM_SIZE + " members (including the leader)");
    }
    return toResponse(team);
}
```

---

### 6.6 update() — Đổi tên team

```java
@Transactional
public TeamResponse update(UUID id, UpdateTeamRequest req) {
    Team team = findOrThrow(id);
    ensureEditable(team.getTrack());
    if (req.name() != null) {
        String name = req.name().trim();
        if (!name.equalsIgnoreCase(team.getName()) &&
                teamRepository.existsByTrackIdAndNameIgnoreCase(team.getTrack().getId(), name))
            throw ApiException.conflict("Team name already exists in this track");
        team.setName(name);
    }
    return toResponse(team);
}
```

---

### 6.7 joinByInviteCode() — Join team bằng mã mời

```java
@Transactional
public TeamResponse joinByInviteCode(JoinTeamRequest req, Authentication auth) {
    UUID callerId = currentUserId(auth);
    // Normalize: bỏ prefix "SEAL-", dấu gạch, uppercase
    String code = req.inviteCode().trim().toUpperCase()
            .replace("SEAL-", "").replace("-", "");
    Team team = teamRepository.findByInviteCodeIgnoreCase(code)
            .orElseThrow(() -> ApiException.notFound("Team invite code not found"));
    ensureEditable(team.getTrack());
    ensureRegistrationOpen(team.getTrack());
    if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), callerId))
        return toResponse(team);  // Idempotent: đã là member rồi thì trả về luôn
    if (teamMemberRepository.countByTeamId(team.getId()) >= MAX_TEAM_SIZE)
        throw ApiException.badRequest("A team can have at most " + MAX_TEAM_SIZE + " members");
    addMemberInternal(team, callerId, TeamMemberRole.member);
    return toResponse(team);
}
```

---

### 6.8 addMember() / removeMember() — Coordinator quản lý thành viên

```java
@Transactional
public TeamResponse addMember(UUID teamId, AddTeamMemberRequest req) {
    Team team = findOrThrow(teamId);
    ensureEditable(team.getTrack());
    if (teamMemberRepository.countByTeamId(teamId) >= MAX_TEAM_SIZE)
        throw ApiException.badRequest("A team can have at most " + MAX_TEAM_SIZE + " members");
    addMemberInternal(team, req.userId(),
            req.role() == null ? TeamMemberRole.member : req.role());
    return toResponse(team);
}

@Transactional
public void removeMember(UUID teamId, UUID userId) {
    Team team = findOrThrow(teamId);
    ensureEditable(team.getTrack());
    TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .orElseThrow(() -> ApiException.notFound("Team member not found"));
    teamMemberRepository.delete(member);
}
```

---

### 6.9 disqualify() — Truất quyền thi đấu ⭐

```java
@Transactional
public TeamResponse disqualify(UUID id, DisqualifyTeamRequest req, Authentication auth) {
    Team team = findOrThrow(id);
    String oldStatus = team.getStatus().name();

    team.setStatus(TeamStatus.disqualified);         // JPA set trực tiếp — không cần native SQL
    team.setDisqualifiedReason(req.reason().trim());

    // Ghi audit trail: ai làm, team nào, trạng thái trước/sau, lý do
    writeAudit(auth, team, AuditAction.DISQUALIFY_TEAM,
            oldStatus, TeamStatus.disqualified.name(), req.reason().trim());

    // Ghi timeline để hiển thị lịch sử hoạt động
    timelineService.record(team.getTrack().getEvent(), team.getId(), null,
            team.getTrack().getId(), "TEAM_DISQUALIFIED",
            TimelineScope.EVENT_PARTICIPANTS, "Team disqualified",
            "The team was disqualified", "TEAM", team.getId(),
            "team:" + team.getId() + ":disqualified:" + team.getUpdatedAt());

    return toResponse(team);
}
```

**Luồng xử lý:**
1. Tìm team → 404 nếu không có.
2. Lưu `oldStatus` để ghi audit log.
3. `team.setStatus(TeamStatus.disqualified)` — JPA dirty-checking tự flush khi transaction commit.
4. Ghi `AuditLog` với `oldValue` / `newValue`.
5. Ghi `Timeline` event.
6. Trả về team response.

---

### 6.10 reactivate() — Khôi phục team bị disqualify

```java
@Transactional
public TeamResponse reactivate(UUID id, Authentication auth) {
    Team team = findOrThrow(id);
    ensureEditable(team.getTrack());                // Event phải còn draft/published

    if (team.getStatus() == TeamStatus.active)
        throw ApiException.conflict("Team is already active");
    if (team.getStatus() != TeamStatus.disqualified)
        throw ApiException.badRequest("Team status cannot be reactivated: " + team.getStatus());
    if (team.getTeamProfile() == null ||
            team.getTeamProfile().getStatus() != TeamProfileStatus.active)
        throw ApiException.badRequest("Team is not linked to an active team profile");

    String oldStatus = team.getStatus().name();
    team.setStatus(TeamStatus.active);
    team.setDisqualifiedReason(null);
    team = teamRepository.saveAndFlush(team);

    writeAudit(auth, team, AuditAction.UPDATE, oldStatus, TeamStatus.active.name(),
            "Coordinator restored this event registration before registration closed");
    timelineService.record(team.getTrack().getEvent(), team.getId(), null,
            team.getTrack().getId(), "TEAM_REACTIVATED",
            TimelineScope.EVENT_PARTICIPANTS, "Team reactivated",
            "The team registration was reactivated", "TEAM", team.getId(),
            "team:" + team.getId() + ":reactivated:" + team.getUpdatedAt());
    return toResponse(team);
}
```

---

### 6.11 delete() — Xóa team

```java
@Transactional
public void delete(UUID id) {
    Team team = findOrThrow(id);
    ensureDraft(team.getTrack());    // Chỉ xóa khi event còn ở trạng thái draft
    teamRepository.delete(team);
}
```

---

### 6.12 moveToTrack() — Di chuyển team sang track khác

```java
@Transactional
public TeamResponse moveToTrack(UUID id, MoveTeamTrackRequest req, Authentication auth) {
    Team team = findOrThrow(id);
    Track target = trackRepository.findById(req.trackId())
            .orElseThrow(() -> ApiException.notFound("Track not found: " + req.trackId()));
    Track current = team.getTrack();

    if (current.getId().equals(target.getId())) return toResponse(team);  // No-op

    if (!current.getEvent().getId().equals(target.getEvent().getId()))
        throw ApiException.badRequest("Target track must belong to the same event");
    ensureEditable(target);
    if (teamRepository.existsByTrackIdAndNameIgnoreCase(target.getId(), team.getName()))
        throw ApiException.conflict("A team with this name already exists in the target track");

    String oldTrack = current.getId().toString();
    team.setTrack(target);
    writeAudit(auth, team, AuditAction.PROMOTE_TEAM, oldTrack,
            target.getId().toString(), "Moved team to track " + target.getName());
    return toResponse(team);
}
```

---

### 6.13 Private Helpers

```java
// addMemberInternal — dùng chung: create, addMember, joinByInviteCode
private TeamMember addMemberInternal(Team team, UUID userId, TeamMemberRole role) {
    if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), userId))
        throw ApiException.conflict("User already belongs to this team");
    if (teamMemberRepository.existsActiveRegistrationInEvent(
            userId, team.getTrack().getEvent().getId()))
        throw ApiException.conflict("User already belongs to another active team in this event");
    if (role == TeamMemberRole.leader &&
            teamMemberRepository.existsByTeamIdAndRole(team.getId(), TeamMemberRole.leader))
        throw ApiException.conflict("Team already has a leader");
    User user = userRepository.findById(userId)
            .orElseThrow(() -> ApiException.notFound("User not found: " + userId));
    if (user.getStatus() != AccountStatus.approved)
        throw ApiException.badRequest("Only approved users can join teams");
    if (role == TeamMemberRole.leader) grantTeamLeaderRole(user);
    return teamMemberRepository.save(
            TeamMember.builder().team(team).user(user).role(role).build());
}

// Cấp role team_leader cho user (idempotent — không duplicate)
private void grantTeamLeaderRole(User user) {
    boolean alreadyLeader = user.getRoles().stream()
            .anyMatch(r -> "team_leader".equals(r.getName()));
    if (alreadyLeader) return;
    roleRepository.findByName("team_leader").ifPresent(role -> {
        user.getRoles().add(role);
        userRepository.save(user);
    });
}

// Guard: event phải còn draft hoặc published mới được chỉnh sửa team
private void ensureEditable(Track track) {
    EventStatus s = track.getEvent().getStatus();
    if (s != EventStatus.draft && s != EventStatus.published)
        throw ApiException.badRequest(
                "Historical team registrations cannot be edited after registration closes (status: " + s + ")");
}

// Guard: chỉ cho tạo/join khi event đang published
private void ensureRegistrationOpen(Track track) {
    EventStatus s = track.getEvent().getStatus();
    if (s != EventStatus.published)
        throw ApiException.badRequest(
                "Registration is not open for this event (status: " + s + ")");
}

// Guard: chỉ cho xóa khi event còn draft
private void ensureDraft(Track track) {
    EventStatus s = track.getEvent().getStatus();
    if (s != EventStatus.draft)
        throw ApiException.badRequest(
                "Teams can only be deleted while event is draft (current: " + s + ")");
}

// Kiểm tra coordinator qua Spring Security authorities (không query DB)
private boolean isCoordinator(Authentication auth) {
    return auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_COORDINATOR"));
}

// Ghi audit log với oldValue / newValue (structured hơn)
private void writeAudit(Authentication auth, Team team, AuditAction action,
                         String oldValue, String newValue, String details) {
    UUID actorId = currentUserIdOrNull(auth);
    User actor = actorId == null ? null : userRepository.findById(actorId).orElse(null);
    auditLogRepository.save(AuditLog.builder()
            .user(actor).team(team).action(action)
            .targetType("team").targetId(team.getId())
            .oldValue(oldValue).newValue(newValue).details(details)
            .build());
}

private Team findOrThrow(UUID id) {
    return teamRepository.findWithTrackById(id)
            .orElseThrow(() -> ApiException.notFound("Team not found: " + id));
}

private UUID currentUserId(Authentication auth) {
    UUID id = currentUserIdOrNull(auth);
    if (id == null) throw ApiException.forbidden("Authentication required");
    return id;
}

private UUID currentUserIdOrNull(Authentication auth) {
    if (auth != null && auth.getPrincipal() instanceof CurrentUser c) return c.getId();
    return null;
}

// Resolve email → userId (phải là user đã registered)
private List<UUID> resolveEmails(List<String> emails) {
    if (emails == null) return List.of();
    List<UUID> ids = new ArrayList<>();
    for (String raw : emails) {
        if (raw == null || raw.isBlank()) continue;
        String email = raw.toLowerCase().trim();
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.badRequest(
                        "No registered user with email: " + email));
        ids.add(u.getId());
    }
    return ids;
}

// Sinh invite code 6 ký tự, unique
private String generateInviteCode() {
    for (int attempt = 0; attempt < 10; attempt++) {
        String code = UUID.randomUUID().toString()
                .replace("-", "").substring(0, 6).toUpperCase();
        if (!teamRepository.existsByInviteCode(code)) return code;
    }
    return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
}
```

---

## 7. TeamJoinRequestService

**File:** `modules/team/service/TeamJoinRequestService.java`

Thay thế cơ chế invite/accept/decline của phiên bản cũ. Student chủ động gửi request xin vào team, leader/coordinator duyệt.

```java
@Slf4j @Service @RequiredArgsConstructor
public class TeamJoinRequestService {

    private static final int MAX_TEAM_SIZE = 5;

    // create() — Student gửi yêu cầu tham gia team
    @Transactional
    public JoinRequestResponse create(CreateJoinRequestRequest req, Authentication auth) {
        UUID callerId = currentUserId(auth);
        Team team = teamRepository.findWithTrackById(req.teamId())
                .orElseThrow(() -> ApiException.notFound("Team not found: " + req.teamId()));
        ensureRegistrationOpen(team);
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        if (caller.getStatus() != AccountStatus.approved)
            throw ApiException.badRequest("Only approved users can request to join teams");
        if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), callerId))
            throw ApiException.conflict("You are already a member of this team");
        if (teamMemberRepository.countByTeamId(team.getId()) >= MAX_TEAM_SIZE)
            throw ApiException.badRequest("This team is already full");
        if (requestRepository.existsByTeamIdAndUserIdAndStatus(team.getId(), callerId, "pending"))
            throw ApiException.conflict("You already have a pending request for this team");
        TeamJoinRequest saved = requestRepository.save(TeamJoinRequest.builder()
                .team(team).user(caller).status("pending")
                .message(req.message() == null ? null : req.message().trim())
                .build());
        return toResponse(saved);
    }

    // listForTeam() — Leader/Coordinator xem danh sách requests
    @Transactional(readOnly = true)
    public List<JoinRequestResponse> listForTeam(UUID teamId, String status, Authentication auth) {
        ensureLeaderOrCoordinator(teamId, auth);
        List<TeamJoinRequest> list = (status == null || status.isBlank())
                ? requestRepository.findByTeamIdOrderByCreatedAtDesc(teamId)
                : requestRepository.findByTeamIdAndStatusOrderByCreatedAtDesc(
                        teamId, status.trim().toLowerCase());
        return list.stream().map(TeamJoinRequestService::toResponse).toList();
    }

    // listMine() — Student xem requests của chính mình
    @Transactional(readOnly = true)
    public List<JoinRequestResponse> listMine(Authentication auth) {
        return requestRepository.findByUserIdOrderByCreatedAtDesc(currentUserId(auth))
                .stream().map(TeamJoinRequestService::toResponse).toList();
    }

    // accept() — Leader chấp nhận → thêm user vào team ngay
    @Transactional
    public JoinRequestResponse accept(UUID id, Authentication auth) {
        TeamJoinRequest r = requestRepository.findWithRelationsById(id)
                .orElseThrow(() -> ApiException.notFound("Join request not found: " + id));
        ensureLeaderOrCoordinator(r.getTeam().getId(), auth);
        if (!"pending".equals(r.getStatus()))
            throw ApiException.badRequest("Request is not pending (current: " + r.getStatus() + ")");
        ensureRegistrationOpen(r.getTeam());

        // Race condition: user đã join bằng invite code trong lúc đó
        if (teamMemberRepository.existsByTeamIdAndUserId(r.getTeam().getId(), r.getUser().getId())) {
            r.setStatus("accepted");
            r.setRespondedAt(LocalDateTime.now());
            return toResponse(r);
        }
        if (teamMemberRepository.countByTeamId(r.getTeam().getId()) >= MAX_TEAM_SIZE)
            throw ApiException.badRequest("Team is already full");
        if (r.getUser().getStatus() != AccountStatus.approved)
            throw ApiException.badRequest("Only approved users can join teams");
        if (teamMemberRepository.existsActiveRegistrationInEvent(
                r.getUser().getId(), r.getTeam().getTrack().getEvent().getId()))
            throw ApiException.conflict("User already belongs to another active team in this event");

        teamMemberRepository.save(TeamMember.builder()
                .team(r.getTeam()).user(r.getUser()).role(TeamMemberRole.member).build());
        r.setStatus("accepted");
        r.setRespondedAt(LocalDateTime.now());
        return toResponse(r);
    }

    // reject() — Leader từ chối request
    @Transactional
    public JoinRequestResponse reject(UUID id, Authentication auth) {
        TeamJoinRequest r = requestRepository.findWithRelationsById(id)
                .orElseThrow(() -> ApiException.notFound("Join request not found: " + id));
        ensureLeaderOrCoordinator(r.getTeam().getId(), auth);
        if (!"pending".equals(r.getStatus()))
            throw ApiException.badRequest("Request is not pending (current: " + r.getStatus() + ")");
        r.setStatus("rejected");
        r.setRespondedAt(LocalDateTime.now());
        return toResponse(r);
    }

    // cancel() — Student hủy request của chính mình
    @Transactional
    public void cancel(UUID id, Authentication auth) {
        TeamJoinRequest r = requestRepository.findWithRelationsById(id)
                .orElseThrow(() -> ApiException.notFound("Join request not found: " + id));
        if (!r.getUser().getId().equals(currentUserId(auth)))
            throw ApiException.forbidden("You can only cancel your own request");
        if (!"pending".equals(r.getStatus()))
            throw ApiException.badRequest("Request is not pending");
        r.setStatus("cancelled");
        r.setRespondedAt(LocalDateTime.now());
    }

    // Guard: chỉ leader hoặc coordinator mới quản lý được requests
    private void ensureLeaderOrCoordinator(UUID teamId, Authentication auth) {
        if (isCoordinator(auth)) return;
        UUID callerId = currentUserId(auth);
        TeamMember m = teamMemberRepository.findByTeamIdAndUserId(teamId, callerId)
                .orElseThrow(() -> ApiException.forbidden(
                        "Only the team leader can manage join requests"));
        if (m.getRole() != TeamMemberRole.leader)
            throw ApiException.forbidden("Only the team leader can manage join requests");
    }
}
```

---

## 8. TeamController

**File:** `modules/team/controller/TeamController.java`

```java
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Hackathon teams and team members")
public class TeamController {

    // GET /teams?eventId=&trackId=
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<TeamResponse>> listByTrack(
            @RequestParam UUID eventId,
            @RequestParam(required = false) UUID trackId,
            Pageable pageable) {
        return ResponseEntity.ok(teamService.listByTrack(eventId, trackId, pageable));
    }

    // GET /teams/me — Danh sách team của user hiện tại
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TeamResponse>> myTeams(Authentication auth) {
        return ResponseEntity.ok(teamService.myTeams(auth));
    }

    // POST /teams/join — Join team bằng invite code
    @PostMapping("/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponse> join(
            @Valid @RequestBody JoinTeamRequest req, Authentication auth) {
        return ResponseEntity.ok(teamService.joinByInviteCode(req, auth));
    }

    // GET /teams/{id}
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponse> get(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(teamService.get(id, auth));
    }

    // POST /teams — Tạo team (user thường tạo cho mình, coordinator tạo thay)
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponse> create(
            @Valid @RequestBody CreateTeamRequest req, Authentication auth) {
        return ResponseEntity.ok(teamService.create(req, auth));
    }

    // PATCH /teams/{id} — Đổi tên team [COORDINATOR]
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<TeamResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateTeamRequest req) {
        return ResponseEntity.ok(teamService.update(id, req));
    }

    // POST /teams/{id}/move-track — Chuyển track [COORDINATOR]
    @PostMapping("/{id}/move-track")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<TeamResponse> moveTrack(
            @PathVariable UUID id,
            @Valid @RequestBody MoveTeamTrackRequest req,
            Authentication auth) {
        return ResponseEntity.ok(teamService.moveToTrack(id, req, auth));
    }

    // POST /teams/bulk-transfer — Chuyển nhiều team cùng lúc [COORDINATOR]
    @PostMapping("/bulk-transfer")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<TeamTransferDtos.TransferResult> bulkTransfer(
            @Valid @RequestBody TeamTransferDtos.BulkTransferRequest req) {
        return ResponseEntity.ok(transferService.bulkTransfer(req));
    }

    // POST /teams/{id}/members — Thêm member [COORDINATOR]
    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<TeamResponse> addMember(
            @PathVariable UUID id, @Valid @RequestBody AddTeamMemberRequest req) {
        return ResponseEntity.ok(teamService.addMember(id, req));
    }

    // DELETE /teams/{id}/members/{userId} — Xóa member [COORDINATOR]
    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id, @PathVariable UUID userId) {
        teamService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    // POST /teams/{id}/disqualify — Truất quyền [COORDINATOR] ⭐
    @PostMapping("/{id}/disqualify")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<TeamResponse> disqualify(
            @PathVariable UUID id,
            @Valid @RequestBody DisqualifyTeamRequest req,
            Authentication auth) {
        return ResponseEntity.ok(teamService.disqualify(id, req, auth));
    }

    // POST /teams/{id}/reactivate — Khôi phục team [COORDINATOR]
    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<TeamResponse> reactivate(
            @PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(teamService.reactivate(id, auth));
    }

    // DELETE /teams/{id} — Xóa team [COORDINATOR]
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        teamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 9. TeamJoinRequestController

**File:** `modules/team/controller/TeamJoinRequestController.java`

```java
@RestController
@RequestMapping("/join-requests")
@RequiredArgsConstructor
@Tag(name = "Team Join Requests")
public class TeamJoinRequestController {

    // POST /join-requests — Student gửi yêu cầu vào team
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JoinRequestResponse> create(
            @Valid @RequestBody CreateJoinRequestRequest req, Authentication auth) {
        return ResponseEntity.ok(service.create(req, auth));
    }

    // GET /join-requests?teamId=&status= — Leader/Coordinator xem requests
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JoinRequestResponse>> listForTeam(
            @RequestParam UUID teamId,
            @RequestParam(required = false) String status,
            Authentication auth) {
        return ResponseEntity.ok(service.listForTeam(teamId, status, auth));
    }

    // GET /join-requests/mine — Student xem requests của mình
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JoinRequestResponse>> listMine(Authentication auth) {
        return ResponseEntity.ok(service.listMine(auth));
    }

    // POST /join-requests/{id}/accept — Leader chấp nhận
    @PostMapping("/{id}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JoinRequestResponse> accept(
            @PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(service.accept(id, auth));
    }

    // POST /join-requests/{id}/reject — Leader từ chối
    @PostMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JoinRequestResponse> reject(
            @PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(service.reject(id, auth));
    }

    // DELETE /join-requests/{id} — Student hủy yêu cầu của mình
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancel(@PathVariable UUID id, Authentication auth) {
        service.cancel(id, auth);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 10. TeamMapper

**File:** `modules/team/mapper/TeamMapper.java`

```java
public final class TeamMapper {

    private TeamMapper() {}  // Utility class — không khởi tạo được

    public static TeamResponse toResponse(Team team, List<TeamMember> members) {
        return toResponse(team, members, true, List.of());
    }

    public static TeamResponse toResponse(
            Team team, List<TeamMember> members, boolean includeInviteCode) {
        return toResponse(team, members, includeInviteCode, List.of());
    }

    public static TeamResponse toResponse(
            Team team, List<TeamMember> members,
            boolean includeInviteCode,
            List<RecognitionDtos.Summary> recognitions) {
        return TeamResponse.builder()
                .id(team.getId())
                .teamProfileId(team.getTeamProfile() == null
                        ? null : team.getTeamProfile().getId())
                .sourceTeamId(team.getSourceTeam() == null
                        ? null : team.getSourceTeam().getId())
                .trackId(team.getTrack().getId())
                .eventId(team.getTrack().getEvent().getId())   // event qua track
                .name(team.getName())
                .inviteCode(includeInviteCode ? team.getInviteCode() : null)
                .status(team.getStatus())
                .disqualifiedReason(team.getDisqualifiedReason())
                .members(members.stream().map(TeamMapper::toMemberResponse).toList())
                .recognitions(recognitions == null ? List.of() : recognitions)
                .activatedFromProfileAt(team.getActivatedFromProfileAt())
                .rosterConfirmedAt(team.getRosterConfirmedAt())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }

    public static TeamMemberResponse toMemberResponse(TeamMember member) {
        return TeamMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .email(member.getUser().getEmail())
                .fullName(member.getUser().getFullName())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
```

---

## 11. So sánh cũ vs mới

### 11.1 Tính năng

| Tính năng | Phiên bản bạn viết (cũ) | Project chung (hiện tại) |
|---|---|---|
| **Lock team** | ✅ `PATCH /teams/{id}/lock` — native SQL `locked_at = NOW()` | ❌ Không merge vào |
| **Disqualify team** | ✅ `PATCH /teams/{id}/disqualify` — native SQL `status = 'disqualified'` | ✅ `POST /teams/{id}/disqualify` — JPA `setStatus()` |
| Reactivate | ❌ | ✅ `POST /teams/{id}/reactivate` |
| Timeline | ❌ | ✅ `TimelineService.record()` |
| Invite code join | ❌ | ✅ `POST /teams/join` |
| Join request flow | ❌ | ✅ `TeamJoinRequestService` |
| Move track | ❌ | ✅ `POST /teams/{id}/move-track` |
| Bulk transfer | ❌ | ✅ `POST /teams/bulk-transfer` |

### 11.2 Thiết kế

| Khía cạnh | Phiên bản cũ | Phiên bản hiện tại |
|---|---|---|
| Auth | `X-User-Id` header → query DB kiểm tra role | JWT → Spring Security `@PreAuthorize` |
| Error | `throw new BusinessException(ErrorCode.TEAM_NOT_FOUND)` | `throw ApiException.notFound("Team not found: " + id)` |
| DB update | Native SQL (trigger-managed columns) | JPA setters trực tiếp |
| EntityManager | Cần `flush()` + `refresh()` sau native query | Không cần |
| Member model | `TeamMember` có status: `invited/accepted/declined/removed` | `TeamMember` chỉ có `role` + `joinedAt` |
| Team → Event | Liên kết trực tiếp `event_id` | Qua `track_id` → `track.event` |
| URL structure | `/events/{eventId}/teams` và `/teams/{teamId}` | `/teams?eventId=` |
| AuditLog | `(userId, eventId, teamId, action, details)` | `(user, team, action, oldValue, newValue, details)` |

### 11.3 TeamRepository

| Method | Cũ | Hiện tại |
|---|---|---|
| `lockTeam()` | `@Modifying` native UPDATE `locked_at = NOW()` | ❌ Không có |
| `disqualifyTeam()` | `@Modifying` native UPDATE `CAST('disqualified' AS team_status)` | ❌ JPA xử lý |
| List teams | Native query với `CAST(:status AS team_status)` | Derived method + `@EntityGraph` |
| Fetch by id | `JOIN FETCH` trong `@Query` | `findWithTrackById()` với `@EntityGraph` |

### 11.4 TeamMemberRepository

| Method | Cũ | Hiện tại |
|---|---|---|
| `findActiveLeaderByTeamId()` | Native query (CAST enum) | ❌ Không cần (không còn status) |
| `existsActiveInEvent()` | Native query (CAST enum) | JPQL `existsActiveRegistrationInEvent()` |
| `countActiveByTeamId()` | Native query (CAST enum) | `countByTeamId()` (không có status) |
| `findByTeamIdFiltered()` | Native query với status filter | `findByTeamIdOrderByRoleAscJoinedAtAsc()` |

---

## 12. Câu hỏi bảo vệ & gợi ý trả lời

### Q1: Bạn implement tính năng gì cho module Team?

> Tôi implement hai tính năng coordinator: **Lock Team** và **Disqualify Team**.
> Ngoài ra implement CRUD team và quản lý thành viên (mời, chấp nhận/từ chối, xóa, đổi vai trò).
> Sau khi merge vào project chung, **Lock Team không được giữ lại** (thay bằng event status guard),
> còn **Disqualify được refactor** từ native SQL sang JPA setStatus() và thêm timeline event.

---

### Q2: Tại sao phiên bản cũ của bạn phải dùng native SQL cho disqualify/lock?

> Trong phiên bản tôi viết, database dùng **PostgreSQL custom ENUM type** (`team_status`).
> Field `status` trong entity được map với `updatable = false` vì DB trigger quản lý nó.
>
> Nếu gọi `team.setStatus(TeamStatus.disqualified)`, Hibernate bỏ qua field này (vì `updatable=false`).
>
> Còn nếu dùng JPQL với parameter binding, PostgreSQL báo lỗi:
> `operator does not exist: team_status = character varying`
> vì không có implicit cast từ `varchar` sang custom enum.
>
> Giải pháp: native SQL với `CAST('disqualified' AS team_status)` để bypass cả hai vấn đề.
> Sau đó `@Modifying(clearAutomatically = true)` để Hibernate reload entity từ DB.

---

### Q3: Phiên bản hiện tại không cần native SQL — tại sao?

> Phiên bản hiện tại thiết kế lại entity: `status` field **không còn** `updatable = false`,
> và database không dùng trigger để quản lý status nữ.
> Vì vậy `team.setStatus(TeamStatus.disqualified)` hoạt động bình thường qua JPA dirty-checking.
> Đơn giản hơn, ít phụ thuộc vào PostgreSQL-specific behavior hơn.

---

### Q4: Lock team và Disqualify team khác nhau như thế nào?

> **Lock** (trong phiên bản cũ): trạng thái tạm thời, granular cho từng team.
> Set `locked_at = NOW()` — team vẫn tham gia thi nhưng không thể thêm/xóa thành viên.
> Coordinator lock để đóng băng roster trước vòng thi. Team có thể vừa `active` vừa `locked`.
>
> **Disqualify**: trạng thái terminal, `status = disqualified`. Team bị loại khỏi cuộc thi hoàn toàn.
> Ảnh hưởng đến prize (PrizeService chặn trao giải cho team disqualified).
> Trong phiên bản mới có thể reactivate nếu event còn editable.

---

### Q5: validateIsCoordinator() trong phiên bản cũ và @PreAuthorize hiện tại khác gì?

> **Cũ:** Mỗi method service phải tự gọi `validateIsCoordinator(callerId)` —
> query DB `userRepository.hasRole(callerId, "coordinator")`. Nếu quên gọi thì endpoint không được bảo vệ.
>
> **Hiện tại:** `@PreAuthorize("hasRole('COORDINATOR')")` ở Controller —
> Spring Security intercept trước khi method được gọi, check từ JWT token đã parsed sẵn.
> Không query DB, không thể bị quên, fail-fast ngay tại framework layer.

---

### Q6: Cơ chế invite code hoạt động thế nào?

> Khi tạo team, `generateInviteCode()` sinh mã 6 ký tự từ UUID (substring 0-5, uppercase).
> Kiểm tra unique trong DB, retry tối đa 10 lần. Lần thứ 11 dùng 10 ký tự để tăng entropy.
>
> Student nhập code vào `POST /teams/join`. Service normalize code (bỏ prefix "SEAL-", dấu gạch),
> tìm team bằng `findByInviteCodeIgnoreCase()`, kiểm tra team chưa full, rồi thêm user vào.
> Idempotent: nếu đã là member rồi thì trả về luôn không báo lỗi.
>
> `inviteCode` chỉ được trả về trong response khi caller là coordinator hoặc member của team.

---

### Q7: Tại sao disqualify trong phiên bản hiện tại dùng POST thay vì PATCH?

> **Cũ dùng PATCH**: đúng về REST conventions vì chỉ update một phần trạng thái (partial update).
>
> **Hiện tại dùng POST**: vì đây là **action** có side effects phức tạp —
> ghi audit log, ghi timeline event, không đơn thuần là update field.
> Theo REST conventions, state transitions với side effects thường được biểu diễn
> bằng POST action endpoint riêng (`/disqualify`, `/reactivate`) để tường minh hơn về intent.

---

### Q8: AuditLog ghi lại gì khi disqualify?

> **Phiên bản cũ:** `(userId, eventId, teamId, action=DISQUALIFY_TEAM, details=reason)` — flat structure.
>
> **Phiên bản hiện tại** (structured hơn):
> ```
> user       = coordinator thực hiện
> team       = team bị tác động
> action     = DISQUALIFY_TEAM
> targetType = "team"
> targetId   = team.id
> oldValue   = "active"
> newValue   = "disqualified"
> details    = reason text
> ```
> `oldValue`/`newValue` cho phép truy vết lịch sử thay đổi trạng thái chính xác hơn.

---

### Q9: addMemberInternal() kiểm tra những gì?

> 1. User đã là member của team này chưa (`existsByTeamIdAndUserId`).
> 2. User đang trong team active khác của cùng event chưa (`existsActiveRegistrationInEvent`) — tránh đăng ký 2 team.
> 3. Nếu role là leader: team đã có leader chưa (`existsByTeamIdAndRole`) — tránh 2 leader.
> 4. User account phải là `approved` — chặn tài khoản chưa được duyệt.
> 5. Nếu là leader: cấp role `team_leader` cho user account (idempotent).

---

### Q10: reactivate() kiểm tra những gì?

> 1. `ensureEditable(track)` — event phải còn `draft` hoặc `published` (chưa kết thúc).
> 2. Team phải đang ở trạng thái `disqualified` (không reactivate team `active` hoặc trạng thái khác).
> 3. TeamProfile phải còn `active` — đảm bảo team có identity hợp lệ.
>
> Sau khi qua hết checks: `setStatus(active)`, `setDisqualifiedReason(null)`,
> ghi audit log và timeline event.
