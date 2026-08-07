package vn.edu.fpt.seal.modules.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.seal.common.enums.AccountStatus;

/**
 * DTO cập nhật trạng thái tài khoản.
 *
 * @param status          trạng thái mới (pending/approved/rejected)
 * @param rejectionReason lý do từ chối; bắt buộc khi status = rejected
 */
public record UpdateUserStatusRequest(
        @NotNull AccountStatus status,
        @Size(max = 2000) String rejectionReason
) {
}
