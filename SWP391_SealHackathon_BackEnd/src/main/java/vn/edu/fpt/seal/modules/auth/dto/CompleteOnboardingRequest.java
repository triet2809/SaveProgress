package vn.edu.fpt.seal.modules.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record CompleteOnboardingRequest(
        @Size(min = 8, max = 72) String newPassword,
        String confirmPassword,
        @jakarta.validation.constraints.NotNull
        @AssertTrue(message = "Terms and Privacy Policy acceptance is required") Boolean acceptedTerms) {}
