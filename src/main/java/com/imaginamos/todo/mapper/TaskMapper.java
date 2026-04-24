package com.imaginamos.todo.mapper;

import com.imaginamos.todo.dto.response.PageResponse;
import com.imaginamos.todo.dto.response.TaskItemResponse;
import com.imaginamos.todo.dto.response.TaskResponse;
import com.imaginamos.todo.entity.Task;
import com.imaginamos.todo.entity.TaskItem;
import com.imaginamos.todo.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Component
public class TaskMapper {

    public TaskResponse toResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setExecutionDate(task.getExecutionDate());
        response.setStatus(task.getStatus());
        response.setItems(task.getItems().stream()
                .sorted(Comparator.comparing(TaskItem::getPosition, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toItemResponse)
                .toList());
        response.setPendingExecution(task.getStatus() == TaskStatus.PROGRAMMED);
        response.setDueNowAlert(task.getStatus() == TaskStatus.PROGRAMMED
                && !task.getExecutionDate().isAfter(LocalDateTime.now()));
        response.setCompletedItems(task.getItems().stream().filter(item -> Boolean.TRUE.equals(item.getCompleted())).count());
        response.setTotalItems(task.getItems().size());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        return response;
    }

    public PageResponse<TaskResponse> toPageResponse(Page<Task> page) {
        PageResponse<TaskResponse> response = new PageResponse<>();
        List<TaskResponse> content = page.getContent().stream().map(this::toResponse).toList();
        response.setContent(content);
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setFirst(page.isFirst());
        response.setLast(page.isLast());
        response.setEmpty(page.isEmpty());
        return response;
    }

    private TaskItemResponse toItemResponse(TaskItem item) {
        TaskItemResponse response = new TaskItemResponse();
        response.setId(item.getId());
        response.setDescription(item.getDescription());
        response.setCompleted(item.getCompleted());
        response.setPosition(item.getPosition());
        return response;
    }
}
