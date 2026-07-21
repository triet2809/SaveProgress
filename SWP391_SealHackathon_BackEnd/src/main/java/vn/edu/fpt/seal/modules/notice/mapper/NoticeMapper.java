package vn.edu.fpt.seal.modules.notice.mapper;

import vn.edu.fpt.seal.modules.notice.dto.NoticeResponse;
import vn.edu.fpt.seal.modules.notice.entity.Notice;

public final class NoticeMapper { private NoticeMapper() {}
    public static NoticeResponse toResponse(Notice n) { var a = n.getAuthor(); return new NoticeResponse(n.getId(), n.getTitle(), n.getContent(), n.getPriority(), n.getTargetRole(), n.getTargetEventId(), n.getTargetTrackId(), n.getTargetTeamId(), a.getId(), a.getEmail(), a.getFullName(), n.getCreatedAt(), n.getUpdatedAt()); }
}
