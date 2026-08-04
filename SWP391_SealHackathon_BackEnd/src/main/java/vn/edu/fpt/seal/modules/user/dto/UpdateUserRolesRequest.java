package vn.edu.fpt.seal.modules.user.dto;

import jakarta.validation.constraints.*;

import java.util.*;

public record UpdateUserRolesRequest(@NotNull @Size(min = 1) Set<@NotBlank String> roles) {
}
