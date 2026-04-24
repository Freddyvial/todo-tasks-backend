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
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private static final Set<TaskStatus> FINAL_STATUSES = Set.of(TaskStatus.FINISHED, TaskStatus.CANCELLED);
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("id", "title", "executionDate", "status", "createdAt", "updatedAt");

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = Map.of(
            TaskStatus.PROGRAMMED,  Set.of(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED),
            TaskStatus.IN_PROGRESS, Set.of(TaskStatus.FINISHED,    TaskStatus.CANCELLED),
            TaskStatus.FINISHED,    Set.of(),
            TaskStatus.CANCELLED,   Set.of()
    );

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Task create(TaskCreateRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle().trim());
        task.setDescription(normalizeText(request.getDescription()));
        task.setExecutionDate(request.getExecutionDate());
        task.setStatus(request.getStatus() == null ? TaskStatus.PROGRAMMED : request.getStatus());
        applyItems(task, request.getItems());
        try {
            Task savedTask = taskRepository.save(task);
            return findTaskWithItems(savedTask.getId());
        } catch (RuntimeException ex) {
            log.error("Service: unexpected error creating task with title='{}'", task.getTitle(), ex);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Task getById(Long id) {
        return findTaskWithItems(id);
    }

    @Transactional(readOnly = true)
    public Page<Task> findAll(int page,
                              int size,
                              String query,
                              TaskStatus status,
                              Boolean pendingOnly,
                              Boolean dueNowOnly,
                              String sort) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        Specification<Task> specification = buildSpecification(query, status, pendingOnly, dueNowOnly);
        try {
            Page<Task> pageResult = taskRepository.findAll(specification, pageable);
            List<Task> hydratedTasks = loadTasksWithItems(pageResult.getContent());
            return new PageImpl<>(hydratedTasks, pageable, pageResult.getTotalElements());
        } catch (RuntimeException ex) {
            log.error("Service: unexpected error searching tasks page={}, size={}, query='{}', status={}, pendingOnly={}, dueNowOnly={}, sort='{}'",
                    page, size, query, status, pendingOnly, dueNowOnly, sort, ex);
            throw ex;
        }
    }

    public Task update(Long id, TaskUpdateRequest request) {
        Task task = findTask(id);
        task.setTitle(request.getTitle().trim());
        task.setDescription(normalizeText(request.getDescription()));
        task.setExecutionDate(request.getExecutionDate());
        validateStatusTransition(task.getStatus(), request.getStatus());
        task.setStatus(request.getStatus());
        applyItems(task, request.getItems());
        try {
            Task savedTask = taskRepository.save(task);
            return findTaskWithItems(savedTask.getId());
        } catch (RuntimeException ex) {
            log.error("Service: unexpected error updating task id={} with status='{}'", id, request.getStatus(), ex);
            throw ex;
        }
    }

    public Task updateStatus(Long id, TaskStatusUpdateRequest request) {
        Task task = findTaskWithItems(id);
        validateStatusTransition(task.getStatus(), request.getStatus());
        task.setStatus(request.getStatus());
        if (TaskStatus.FINISHED == request.getStatus()) {
            task.getItems().forEach(item -> item.setCompleted(true));
        }
        try {
            Task savedTask = taskRepository.save(task);
            return findTaskWithItems(savedTask.getId());
        } catch (RuntimeException ex) {
            log.error("Service: unexpected error updating status for task id={} to '{}'", id, request.getStatus(), ex);
            throw ex;
        }
    }

    public void delete(Long id) {
        Task task = findTask(id);
        try {
            taskRepository.delete(task);
        } catch (RuntimeException ex) {
            log.error("Service: unexpected error deleting task id={}", id, ex);
            throw ex;
        }
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> {
            log.warn("Service: task id={} was not found", id);
            return new TaskNotFoundException(id);
        });
    }

    private Task findTaskWithItems(Long id) {
        return taskRepository.findByIdWithItems(id).orElseThrow(() -> {
            log.warn("Service: task id={} was not found", id);
            return new TaskNotFoundException(id);
        });
    }

    private List<Task> loadTasksWithItems(Collection<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }

        List<Long> orderedIds = tasks.stream()
                .map(Task::getId)
                .toList();

        Map<Long, Task> tasksById = taskRepository.findAllByIdInWithItems(orderedIds).stream()
                .collect(Collectors.toMap(Task::getId, Function.identity()));

        return orderedIds.stream()
                .map(tasksById::get)
                .filter(task -> task != null)
                .toList();
    }

    private void applyItems(Task task, List<TaskItemRequest> itemRequests) {
        Map<Long, TaskItem> existingItemsById = new HashMap<>();
        for (TaskItem existingItem : task.getItems()) {
            if (existingItem.getId() != null) {
                existingItemsById.put(existingItem.getId(), existingItem);
            }
        }

        task.clearItems();
        if (itemRequests == null || itemRequests.isEmpty()) {
            return;
        }

        List<TaskItemRequest> orderedItems = new ArrayList<>(itemRequests);
        orderedItems.sort(Comparator.comparing(TaskItemRequest::getPosition, Comparator.nullsLast(Integer::compareTo)));

        int fallbackPosition = 1;
        for (TaskItemRequest itemRequest : orderedItems) {
            TaskItem item = itemRequest.getId() != null
                    ? existingItemsById.getOrDefault(itemRequest.getId(), new TaskItem())
                    : new TaskItem();
            item.setDescription(itemRequest.getDescription().trim());
            item.setCompleted(Boolean.TRUE.equals(itemRequest.getCompleted()));
            item.setPosition(itemRequest.getPosition() != null ? itemRequest.getPosition() : fallbackPosition++);
            task.addItem(item);
        }
    }

    private void validateStatusTransition(TaskStatus currentStatus, TaskStatus newStatus) {
        if (currentStatus == null || newStatus == null || currentStatus == newStatus) {
            return;
        }
        Set<TaskStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            log.warn("Service: invalid status transition from '{}' to '{}'", currentStatus, newStatus);
            throw new InvalidTaskStateException(
                    "Transición de estado no permitida: de " + currentStatus + " a " + newStatus);
        }
    }

    private Specification<Task> buildSpecification(String query,
                                                   TaskStatus status,
                                                   Boolean pendingOnly,
                                                   Boolean dueNowOnly) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            criteriaQuery.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String searchValue = "%" + query.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchValue),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchValue)
                ));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (Boolean.TRUE.equals(pendingOnly)) {
                predicates.add(criteriaBuilder.equal(root.get("status"), TaskStatus.PROGRAMMED));
            }

            if (Boolean.TRUE.equals(dueNowOnly)) {
                predicates.add(criteriaBuilder.equal(root.get("status"), TaskStatus.PROGRAMMED));
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("executionDate"), LocalDateTime.now()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort buildSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "executionDate").and(Sort.by(Sort.Direction.ASC, "id"));
        }

        String[] parts = sort.split(",");
        String property = parts[0].trim();
        if (!ALLOWED_SORT_PROPERTIES.contains(property)) {
            log.warn("Service: unsupported sort property received '{}'", property);
            throw new InvalidSortParameterException(property);
        }
        Sort.Direction direction = parts.length > 1 ? Sort.Direction.fromOptionalString(parts[1].trim()).orElse(Sort.Direction.ASC)
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
