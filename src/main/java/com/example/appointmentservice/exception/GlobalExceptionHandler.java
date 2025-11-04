package com.example.appointmentservice.exception;

import com.example.appointmentservice.domain.response.AppointmentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for Appointment Service
 * Handles all exceptions across the application with proper HTTP status codes
 *
 * Exception Hierarchy:
 * 1. Validation Errors (400)
 * 2. Business Logic Errors (404, 409)
 * 3. Security Errors (401, 403)
 * 4. External Service Errors (503)
 * 5. Generic Errors (500)
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ========== VALIDATION EXCEPTIONS ==========

    /**
     * Handle validation errors from @Valid annotations
     * Returns 400 Bad Request with field-level error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AppointmentResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String errorMessage = "Validation failed: " + errors.toString();
        AppointmentResponse response = AppointmentResponse.error(errorMessage, "VALIDATION_ERROR");
        response.setTimestamp(LocalDateTime.now());

        log.error("Validation error: {}", errors);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle type mismatch errors (e.g., passing "abc" for Long parameter)
     * Returns 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<AppointmentResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String errorMessage = String.format(
                "Invalid parameter '%s': expected type %s but got '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
                ex.getValue()
        );

        AppointmentResponse response = AppointmentResponse.error(errorMessage, "INVALID_PARAMETER_TYPE");
        response.setTimestamp(LocalDateTime.now());

        log.error("Type mismatch error: {}", errorMessage);
        return ResponseEntity.badRequest().body(response);
    }

    // ========== BUSINESS LOGIC EXCEPTIONS ==========

    /**
     * Handle appointment not found errors
     * Returns 404 Not Found
     */
    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<AppointmentResponse> handleAppointmentNotFound(AppointmentNotFoundException ex) {
        AppointmentResponse response = AppointmentResponse.error(ex.getMessage(), "APPOINTMENT_NOT_FOUND");
        response.setTimestamp(LocalDateTime.now());

        log.error("Appointment not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle appointment conflicts (e.g., double booking)
     * Returns 409 Conflict
     */
    @ExceptionHandler(AppointmentConflictException.class)
    public ResponseEntity<AppointmentResponse> handleAppointmentConflict(AppointmentConflictException ex) {
        AppointmentResponse response = AppointmentResponse.error(ex.getMessage(), "APPOINTMENT_CONFLICT");
        response.setTimestamp(LocalDateTime.now());

        log.error("Appointment conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ========== SECURITY EXCEPTIONS ==========

    /**
     * Handle authentication failures (invalid/expired token)
     * Returns 401 Unauthorized
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<AppointmentResponse> handleAuthenticationException(AuthenticationException ex) {
        AppointmentResponse response = AppointmentResponse.error(
                "Authentication failed: " + ex.getMessage(),
                "AUTHENTICATION_ERROR"
        );
        response.setTimestamp(LocalDateTime.now());

        log.error("Authentication error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle authorization failures (insufficient permissions)
     * Returns 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AppointmentResponse> handleAccessDenied(AccessDeniedException ex) {
        AppointmentResponse response = AppointmentResponse.error(
                "Access denied: " + ex.getMessage(),
                "ACCESS_DENIED"
        );
        response.setTimestamp(LocalDateTime.now());

        log.error("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ========== REST CLIENT EXCEPTIONS (External Service Calls) ==========

    /**
     * Handle 4xx client errors from external services (User/Property Service)
     * Examples: 400 Bad Request, 404 Not Found, 401 Unauthorized
     * Returns the same status code from the external service
     */
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<AppointmentResponse> handleHttpClientError(HttpClientErrorException ex) {
        String errorMessage = String.format(
                "External service client error [%s]: %s",
                ex.getStatusCode(),
                ex.getResponseBodyAsString()
        );

        AppointmentResponse response = AppointmentResponse.error(
                errorMessage,
                "EXTERNAL_SERVICE_CLIENT_ERROR"
        );
        response.setTimestamp(LocalDateTime.now());

        log.error("HTTP Client Error [{}]: {}", ex.getStatusCode(), ex.getMessage());

        // Return the same status code from the external service
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    /**
     * Handle 5xx server errors from external services
     * Examples: 500 Internal Server Error, 503 Service Unavailable
     * Returns 503 Service Unavailable
     */
    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<AppointmentResponse> handleHttpServerError(HttpServerErrorException ex) {
        String errorMessage = String.format(
                "External service server error [%s]: %s",
                ex.getStatusCode(),
                ex.getMessage()
        );

        AppointmentResponse response = AppointmentResponse.error(
                errorMessage,
                "EXTERNAL_SERVICE_SERVER_ERROR"
        );
        response.setTimestamp(LocalDateTime.now());

        log.error("HTTP Server Error [{}]: {}", ex.getStatusCode(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle connection/timeout errors to external services
     * Examples: Connection refused, timeout, DNS resolution failure
     * Returns 503 Service Unavailable
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<AppointmentResponse> handleResourceAccessException(ResourceAccessException ex) {
        String errorMessage = "Cannot reach external service: " + ex.getMessage();

        AppointmentResponse response = AppointmentResponse.error(
                errorMessage,
                "EXTERNAL_SERVICE_UNAVAILABLE"
        );
        response.setTimestamp(LocalDateTime.now());

        log.error("Resource Access Error - Service unavailable: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Generic RestClient exceptions (catch-all for HTTP communication errors)
     * Returns 503 Service Unavailable
     */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<AppointmentResponse> handleRestClientException(RestClientException ex) {
        String errorMessage = "External service communication error: " + ex.getMessage();

        AppointmentResponse response = AppointmentResponse.error(
                errorMessage,
                "EXTERNAL_SERVICE_ERROR"
        );
        response.setTimestamp(LocalDateTime.now());

        log.error("RestClient error: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    // ========== GENERIC EXCEPTIONS ==========

    /**
     * Catch-all for any unexpected exceptions
     * Returns 500 Internal Server Error
     *
     * This should be the last handler - it catches everything not caught above
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AppointmentResponse> handleGenericException(Exception ex) {
        String errorMessage = "An unexpected error occurred: " + ex.getMessage();

        AppointmentResponse response = AppointmentResponse.error(
                errorMessage,
                "INTERNAL_SERVER_ERROR"
        );
        response.setTimestamp(LocalDateTime.now());

        log.error("Unexpected error: ", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ========== HELPER METHOD ==========

    /**
     * Create error response with consistent structure
     */
    private AppointmentResponse createErrorResponse(String message, String errorCode, LocalDateTime timestamp) {
        AppointmentResponse response = AppointmentResponse.error(message, errorCode);
        response.setTimestamp(timestamp);
        return response;
    }
}