package vn.edu.fpt.seal.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRolesRequest(@NotNull @Size(min = 1) Set<@NotBlank String> roles) {
}
