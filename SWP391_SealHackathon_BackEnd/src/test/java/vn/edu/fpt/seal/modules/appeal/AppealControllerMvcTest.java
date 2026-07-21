package vn.edu.fpt.seal.modules.appeal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.fpt.seal.common.exception.GlobalExceptionHandler;
import vn.edu.fpt.seal.config.AppProperties;
import vn.edu.fpt.seal.modules.appeal.controller.AppealController;
import vn.edu.fpt.seal.modules.appeal.service.AppealService;
import vn.edu.fpt.seal.security.JwtAuthenticationFilter;
import vn.edu.fpt.seal.security.JwtService;
import vn.edu.fpt.seal.security.SecurityConfig;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppealController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ContextConfiguration(classes = {AppealController.class, SecurityConfig.class, JwtAuthenticationFilter.class,
        AppProperties.class, GlobalExceptionHandler.class})
class AppealControllerMvcTest {
    @Autowired MockMvc mvc;
    @MockBean AppealService appeals;
    @MockBean JwtService jwtService;

    @Test
    void unauthenticatedSubmissionIs401() throws Exception {
        mvc.perform(post("/appeals").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roundId\":\"" + UUID.randomUUID() + "\",\"reason\":\"Review\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PARTICIPANT")
    void participantCannotListEventAppeals() throws Exception {
        mvc.perform(get("/appeals").param("eventId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COORDINATOR")
    void coordinatorCanListAndResolveAppeals() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID appealId = UUID.randomUUID();
        when(appeals.list(eventId, null)).thenReturn(List.of());

        mvc.perform(get("/appeals").param("eventId", eventId.toString()))
                .andExpect(status().isOk());
        mvc.perform(post("/appeals/" + appealId + "/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\",\"response\":\"Reviewed\"}"))
                .andExpect(status().isOk());
    }
}
