package com.nido.api.calendar.infrastructure.config;

import com.nido.api.infrastructure.config.ExistingCiphertextCheck;
import com.nido.api.infrastructure.config.SpaceCiphertextCheck;
import com.nido.api.infrastructure.config.SpaceEncryptorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration
public class CalendarEncryptionConfig {

    // One key per space, shared with every module that encrypts a space's data — see SpaceKeyCache.
    @Bean
    CalendarEncryptorFactory calendarEncryptorFactory(SpaceEncryptorFactory spaces) {
        return spaces::forSpace;
    }

    /** The calendar has been encrypted since it exists: one of its titles per space proves the key. */
    @Bean
    ExistingCiphertextCheck calendarCiphertextCheck(JdbcClient jdbc, SpaceEncryptorFactory spaces) {
        return new SpaceCiphertextCheck(jdbc, spaces::forSpace, "calendar events", """
            SELECT DISTINCT ON (space_id) space_id, value FROM (
                SELECT space_id, title_encrypted AS value FROM calendar_events
                UNION ALL
                SELECT space_id, title_encrypted FROM calendar_recurring_event_series
            ) sample
            ORDER BY space_id""");
    }
}
