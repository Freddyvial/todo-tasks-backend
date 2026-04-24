package com.imaginamos.todo.exception;

import com.imaginamos.todo.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleNotFound(CapturedOutput output) {
        ResponseEntity<ApiErrorResponse> response = handler.handleTaskNotFound(
                new TaskNotFoundException(10L),
                request("/api/tasks/10")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Task with id 10 was not found");
        assertThat(output).contains("task not found");
    }

    @Test
    void shouldHandleInvalidState() {
        ResponseEntity<ApiErrorResponse> response = handler.handleInvalidState(
                new InvalidTaskStateException("Invalid transition"),
                request("/api/tasks/1/status")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid transition");
    }

    @Test
    void shouldHandleValidationErrors(CapturedOutput output) {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "taskCreateRequest");
        bindingResult.addError(new FieldError("taskCreateRequest", "title", "must not be blank"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(exception, request("/api/tasks"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("title must not be blank");
        assertThat(output).contains("validation error");
    }

    @Test
    void shouldHandleConstraintViolation() {
        ConstraintViolationException exception = new ConstraintViolationException("page must be greater than or equal to 0", null);

        ResponseEntity<ApiErrorResponse> response = handler.handleConstraintViolation(exception, request("/api/tasks"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("page must be greater than or equal to 0");
    }

    @Test
    void shouldHandleInvalidSort() {
        ResponseEntity<ApiErrorResponse> response = handler.handleInvalidSort(
                new InvalidSortParameterException("items"),
                request("/api/tasks")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Unsupported sort field: items");
    }

    @Test
    void shouldHideInternalErrorDetails(CapturedOutput output) {
        ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(
                new IllegalStateException("Database exploded"),
                request("/api/tasks")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(output).contains("unexpected error");
        assertThat(output).contains("Database exploded");
    }

    private HttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}
