package vn.edu.fpt.seal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/*
 * =================================================================================================
 * MAIN ENTRY + FAST CODE FLOW MAP
 * =================================================================================================
 *
 * 1) Application boot
 *    main()
 *      -> SpringApplication.run(...)
 *      -> Spring scans controllers / services / repositories / configs
 *      -> HTTP requests start entering controller classes
 *
 * 2) Event lifecycle flow: create event -> open registration -> close registration -> setup competition -> run rounds -> complete event
 *
 *    FE start:
 *      AppRoutes.jsx
 *        -> /coordinator/events
 *        -> renders EventManagement.jsx
 *
 *    FE create event:
 *      EventManagement.handleSaveEvent()
 *        -> createEvent(payload) in hackathonApi.js
 *        -> POST /events
 *
 *    BE create event:
 *      EventController.create()
 *        -> EventService.create(req)
 *        -> validate title uniqueness
 *        -> save Event(status=draft)
 *        -> return EventResponse
 *
 *    FE open registration:
 *      EventManagement.handleOpenRegistration(event)
 *        -> openEventRegistration(event.id)
 *        -> POST /events/{id}/open-registration
 *
 *    BE open registration:
 *      EventController.openRegistration()
 *        -> EventService.openRegistration(id)
 *        -> require current status=draft
 *        -> status = published
 *        -> ensure General track exists
 *        -> write timeline
 *        -> return EventResponse
 *
 *    FE close registration:
 *      EventManagement.handleCloseRegistration(event)
 *        -> closeEventRegistration(event.id)
 *        -> POST /events/{id}/close-registration
 *
 *    BE close registration:
 *      EventController.closeRegistration()
 *        -> EventService.closeRegistration(id, auth)
 *        -> require current status=published
 *        -> check each active team member count
 *        -> disqualify under-sized teams
 *        -> status = ongoing
 *        -> write audit + timeline
 *        -> return EventResponse
 *
 *    FE setup competition:
 *      EventManagement.handleSetupCompetition()
 *        -> setupCompetition(event.id, payload)
 *        -> POST /events/{id}/setup-competition
 *
 *    BE setup competition:
 *      EventController.setupCompetition()
 *        -> EventService.setupCompetition(id, req)
 *        -> require event status=ongoing
 *        -> require no rounds already created
 *        -> collect active teams
 *        -> validate logical round plan
 *        -> EventService.setupFromLogicalRoundPlan(...)
 *            -> create/find tracks from plan
 *            -> assign teams into first-round tracks
 *            -> create RoundDefinition per logical round
 *            -> create Round execution rows per track
 *            -> seed first-round RoundParticipant rows
 *        -> return SetupCompetitionResponse
 *
 *    FE event detail tracking:
 *      /coordinator/events/:id
 *        -> EventDetails.jsx
 *        -> load event / tracks / teams / logical rounds
 *        -> coordinator sees built structure and participants
 *
 *    FE publish / resume / advance rounds:
 *      usually from round-related screens
 *        -> round APIs
 *        -> BE RoundController / RoundService / CompetitionLifecycleService
 *        -> teams advance through round participants and result state
 *
 *    FE complete event:
 *      event status action
 *        -> changeEventStatus(id, 'completed')
 *        -> POST /events/{id}/status
 *
 *    BE complete event:
 *      EventController.changeStatus()
 *        -> EventService.changeStatus(id, completed, auth)
 *        -> validate status transition
 *        -> require awards allowed
 *        -> require seeding finalized
 *        -> status = completed
 *        -> run recognition evaluation
 *        -> write final timeline
 *        -> return EventResponse
 *
 * 3) Trace rule for finding code fast
 *    FE screen
 *      -> FE handler function
 *      -> hackathonApi.js function
 *      -> backend controller endpoint
 *      -> backend service method
 *      -> repository save/query
 *      -> response DTO back to FE
 *
 * 4) Files to read first for event flow
 *    FE
 *      - src/routes/AppRoutes.jsx
 *      - src/pages/coordinator/EventManagement.jsx
 *      - src/pages/coordinator/EventDetails.jsx
 *      - src/api/hackathonApi.js
 *    BE
 *      - SealHackathonApplication.java
 *      - modules/event/controller/EventController.java
 *      - modules/event/service/EventService.java
 *      - modules/round/service/CompetitionLifecycleService.java
 *
 * This class is comment map only. Real event business logic lives in controller/service layers above.
 * =================================================================================================
 */
@SpringBootApplication
@EnableJpaAuditing
public class SealHackathonApplication {
    public static void main(String[] args) {
        SpringApplication.run(SealHackathonApplication.class, args);
    }
}
