package com.nido.api.identity.application.handler;

import com.nido.api.identity.domain.model.InvitationState;
import com.nido.api.identity.domain.port.out.AccountInvitationPort;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.model.UserAdminView;
import com.nido.api.identity.domain.port.out.TwoFactorMethodsPort;
import com.nido.api.identity.domain.port.out.UserAdminPort;
import com.nido.api.shared.model.PageResult;
import com.nido.api.shared.model.Role;
import com.nido.api.shared.model.SortRequest;
import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListUsersHandlerTest {

    @Mock UserAdminPort userAdminPort;
    @Mock TwoFactorMethodsPort twoFactorMethods;
    @Mock AccountInvitationPort invitations;

    private ListUsersHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ListUsersHandler(userAdminPort, twoFactorMethods, invitations);
    }

    @Test
    void listUsers_returnsPageWithEachAccountsMethods() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        User user1 = new User(id1, "alice", "alice@test.com", Role.USER, true, Instant.now(), null);
        User user2 = new User(id2, "bob", "bob@test.com", Role.ADMIN, false, Instant.now(), null);
        SortRequest sort = SortRequest.descBy("createdAt");

        when(userAdminPort.findAll(0, 20, sort, null))
            .thenReturn(new PageResult<>(List.of(user1, user2), 2L, 0, 20));
        when(twoFactorMethods.activeMethodsAmong(Set.of(id1, id2))).thenReturn(Map.of(id1, Set.of(TwoFactorMethod.APP), id2, Set.of()));
        InvitationState pending = new InvitationState(false, Instant.parse("2026-10-12T10:00:00Z"));
        when(invitations.invitationsAmong(Set.of(id1, id2))).thenReturn(Map.of(id2, pending));

        PageResult<UserAdminView> result = handler.listUsers(0, 20, sort, null);

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2L);
        UserAdminView view1 = result.content().stream().filter(v -> v.id().equals(id1)).findFirst().orElseThrow();
        UserAdminView view2 = result.content().stream().filter(v -> v.id().equals(id2)).findFirst().orElseThrow();
        assertThat(view1.twoFactorMethods()).containsExactly(TwoFactorMethod.APP);
        assertThat(view1.isActive()).isTrue();
        assertThat(view2.twoFactorMethods()).isEmpty();
        assertThat(view2.isActive()).isFalse();
        assertThat(view1.invitation()).isNull();
        assertThat(view2.invitation()).isEqualTo(pending);
    }

    @Test
    void listUsers_emptyPage_doesNotCallTotpPort() {
        SortRequest sort = SortRequest.descBy("createdAt");
        when(userAdminPort.findAll(0, 20, sort, null))
            .thenReturn(new PageResult<>(List.of(), 0L, 0, 20));

        PageResult<UserAdminView> result = handler.listUsers(0, 20, sort, null);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verify(twoFactorMethods, never()).activeMethodsAmong(any());
        verify(invitations, never()).invitationsAmong(any());
    }

    @Test
    void listUsers_passesSearchThroughToPort() {
        SortRequest sort = SortRequest.descBy("createdAt");
        when(userAdminPort.findAll(0, 20, sort, "alice"))
            .thenReturn(new PageResult<>(List.of(), 0L, 0, 20));

        handler.listUsers(0, 20, sort, "alice");

        verify(userAdminPort).findAll(0, 20, sort, "alice");
    }
}