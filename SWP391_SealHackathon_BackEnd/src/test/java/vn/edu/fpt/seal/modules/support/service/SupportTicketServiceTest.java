package vn.edu.fpt.seal.modules.support.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.support.dto.UpdateSupportTicketStatusRequest;
import vn.edu.fpt.seal.modules.support.entity.SupportTicket;
import vn.edu.fpt.seal.modules.support.repository.SupportTicketRepository;
import vn.edu.fpt.seal.modules.user.entity.User;
import vn.edu.fpt.seal.modules.user.repository.UserRepository;
import vn.edu.fpt.seal.security.AuthorizationService;
import vn.edu.fpt.seal.security.CurrentUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceTest {
    @Mock SupportTicketRepository tickets;
    @Mock UserRepository users;
    @Mock AuthorizationService authorization;
    SupportTicketService service;
    CurrentUser current;
    UsernamePasswordAuthenticationToken authentication;
    User owner;
    SupportTicket ticket;

    @BeforeEach
    void setUp() {
        service = new SupportTicketService(tickets, users, authorization);
        current = CurrentUser.builder().id(UUID.randomUUID()).roles(List.of("team_member")).build();
        authentication = new UsernamePasswordAuthenticationToken(current, null);
        owner = User.builder().email("owner@example.com").fullName("Owner").passwordHash("x").build();
        owner.setId(current.getId());
        ticket = SupportTicket.builder().requester(owner).category("technical").priority("normal")
                .subject("Help").description("Details").status("open").build();
        ticket.setId(UUID.randomUUID());
        lenient().when(authorization.current(authentication)).thenReturn(current);
    }

    @Test
    void regularUserListsOnlyOwnTickets() {
        when(tickets.findByRequesterIdOrderByCreatedAtDesc(current.getId())).thenReturn(List.of(ticket));
        assertEquals(1, service.list(null, authentication).size());
        verify(tickets, never()).findTop100ByOrderByCreatedAtDesc();
    }

    @Test
    void regularUserCannotSupplyAnotherRequesterId() {
        assertEquals(403, assertThrows(ApiException.class,
                () -> service.list(UUID.randomUUID(), authentication)).getStatus().value());
    }

    @Test
    void coordinatorCanListAllTickets() {
        when(authorization.isCoordinator(current)).thenReturn(true);
        when(tickets.findTop100ByOrderByCreatedAtDesc()).thenReturn(List.of(ticket));
        assertEquals(1, service.list(null, authentication).size());
    }

    @Test
    void ownerCanViewButOtherUserCannot() {
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        assertDoesNotThrow(() -> service.get(ticket.getId(), authentication));

        CurrentUser other = CurrentUser.builder().id(UUID.randomUUID()).roles(List.of("team_member")).build();
        var otherAuth = new UsernamePasswordAuthenticationToken(other, null);
        when(authorization.current(otherAuth)).thenReturn(other);
        doThrow(ApiException.forbidden("denied")).when(authorization)
                .require(false, "You can only view your own support tickets");
        assertEquals(403, assertThrows(ApiException.class,
                () -> service.get(ticket.getId(), otherAuth)).getStatus().value());
    }

    @Test
    void openCanMoveToInProgress() {
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        assertEquals("in_progress",
                service.updateStatus(ticket.getId(), new UpdateSupportTicketStatusRequest("in_progress")).status());
    }

    @Test
    void inProgressCanMoveToOpen() {
        ticket.setStatus("in_progress");
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        assertEquals("open",
                service.updateStatus(ticket.getId(), new UpdateSupportTicketStatusRequest("open")).status());
    }

    @Test
    void inProgressCanMoveToResolved() {
        ticket.setStatus("in_progress");
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        assertEquals("resolved",
                service.updateStatus(ticket.getId(), new UpdateSupportTicketStatusRequest("resolved")).status());
    }

    @Test
    void resolvedCanReopen() {
        ticket.setStatus("resolved");
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        assertEquals("open",
                service.updateStatus(ticket.getId(), new UpdateSupportTicketStatusRequest("open")).status());
    }

    @Test
    void invalidTransitionIsRejected() {
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        assertEquals(400, assertThrows(ApiException.class,
                () -> service.updateStatus(ticket.getId(), new UpdateSupportTicketStatusRequest("resolved")))
                .getStatus().value());
    }
}
