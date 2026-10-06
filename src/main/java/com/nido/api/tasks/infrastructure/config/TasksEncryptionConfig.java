package com.nido.api.tasks.infrastructure.config;

import com.nido.api.infrastructure.config.EncryptionBackfill;
import com.nido.api.infrastructure.config.PlaintextTable;
import com.nido.api.infrastructure.config.PlaintextTableEncryptor;
import com.nido.api.infrastructure.config.PlaintextTablesBackfill;
import com.nido.api.infrastructure.config.SpaceEncryptorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TasksEncryptionConfig {

    /** What versions before 0.13.1 stored in clear — see EncryptionBackfillRunner. */
    @Bean
    EncryptionBackfill tasksEncryptionBackfill(PlaintextTableEncryptor encryptor, SpaceEncryptorFactory spaces) {
        return new PlaintextTablesBackfill(encryptor, spaces::forSpace,
            PlaintextTable.ofSpace("tasks", "title"),
            PlaintextTable.throughParent("task_subtasks", "task_id", "tasks", "text"),
            PlaintextTable.ofSpace("recurring_task_series", "title"),
            PlaintextTable.throughParent("recurring_task_series_subtask_templates", "series_id", "recurring_task_series", "text"));
    }
}
