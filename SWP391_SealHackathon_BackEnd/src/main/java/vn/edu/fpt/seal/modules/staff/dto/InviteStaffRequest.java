package vn.edu.fpt.seal.modules.staff.dto;

import jakarta.validation.constraints.*;
import java.util.*;

public record InviteStaffRequest(
        @NotBlank @Size(max=255) String fullName,
        @NotBlank @Email String email,
        @NotEmpty Set<@NotBlank String> roles,
        Set<UUID> mentorTrackIds,
        Set<UUID> judgeTrackIds,
        Set<UUID> judgeRoundIds,
        UUID universityId,
        UUID campusId,
        @Size(min=8, max=72) String temporaryPassword) {}
