package vn.edu.fpt.seal.modules.appeal.dto;
import jakarta.validation.constraints.*; import java.time.LocalDateTime; import java.util.UUID;
public final class AppealDtos {
 private AppealDtos(){}
 public record Create(@NotNull UUID roundId,@NotBlank @Size(max=10000) String reason){}
 public record Respond(@NotBlank @Size(max=10000) String response){}
 public record Resolve(@NotBlank String status,String response,Boolean recalculationRequired){}
 public record Response(UUID id,UUID eventId,UUID roundId,String roundName,UUID teamId,String teamName,UUID submittedBy,
  String reason,String status,String response,String decision,boolean recalculationRequired,Integer resultVersion,
  LocalDateTime resultPublishedAt,LocalDateTime appealDeadline,LocalDateTime createdAt,LocalDateTime resolvedAt,
  String lifecycleState,long remainingSeconds){}
}
