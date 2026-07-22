package vn.edu.fpt.seal.modules.casework.dto;
import jakarta.validation.constraints.NotBlank;
/**
 * DTO đầu vào cập nhật trạng thái case, kèm ghi chú tuỳ chọn.
 * status được ánh xạ sang IncidentStatus ở tầng service.
 */
public record UpdateCaseStatusRequest(@NotBlank String status, String note) {}
