package vn.edu.fpt.seal.modules.appeal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tập hợp các DTO (record) dùng cho luồng khiếu nại kết quả.
 * Lớp final chứa các record lồng nhau, không cho phép khởi tạo.
 */
public final class AppealDtos {
    private AppealDtos() {
    }

    /**
     * Dữ liệu đầu vào tạo khiếu nại: id vòng thi và lý do (tối đa 10000 ký tự).
     */
    public record Create(@NotNull UUID roundId, @NotBlank @Size(max = 10000) String reason) {
    }

    /**
     * Dữ liệu phản hồi/yêu cầu làm rõ từ coordinator.
     */
    public record Respond(@NotBlank @Size(max = 10000) String response) {
    }

    /**
     * Dữ liệu giải quyết khiếu nại: trạng thái quyết định, phản hồi, cờ tính lại.
     */
    public record Resolve(@NotBlank String status, String response, Boolean recalculationRequired) {
    }

    /**
     * Dữ liệu trả về đầy đủ của một khiếu nại, gồm thông tin đội, vòng, trạng thái và số giây còn lại để khiếu nại.
     */
    public record Response(UUID id, UUID eventId, UUID roundId, String roundName, UUID teamId, String teamName,
                           UUID submittedBy,
                           String reason, String status, String response, String decision,
                           boolean recalculationRequired, Integer resultVersion,
                           LocalDateTime resultPublishedAt, LocalDateTime appealDeadline, LocalDateTime createdAt,
                           LocalDateTime resolvedAt,
                           String lifecycleState, long remainingSeconds) {
    }
}
