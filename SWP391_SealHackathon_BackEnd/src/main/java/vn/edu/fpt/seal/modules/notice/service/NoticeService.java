package vn.edu.fpt.seal.modules.notice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.notice.dto.CreateNoticeRequest;
import vn.edu.fpt.seal.modules.notice.dto.NoticeResponse;
import vn.edu.fpt.seal.modules.notice.entity.Notice;
import vn.edu.fpt.seal.modules.notice.mapper.NoticeMapper;
import vn.edu.fpt.seal.modules.notice.repository.NoticeRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoticeService {
    private final NoticeRepository repo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public Page<NoticeResponse> list(String targetRole, UUID eventId, UUID trackId, Pageable p) {
        return repo.search(clean(targetRole), eventId, trackId, p).map(NoticeMapper::toResponse);
    }

    @Transactional
    public NoticeResponse create(CreateNoticeRequest r, UUID authorId) {
        User author = userRepo.findById(authorId).orElseThrow(() -> ApiException.notFound("Author not found"));
        Notice n = Notice.builder().title(r.title().trim()).content(r.content().trim()).priority(clean(r.priority()) == null ? "normal" : clean(r.priority())).targetRole(clean(r.targetRole())).targetEventId(r.targetEventId()).targetTrackId(r.targetTrackId()).targetTeamId(r.targetTeamId()).author(author).build();
        return NoticeMapper.toResponse(repo.save(n));
    }

    private String clean(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
