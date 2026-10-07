package com.nido.api.tasks.infrastructure.persistence.entity;

import com.nido.api.infrastructure.persistence.entity.AssignedUuidEntity;
import com.nido.api.infrastructure.sealing.SealedColumn;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "task_subtasks")
@Getter
@Setter
@NoArgsConstructor
public class TaskSubtaskEntity extends AssignedUuidEntity {

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(nullable = false)
    private int position;

    public static final SealedColumn TEXT = SealedColumn.throughParent("task_subtasks", "text_encrypted", "task_id", "tasks").withClearColumn("text");

    @Column(name = "text_encrypted", nullable = false)
    private String textEncrypted;

    @Column(nullable = false)
    private boolean done;
}
