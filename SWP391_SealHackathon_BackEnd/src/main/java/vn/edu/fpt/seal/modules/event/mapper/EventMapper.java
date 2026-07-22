package vn.edu.fpt.seal.modules.event.mapper;

import vn.edu.fpt.seal.modules.event.dto.EventResponse;
import vn.edu.fpt.seal.modules.event.entity.Event;

/**
 * Mapper chuyển đổi entity {@link Event} sang DTO {@link EventResponse}.
 * Lớp tiện ích tĩnh, không cho phép khởi tạo.
 */
public final class EventMapper {

    private EventMapper() {
    }

    /**
     * Chuyển Event sang response không kèm số liệu thống kê.
     *
     * @param e entity sự kiện
     * @return DTO phản hồi (các số liệu thống kê để null)
     */
    public static EventResponse toResponse(Event e) {
        return build(e, null, null, null);
    }

    /**
     * Chuyển Event sang response kèm số liệu thống kê.
     *
     * @param e            entity sự kiện
     * @param tracksCount  số track
     * @param roundsCount  số vòng thi
     * @param participants số đội/người tham gia
     * @return DTO phản hồi đầy đủ
     */
    public static EventResponse toResponse(Event e, int tracksCount, int roundsCount, long participants) {
        return build(e, tracksCount, roundsCount, participants);
    }

    /**
     * Hàm dựng chung: map các trường từ entity sang DTO.
     *
     * @param e            entity sự kiện
     * @param tracksCount  số track (có thể null)
     * @param roundsCount  số vòng (có thể null)
     * @param participants số người tham gia (có thể null)
     * @return DTO phản hồi
     */
    private static EventResponse build(Event e, Integer tracksCount, Integer roundsCount, Long participants) {
        return EventResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .status(e.getStatus())
                .term(e.getTerm())
                .prizePool(e.getPrizePool())
                .registrationStart(e.getRegistrationStart())
                .registrationEnd(e.getRegistrationEnd())
                .eventStart(e.getEventStart())
                .eventEnd(e.getEventEnd())
                .tracksCount(tracksCount)
                .roundsCount(roundsCount)
                .participantsCount(participants)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
