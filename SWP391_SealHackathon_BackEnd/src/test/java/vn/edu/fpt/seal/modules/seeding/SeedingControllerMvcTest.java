package vn.edu.fpt.seal.modules.seeding;

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
import vn.edu.fpt.seal.modules.seeding.controller.SeedingController;
import vn.edu.fpt.seal.modules.seeding.service.SeedingService;
import vn.edu.fpt.seal.security.*;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeedingController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ContextConfiguration(classes = {SeedingController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, AppProperties.class, GlobalExceptionHandler.class})
class SeedingControllerMvcTest {
    @Autowired MockMvc mvc;
    @MockBean SeedingService service;
    @MockBean JwtService jwtService;

    @Test
    void unauthenticatedReturns401() throws Exception {
        mvc.perform(get("/events/" + UUID.randomUUID() + "/seed-candidates"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "JUDGE")
    void nonCoordinatorCannotWrite() throws Exception {
        mvc.perform(put(path()).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COORDINATOR")
    void invalidPayloadReturns400() throws Exception {
        mvc.perform(put(path()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"automatic\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "COORDINATOR")
    void invalidHierarchyReturns400() throws Exception {
        when(service.decide(any(), any(), any(), any()))
                .thenThrow(ApiException.badRequest("Team does not belong to event"));
        mvc.perform(put(path()).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "COORDINATOR")
    void duplicateSeedReturns409() throws Exception {
        when(service.decide(any(), any(), any(), any()))
                .thenThrow(ApiException.conflict("Duplicate seed"));
        mvc.perform(put(path()).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isConflict());
    }

    private String path() {
        return "/events/" + UUID.randomUUID() + "/teams/" + UUID.randomUUID() + "/seed";
    }

    private String validBody() {
        return """
                {"status":"confirmed","seedNumber":1,"seedTier":"TIER_1",
                 "candidateSourceFinishId":"%s"}
                """.formatted(UUID.randomUUID());
    }
}
