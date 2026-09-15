package ru.practicum.shareit.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErrorHandlerTest {
    private final ErrorHandler errorHandler = new ErrorHandler();

    @Test
    void shouldHandleDomainExceptions() {
        ProblemDetail notFound = errorHandler.handleNotFoundException(
                new NotFoundException("missing")
        );
        ProblemDetail forbidden = errorHandler.handleForbiddenException(
                new ForbiddenException("forbidden")
        );
        ProblemDetail conflict = errorHandler.handleEmailAlreadyExistsException(
                new EmailAlreadyExistsException("duplicate")
        );
        ProblemDetail invalid = errorHandler.handleIllegalArgumentException(
                new IllegalArgumentException("invalid")
        );

        assertProblem(notFound, HttpStatus.NOT_FOUND, "Resource not found", "missing");
        assertProblem(forbidden, HttpStatus.FORBIDDEN, "Access denied", "forbidden");
        assertProblem(conflict, HttpStatus.CONFLICT, "Email already exists", "duplicate");
        assertProblem(invalid, HttpStatus.BAD_REQUEST, "Invalid request", "invalid");
    }

    @Test
    void shouldCollectValidationErrorsByField() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "Wrong email"));
        bindingResult.addError(new FieldError("request", "email", "Duplicate error"));
        bindingResult.addError(new FieldError("request", "name", "Name is blank"));
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ProblemDetail result = errorHandler.handleValidationException(exception);

        assertProblem(result, HttpStatus.BAD_REQUEST, "Validation failed",
                "Запрос содержит некорректные данные");
        assertThat(result.getProperties()).isNotNull();
        assertThat(result.getProperties().get("errors"))
                .isEqualTo(Map.of("email", "Wrong email", "name", "Name is blank"));
    }

    private void assertProblem(ProblemDetail problem, HttpStatus status, String title, String detail) {
        assertThat(problem.getStatus()).isEqualTo(status.value());
        assertThat(problem.getTitle()).isEqualTo(title);
        assertThat(problem.getDetail()).isEqualTo(detail);
        assertThat(problem.getProperties()).isNotNull();
        assertThat(problem.getProperties().get("error")).isEqualTo(detail);
    }
}
