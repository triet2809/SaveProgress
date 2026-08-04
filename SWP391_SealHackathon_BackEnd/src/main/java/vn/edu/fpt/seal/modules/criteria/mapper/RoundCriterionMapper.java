package vn.edu.fpt.seal.modules.criteria.mapper;

import vn.edu.fpt.seal.modules.criteria.dto.CriteriaTemplateResponse;
import vn.edu.fpt.seal.modules.criteria.dto.RoundCriterionResponse;
import vn.edu.fpt.seal.modules.criteria.entity.CriteriaTemplate;
import vn.edu.fpt.seal.modules.criteria.entity.RoundCriterion;

/**
 * Mapper chuyển thực thể tiêu chí sang DTO.
 * Hỗ trợ cả RoundCriterion và CriteriaTemplate. Lớp tiện ích tĩnh, không cho khởi tạo.
 */
public final class RoundCriterionMapper {
    private RoundCriterionMapper() {
    }

    /**
     * Ánh xạ RoundCriterion sang RoundCriterionResponse.
     */
    public static RoundCriterionResponse toResponse(RoundCriterion c) {
        return RoundCriterionResponse.builder().id(c.getId()).roundId(c.getRound().getId()).templateId(c.getTemplateId()).name(c.getName()).weight(c.getWeight()).description(c.getDescription()).status(c.getStatus()).createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt()).build();
    }

    /**
     * Ánh xạ CriteriaTemplate sang CriteriaTemplateResponse.
     */
    public static CriteriaTemplateResponse toTemplateResponse(CriteriaTemplate t) {
        return CriteriaTemplateResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .defaultWeight(t.getDefaultWeight())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
