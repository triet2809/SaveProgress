package vn.edu.fpt.seal.modules.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO trả về một tin nhắn chat đội, kèm thông tin người gửi (tên, email).
 */
public record TeamChatMessageResponse(UUID id, UUID teamId, UUID senderId, String senderName, String senderEmail,
                                      String message, LocalDateTime createdAt) {
}
