package vn.edu.fpt.seal.modules.criteria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.criteria.dto.*;
import vn.edu.fpt.seal.modules.criteria.entity.CriteriaTemplate;
import vn.edu.fpt.seal.modules.criteria.mapper.RoundCriterionMapper;
import vn.edu.fpt.seal.modules.criteria.repository.CriteriaTemplateRepository;

import java.util.UUID;

/**
 * Service nghiệp vụ mẫu tiêu chí chấm điểm (CriteriaTemplate).
 * Quản lý CRUD mẫu, chống trùng tên mẫu (không phân biệt hoa thường).
 */
@Service @RequiredArgsConstructor
public class CriteriaTemplateService {
    private final CriteriaTemplateRepository repository;

    /** Liệt kê tất cả mẫu tiêu chí, có phân trang. */
    @Transactional(readOnly = true)
    public Page<CriteriaTemplateResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(RoundCriterionMapper::toTemplateResponse);
    }

    /** Lấy chi tiết một mẫu theo id. @throws ApiException nếu không tìm thấy */
    @Transactional(readOnly = true)
    public CriteriaTemplateResponse get(UUID id) { return RoundCriterionMapper.toTemplateResponse(find(id)); }

    /**
     * Tạo mẫu tiêu chí mới.
     * @throws ApiException nếu tên mẫu đã tồn tại
     */
    @Transactional
    public CriteriaTemplateResponse create(CreateCriteriaTemplateRequest req) {
        String name = req.name().trim();
        if (repository.existsByNameIgnoreCase(name)) throw ApiException.conflict("Criteria template name already exists");
        return RoundCriterionMapper.toTemplateResponse(repository.save(CriteriaTemplate.builder().name(name).description(trim(req.description())).defaultWeight(req.defaultWeight()).build()));
    }

    /**
     * Cập nhật mẫu tiêu chí (partial update).
     * Chỉ cập nhật trường khác null; nếu đổi tên phải kiểm tra trùng.
     * @throws ApiException nếu không tìm thấy hoặc tên trùng
     */
    @Transactional
    public CriteriaTemplateResponse update(UUID id, UpdateCriteriaTemplateRequest req) {
        CriteriaTemplate t = find(id);
        if (req.name() != null) {
            String name = req.name().trim();
            if (!name.equalsIgnoreCase(t.getName()) && repository.existsByNameIgnoreCase(name)) throw ApiException.conflict("Criteria template name already exists");
            t.setName(name);
        }
        if (req.description() != null) t.setDescription(trim(req.description()));
        if (req.defaultWeight() != null) t.setDefaultWeight(req.defaultWeight());
        return RoundCriterionMapper.toTemplateResponse(t);
    }

    /** Xóa mẫu tiêu chí. @throws ApiException nếu không tìm thấy */
    @Transactional public void delete(UUID id) { repository.delete(find(id)); }
    /** Tìm mẫu theo id; ném 404 nếu không tồn tại. */
    private CriteriaTemplate find(UUID id) { return repository.findById(id).orElseThrow(() -> ApiException.notFound("Criteria template not found: " + id)); }
    /** Trim chuỗi null-safe. */
    private String trim(String s) { return s == null ? null : s.trim(); }
}
