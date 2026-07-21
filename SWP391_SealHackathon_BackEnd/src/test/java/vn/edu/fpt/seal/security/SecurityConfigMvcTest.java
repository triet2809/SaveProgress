package vn.edu.fpt.seal.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.fpt.seal.config.AppProperties;
import vn.edu.fpt.seal.modules.chat.controller.TeamChatController;
import vn.edu.fpt.seal.modules.chat.service.TeamChatService;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamChatController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ContextConfiguration(classes = {
        TeamChatController.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        AppProperties.class
})
class SecurityConfigMvcTest {
    @Autowired MockMvc mockMvc;
    @MockBean TeamChatService teamChatService;
    @MockBean JwtService jwtService;

    @Test
    void productionSecurityRejectsUnauthenticatedProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/team-chat")
                        .param("teamId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }
}
