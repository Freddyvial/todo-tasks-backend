package com.imaginamos.todo.service;

import com.imaginamos.todo.dto.request.TaskCreateRequest;
import com.imaginamos.todo.dto.request.TaskItemRequest;
import com.imaginamos.todo.dto.request.TaskStatusUpdateRequest;
import com.imaginamos.todo.dto.request.TaskUpdateRequest;
import com.imaginamos.todo.entity.Task;
import com.imaginamos.todo.entity.TaskItem;
import com.imaginamos.todo.entity.TaskStatus;
import com.imaginamos.todo.exception.InvalidSortParameterException;
import com.imaginamos.todo.exception.InvalidTaskStateException;
import com.imaginamos.todo.exception.TaskNotFoundException;
import com.imaginamos.todo.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task task;

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(1L);
        task.setTitle("Existing task");
        task.setExecutionDate(LocalDateTime.now().plusDays(1));
        task.setStatus(TaskStatus.PROGRAMMED);
    }

    @Test
    void createShouldApplyDefaultStatusAndNormalizeFields() {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("  New task  ");
        request.setDescription("  Description  ");
        request.setExecutionDate(LocalDateTime.now().plusDays(2));
        request.setItems(List.of(itemRequest(null, "  First item  ", true, 2)));

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task savedTask = invocation.getArgument(0);
            savedTask.setId(10L);
            return savedTask;
        });
        when(taskRepository.findByIdWithItems(10L)).thenAnswer(invocation -> {
            Task persistedTask = new Task();
            persistedTask.setId(10L);
            persistedTask.setTitle("New task");
            persistedTask.setDescription("Description");
            persistedTask.setExecutionDate(request.getExecutionDate());
            persistedTask.setStatus(TaskStatus.PROGRAMMED);

            TaskItem persistedItem = new TaskItem();
            persistedItem.setDescription("First item");
            persistedItem.setCompleted(true);
            persistedItem.setPosition(2);
            persistedTask.addItem(persistedItem);

            return Optional.of(persistedTask);
        });

        Task created = taskService.create(request);

        assertThat(created.getTitle()).isEqualTo("New task");
        assertThat(created.getDescription()).isEqualTo("Description");
        assertThat(created.getStatus()).isEqualTo(TaskStatus.PROGRAMMED);
        assertThat(created.getItems()).hasSize(1);
        assertThat(created.getItems().getFirst().getDescription()).isEqualTo("First item");
        assertThat(created.getItems().getFirst().getCompleted()).isTrue();
    }

    @Test
    void getByIdShouldReturnTaskWhenExists() {
        when(taskRepository.findByIdWithItems(1L)).thenReturn(Optional.of(task));

        Task found = taskService.getById(1L);

        assertThat(found).isSameAs(task);
    }

    @Test
    void getByIdShouldThrowWhenTaskDoesNotExist() {
        when(taskRepository.findByIdWithItems(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task with id 99 was not found");
    }

    @Test
    void updateShouldReplaceItemsAndPreserveRequestedOrder() {
        TaskItem existingItem = new TaskItem();
        existingItem.setId(5L);
        existingItem.setDescription("Legacy");
        existingItem.setCompleted(false);
        existingItem.setPosition(10);
        task.addItem(existingItem);

        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("  Updated task  ");
        request.setDescription("   ");
        request.setExecutionDate(LocalDateTime.now().plusHours(2));
        request.setStatus(TaskStatus.IN_PROGRESS);
        request.setItems(List.of(
                itemRequest(null, "Second", false, 2),
                itemRequest(5L, "First", true, 1)
        ));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.findByIdWithItems(1L)).thenReturn(Optional.of(task));

        Task updated = taskService.update(1L, request);

        assertThat(updated.getTitle()).isEqualTo("Updated task");
        assertThat(updated.getDescription()).isNull();
        assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(updated.getItems()).hasSize(2);
        assertThat(updated.getItems().get(0).getId()).isEqualTo(5L);
        assertThat(updated.getItems().get(0).getDescription()).isEqualTo("First");
        assertThat(updated.getItems().get(0).getCompleted()).isTrue();
        assertThat(updated.getItems().get(1).getDescription()).isEqualTo("Second");
    }

    @Test
    void updateStatusShouldRejectTransitionFromFinalStatus(CapturedOutput output) {
        task.setStatus(TaskStatus.FINISHED);
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findByIdWithItems(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(1L, request))
                .isInstanceOf(InvalidTaskStateException.class)
                .hasMessageContaining("Transición de estado no permitida");
        assertThat(output).contains("invalid status transition from");
    }

    @Test
    void deleteShouldRemoveExistingTask() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        doNothing().when(taskRepository).delete(task);

        taskService.delete(1L);

        verify(taskRepository).delete(task);
    }

    @Test
    void findAllShouldUseDefaultSortWhenSortIsMissing() {
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));
        when(taskRepository.findAllByIdInWithItems(List.of(1L))).thenReturn(List.of(task));

        taskService.findAll(0, 10, null, null, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().toString()).isEqualTo("executionDate: ASC,id: ASC");
    }

    @Test
    void findAllShouldRejectUnsupportedSortProperty(CapturedOutput output) {
        assertThatThrownBy(() -> taskService.findAll(0, 10, null, null, null, null, "items,asc"))
                .isInstanceOf(InvalidSortParameterException.class)
                .hasMessage("Unsupported sort field: items");

        verify(taskRepository, never()).findAll(any(Specification.class), any(Pageable.class));
        assertThat(output).contains("unsupported sort property received");
    }

    @Test
    void createShouldLogAndRethrowUnexpectedPersistenceError(CapturedOutput output) {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Failure task");
        request.setExecutionDate(LocalDateTime.now().plusDays(1));
        when(taskRepository.save(any(Task.class))).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> taskService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
        assertThat(output).contains("unexpected error creating task");
        assertThat(output).contains("database unavailable");
    }

    @Test
    void deleteShouldLogAndRethrowUnexpectedDeletionError(CapturedOutput output) {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        doThrow(new IllegalStateException("delete failed")).when(taskRepository).delete(task);

        assertThatThrownBy(() -> taskService.delete(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("delete failed");
        assertThat(output).contains("unexpected error deleting task id=1");
        assertThat(output).contains("delete failed");
    }

    @Test
    void createShouldUseExplicitStatusWhenProvided() {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Task");
        request.setExecutionDate(LocalDateTime.now().plusDays(1));
        request.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });
        when(taskRepository.findByIdWithItems(5L)).thenReturn(Optional.of(task));

        taskService.create(request);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void updateStatusShouldTransitionSuccessfully() {
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findByIdWithItems(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        taskService.updateStatus(1L, request);

        verify(taskRepository).save(task);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void updateStatusShouldSkipValidationWhenStatusIsUnchanged() {
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus(TaskStatus.PROGRAMMED);

        when(taskRepository.findByIdWithItems(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        assertThatNoException().isThrownBy(() -> taskService.updateStatus(1L, request));
        verify(taskRepository).save(task);
    }

    @Test
    void updateStatusShouldSkipValidationWhenCurrentStatusIsNull() {
        task.setStatus(null);
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findByIdWithItems(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        assertThatNoException().isThrownBy(() -> taskService.updateStatus(1L, request));
        verify(taskRepository).save(task);
    }

    @Test
    void updateStatusShouldLogAndRethrowUnexpectedError(CapturedOutput output) {
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findByIdWithItems(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenThrow(new IllegalStateException("status update failed"));

        assertThatThrownBy(() -> taskService.updateStatus(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("status update failed");
        assertThat(output).contains("unexpected error updating status for task id=1");
    }

    @Test
    void updateShouldLogAndRethrowUnexpectedError(CapturedOutput output) {
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("Title");
        request.setExecutionDate(LocalDateTime.now().plusDays(1));
        request.setStatus(TaskStatus.PROGRAMMED);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenThrow(new IllegalStateException("update failed"));

        assertThatThrownBy(() -> taskService.update(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("update failed");
        assertThat(output).contains("unexpected error updating task id=1");
    }

    @Test
    void findAllShouldReturnEmptyPageWhenNoResults() {
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Task> result = taskService.findAll(0, 10, null, null, null, null, null);

        assertThat(result.getContent()).isEmpty();
        verify(taskRepository, never()).findAllByIdInWithItems(any());
    }

    @Test
    void findAllShouldSkipTasksNotReturnedByBatchLoad() {
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));
        when(taskRepository.findAllByIdInWithItems(List.of(1L))).thenReturn(List.of());

        Page<Task> result = taskService.findAll(0, 10, null, null, null, null, null);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findAllShouldTreatBlankSortAsDefault() {
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        taskService.findAll(0, 10, null, null, null, null, "   ");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().toString()).isEqualTo("executionDate: ASC,id: ASC");
    }

    @Test
    void findAllShouldDefaultToAscWhenNoDirectionInSort() {
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        taskService.findAll(0, 10, null, null, null, null, "title");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().toString()).isEqualTo("title: ASC");
    }

    @Test
    void findAllShouldLogAndRethrowUnexpectedError(CapturedOutput output) {
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenThrow(new IllegalStateException("db error"));

        assertThatThrownBy(() -> taskService.findAll(0, 10, null, null, null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db error");
        assertThat(output).contains("unexpected error searching tasks");
    }

    @Test
    void updateShouldHandleEmptyItemsList() {
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("Updated");
        request.setExecutionDate(LocalDateTime.now().plusDays(1));
        request.setStatus(TaskStatus.PROGRAMMED);
        request.setItems(List.of());

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.findByIdWithItems(1L)).thenReturn(Optional.of(task));

        taskService.update(1L, request);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getItems()).isEmpty();
    }

    @Test
    void updateShouldUseFallbackPositionWhenItemPositionIsNull() {
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("Updated");
        request.setExecutionDate(LocalDateTime.now().plusDays(1));
        request.setStatus(TaskStatus.PROGRAMMED);
        request.setItems(List.of(
                itemRequest(null, "Item A", false, null),
                itemRequest(null, "Item B", false, null)
        ));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.findByIdWithItems(1L)).thenReturn(Optional.of(task));

        taskService.update(1L, request);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        List<TaskItem> savedItems = captor.getValue().getItems();
        assertThat(savedItems).hasSize(2);
        assertThat(savedItems.get(0).getPosition()).isEqualTo(1);
        assertThat(savedItems.get(1).getPosition()).isEqualTo(2);
    }

    private TaskItemRequest itemRequest(Long id, String description, Boolean completed, Integer position) {
        TaskItemRequest itemRequest = new TaskItemRequest();
        itemRequest.setId(id);
        itemRequest.setDescription(description);
        itemRequest.setCompleted(completed);
        itemRequest.setPosition(position);
        return itemRequest;
    }
}
