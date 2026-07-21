package vn.edu.fpt.seal.modules.teamprofile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.fpt.seal.common.exception.*;
import vn.edu.fpt.seal.config.AppProperties;
import vn.edu.fpt.seal.modules.teamprofile.controller.TeamProfileController;
import vn.edu.fpt.seal.modules.teamprofile.service.TeamProfileService;
import vn.edu.fpt.seal.security.*;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamProfileController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ContextConfiguration(classes = {TeamProfileController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, AppProperties.class, GlobalExceptionHandler.class})
class TeamProfileControllerMvcTest {
    @Autowired MockMvc mvc;
    @MockBean TeamProfileService service;
    @MockBean JwtService jwtService;

    @Test
    void protectedEndpointReturns401() throws Exception {
        mvc.perform(get("/team-profiles/mine")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TEAM_MEMBER")
    void forbiddenUserReturns403() throws Exception {
        when(service.preview(any(), any(), any())).thenThrow(ApiException.forbidden("Not historical leader"));
        mvc.perform(post("/team-profiles/" + UUID.randomUUID() + "/reactivation-preview")
                        .contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void invalidPayloadReturns400() throws Exception {
        mvc.perform(post("/team-profiles/" + UUID.randomUUID() + "/reactivation-preview")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void duplicateRegistrationReturns409() throws Exception {
        when(service.reactivate(any(), any(), any()))
                .thenThrow(ApiException.conflict("Already registered"));
        mvc.perform(post("/team-profiles/" + UUID.randomUUID() + "/reactivate")
                        .contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void authenticatedHistoryViewSucceeds() throws Exception {
        when(service.mine(isNull(), any())).thenReturn(List.of());
        mvc.perform(get("/team-profiles/mine")).andExpect(status().isOk());
    }

    private String validBody() {
        UUID first = UUID.randomUUID();
        return """
                {"sourceTeamId":"%s","targetEventId":"%s","targetTrackId":"%s",
                 "returningMemberIds":["%s"],"leaderId":"%s"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), first, first);
    }
}
