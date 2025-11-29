package com.intern.paymentservice.exception;

import jakarta.validation.ConstraintViolationException;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@NullMarked
public class GlobalExceptionHandler {

    /**
     * Handles PaymentNotFoundException and returns 404 Not Found.
     * @param ex The PaymentNotFoundException instance.
     * @return ResponseEntity with 404 status and error details.
     */
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handlePaymentNotFoundException(PaymentNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    /**
     * Handles UserAccessDeniedException and returns 403 Forbidden.
     * This typically happens when a UserPaymentServiceImpl enforces self-access.
     * @param ex The UserAccessDeniedException instance.
     * @return ResponseEntity with 403 status and error details.
     */
    @ExceptionHandler(UserAccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleUserAccessDeniedException(UserAccessDeniedException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problemDetail.setTitle("Access Denied");
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }

    /**
     * Handles ResponseStatusException, primarily used for the 403 "Insufficient authority"
     * thrown by the PaymentServiceAuthorizationDecorator.
     * @param ex The ResponseStatusException instance.
     * @return ResponseEntity matching the status code embedded in the exception.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(ResponseStatusException ex) {
        // This catches the FORBIDDEN status thrown by the decorator, but is general purpose.
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
        problemDetail.setTitle(ex.getStatusCode().toString());
        problemDetail.setProperty("timestamp", Instant.now());
        
        return ResponseEntity.status(ex.getStatusCode()).body(problemDetail);
    }

    /**
     * Handles validation errors for request bodies (@RequestBody) and maps them to 400 Bad Request.
     * Provides a structured list of field-specific errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Validation Failed");
        problemDetail.setDetail("The request body contains invalid data.");

        // Extract and map all field errors
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        problemDetail.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problemDetail);
    }

    /**
     * Handles validation errors for path variables or query parameters in @Validated controllers.
     * Maps the error to 400 Bad Request.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Invalid Parameters");
        problemDetail.setDetail("One or more request parameters failed validation.");

        // Extract and map all constraint violations
        List<String> errors = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath().toString() + ": " + violation.getMessage())
                .collect(Collectors.toList());

        problemDetail.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problemDetail);
    }
}