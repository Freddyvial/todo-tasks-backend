package com.imaginamos.todo.dto.request;

import com.imaginamos.todo.entity.TaskStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TaskCreateRequest {

    @NotBlank
    @Size(min = 3, max = 120)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotNull
    private LocalDateTime executionDate;

    private TaskStatus status;

    @Valid
    private List<TaskItemRequest> items = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(LocalDateTime executionDate) {
        this.executionDate = executionDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public List<TaskItemRequest> getItems() {
        return items;
    }

    public void setItems(List<TaskItemRequest> items) {
        this.items = items;
    }
}
