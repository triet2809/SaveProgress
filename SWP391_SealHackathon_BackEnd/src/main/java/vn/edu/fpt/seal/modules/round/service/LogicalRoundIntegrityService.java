package vn.edu.fpt.seal.modules.round.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.RoundLifecycleState;
import vn.edu.fpt.seal.common.enums.RoundParticipantStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.appeal.repository.AppealRepository;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.criteria.repository.RoundCriterionRepository;
import vn.edu.fpt.seal.modules.judge.repository.RoundJudgeRepository;
import vn.edu.fpt.seal.modules.participant.entity.RoundParticipant;
import vn.edu.fpt.seal.modules.participant.repository.RoundParticipantRepository;
import vn.edu.fpt.seal.modules.ranking.repository.RoundRankingRepository;
import vn.edu.fpt.seal.modules.resultversion.repository.RoundResultVersionRepository;
import vn.edu.fpt.seal.modules.resultversion.repository.RoundResultVersionEntryRepository;
import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersion;
import vn.edu.fpt.seal.modules.resultversion.entity.RoundResultVersionEntry;
import vn.edu.fpt.seal.modules.round.dto.LogicalRoundProgressionDtos.*;
import vn.edu.fpt.seal.modules.round.dto.LogicalRoundResponse;
import vn.edu.fpt.seal.modules.round.dto.UpdateLogicalRoundRequest;
import vn.edu.fpt.seal.modules.round.entity.*;
import vn.edu.fpt.seal.modules.round.mapper.RoundMapper;
import vn.edu.fpt.seal.modules.round.repository.*;
import vn.edu.fpt.seal.modules.submission.repository.SubmissionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogicalRoundIntegrityService {
    private final RoundDefinitionRepository definitions;
    private final RoundRepository rounds;
    private final LogicalRoundPromotionRepository promotions;
    private final RoundParticipantRepository participants;
    private final SubmissionRepository submissions;
    private final RoundRankingRepository rankings;
    private final RoundResultVersionRepository resultVersions;
    private final RoundResultVersionEntryRepository resultVersionEntries;
    private final AppealRepository appeals;
    private final RoundCriterionRepository criteria;
    private final RoundJudgeRepository roundJudges;

    @Transactional(readOnly = true)
    public List<LogicalRoundResponse> listByEvent(UUID eventId) {
        List<RoundDefinition> defs = definitions.findByEventIdOrderBySequenceNumberAsc(eventId);
        return defs.stream()
                .map(def -> response(def, rounds.findByLogicalRoundId(def.getId())))
                .toList();
    }

    @Transactional
    public LogicalRoundResponse update(UUID id, UpdateLogicalRoundRequest request) {
        RoundDefinition definition = definition(id);
        List<Round> executions = rounds.findByLogicalRoundId(id);
        String name = request.name() == null ? definition.getName() : request.name().trim();
        int sequence = request.sequenceNumber() == null ? definition.getSequenceNumber() : request.sequenceNumber();
        boolean finalRound = request.finalRound() == null ? definition.isFinalRound() : request.finalRound();
        definitions.findByEventIdAndNameIgnoreCase(definition.getEvent().getId(), name)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw ApiException.conflict("Logical round name already exists in this event"); });
        definitions.findByEventIdAndSequenceNumber(definition.getEvent().getId(), sequence)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw ApiException.conflict("Logical round sequence already exists in this event"); });
        if (sequence != definition.getSequenceNumber()
                && (promotions.existsBySourceLogicalRoundId(id)
                || promotions.existsByTargetLogicalRoundId(id))) {
            throw ApiException.conflict("Logical round sequence cannot change after promotion dependencies exist");
        }
        if (finalRound && executions.size() != 1) {
            throw ApiException.conflict("A final logical round must contain exactly one track execution");
        }
        definition.setName(name);
        definition.setSequenceNumber(sequence);
        definition.setFinalRound(finalRound);
        if (request.defaultTopNToPromote() != null) {
            definition.setDefaultTopNToPromote(request.defaultTopNToPromote());
        }
        executions.forEach(round -> {
            round.setName(name);
            round.setSequenceNumber(sequence);
        });
        definitions.flush();
        return response(definition, executions);
    }

    @Transactional
    public void deleteExecution(UUID roundId) {
        Round execution = rounds.findById(roundId)
                .orElseThrow(() -> ApiException.notFound("Round track execution not found: " + roundId));
        assertSafe(execution);
        RoundDefinition definition = execution.getLogicalRound();
        rounds.delete(execution);
        rounds.flush();
        if (rounds.findByLogicalRoundId(definition.getId()).isEmpty()) {
            assertNoPromotionDependency(definition.getId());
            definitions.delete(definition);
        }
    }

    @Transactional
    public void deleteLogical(UUID logicalRoundId) {
        RoundDefinition definition = definition(logicalRoundId);
        assertNoPromotionDependency(logicalRoundId);
        List<Round> executions = rounds.findByLogicalRoundId(logicalRoundId);
        executions.forEach(this::assertSafe);
        rounds.deleteAll(executions);
        rounds.flush();
        definitions.delete(definition);
    }

    @Transactional(readOnly = true)
    public List<PromotedTeam> promotedUnassigned(UUID targetLogicalRoundId) {
        definition(targetLogicalRoundId);
        return promotions.findByTargetLogicalRoundIdOrderByTeamNameAsc(targetLogicalRoundId).stream()
                .filter(p -> !participants.existsByRoundLogicalRoundIdAndTeamId(targetLogicalRoundId, p.getTeam().getId()))
                .map(p -> new PromotedTeam(p.getTeam().getId(), p.getTeam().getName(),
                        p.getSourceLogicalRound().getId(), targetLogicalRoundId, null))
                .toList();
    }

    @Transactional
    public void assignManual(UUID targetLogicalRoundId, ManualAssignmentRequest request) {
        RoundDefinition target = definition(targetLogicalRoundId);
        List<Round> executions = rounds.findByLogicalRoundId(targetLogicalRoundId);
        Map<UUID, Round> byId = executions.stream().collect(Collectors.toMap(Round::getId, r -> r));
        if (target.isFinalRound() && executions.size() != 1) {
            throw ApiException.conflict("Final logical round must contain exactly one track execution");
        }
        Set<UUID> requestTeams = new HashSet<>();
        Set<UUID> unassignedPromotionIds = promotions
                .findByTargetLogicalRoundIdOrderByTeamNameAsc(targetLogicalRoundId).stream()
                .map(p -> p.getTeam().getId())
                .filter(teamId -> !participants.existsByRoundLogicalRoundIdAndTeamId(targetLogicalRoundId, teamId))
                .collect(Collectors.toSet());
        Set<UUID> submittedTeamIds = request.assignments().stream()
                .flatMap(assignment -> assignment.teamIds().stream())
                .collect(Collectors.toSet());
        if (target.isFinalRound() && !submittedTeamIds.equals(unassignedPromotionIds)) {
            throw ApiException.conflict("All promoted teams must be assigned to the final logical round together");
        }
        for (Assignment assignment : request.assignments()) {
            Round execution = byId.get(assignment.roundId());
            if (execution == null) throw ApiException.badRequest("Target execution belongs to another logical round");
            if (target.isFinalRound() && !execution.getId().equals(executions.get(0).getId())) {
                throw ApiException.badRequest("Final-round assignments must target its single track");
            }
            for (UUID teamId : assignment.teamIds()) {
                if (!requestTeams.add(teamId)) throw ApiException.conflict("Team assigned more than once in this logical round");
                LogicalRoundPromotion promotion = promotions
                        .findByTargetLogicalRoundIdOrderByTeamNameAsc(targetLogicalRoundId).stream()
                        .filter(p -> p.getTeam().getId().equals(teamId)).findFirst()
                        .orElseThrow(() -> ApiException.badRequest("Only teams promoted from the immediately preceding logical round may be assigned"));
                validatePromotionProvenance(promotion);
                if (participants.existsByRoundLogicalRoundIdAndTeamId(targetLogicalRoundId, teamId)) {
                    throw ApiException.conflict("Team is already assigned in this logical round");
                }
                Team team = promotion.getTeam();
                if (!team.getTrack().getId().equals(execution.getTrack().getId())) {
                    team.setTrack(execution.getTrack());
                }
                participants.save(RoundParticipant.builder().round(execution).team(team)
                        .status(RoundParticipantStatus.active).build());
            }
        }
    }

    @Transactional(readOnly = true)
    public BalancePreview previewBalanced(UUID targetLogicalRoundId, BalanceRequest request) {
        RoundDefinition target = definition(targetLogicalRoundId);
        List<Round> executions = new ArrayList<>(rounds.findByLogicalRoundId(targetLogicalRoundId));
        if (executions.isEmpty()) throw ApiException.conflict("Logical round has no track executions");
        if (target.isFinalRound() && executions.size() != 1) {
            throw ApiException.conflict("Final logical round must contain exactly one track execution");
        }
        executions.sort(Comparator.comparing(r -> r.getId().toString()));
        long seed = request == null || request.seed() == null ? 0L : request.seed();
        List<PromotedTeam> unassigned = new ArrayList<>(promotedUnassigned(targetLogicalRoundId));
        Collections.shuffle(unassigned, new Random(seed));
        Map<UUID, Long> counts = new LinkedHashMap<>();
        Map<UUID, List<UUID>> assignments = new LinkedHashMap<>();
        executions.forEach(round -> {
            counts.put(round.getId(), participants.countByRoundId(round.getId()));
            assignments.put(round.getId(), new ArrayList<>());
        });
        for (PromotedTeam team : unassigned) {
            Round selected = executions.stream().min(Comparator
                    .comparingLong((Round r) -> counts.get(r.getId()))
                    .thenComparing(r -> r.getId().toString())).orElseThrow();
            assignments.get(selected.getId()).add(team.teamId());
            counts.put(selected.getId(), counts.get(selected.getId()) + 1);
        }
        List<AssignmentPlan> plans = executions.stream().map(round -> new AssignmentPlan(
                round.getId(), round.getTrack().getId(), List.copyOf(assignments.get(round.getId())))).toList();
        return new BalancePreview(targetLogicalRoundId, seed, plans, Map.copyOf(counts));
    }

    @Transactional
    public BalancePreview applyBalanced(UUID targetLogicalRoundId, BalanceRequest request) {
        BalancePreview preview = previewBalanced(targetLogicalRoundId, request);
        List<Assignment> assignments = preview.assignments().stream()
                .filter(plan -> !plan.teamIds().isEmpty())
                .map(plan -> new Assignment(plan.roundId(), new LinkedHashSet<>(plan.teamIds()))).toList();
        if (!assignments.isEmpty()) assignManual(targetLogicalRoundId, new ManualAssignmentRequest(assignments));
        return preview;
    }

    private void assertSafe(Round round) {
        UUID id = round.getId();
        if (round.getLifecycleState() != RoundLifecycleState.SCORING || round.getResultPublishedAt() != null) {
            throw ApiException.conflict("Locked round track execution cannot be deleted");
        }
        if (participants.existsByRoundId(id)) throw ApiException.conflict("Round track execution has assigned teams");
        if (submissions.existsByRoundId(id)) throw ApiException.conflict("Round track execution has submissions or scores");
        if (rankings.existsByRoundId(id)) throw ApiException.conflict("Round track execution has scoring results");
        if (resultVersions.existsByRoundId(id)) throw ApiException.conflict("Round track execution has published result versions");
        if (appeals.existsByRoundId(id)) throw ApiException.conflict("Round track execution has appeals");
        if (criteria.existsByRoundId(id)) throw ApiException.conflict("Round track execution has criteria");
        if (roundJudges.existsByRoundId(id)) throw ApiException.conflict("Round track execution has judge assignments");
    }

    private void assertNoPromotionDependency(UUID logicalRoundId) {
        if (promotions.existsBySourceLogicalRoundId(logicalRoundId)
                || promotions.existsByTargetLogicalRoundId(logicalRoundId)) {
            throw ApiException.conflict("Logical round has promotion dependencies");
        }
    }

    private void validatePromotionProvenance(LogicalRoundPromotion promotion) {
        if (promotion.getSourceResultVersionId() == null || promotion.getSourceResultEntryId() == null) {
            throw ApiException.conflict("Promotion has no published-result provenance");
        }
        RoundResultVersion version = resultVersions.findById(promotion.getSourceResultVersionId())
                .orElseThrow(() -> ApiException.conflict("Promotion result version is missing"));
        if (!"published".equalsIgnoreCase(version.getStatus())) {
            throw ApiException.conflict("Promotion result version is not the active published version");
        }
        if (version.getRound() == null || versionsActivePublished(version) == false) {
            throw ApiException.conflict("Promotion result version is not the active published version");
        }
        RoundResultVersionEntry entry = resultVersionEntries.findById(promotion.getSourceResultEntryId())
                .orElseThrow(() -> ApiException.conflict("Promotion result entry is missing"));
        if (!version.getId().equals(entry.getResultVersion().getId())
                || !promotion.getTeam().getId().equals(entry.getTeam().getId())
                || !"promoted".equalsIgnoreCase(entry.getPromotionStatus())) {
            throw ApiException.conflict("Promotion provenance does not match the promoted team and status");
        }
        if (version.getRound() == null || version.getRound().getLogicalRound() == null
                || !version.getRound().getLogicalRound().getId().equals(promotion.getSourceLogicalRound().getId())) {
            throw ApiException.conflict("Promotion result version does not belong to its source logical round");
        }
    }

    private boolean versionsActivePublished(RoundResultVersion version) {
        return versionsByRound(version.getRound().getId())
                .map(active -> active.getId().equals(version.getId()))
                .orElse(false);
    }

    private java.util.Optional<RoundResultVersion> versionsByRound(UUID roundId) {
        return resultVersions.findByRoundIdAndStatus(roundId, "published");
    }

    private RoundDefinition definition(UUID id) {
        return definitions.findById(id)
                .orElseThrow(() -> ApiException.notFound("Logical round not found: " + id));
    }

    private LogicalRoundResponse response(RoundDefinition definition, List<Round> executions) {
        return new LogicalRoundResponse(definition.getId(), definition.getEvent().getId(),
                definition.getName(), definition.getSequenceNumber(), definition.isFinalRound(),
                definition.getDefaultTopNToPromote(), definition.getLifecycleState().name(),
                executions.stream().map(RoundMapper::toResponse).toList());
    }
}
