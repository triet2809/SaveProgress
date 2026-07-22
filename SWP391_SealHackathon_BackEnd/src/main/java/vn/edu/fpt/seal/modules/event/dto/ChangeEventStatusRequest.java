package vn.edu.fpt.seal.modules.event.dto;

import jakarta.validation.constraints.NotNull;
import vn.edu.fpt.seal.common.enums.EventStatus;

/**
 * DTO thay đổi trạng thái của sự kiện.
 *
 * @param status trạng thái mới của sự kiện, bắt buộc
 */
public record ChangeEventStatusRequest(
        @NotNull EventStatus status
) {
}
