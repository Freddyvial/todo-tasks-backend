package com.imaginamos.todo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.imaginamos.todo.dto.request.TaskCreateRequest;
import com.imaginamos.todo.dto.request.TaskStatusUpdateRequest;
import com.imaginamos.todo.dto.request.TaskUpdateRequest;
import com.imaginamos.todo.dto.response.PageResponse;
import com.imaginamos.todo.dto.response.TaskResponse;
import com.imaginamos.todo.entity.Task;
import com.imaginamos.todo.entity.TaskStatus;
import com.imaginamos.todo.exception.GlobalExceptionHandler;
import com.imaginamos.todo.exception.InvalidSortParameterException;
import com.imaginamos.todo.exception.TaskNotFoundException;
import com.imaginamos.todo.mapper.TaskMapper;
import com.imaginamos.todo.service.TaskService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private TaskMapper taskMapper;

    @Test
    void createShouldReturnCreatedResponseWithLocationHeader() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Task title");
        request.setExecutionDate(LocalDateTime.of(2026, 4, 22, 10, 0));

        Task task = new Task();
        TaskResponse response = response(1L, "Task title", TaskStatus.PROGRAMMED);

        when(taskService.create(any(TaskCreateRequest.class))).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(response);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/tasks/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PROGRAMMED"));
    }

    @Test
    void createShouldReturnBadRequestForInvalidBody() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("  ");

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getByIdShouldReturnTaskResponse() throws Exception {
        Task task = new Task();
        TaskResponse response = response(7L, "Task 7", TaskStatus.IN_PROGRESS);

        when(taskService.getById(7L)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(response);

        mockMvc.perform(get("/api/tasks/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Task 7"));
    }

    @Test
    void getByIdShouldReturnNotFoundWhenTaskDoesNotExist() throws Exception {
        when(taskService.getById(99L)).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task with id 99 was not found"));
    }

    @Test
    void findAllShouldReturnPagedResponse() throws Exception {
        PageResponse<TaskResponse> pageResponse = new PageResponse<>();
        pageResponse.setContent(List.of(response(3L, "Task 3", TaskStatus.PROGRAMMED)));
        pageResponse.setPage(0);
        pageResponse.setSize(10);
        pageResponse.setTotalElements(1);
        pageResponse.setTotalPages(1);
        pageResponse.setFirst(true);
        pageResponse.setLast(true);
        pageResponse.setEmpty(false);

        when(taskService.findAll(0, 10, "task", TaskStatus.PROGRAMMED, true, false, "executionDate,asc"))
                .thenReturn(Page.empty());
        when(taskMapper.toPageResponse(ArgumentMatchers.<Page<Task>>any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/tasks")
                        .param("query", "task")
                        .param("status", "PROGRAMMED")
                        .param("pendingOnly", "true")
                        .param("dueNowOnly", "false")
                        .param("sort", "executionDate,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(3))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void findAllShouldReturnBadRequestForUnsupportedSort() throws Exception {
        when(taskService.findAll(0, 10, null, null, null, null, "items,asc"))
                .thenThrow(new InvalidSortParameterException("items"));

        mockMvc.perform(get("/api/tasks").param("sort", "items,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported sort field: items"));
    }

    @Test
    void updateShouldReturnUpdatedResponse() throws Exception {
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("Updated task");
        request.setExecutionDate(LocalDateTime.of(2026, 4, 22, 11, 0));
        request.setStatus(TaskStatus.IN_PROGRESS);

        Task task = new Task();
        TaskResponse response = response(2L, "Updated task", TaskStatus.IN_PROGRESS);

        when(taskService.update(eq(2L), any(TaskUpdateRequest.class))).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(response);

        mockMvc.perform(put("/api/tasks/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void patchStatusShouldReturnUpdatedTask() throws Exception {
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus(TaskStatus.FINISHED);

        Task task = new Task();
        TaskResponse response = response(5L, "Task 5", TaskStatus.FINISHED);

        when(taskService.updateStatus(eq(5L), any(TaskStatusUpdateRequest.class))).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(response);

        mockMvc.perform(patch("/api/tasks/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    void deleteShouldReturnNoContent() throws Exception {
        doNothing().when(taskService).delete(4L);

        mockMvc.perform(delete("/api/tasks/4"))
                .andExpect(status().isNoContent());
    }

    private TaskResponse response(Long id, String title, TaskStatus status) {
        TaskResponse response = new TaskResponse();
        response.setId(id);
        response.setTitle(title);
        response.setStatus(status);
        response.setExecutionDate(LocalDateTime.of(2026, 4, 22, 10, 0));
        response.setItems(List.of());
        response.setCreatedAt(LocalDateTime.of(2026, 4, 22, 9, 0));
        response.setUpdatedAt(LocalDateTime.of(2026, 4, 22, 9, 30));
        return response;
    }
}
