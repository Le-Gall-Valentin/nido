package com.nido.api.notifications.infrastructure.persistence;

import com.nido.api.notifications.domain.model.NotificationChannel;
import com.nido.api.notifications.domain.model.NotificationPreferences;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.notifications.domain.port.out.NotificationPreferencesRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The choices in Postgres, through plain SQL: a switch is an upsert ({@code ON CONFLICT … DO UPDATE}), so
 * two saves of the same switch — a double click, two tabs — replace each other instead of colliding on the
 * key. A stored code the application no longer knows is skipped on read: a kind removed from the code
 * leaves rows nobody asks about.
 */
@Component
public class NotificationPreferencesJdbcAdapter implements NotificationPreferencesRepository {

    private final JdbcClient jdbc;

    public NotificationPreferencesJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public NotificationPreferences find(UUID userId) {
        Map<NotificationChannel, Boolean> channels = new EnumMap<>(NotificationChannel.class);
        jdbc.sql("SELECT channel, enabled FROM notification_channel_preferences WHERE user_id = :userId")
            .param("userId", userId)
            .query((row, number) -> Map.entry(row.getString("channel"), row.getBoolean("enabled")))
            .list()
            .forEach(choice -> NotificationChannel.fromCode(choice.getKey())
                .ifPresent(channel -> channels.put(channel, choice.getValue())));
        Map<NotificationType, Boolean> types = new HashMap<>();
        jdbc.sql("SELECT type, enabled FROM notification_type_preferences WHERE user_id = :userId")
            .param("userId", userId)
            .query((row, number) -> Map.entry(row.getString("type"), row.getBoolean("enabled")))
            .list()
            .forEach(choice -> parse(choice.getKey()).ifPresent(type -> types.put(type, choice.getValue())));
        return new NotificationPreferences(channels, types);
    }

    @Override
    public void saveChannel(UUID userId, NotificationChannel channel, boolean enabled, Instant now) {
        jdbc.sql("""
                INSERT INTO notification_channel_preferences (user_id, channel, enabled, updated_at)
                VALUES (:userId, :channel, :enabled, :now)
                ON CONFLICT (user_id, channel) DO UPDATE SET enabled = EXCLUDED.enabled, updated_at = EXCLUDED.updated_at
                """)
            .param("userId", userId)
            .param("channel", channel.code())
            .param("enabled", enabled)
            .param("now", utc(now))
            .update();
    }

    @Override
    public void saveType(UUID userId, NotificationType type, boolean enabled, Instant now) {
        jdbc.sql("""
                INSERT INTO notification_type_preferences (user_id, type, enabled, updated_at)
                VALUES (:userId, :type, :enabled, :now)
                ON CONFLICT (user_id, type) DO UPDATE SET enabled = EXCLUDED.enabled, updated_at = EXCLUDED.updated_at
                """)
            .param("userId", userId)
            .param("type", type.code())
            .param("enabled", enabled)
            .param("now", utc(now))
            .update();
    }

    @Override
    public void deleteAllFor(UUID userId) {
        jdbc.sql("DELETE FROM notification_channel_preferences WHERE user_id = :userId").param("userId", userId).update();
        jdbc.sql("DELETE FROM notification_type_preferences WHERE user_id = :userId").param("userId", userId).update();
    }

    private static Optional<NotificationType> parse(String code) {
        try {
            return Optional.of(new NotificationType(code));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
