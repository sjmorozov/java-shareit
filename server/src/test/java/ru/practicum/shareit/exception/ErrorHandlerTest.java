package ru.practicum.shareit.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

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

    private void assertProblem(ProblemDetail problem, HttpStatus status, String title, String detail) {
        assertThat(problem.getStatus()).isEqualTo(status.value());
        assertThat(problem.getTitle()).isEqualTo(title);
        assertThat(problem.getDetail()).isEqualTo(detail);
        assertThat(problem.getProperties()).isNotNull();
        assertThat(problem.getProperties().get("error")).isEqualTo(detail);
    }
}
