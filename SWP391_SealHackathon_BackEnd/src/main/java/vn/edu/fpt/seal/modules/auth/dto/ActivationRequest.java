package vn.edu.fpt.seal.modules.auth.dto;

import jakarta.validation.constraints.*;

public record ActivationRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank String confirmPassword,
        @NotNull @AssertTrue(message = "Terms and Privacy Policy acceptance is required") Boolean acceptedTerms) {}
