package com.nido.api.identity.infrastructure.notification;

import com.nido.api.identity.domain.model.User;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.notifications.application.port.in.NotifyUseCase;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminActivityNotificationAdapterTest {

    private static final AppPath USERS = new AppPath("/administration/users");

    @Mock NotifyUseCase notify;

    private final User alice = new User(UUID.randomUUID(), "alice", "alice@test.com", Role.SUPER_ADMIN, true, Instant.now(), null);
    private final User root = new User(UUID.randomUUID(), "root", "root@test.com", Role.SUPER_ADMIN, true, Instant.now(), null);

    private List<NotificationRequest> sent(int count) {
        ArgumentCaptor<NotificationRequest> requests = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notify, times(count)).notify(requests.capture());
        return requests.getAllValues();
    }

    @Test
    void each_super_administrator_gets_a_mail_greeting_them_that_never_expires() {
        new AdminActivityNotificationAdapter(notify).accountDeactivated(List.of(alice, root), "bob", "carol");

        List<NotificationRequest> requests = sent(2);
        assertThat(requests).extracting(NotificationRequest::recipientId).containsExactly(alice.id(), root.id());
        assertThat(requests).extracting(NotificationRequest::expiresAt).containsOnlyNulls();
        assertThat(requests.get(0).notification()).isEqualTo(new AccountDeactivatedNotification("alice", "bob", "carol", USERS));
        assertThat(requests.get(1).notification()).isEqualTo(new AccountDeactivatedNotification("root", "bob", "carol", USERS));
    }

    @Test
    void each_gesture_has_its_kind_of_notification() {
        AdminActivityNotificationAdapter adapter = new AdminActivityNotificationAdapter(notify);

        adapter.accountCreated(List.of(alice), "bob", "carol");
        adapter.accountReactivated(List.of(alice), "bob", "carol");
        adapter.accountDeleted(List.of(alice), "bob", "carol");
        adapter.totpReset(List.of(alice), "bob", "carol");

        assertThat(sent(4)).extracting(NotificationRequest::notification).containsExactly(
            new AccountCreatedNotification("alice", "bob", "carol", USERS),
            new AccountReactivatedNotification("alice", "bob", "carol", USERS),
            new AccountDeletedNotification("alice", "bob", "carol", USERS),
            new AccountTotpResetNotification("alice", "bob", "carol", USERS));
    }
}
