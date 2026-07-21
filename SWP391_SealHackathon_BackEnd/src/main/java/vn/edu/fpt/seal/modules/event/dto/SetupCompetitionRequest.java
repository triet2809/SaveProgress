package vn.edu.fpt.seal.modules.event.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * One-shot "build the competition" wizard input (fired when registration closes).
 * <p>
 * - tracks: the thematic groups to create. If null/empty, a single "General" track
 *   is used and every registered team stays in it.
 * - finalistCount (F): desired number of finalists PER TRACK. If null, defaults to
 *   max(3, ceil(0.1 * teamsInTrack)).
 * - roundCount (R): number of elimination rounds PER TRACK. If null, suggested from
 *   team count: &lt;=8 -&gt; 1, 9-30 -&gt; 2, 31-80 -&gt; 3, &gt;80 -&gt; 4.
 */
public record SetupCompetitionRequest(
        List<TrackSpec> tracks,
        @Min(1) Integer finalistCount,
        @Min(1) Integer roundCount,
        List<LogicalRoundSpec> roundPlan
) {
    public SetupCompetitionRequest(List<TrackSpec> tracks, Integer finalistCount, Integer roundCount) {
        this(tracks, finalistCount, roundCount, null);
    }

    public record TrackSpec(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 10000) String description
    ) {
    }

    public record LogicalRoundSpec(
            @NotBlank @Size(max = 255) String name,
            @Min(1) Integer sequenceNumber,
            Boolean finalRound,
            @Min(1) Integer defaultTopNToPromote,
            List<RoundTrackSpec> tracks
    ) {
    }

    public record RoundTrackSpec(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 10000) String description,
            @Min(1) Integer topNToPromote
    ) {
    }
}
