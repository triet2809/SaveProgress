package vn.edu.fpt.seal.modules.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO đầu vào gửi tin nhắn chat đội: teamId bắt buộc, nội dung tối đa 5000 ký tự.
 */
public record CreateTeamChatMessageRequest(@NotNull UUID teamId, @NotBlank @Size(max = 5000) String message) {
}
