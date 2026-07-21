package vn.edu.fpt.seal.modules.user.dto;

import jakarta.validation.constraints.*;
import vn.edu.fpt.seal.common.enums.AccountStatus;
import vn.edu.fpt.seal.common.enums.StudentType;

import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(max = 255) String fullName,
        StudentType studentType,
        @Size(max = 100) String studentId,
        @Size(max = 50) String phone,
        @Size(max = 255) String department,
        @Size(max = 255) String position,
        @Size(max = 255) String company,
        @Size(max = 255) String expertise,
        @Size(max = 2000) String bio,
        UUID universityId,
        UUID campusId,
        AccountStatus status,
        Set<@NotBlank String> roles
) {}
