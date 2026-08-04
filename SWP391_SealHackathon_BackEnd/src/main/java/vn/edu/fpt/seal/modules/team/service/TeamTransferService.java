package vn.edu.fpt.seal.modules.team.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.common.enums.TeamStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.participant.repository.RoundParticipantRepository;
import vn.edu.fpt.seal.modules.ranking.repository.RoundRankingRepository;
import vn.edu.fpt.seal.modules.resultversion.repository.RoundResultVersionEntryRepository;
import vn.edu.fpt.seal.modules.seeding.repository.EventSeedAssignmentRepository;
import vn.edu.fpt.seal.modules.seeding.repository.EventTeamFinishRepository;
import vn.edu.fpt.seal.modules.submission.repository.SubmissionRepository;
import vn.edu.fpt.seal.modules.team.dto.TeamTransferDtos;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamTransferService {
    private final TeamRepository teams;
    private final TrackRepository tracks;
    private final SubmissionRepository submissions;
    private final RoundParticipantRepository participants;
    private final RoundRankingRepository rankings;
    private final RoundResultVersionEntryRepository resultEntries;
    private final EventTeamFinishRepository finishes;
    private final EventSeedAssignmentRepository seeds;

    @Transactional
    public TeamTransferDtos.TransferResult bulkTransfer(TeamTransferDtos.BulkTransferRequest request) {
        if (new HashSet<>(request.teamIds()).size() != request.teamIds().size()) {
            throw ApiException.badRequest("Duplicate team selection is not allowed");
        }
        Track target = tracks.findById(request.targetTrackId())
                .orElseThrow(() -> ApiException.notFound("Target track not found"));
        requireOpen(target);
        Map<UUID, Team> selected = teams.findAllById(request.teamIds()).stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
        List<TeamTransferDtos.ExcludedTeam> excluded = new ArrayList<>();
        for (UUID id : request.teamIds()) {
            Team team = selected.get(id);
            if (team == null) {
                excluded.add(new TeamTransferDtos.ExcludedTeam(id, null, "Team not found"));
                continue;
            }
            if (!team.getTrack().getEvent().getId().equals(target.getEvent().getId())) {
                excluded.add(excluded(team, "Team belongs to another event"));
                continue;
            }
            String reason = ineligibleReason(team);
            if (reason != null) excluded.add(excluded(team, reason));
            else if (!team.getTrack().getId().equals(target.getId())
                    && teams.existsByTrackIdAndNameIgnoreCase(target.getId(), team.getName())) {
                excluded.add(excluded(team, "Team name already exists in target track"));
            }
        }
        if (!excluded.isEmpty()) {
            throw ApiException.badRequest("Bulk transfer validation failed: " + excluded.stream()
                    .map(item -> item.teamId() + " (" + item.reason() + ")").collect(Collectors.joining(", ")));
        }
        List<TeamTransferDtos.TeamMove> moves = new ArrayList<>();
        for (UUID id : request.teamIds()) {
            Team team = selected.get(id);
            UUID current = team.getTrack().getId();
            if (!current.equals(target.getId())) {
                team.setTrack(target);
                moves.add(new TeamTransferDtos.TeamMove(team.getId(), team.getName(), current, target.getId()));
            }
        }
        teams.saveAll(selected.values());
        return new TeamTransferDtos.TransferResult(moves, List.of(), counts(target.getEvent().getId(), List.of(target)));
    }

    @Transactional(readOnly = true)
    public TeamTransferDtos.TransferResult previewBalance(UUID eventId, TeamTransferDtos.BalanceRequest request) {
        List<Track> targetTracks = targetTracks(eventId, request.targetTrackIds());
        List<Team> eventTeams = teams.findByTrackEventId(eventId);
        List<TeamTransferDtos.ExcludedTeam> excluded = new ArrayList<>();
        List<Team> eligible = new ArrayList<>();
        for (Team team : eventTeams) {
            String reason = ineligibleReason(team);
            if (reason == null) eligible.add(team);
            else excluded.add(excluded(team, reason));
        }
        eligible.sort(Comparator.comparing(team -> team.getId().toString()));
        if (request.randomSeed() != null) Collections.shuffle(eligible, new Random(request.randomSeed()));

        Map<UUID, Long> before = eventTeams.stream().collect(Collectors.groupingBy(
                team -> team.getTrack().getId(), Collectors.counting()));
        Map<UUID, Long> after = new LinkedHashMap<>();
        Map<UUID, Set<String>> names = new HashMap<>();
        for (Track track : targetTracks) {
            after.put(track.getId(), eventTeams.stream()
                    .filter(team -> team.getTrack().getId().equals(track.getId()) && !eligible.contains(team)).count());
            names.put(track.getId(), eventTeams.stream()
                    .filter(team -> team.getTrack().getId().equals(track.getId()) && !eligible.contains(team))
                    .map(team -> team.getName().toLowerCase(Locale.ROOT)).collect(Collectors.toSet()));
        }
        List<TeamTransferDtos.TeamMove> moves = new ArrayList<>();
        for (Team team : eligible) {
            Track target = targetTracks.stream()
                    .filter(track -> !names.get(track.getId()).contains(team.getName().toLowerCase(Locale.ROOT)))
                    .min(Comparator.comparingLong(track -> after.get(track.getId())))
                    .orElse(null);
            if (target == null) {
                excluded.add(excluded(team, "No target track can accept the team name without conflict"));
                UUID currentTrackId = team.getTrack().getId();
                if (after.containsKey(currentTrackId)) {
                    after.put(currentTrackId, after.get(currentTrackId) + 1);
                }
                continue;
            }
            names.get(target.getId()).add(team.getName().toLowerCase(Locale.ROOT));
            after.put(target.getId(), after.get(target.getId()) + 1);
            moves.add(new TeamTransferDtos.TeamMove(team.getId(), team.getName(),
                    team.getTrack().getId(), target.getId()));
        }
        List<TeamTransferDtos.TrackCount> trackCounts = targetTracks.stream()
                .map(track -> new TeamTransferDtos.TrackCount(track.getId(), track.getName(),
                        before.getOrDefault(track.getId(), 0L), after.getOrDefault(track.getId(), 0L))).toList();
        return new TeamTransferDtos.TransferResult(moves, excluded, trackCounts);
    }

    @Transactional
    public TeamTransferDtos.TransferResult applyBalance(UUID eventId, TeamTransferDtos.BalanceRequest request) {
        TeamTransferDtos.TransferResult preview = previewBalance(eventId, request);
        Map<UUID, Team> byId = teams.findAllById(preview.moves().stream()
                        .map(TeamTransferDtos.TeamMove::teamId).toList()).stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
        Map<UUID, Track> trackById = targetTracks(eventId, request.targetTrackIds()).stream()
                .collect(Collectors.toMap(Track::getId, Function.identity()));
        for (TeamTransferDtos.TeamMove move : preview.moves()) {
            if (!move.currentTrackId().equals(move.proposedTrackId())) {
                byId.get(move.teamId()).setTrack(trackById.get(move.proposedTrackId()));
            }
        }
        teams.saveAll(byId.values());
        return preview;
    }

    private List<Track> targetTracks(UUID eventId, List<UUID> ids) {
        if (new HashSet<>(ids).size() != ids.size()) throw ApiException.badRequest("Duplicate target track selection");
        List<Track> result = new ArrayList<>(tracks.findAllById(ids));
        if (result.size() != ids.size()) throw ApiException.notFound("One or more target tracks were not found");
        if (result.stream().anyMatch(track -> !track.getEvent().getId().equals(eventId))) {
            throw ApiException.badRequest("All target tracks must belong to the selected event");
        }
        result.forEach(this::requireOpen);
        result.sort(Comparator.comparing(track -> ids.indexOf(track.getId())));
        return result;
    }

    private String ineligibleReason(Team team) {
        if (team.getStatus() != TeamStatus.active) return "Team is not active";
        EventStatus status = team.getTrack().getEvent().getStatus();
        if (status != EventStatus.draft && status != EventStatus.published)
            return "Event lifecycle locks team transfers";
        if (submissions.existsByTeamId(team.getId())) return "Team has submissions";
        if (participants.existsByTeamId(team.getId())) return "Team participates in a round";
        if (rankings.existsByTeamId(team.getId())) return "Team has ranking data";
        if (resultEntries.existsByTeamId(team.getId())) return "Team has published result data";
        if (finishes.existsByTeamId(team.getId())) return "Team has finalized historical results";
        if (seeds.existsByEventIdAndTeamId(team.getTrack().getEvent().getId(), team.getId()))
            return "Team has seed metadata";
        return null;
    }

    private void requireOpen(Track track) {
        EventStatus status = track.getEvent().getStatus();
        if (status != EventStatus.draft && status != EventStatus.published) {
            throw ApiException.badRequest("Team transfers are closed for event status " + status);
        }
    }

    private TeamTransferDtos.ExcludedTeam excluded(Team team, String reason) {
        return new TeamTransferDtos.ExcludedTeam(team.getId(), team.getName(), reason);
    }

    private List<TeamTransferDtos.TrackCount> counts(UUID eventId, List<Track> selectedTracks) {
        Map<UUID, Long> count = teams.findByTrackEventId(eventId).stream()
                .collect(Collectors.groupingBy(team -> team.getTrack().getId(), Collectors.counting()));
        return selectedTracks.stream().map(track -> new TeamTransferDtos.TrackCount(track.getId(), track.getName(),
                count.getOrDefault(track.getId(), 0L), count.getOrDefault(track.getId(), 0L))).toList();
    }
}
