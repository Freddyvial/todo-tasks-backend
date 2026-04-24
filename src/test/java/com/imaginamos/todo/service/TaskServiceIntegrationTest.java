package com.imaginamos.todo.service;

import com.imaginamos.todo.entity.Task;
import com.imaginamos.todo.entity.TaskStatus;
import com.imaginamos.todo.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TaskServiceIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void findAllShouldFilterByQueryMatchingTitle() {
        taskRepository.save(task("Alpha task", "First description", LocalDateTime.now().plusDays(1), TaskStatus.PROGRAMMED));
        taskRepository.save(task("Beta task", "Second description", LocalDateTime.now().plusDays(2), TaskStatus.PROGRAMMED));

        Page<Task> result = taskService.findAll(0, 10, "alpha", null, null, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Alpha task");
    }

    @Test
    void findAllShouldIgnoreBlankQuery() {
        taskRepository.save(task("Task 1", null, LocalDateTime.now().plusDays(1), TaskStatus.PROGRAMMED));
        taskRepository.save(task("Task 2", null, LocalDateTime.now().plusDays(2), TaskStatus.PROGRAMMED));

        Page<Task> result = taskService.findAll(0, 10, "   ", null, null, null, null);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findAllShouldFilterByStatus() {
        taskRepository.save(task("Programmed task", null, LocalDateTime.now().plusDays(1), TaskStatus.PROGRAMMED));
        taskRepository.save(task("In progress task", null, LocalDateTime.now().plusDays(1), TaskStatus.IN_PROGRESS));

        Page<Task> result = taskService.findAll(0, 10, null, TaskStatus.IN_PROGRESS, null, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void findAllShouldFilterPendingOnly() {
        taskRepository.save(task("Programmed", null, LocalDateTime.now().plusDays(1), TaskStatus.PROGRAMMED));
        taskRepository.save(task("In Progress", null, LocalDateTime.now().plusDays(1), TaskStatus.IN_PROGRESS));
        taskRepository.save(task("Finished", null, LocalDateTime.now().plusDays(1), TaskStatus.FINISHED));

        Page<Task> result = taskService.findAll(0, 10, null, null, true, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(TaskStatus.PROGRAMMED);
    }

    @Test
    void findAllShouldFilterDueNowOnly() {
        taskRepository.save(task("Overdue", null, LocalDateTime.now().minusDays(1), TaskStatus.PROGRAMMED));
        taskRepository.save(task("Future", null, LocalDateTime.now().plusDays(1), TaskStatus.PROGRAMMED));
        taskRepository.save(task("In Progress overdue", null, LocalDateTime.now().minusHours(1), TaskStatus.IN_PROGRESS));

        Page<Task> result = taskService.findAll(0, 10, null, null, null, true, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Overdue");
    }

    private Task task(String title, String description, LocalDateTime executionDate, TaskStatus status) {
        Task t = new Task();
        t.setTitle(title);
        t.setDescription(description);
        t.setExecutionDate(executionDate);
        t.setStatus(status);
        return t;
    }
}
