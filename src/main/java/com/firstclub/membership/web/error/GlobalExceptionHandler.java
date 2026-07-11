package com.firstclub.membership.web.error;

import com.firstclub.membership.domain.exception.DuplicateActiveSubscriptionException;
import com.firstclub.membership.domain.exception.IllegalStateTransitionException;
import com.firstclub.membership.domain.exception.InvalidMembershipRequestException;
import com.firstclub.membership.domain.exception.ResourceNotFoundException;
import com.firstclub.membership.domain.exception.UnknownBenefitTypeException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Translates the domain exception family and common framework errors into RFC-7807 {@code ProblemDetail}
 * responses, so every failure has a consistent, machine-readable shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler({DuplicateActiveSubscriptionException.class, IllegalStateTransitionException.class})
    public ProblemDetail handleConflict(RuntimeException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        // A unique-constraint race that reached the client (e.g. concurrent create). Safe to retry.
        log.debug("Data integrity violation surfaced to client: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Conflict", "The resource was modified concurrently; please retry.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid Parameter",
                "Parameter '" + ex.getName() + "' has an invalid value");
    }

    @ExceptionHandler(InvalidMembershipRequestException.class)
    public ProblemDetail handleUnprocessable(InvalidMembershipRequestException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Request", ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        // Retries were exhausted — the caller can safely try again.
        ProblemDetail pd = problem(HttpStatus.CONFLICT, "Concurrent Modification",
                "The subscription was modified concurrently; please retry.");
        log.debug("Optimistic lock conflict surfaced to client: {}", ex.getMessage());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validation Failed", "Request validation failed");
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Validation Failed", ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed Request", "Request body is missing or malformed");
    }

    @ExceptionHandler(UnknownBenefitTypeException.class)
    public ProblemDetail handleConfigError(UnknownBenefitTypeException ex) {
        log.error("Benefit configuration error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Configuration Error", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        return pd;
    }
}
