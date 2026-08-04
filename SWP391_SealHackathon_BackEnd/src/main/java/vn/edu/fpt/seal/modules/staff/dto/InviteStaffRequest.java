package vn.edu.fpt.seal.modules.staff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record InviteStaffRequest(
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Email String email,
        @NotEmpty Set<@NotBlank String> roles,
        Set<UUID> mentorTrackIds,
        Set<UUID> judgeTrackIds,
        Set<UUID> judgeRoundIds,
        UUID universityId,
        UUID campusId,
        @Size(min = 8, max = 72) String temporaryPassword) {
}
