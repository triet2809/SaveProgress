package vn.edu.fpt.seal.modules.staff.dto;

import jakarta.validation.constraints.NotNull;
import java.util.*;

public record UpdateStaffAssignmentsRequest(
        @NotNull Set<UUID> mentorTrackIds,
        @NotNull Set<UUID> judgeTrackIds,
        @NotNull Set<UUID> judgeRoundIds) {}
