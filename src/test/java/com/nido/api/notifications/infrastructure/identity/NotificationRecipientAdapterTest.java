package com.nido.api.notifications.infrastructure.identity;

import com.nido.api.identity.application.port.in.FindUserUseCase;
import com.nido.api.identity.domain.model.User;
import com.nido.api.notifications.domain.model.NotificationRecipient;
import com.nido.api.shared.model.Language;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRecipientAdapterTest {

    @Mock FindUserUseCase findUser;

    private final UUID janeId = UUID.randomUUID();

    @Test
    void an_account_is_found_with_what_the_channels_need() {
        when(findUser.findById(janeId)).thenReturn(Optional.of(
            new User(janeId, "jane", "jane@test.local", Role.USER, true, Instant.now(), Language.FR)));

        assertThat(new NotificationRecipientAdapter(findUser).find(janeId))
            .contains(new NotificationRecipient(janeId, "jane", "jane@test.local", Language.FR, Role.USER, true));
    }

    @Test
    void a_deactivated_account_is_found_and_says_so() {
        when(findUser.findById(janeId)).thenReturn(Optional.of(
            new User(janeId, "jane", "jane@test.local", Role.USER, false, Instant.now(), null)));

        assertThat(new NotificationRecipientAdapter(findUser).find(janeId))
            .hasValueSatisfying(recipient -> assertThat(recipient.active()).isFalse());
    }

    @Test
    void an_account_that_is_gone_is_not_found() {
        when(findUser.findById(janeId)).thenReturn(Optional.empty());

        assertThat(new NotificationRecipientAdapter(findUser).find(janeId)).isEmpty();
    }
}
