package com.imaginamos.todo.dto.response;

import com.imaginamos.todo.entity.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;

public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime executionDate;
    private TaskStatus status;
    private List<TaskItemResponse> items;
    private boolean pendingExecution;
    private boolean dueNowAlert;
    private long completedItems;
    private long totalItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public List<TaskItemResponse> getItems() {
        return items;
    }

    public void setItems(List<TaskItemResponse> items) {
        this.items = items;
    }

    public boolean isPendingExecution() {
        return pendingExecution;
    }

    public void setPendingExecution(boolean pendingExecution) {
        this.pendingExecution = pendingExecution;
    }

    public boolean isDueNowAlert() {
        return dueNowAlert;
    }

    public void setDueNowAlert(boolean dueNowAlert) {
        this.dueNowAlert = dueNowAlert;
    }

    public long getCompletedItems() {
        return completedItems;
    }

    public void setCompletedItems(long completedItems) {
        this.completedItems = completedItems;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
