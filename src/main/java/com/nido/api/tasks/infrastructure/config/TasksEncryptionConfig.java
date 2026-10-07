package com.nido.api.tasks.infrastructure.config;

import com.nido.api.infrastructure.sealing.SealedColumns;
import com.nido.api.tasks.infrastructure.persistence.entity.RecurringTaskSeriesEntity;
import com.nido.api.tasks.infrastructure.persistence.entity.RecurringTaskSeriesSubtaskTemplateEntity;
import com.nido.api.tasks.infrastructure.persistence.entity.TaskEntity;
import com.nido.api.tasks.infrastructure.persistence.entity.TaskSubtaskEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TasksEncryptionConfig {

    /** The columns of tasks that hold sealed values — see SealedValueMigration. */
    @Bean
    SealedColumns tasksSealedColumns() {
        return SealedColumns.of(TaskEntity.TITLE, TaskSubtaskEntity.TEXT, RecurringTaskSeriesEntity.TITLE,
            RecurringTaskSeriesSubtaskTemplateEntity.TEXT);
    }
}
