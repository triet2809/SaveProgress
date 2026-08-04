package vn.edu.fpt.seal.modules.user.dto;

import lombok.Builder;
import vn.edu.fpt.seal.common.enums.AccountStatus;
import vn.edu.fpt.seal.common.enums.StudentType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String email,
        String fullName,
        StudentType studentType,
        String studentId,
        String phone,
        String department,
        String position,
        String company,
        String expertise,
        String bio,
        UUID universityId,
        String universityName,
        UUID campusId,
        String campusName,
        boolean isGuest,
        AccountStatus status,
        List<String> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
