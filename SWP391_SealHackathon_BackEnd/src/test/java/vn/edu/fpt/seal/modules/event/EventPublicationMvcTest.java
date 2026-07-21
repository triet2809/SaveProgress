package vn.edu.fpt.seal.modules.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.fpt.seal.config.AppProperties;
import vn.edu.fpt.seal.common.exception.GlobalExceptionHandler;
import vn.edu.fpt.seal.modules.event.controller.EventController;
import vn.edu.fpt.seal.modules.event.service.EventService;
import vn.edu.fpt.seal.modules.round.dto.RoundResponse;
import vn.edu.fpt.seal.modules.round.service.RoundService;
import vn.edu.fpt.seal.security.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ContextConfiguration(classes = {EventController.class, SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class, GlobalExceptionHandler.class})
class EventPublicationMvcTest {
    @Autowired MockMvc mvc;
    @MockBean EventService events;
    @MockBean RoundService rounds;
    @MockBean JwtService jwtService;

    @Test
    void unauthenticatedIs401() throws Exception {
        mvc.perform(post(path(UUID.randomUUID(), UUID.randomUUID()))).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PARTICIPANT")
    void nonCoordinatorIs403() throws Exception {
        mvc.perform(post(path(UUID.randomUUID(), UUID.randomUUID()))).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COORDINATOR")
    void coordinatorPublishesAndResponseIncludesTimestamp() throws Exception {
        UUID eventId = UUID.randomUUID(), roundId = UUID.randomUUID();
        LocalDateTime published = LocalDateTime.of(2026, 7, 19, 12, 0);
        when(rounds.publishResults(eq(eventId), eq(roundId), any())).thenReturn(
                RoundResponse.builder().id(roundId).eventId(eventId).resultPublishedAt(published).build());
        mvc.perform(post(path(eventId, roundId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultPublishedAt").value("2026-07-19T12:00:00"));
    }

    @Test
    @WithMockUser(roles = "COORDINATOR")
    void invalidHierarchyIs400() throws Exception {
        when(rounds.publishResults(any(), any(), any())).thenThrow(vn.edu.fpt.seal.common.exception.ApiException.badRequest("Round does not belong to event"));
        mvc.perform(post(path(UUID.randomUUID(), UUID.randomUUID()))).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "COORDINATOR")
    void duplicatePublicationReturnsExistingTimestamp() throws Exception {
        UUID eventId = UUID.randomUUID(), roundId = UUID.randomUUID();
        LocalDateTime published = LocalDateTime.of(2026, 7, 19, 12, 0);
        when(rounds.publishResults(eq(eventId), eq(roundId), any())).thenReturn(
                RoundResponse.builder().id(roundId).eventId(eventId).resultPublishedAt(published).build());
        mvc.perform(post(path(eventId, roundId))).andExpect(status().isOk());
        mvc.perform(post(path(eventId, roundId))).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultPublishedAt").value("2026-07-19T12:00:00"));
        verify(rounds, times(2)).publishResults(eq(eventId), eq(roundId), any());
    }

    private static String path(UUID eventId, UUID roundId) {
        return "/events/" + eventId + "/rounds/" + roundId + "/publish-results";
    }
}
