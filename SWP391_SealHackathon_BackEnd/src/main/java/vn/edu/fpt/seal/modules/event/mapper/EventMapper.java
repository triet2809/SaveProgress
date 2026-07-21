package vn.edu.fpt.seal.modules.event.mapper;

import vn.edu.fpt.seal.modules.event.dto.EventResponse;
import vn.edu.fpt.seal.modules.event.entity.Event;

public final class EventMapper {

    private EventMapper() {
    }

    public static EventResponse toResponse(Event e) {
        return build(e, null, null, null);
    }

    public static EventResponse toResponse(Event e, int tracksCount, int roundsCount, long participants) {
        return build(e, tracksCount, roundsCount, participants);
    }

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
