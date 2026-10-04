package com.nido.api.notifications.infrastructure.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.mail.application.port.in.MailAvailabilityQuery;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.notifications.domain.model.NotificationCatalog;
import com.nido.api.notifications.domain.model.NotificationChannel;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.notifications.infrastructure.channel.mail.MailChannelAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The one place that knows both the catalogue and the card's words. A kind added in a sending context
 * without its label would show its raw key on the preferences page; this fails the build instead.
 * Labels are nested by code because '.' is i18next's key separator: space.invitation → type.space.invitation.
 */
class NotificationLabelsTest {

    private static final Path LOCALES = Path.of("src/main/frontend/src/features/notification-preferences/locales");
    private static final NotificationCatalog CATALOG = new ScannedNotificationCatalogAdapter(List.of("com.nido.api"),
        List.of(new MailChannelAdapter(mock(SendMailUseCase.class), mock(MailAvailabilityQuery.class)))).catalog();

    @ParameterizedTest(name = "{0}.json")
    @ValueSource(strings = {"fr", "en"})
    void every_channel_every_kind_and_every_group_has_its_words(String language) throws Exception {
        JsonNode labels = new ObjectMapper().readTree(LOCALES.resolve(language + ".json").toFile());
        List<String> missing = new ArrayList<>();
        for (NotificationChannel channel : NotificationChannel.values()) {
            requireLine(labels.path("channel").path(channel.code()), "channel." + channel.code(), missing);
        }
        for (NotificationType type : CATALOG.types()) {
            String name = type.code().substring(type.code().indexOf('.') + 1);
            if (!hasText(labels.path("group").path(type.group()))) {
                missing.add("group." + type.group());
            }
            requireLine(labels.path("type").path(type.group()).path(name), "type." + type.code(), missing);
        }

        assertThat(missing).as("labels missing from %s.json", language).isEmpty();
    }

    @Test
    void the_rule_actually_looked_at_something() {
        assertThat(CATALOG.types()).extracting(NotificationType::code).contains("space.invitation");
    }

    private static void requireLine(JsonNode entry, String key, List<String> missing) {
        if (!hasText(entry.path("label"))) {
            missing.add(key + ".label");
        }
        if (!hasText(entry.path("description"))) {
            missing.add(key + ".description");
        }
    }

    /** A label shows on the card: an empty one is as missing as an absent one. */
    private static boolean hasText(JsonNode node) {
        return node.isTextual() && !node.asText().isBlank();
    }
}
