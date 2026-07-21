package vn.edu.fpt.seal.modules.recognition;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.fpt.seal.common.exception.GlobalExceptionHandler;
import vn.edu.fpt.seal.config.AppProperties;
import vn.edu.fpt.seal.modules.recognition.controller.TeamRecognitionController;
import vn.edu.fpt.seal.modules.recognition.dto.RecognitionDtos;
import vn.edu.fpt.seal.modules.recognition.service.TeamRecognitionService;
import vn.edu.fpt.seal.security.*;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeamRecognitionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ContextConfiguration(classes = {TeamRecognitionController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, AppProperties.class, GlobalExceptionHandler.class})
class TeamRecognitionControllerMvcTest {
    @Autowired MockMvc mvc;
    @MockBean TeamRecognitionService service;
    @MockBean JwtService jwtService;

    @Test
    void unauthenticatedProtectedOperationReturns401() throws Exception {
        mvc.perform(post(base() + "/recalculate")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "JUDGE")
    void nonCoordinatorCannotReadEvidenceOrRecalculate() throws Exception {
        mvc.perform(get(base() + "/evidence")).andExpect(status().isForbidden());
        mvc.perform(post(base() + "/recalculate")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COORDINATOR")
    void validCoordinatorRecalculationSucceedsWithSafeProjection() throws Exception {
        UUID profileId = UUID.randomUUID();
        when(service.recalculate(eq(profileId), any())).thenReturn(new RecognitionDtos.EvidenceResponse(
                profileId, UUID.randomUUID(),
                new RecognitionDtos.Summary("HACKATHON_VETERAN", "Hackathon Veteran",
                        "3+ Seasons", 3, null, true),
                3, List.of()));

        mvc.perform(post("/team-profiles/" + profileId + "/recognitions/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recognition.code").value("HACKATHON_VETERAN"))
                .andExpect(jsonPath("$.qualificationEvidence").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("inviteCode"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("email"))));
    }

    @Test
    @WithMockUser(roles = "COORDINATOR")
    void revokeRequiresNonblankReason() throws Exception {
        mvc.perform(post(base() + "/" + UUID.randomUUID() + "/revoke")
                        .contentType(APPLICATION_JSON).content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "COORDINATOR")
    void restoreIsCoordinatorOnlyAndCanSucceed() throws Exception {
        UUID profileId = UUID.randomUUID();
        when(service.restore(eq(profileId), any(), any())).thenReturn(
                new RecognitionDtos.EvidenceResponse(profileId, UUID.randomUUID(), null, 3, List.of()));
        // The service mock uses argument matching for the opaque recognition id.
        mvc.perform(post("/team-profiles/" + profileId + "/recognitions/"
                        + UUID.randomUUID() + "/restore"))
                .andExpect(status().isOk());
    }

    private String base() {
        return "/team-profiles/" + UUID.randomUUID() + "/recognitions";
    }
}
