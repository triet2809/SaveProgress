package vn.edu.fpt.seal.modules.casework.dto;
import jakarta.validation.constraints.NotBlank;
public record UpdateCaseStatusRequest(@NotBlank String status, String note) {}
