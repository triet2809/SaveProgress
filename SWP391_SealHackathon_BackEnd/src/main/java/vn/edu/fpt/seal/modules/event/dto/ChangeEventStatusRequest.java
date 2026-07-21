package vn.edu.fpt.seal.modules.event.dto;

import jakarta.validation.constraints.NotNull;
import vn.edu.fpt.seal.common.enums.EventStatus;

public record ChangeEventStatusRequest(
        @NotNull EventStatus status
) {
}
