package vn.edu.fpt.seal.modules.chat.dto;
import jakarta.validation.constraints.*; import java.util.UUID;
public record CreateTeamChatMessageRequest(@NotNull UUID teamId,@NotBlank @Size(max=5000) String message){}
