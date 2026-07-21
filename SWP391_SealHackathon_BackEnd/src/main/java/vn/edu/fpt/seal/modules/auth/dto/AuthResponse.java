package vn.edu.fpt.seal.modules.auth.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserSummary user
) {
    @Builder
    public record UserSummary(
            UUID id,
            String email,
            String fullName,
            String status,
            String studentType,
            UUID universityId,
            String universityName,
            UUID campusId,
            String campusName,
            boolean isGuest,
            List<String> roles,
            boolean mustChangePassword,
            boolean termsAcceptanceRequired,
            boolean onboardingRequired
    ) {}
}
