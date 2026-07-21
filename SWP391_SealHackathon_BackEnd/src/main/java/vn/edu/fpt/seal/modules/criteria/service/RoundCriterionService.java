package vn.edu.fpt.seal.modules.criteria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.EventStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.criteria.dto.*;
import vn.edu.fpt.seal.modules.criteria.entity.RoundCriterion;
import vn.edu.fpt.seal.modules.criteria.mapper.RoundCriterionMapper;
import vn.edu.fpt.seal.modules.criteria.repository.RoundCriterionRepository;
import vn.edu.fpt.seal.modules.round.entity.Round;
import vn.edu.fpt.seal.modules.round.repository.RoundRepository;

import java.util.UUID;

@Service @RequiredArgsConstructor
public class RoundCriterionService {
    private final RoundCriterionRepository criterionRepository;
    private final RoundRepository roundRepository;
    @Transactional(readOnly = true) public Page<RoundCriterionResponse> list(UUID eventId, UUID roundId, UUID trackId, Pageable pageable) {
        Round round = roundRepository.findById(roundId).orElseThrow(() -> ApiException.notFound("Round not found: " + roundId));
        if (eventId != null && (!round.getTrack().getEvent().getId().equals(eventId)
                || (trackId != null && !round.getTrack().getId().equals(trackId)))) {
            throw ApiException.badRequest("Criteria hierarchy does not match event, track, and round");
        }
        return criterionRepository.findByRoundId(roundId, pageable).map(RoundCriterionMapper::toResponse);
    }
    @Transactional(readOnly = true) public RoundCriterionResponse get(UUID id) { return RoundCriterionMapper.toResponse(findOrThrow(id)); }
    @Transactional public RoundCriterionResponse create(CreateRoundCriterionRequest req) {
        Round round = roundRepository.findById(req.roundId()).orElseThrow(() -> ApiException.notFound("Round not found: " + req.roundId())); ensureEditable(round);
        String name = req.name().trim(); if (criterionRepository.existsByRoundIdAndNameIgnoreCase(round.getId(), name)) throw ApiException.conflict("Criterion name already exists in this round");
        RoundCriterion c = criterionRepository.save(RoundCriterion.builder().round(round).templateId(req.templateId()).name(name).weight(req.weight()).description(trim(req.description())).status(req.status()==null?"active":req.status().trim()).build());
        return RoundCriterionMapper.toResponse(c);
    }
    @Transactional public RoundCriterionResponse update(UUID id, UpdateRoundCriterionRequest req) {
        RoundCriterion c = findOrThrow(id); ensureEditable(c.getRound());
        if (req.name()!=null) { String name=req.name().trim(); if (!name.equalsIgnoreCase(c.getName()) && criterionRepository.existsByRoundIdAndNameIgnoreCase(c.getRound().getId(), name)) throw ApiException.conflict("Criterion name already exists in this round"); c.setName(name); }
        if (req.weight()!=null) c.setWeight(req.weight()); if (req.description()!=null) c.setDescription(trim(req.description())); if (req.status()!=null) c.setStatus(req.status().trim()); return RoundCriterionMapper.toResponse(c);
    }
    @Transactional public void delete(UUID id) { RoundCriterion c=findOrThrow(id); ensureEditable(c.getRound()); criterionRepository.delete(c); }
    private RoundCriterion findOrThrow(UUID id) { return criterionRepository.findWithRoundById(id).orElseThrow(() -> ApiException.notFound("Criterion not found: " + id)); }
    private void ensureEditable(Round round) { EventStatus s=round.getTrack().getEvent().getStatus(); if (s==EventStatus.completed||s==EventStatus.cancelled) throw ApiException.badRequest("Cannot edit criteria in event status " + s); }
    private String trim(String s) { return s==null?null:s.trim(); }
}
