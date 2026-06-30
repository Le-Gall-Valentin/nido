package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.model.UserAdminView;
import com.synapsys.api.identity.domain.port.out.TotpStatusPort;
import com.synapsys.api.identity.domain.port.out.UserAdminPort;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.Role;
import com.synapsys.api.shared.model.SortRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListUsersHandlerTest {

    @Mock UserAdminPort userAdminPort;
    @Mock TotpStatusPort totpStatusPort;

    private ListUsersHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ListUsersHandler(userAdminPort, totpStatusPort);
    }

    @Test
    void listUsers_returnsPageWithTotpStatus() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        User user1 = new User(id1, "alice", "alice@test.com", Role.USER, true, Instant.now());
        User user2 = new User(id2, "bob", "bob@test.com", Role.ADMIN, false, Instant.now());
        SortRequest sort = SortRequest.descBy("createdAt");

        when(userAdminPort.findAll(0, 20, sort, null))
            .thenReturn(new PageResult<>(List.of(user1, user2), 2L, 0, 20));
        when(totpStatusPort.findTotpEnabledAmong(Set.of(id1, id2))).thenReturn(Set.of(id1));

        PageResult<UserAdminView> result = handler.listUsers(0, 20, sort, null);

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2L);
        UserAdminView view1 = result.content().stream().filter(v -> v.id().equals(id1)).findFirst().orElseThrow();
        UserAdminView view2 = result.content().stream().filter(v -> v.id().equals(id2)).findFirst().orElseThrow();
        assertThat(view1.totpEnabled()).isTrue();
        assertThat(view1.isActive()).isTrue();
        assertThat(view2.totpEnabled()).isFalse();
        assertThat(view2.isActive()).isFalse();
    }

    @Test
    void listUsers_emptyPage_doesNotCallTotpPort() {
        SortRequest sort = SortRequest.descBy("createdAt");
        when(userAdminPort.findAll(0, 20, sort, null))
            .thenReturn(new PageResult<>(List.of(), 0L, 0, 20));

        PageResult<UserAdminView> result = handler.listUsers(0, 20, sort, null);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verify(totpStatusPort, never()).findTotpEnabledAmong(any());
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