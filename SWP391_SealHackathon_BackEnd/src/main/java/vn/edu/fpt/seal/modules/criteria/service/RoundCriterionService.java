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

/**
 * Service nghiệp vụ tiêu chí chấm điểm theo vòng thi (RoundCriterion).
 * Quản lý CRUD tiêu chí; kiểm tra tính nhất quán cấp bậc (event/track/round),
 * chống trùng tên trong cùng vòng, và chặn chỉnh sửa khi sự kiện đã đóng/huỷ.
 */
@Service @RequiredArgsConstructor
public class RoundCriterionService {
    private final RoundCriterionRepository criterionRepository;
    private final RoundRepository roundRepository;
    /**
     * Liệt kê tiêu chí của một vòng thi, có phân trang.
     * Nếu truyền eventId/trackId, kiểm tra phù hợp với cấp bậc của vòng.
     * @throws ApiException nếu vòng không tồn tại hoặc cấp bậc không khớp
     */
    @Transactional(readOnly = true) public Page<RoundCriterionResponse> list(UUID eventId, UUID roundId, UUID trackId, Pageable pageable) {
        Round round = roundRepository.findById(roundId).orElseThrow(() -> ApiException.notFound("Round not found: " + roundId));
        // Kiểm tra cấp bậc event/track/round nhất quán nếu client cung cấp
        if (eventId != null && (!round.getTrack().getEvent().getId().equals(eventId)
                || (trackId != null && !round.getTrack().getId().equals(trackId)))) {
            throw ApiException.badRequest("Criteria hierarchy does not match event, track, and round");
        }
        return criterionRepository.findByRoundId(roundId, pageable).map(RoundCriterionMapper::toResponse);
    }
    /** Lấy chi tiết một tiêu chí theo id. @throws ApiException nếu không tìm thấy */
    @Transactional(readOnly = true) public RoundCriterionResponse get(UUID id) { return RoundCriterionMapper.toResponse(findOrThrow(id)); }
    /**
     * Tạo tiêu chí mới cho vòng thi.
     * Kiểm tra vòng còn chỉnh sửa được và tên không trùng.
     * @throws ApiException nếu vòng không tồn tại, không cho chỉnh sửa, hoặc tên trùng
     */
    @Transactional public RoundCriterionResponse create(CreateRoundCriterionRequest req) {
        Round round = roundRepository.findById(req.roundId()).orElseThrow(() -> ApiException.notFound("Round not found: " + req.roundId())); ensureEditable(round);
        String name = req.name().trim(); if (criterionRepository.existsByRoundIdAndNameIgnoreCase(round.getId(), name)) throw ApiException.conflict("Criterion name already exists in this round");
        RoundCriterion c = criterionRepository.save(RoundCriterion.builder().round(round).templateId(req.templateId()).name(name).weight(req.weight()).description(trim(req.description())).status(req.status()==null?"active":req.status().trim()).build());
        return RoundCriterionMapper.toResponse(c);
    }
    /**
     * Cập nhật tiêu chí (partial update).
     * Chỉ cập nhật trường khác null; nếu đổi tên phải kiểm tra trùng.
     * @throws ApiException nếu không tìm thấy, không cho chỉnh sửa, hoặc tên trùng
     */
    @Transactional public RoundCriterionResponse update(UUID id, UpdateRoundCriterionRequest req) {
        RoundCriterion c = findOrThrow(id); ensureEditable(c.getRound());
        if (req.name()!=null) { String name=req.name().trim(); if (!name.equalsIgnoreCase(c.getName()) && criterionRepository.existsByRoundIdAndNameIgnoreCase(c.getRound().getId(), name)) throw ApiException.conflict("Criterion name already exists in this round"); c.setName(name); }
        if (req.weight()!=null) c.setWeight(req.weight()); if (req.description()!=null) c.setDescription(trim(req.description())); if (req.status()!=null) c.setStatus(req.status().trim()); return RoundCriterionMapper.toResponse(c);
    }
    /** Xóa tiêu chí; chỉ khi vòng còn chỉnh sửa được. @throws ApiException nếu không tìm thấy hoặc không cho chỉnh sửa */
    @Transactional public void delete(UUID id) { RoundCriterion c=findOrThrow(id); ensureEditable(c.getRound()); criterionRepository.delete(c); }
    /** Tìm tiêu chí kèm vòng; ném 404 nếu không tồn tại. */
    private RoundCriterion findOrThrow(UUID id) { return criterionRepository.findWithRoundById(id).orElseThrow(() -> ApiException.notFound("Criterion not found: " + id)); }
    /** Đảm bảo vòng thi còn chỉnh sửa được: chặn khi sự kiện đã completed hoặc cancelled. */
    private void ensureEditable(Round round) { EventStatus s=round.getTrack().getEvent().getStatus(); if (s==EventStatus.completed||s==EventStatus.cancelled) throw ApiException.badRequest("Cannot edit criteria in event status " + s); }
    /** Trim chuỗi null-safe. */
    private String trim(String s) { return s==null?null:s.trim(); }
}
