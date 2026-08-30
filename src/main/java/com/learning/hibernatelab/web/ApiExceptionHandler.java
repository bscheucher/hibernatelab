package com.learning.hibernatelab.web;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns domain and request failures into the right status codes.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * A lookup that found nothing is a 404, not a 500.
     *
     * <p>The services throw {@link EntityNotFoundException} when an id does not
     * resolve. Left alone that surfaces as a 500, which is wrong: the request
     * was fine, the row simply is not there.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /**
     * A request body that fails its constraints is a 400, and the response says
     * which field failed.
     *
     * <p>Spring already answers 400 here on its own; what this adds is the
     * per-field detail, so a client is not left guessing which of
     * {@code title} or {@code description} it got wrong.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleInvalidBody(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            String message = error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage();
            // A field can break several constraints at once; keep them all.
            errors.merge(error.getField(), message, (first, second) -> first + "; " + second);
        }

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request body failed validation");
        problem.setProperty("errors", errors);
        return problem;
    }
}
