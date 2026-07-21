package vn.edu.fpt.seal.modules.staff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import vn.edu.fpt.seal.common.exception.GlobalExceptionHandler;
import vn.edu.fpt.seal.config.AppProperties;
import vn.edu.fpt.seal.modules.staff.controller.EventStaffController;
import vn.edu.fpt.seal.modules.staff.service.EventStaffService;
import vn.edu.fpt.seal.security.*;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventStaffController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class, GlobalExceptionHandler.class})
@ContextConfiguration(classes = {EventStaffController.class, SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class, GlobalExceptionHandler.class})
class EventStaffMvcTest {
    @Autowired MockMvc mvc;
    @MockBean EventStaffService service;
    @MockBean JwtService jwtService;

    @Test
    void unauthenticatedIs401() throws Exception {
        mvc.perform(get("/events/{eventId}/staff", UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MENTOR")
    void nonCoordinatorIs403() throws Exception {
        mvc.perform(get("/events/{eventId}/staff", UUID.randomUUID())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MENTOR")
    void nonCoordinatorCannotCreateStaff() throws Exception {
        mvc.perform(post("/events/{eventId}/staff/invite", UUID.randomUUID())
                        .contentType("application/json")
                        .content("{\"fullName\":\"New\",\"email\":\"new@example.test\",\"roles\":[\"judge\"],\"temporaryPassword\":\"Temporary123\"}"))
                .andExpect(status().isForbidden());
    }
}
