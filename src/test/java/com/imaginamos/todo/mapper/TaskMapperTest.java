package com.imaginamos.todo.mapper;

import com.imaginamos.todo.dto.response.PageResponse;
import com.imaginamos.todo.dto.response.TaskResponse;
import com.imaginamos.todo.entity.Task;
import com.imaginamos.todo.entity.TaskItem;
import com.imaginamos.todo.entity.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskMapperTest {

    private final TaskMapper taskMapper = new TaskMapper();

    @Test
    void toResponseShouldMapDerivedFieldsAndSortItemsByPosition() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Task");
        task.setDescription("Description");
        task.setExecutionDate(LocalDateTime.now().minusMinutes(5));
        task.setStatus(TaskStatus.PROGRAMMED);
        task.setCreatedAt(LocalDateTime.now().minusDays(1));
        task.setUpdatedAt(LocalDateTime.now());

        task.addItem(item(2L, "Second", false, 2));
        task.addItem(item(1L, "First", true, 1));

        TaskResponse response = taskMapper.toResponse(task);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getDescription()).isEqualTo("First");
        assertThat(response.isPendingExecution()).isTrue();
        assertThat(response.isDueNowAlert()).isTrue();
        assertThat(response.getCompletedItems()).isEqualTo(1);
        assertThat(response.getTotalItems()).isEqualTo(2);
    }

    @Test
    void toPageResponseShouldExposePaginationMetadata() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Task");
        task.setExecutionDate(LocalDateTime.now().plusDays(1));
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setCreatedAt(LocalDateTime.now().minusDays(1));
        task.setUpdatedAt(LocalDateTime.now());

        PageResponse<TaskResponse> pageResponse = taskMapper.toPageResponse(
                new PageImpl<>(List.of(task), PageRequest.of(0, 10), 1)
        );

        assertThat(pageResponse.getContent()).hasSize(1);
        assertThat(pageResponse.getPage()).isZero();
        assertThat(pageResponse.getSize()).isEqualTo(10);
        assertThat(pageResponse.getTotalElements()).isEqualTo(1);
        assertThat(pageResponse.getTotalPages()).isEqualTo(1);
        assertThat(pageResponse.isFirst()).isTrue();
        assertThat(pageResponse.isLast()).isTrue();
        assertThat(pageResponse.isEmpty()).isFalse();
    }

    @Test
    void toResponseShouldNotAlertWhenProgrammedTaskHasFutureDate() {
        Task task = new Task();
        task.setId(2L);
        task.setTitle("Future task");
        task.setExecutionDate(LocalDateTime.now().plusDays(1));
        task.setStatus(TaskStatus.PROGRAMMED);
        task.setCreatedAt(LocalDateTime.now().minusHours(1));
        task.setUpdatedAt(LocalDateTime.now());

        TaskResponse response = taskMapper.toResponse(task);

        assertThat(response.isPendingExecution()).isTrue();
        assertThat(response.isDueNowAlert()).isFalse();
        assertThat(response.getCompletedItems()).isZero();
        assertThat(response.getTotalItems()).isZero();
    }

    private TaskItem item(Long id, String description, boolean completed, int position) {
        TaskItem item = new TaskItem();
        item.setId(id);
        item.setDescription(description);
        item.setCompleted(completed);
        item.setPosition(position);
        return item;
    }
}
