package vn.edu.fpt.seal.modules.criteria.mapper;

import vn.edu.fpt.seal.modules.criteria.dto.RoundCriterionResponse;
import vn.edu.fpt.seal.modules.criteria.dto.CriteriaTemplateResponse;
import vn.edu.fpt.seal.modules.criteria.entity.RoundCriterion;
import vn.edu.fpt.seal.modules.criteria.entity.CriteriaTemplate;

public final class RoundCriterionMapper {
    private RoundCriterionMapper() {}
    public static RoundCriterionResponse toResponse(RoundCriterion c) {
        return RoundCriterionResponse.builder().id(c.getId()).roundId(c.getRound().getId()).templateId(c.getTemplateId()).name(c.getName()).weight(c.getWeight()).description(c.getDescription()).status(c.getStatus()).createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt()).build();
    }
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
