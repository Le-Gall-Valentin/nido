package com.nido.api.calendar.infrastructure.config;

import com.nido.api.calendar.infrastructure.persistence.entity.CalendarEventEntity;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarRecurringEventSeriesEntity;
import com.nido.api.infrastructure.sealing.SealedColumns;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CalendarEncryptionConfig {

    /** The columns of the calendar that hold sealed values — see SealedValueMigration and SpaceKeyGuard. */
    @Bean
    SealedColumns calendarSealedColumns() {
        return SealedColumns.of(CalendarEventEntity.TITLE, CalendarEventEntity.DESCRIPTION, CalendarEventEntity.LOCATION,
            CalendarRecurringEventSeriesEntity.TITLE, CalendarRecurringEventSeriesEntity.DESCRIPTION,
            CalendarRecurringEventSeriesEntity.LOCATION);
    }
}
