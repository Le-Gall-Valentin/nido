package com.nido.api.notifications.infrastructure.persistence;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.notifications.domain.model.NotificationChannel;
import com.nido.api.notifications.domain.model.NotificationPreferences;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each test makes its own account rather than emptying the users table: other integration tests own that
 * table's cleanup, and the foreign keys cascade, so whatever is left here goes with their deleteAll().
 */
@IntegrationTestConfig
class NotificationPreferencesJdbcAdapterIT {

    private static final NotificationType INVITATION = new NotificationType("space.invitation");
    private static final NotificationType REMINDER = new NotificationType("agenda.reminder");
    private static final Instant NOW = Instant.parse("2026-10-02T10:00:00Z");

    @Autowired NotificationPreferencesJdbcAdapter adapter;
    @Autowired UserIdentityJpaRepository users;
    @Autowired JdbcClient jdbc;

    private UUID janeId;

    @BeforeEach
    void setUp() {
        janeId = newAccount();
    }

    private UUID newAccount() {
        String name = "np-" + UUID.randomUUID().toString().substring(0, 8);
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(name);
        user.setEmail(name + "@test.local");
        user.setRole(Role.USER);
        return users.saveAndFlush(user).getId();
    }

    private int rowsOf(UUID userId) {
        return jdbc.sql("""
                SELECT (SELECT count(*) FROM notification_channel_preferences WHERE user_id = :id)
                     + (SELECT count(*) FROM notification_type_preferences WHERE user_id = :id)
                """)
            .param("id", userId).query(Integer.class).single();
    }

    @Test
    void an_account_that_never_chose_has_everything_on() {
        assertThat(adapter.find(janeId)).isEqualTo(NotificationPreferences.DEFAULTS);
    }

    @Test
    void a_choice_is_saved_and_read_back() {
        adapter.saveChannel(janeId, NotificationChannel.EMAIL, false, NOW);
        adapter.saveType(janeId, INVITATION, false, NOW);
        adapter.saveType(janeId, REMINDER, true, NOW);

        assertThat(adapter.find(janeId)).isEqualTo(new NotificationPreferences(
            Map.of(NotificationChannel.EMAIL, false), Map.of(INVITATION, false, REMINDER, true)));
    }

    @Test
    void saving_the_same_switch_again_replaces_it() {
        adapter.saveChannel(janeId, NotificationChannel.EMAIL, false, NOW);
        adapter.saveChannel(janeId, NotificationChannel.EMAIL, true, NOW.plusSeconds(1));
        adapter.saveType(janeId, INVITATION, false, NOW);
        adapter.saveType(janeId, INVITATION, true, NOW.plusSeconds(1));

        assertThat(adapter.find(janeId).isOn(NotificationChannel.EMAIL)).isTrue();
        assertThat(adapter.find(janeId).isOn(INVITATION)).isTrue();
        assertThat(rowsOf(janeId)).isEqualTo(2);
        assertThat(jdbc.sql("SELECT updated_at FROM notification_type_preferences WHERE user_id = :id")
                .param("id", janeId).query(OffsetDateTime.class).single().toInstant())
            .isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void a_stored_code_the_application_no_longer_knows_is_ignored() {
        jdbc.sql("INSERT INTO notification_channel_preferences VALUES (:id, 'fax', false, :now)")
            .param("id", janeId).param("now", NOW.atOffset(ZoneOffset.UTC)).update();
        jdbc.sql("INSERT INTO notification_type_preferences VALUES (:id, 'Not A Code', false, :now)")
            .param("id", janeId).param("now", NOW.atOffset(ZoneOffset.UTC)).update();
        adapter.saveType(janeId, INVITATION, false, NOW);

        assertThat(adapter.find(janeId)).isEqualTo(new NotificationPreferences(Map.of(), Map.of(INVITATION, false)));
    }

    @Test
    void forgetting_an_account_leaves_the_others_alone() {
        UUID bobId = newAccount();
        adapter.saveChannel(janeId, NotificationChannel.EMAIL, false, NOW);
        adapter.saveType(janeId, INVITATION, false, NOW);
        adapter.saveType(bobId, INVITATION, false, NOW);

        adapter.deleteAllFor(janeId);

        assertThat(rowsOf(janeId)).isZero();
        assertThat(adapter.find(bobId).isOn(INVITATION)).isFalse();
    }

    @Test
    void the_choices_go_with_the_account_row() {
        adapter.saveType(janeId, INVITATION, false, NOW);

        users.deleteById(janeId);
        users.flush();

        assertThat(rowsOf(janeId)).isZero();
    }
}
