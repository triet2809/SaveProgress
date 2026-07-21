package vn.edu.fpt.seal.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import vn.edu.fpt.seal.common.enums.StudentType;

import java.util.UUID;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(max = 255) String fullName,
        StudentType studentType,
        String studentId,
        UUID universityId,
        @Size(max = 255) String universityName,
        UUID campusId,
        @NotNull @AssertTrue(message = "Terms and Privacy Policy acceptance is required") Boolean acceptedTerms
) {}
