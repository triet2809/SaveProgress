package vn.edu.fpt.seal.modules.chat.dto;
import java.time.LocalDateTime; import java.util.UUID;
public record TeamChatMessageResponse(UUID id,UUID teamId,UUID senderId,String senderName,String senderEmail,String message,LocalDateTime createdAt){}
