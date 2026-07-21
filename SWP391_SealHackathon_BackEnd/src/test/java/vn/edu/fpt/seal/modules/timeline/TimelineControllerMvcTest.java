package vn.edu.fpt.seal.modules.timeline;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.fpt.seal.common.exception.GlobalExceptionHandler;
import vn.edu.fpt.seal.config.AppProperties;
import vn.edu.fpt.seal.modules.timeline.controller.TimelineController;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;
import vn.edu.fpt.seal.security.*;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimelineController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ContextConfiguration(classes={TimelineController.class,SecurityConfig.class,JwtAuthenticationFilter.class,
        AppProperties.class,GlobalExceptionHandler.class})
class TimelineControllerMvcTest {
    @Autowired MockMvc mvc;
    @MockBean TimelineService service;
    @MockBean JwtService jwtService;

    @Test void anonymousMayRequestPublicEventTimeline() throws Exception {
        UUID event=UUID.randomUUID();
        when(service.event(eq(event),isNull(),isNull(),isNull(),isNull(),any(),isNull()))
                .thenReturn(Page.empty());
        mvc.perform(get("/events/"+event+"/timeline")).andExpect(status().isOk());
    }

    @Test void anonymousTeamTimelineIs401() throws Exception {
        mvc.perform(get("/teams/"+UUID.randomUUID()+"/timeline")).andExpect(status().isUnauthorized());
    }

    @Test void anonymousPrivateDetailUsesSafeNotFound() throws Exception {
        UUID id=UUID.randomUUID();
        when(service.get(eq(id),isNull())).thenThrow(vn.edu.fpt.seal.common.exception.ApiException.notFound("Timeline event not found"));
        mvc.perform(get("/timeline/"+id)).andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles="PARTICIPANT")
    void ordinaryUserCannotRequestVisibilityScope() throws Exception {
        UUID event=UUID.randomUUID();
        when(service.event(eq(event),isNull(),isNull(),isNull(),eq(TimelineScope.COORDINATOR_PRIVATE),any(),any()))
                .thenThrow(vn.edu.fpt.seal.common.exception.ApiException.forbidden("Only coordinators may request a visibility scope"));
        mvc.perform(get("/events/"+event+"/timeline").param("visibility","COORDINATOR_PRIVATE"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles="COORDINATOR")
    void coordinatorMayUseTypeAndVisibilityFilters() throws Exception {
        UUID event=UUID.randomUUID();
        when(service.event(eq(event),isNull(),isNull(),eq(TimelineEventType.PRIZE_AWARDED),
                eq(TimelineScope.EVENT_PUBLIC),any(),any())).thenReturn(Page.empty());
        mvc.perform(get("/events/"+event+"/timeline").param("eventType","PRIZE_AWARDED")
                .param("visibility","EVENT_PUBLIC")).andExpect(status().isOk());
    }
}
