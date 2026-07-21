package vn.edu.fpt.seal.modules.team;

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
import vn.edu.fpt.seal.modules.team.controller.TeamController;
import vn.edu.fpt.seal.modules.team.service.TeamService;
import vn.edu.fpt.seal.modules.team.service.TeamTransferService;
import vn.edu.fpt.seal.security.JwtAuthenticationFilter;
import vn.edu.fpt.seal.security.JwtService;
import vn.edu.fpt.seal.security.SecurityConfig;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class, GlobalExceptionHandler.class})
@ContextConfiguration(classes = {TeamController.class, SecurityConfig.class, JwtAuthenticationFilter.class,
        AppProperties.class, GlobalExceptionHandler.class})
class TeamControllerSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean TeamService service;
    @MockBean TeamTransferService transferService;
    @MockBean JwtService jwtService;

    @Test
    @WithMockUser(roles = "MENTOR")
    void nonCoordinatorCannotReactivateTeam() throws Exception {
        mvc.perform(post("/teams/{id}/reactivate", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
