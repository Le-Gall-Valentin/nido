package com.nido.api.tasks.infrastructure.persistence.adapter;

import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.SubtaskInput;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.model.TaskStatus;
import com.nido.api.tasks.domain.model.Subtask;
import com.nido.api.tasks.domain.model.UpdateTaskCommand;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import com.nido.api.tasks.infrastructure.persistence.entity.TaskAssigneeEntity;
import com.nido.api.tasks.infrastructure.persistence.entity.TaskEntity;
import com.nido.api.tasks.infrastructure.persistence.entity.TaskSubtaskEntity;
import com.nido.api.tasks.infrastructure.persistence.repository.TaskAssigneeJpaRepository;
import com.nido.api.tasks.infrastructure.persistence.repository.TaskJpaRepository;
import com.nido.api.tasks.infrastructure.persistence.repository.TaskSubtaskJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TaskRepositoryAdapter implements TaskRepository {

    private final TaskJpaRepository tasks;
    private final TaskAssigneeJpaRepository assignees;
    private final TaskSubtaskJpaRepository subtasks;

    public TaskRepositoryAdapter(TaskJpaRepository tasks, TaskAssigneeJpaRepository assignees, TaskSubtaskJpaRepository subtasks) {
        this.tasks = tasks;
        this.assignees = assignees;
        this.subtasks = subtasks;
    }

    @Override
    public Optional<Task> findById(UUID taskId) {
        return tasks.findById(taskId).map(this::toDomain);
    }

    @Override
    public List<Task> findBySpaceId(UUID spaceId) {
        return toDomainList(tasks.findBySpaceId(spaceId));
    }

    @Override
    @Transactional
    public Task create(CreateTaskCommand command) {
        TaskEntity e = new TaskEntity();
        e.setSpaceId(command.spaceId());
        e.setTitle(command.title());
        e.setStatus(TaskStatus.TODO);
        e.setPriority(command.priority());
        e.setDueDate(command.dueDate());
        e.setRecurringSeriesId(command.recurringSeriesId());
        TaskEntity saved = tasks.saveAndFlush(e);
        saveAssigneesAndSubtasks(saved.getId(), command.assigneeIds(), command.subtasks());
        return findById(saved.getId()).orElseThrow(TaskException.TaskNotFound::new);
    }

    @Override
    @Transactional
    public Task update(UpdateTaskCommand command) {
        TaskEntity e = tasks.findById(command.taskId()).orElseThrow(TaskException.TaskNotFound::new);
        e.setTitle(command.title());
        e.setPriority(command.priority());
        e.setDueDate(command.dueDate());
        tasks.saveAndFlush(e);
        assignees.deleteByTaskId(e.getId());
        for (UUID userId : command.assigneeIds()) {
            TaskAssigneeEntity ae = new TaskAssigneeEntity();
            ae.setTaskId(e.getId());
            ae.setUserId(userId);
            assignees.save(ae);
        }
        assignees.flush();
        return findById(e.getId()).orElseThrow(TaskException.TaskNotFound::new);
    }

    @Override
    @Transactional
    public Task updateStatus(UUID taskId, TaskStatus status) {
        TaskEntity e = tasks.findById(taskId).orElseThrow(TaskException.TaskNotFound::new);
        e.setStatus(status);
        tasks.saveAndFlush(e);
        return findById(taskId).orElseThrow(TaskException.TaskNotFound::new);
    }

    @Override
    @Transactional
    public Task toggleSubtask(UUID taskId, UUID subtaskId) {
        TaskSubtaskEntity se = subtasks.findById(subtaskId).orElseThrow(TaskException.TaskNotFound::new);
        se.setDone(!se.isDone());
        subtasks.saveAndFlush(se);
        return findById(taskId).orElseThrow(TaskException.TaskNotFound::new);
    }

    @Override
    public void delete(UUID taskId) {
        tasks.deleteById(taskId);
        tasks.flush();
    }

    private void saveAssigneesAndSubtasks(UUID taskId, List<UUID> assigneeIds, List<SubtaskInput> subtaskInputs) {
        for (UUID userId : assigneeIds) {
            TaskAssigneeEntity ae = new TaskAssigneeEntity();
            ae.setTaskId(taskId);
            ae.setUserId(userId);
            assignees.save(ae);
        }
        for (int i = 0; i < subtaskInputs.size(); i++) {
            SubtaskInput input = subtaskInputs.get(i);
            TaskSubtaskEntity se = new TaskSubtaskEntity();
            se.setTaskId(taskId);
            se.setPosition(i);
            se.setText(input.text());
            se.setDone(input.done());
            subtasks.save(se);
        }
        assignees.flush();
        subtasks.flush();
    }

    private List<Task> toDomainList(List<TaskEntity> found) {
        if (found.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = found.stream().map(TaskEntity::getId).toList();
        Map<UUID, List<TaskAssigneeEntity>> assigneesByTask = assignees
            .findByTaskIdInOrderByTaskIdAsc(ids).stream()
            .collect(Collectors.groupingBy(TaskAssigneeEntity::getTaskId));
        Map<UUID, List<TaskSubtaskEntity>> subtasksByTask = subtasks
            .findByTaskIdInOrderByTaskIdAscPositionAsc(ids).stream()
            .collect(Collectors.groupingBy(TaskSubtaskEntity::getTaskId));
        return found.stream()
            .map(e -> toDomain(e,
                assigneesByTask.getOrDefault(e.getId(), List.of()),
                subtasksByTask.getOrDefault(e.getId(), List.of())))
            .toList();
    }

    private Task toDomain(TaskEntity e) {
        return toDomain(e, assignees.findByTaskId(e.getId()), subtasks.findByTaskIdOrderByPositionAsc(e.getId()));
    }

    private Task toDomain(TaskEntity e, Collection<TaskAssigneeEntity> assigneeEntities, List<TaskSubtaskEntity> subtaskEntities) {
        return new Task(e.getId(), e.getSpaceId(), e.getTitle(), e.getStatus(), e.getPriority(), e.getDueDate(),
            assigneeEntities.stream().map(TaskAssigneeEntity::getUserId).toList(),
            subtaskEntities.stream().map(s -> new Subtask(s.getId(), s.getText(), s.isDone())).toList(),
            e.getRecurringSeriesId(), e.getCreatedAt());
    }
}
