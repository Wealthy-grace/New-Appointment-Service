package com.example.appointmentservice.exception;
import com.example.appointmentservice.domain.response.AppointmentResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

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

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<AppointmentResponse> handleAppointmentNotFound(AppointmentNotFoundException ex) {
        AppointmentResponse response = AppointmentResponse.error(ex.getMessage(), "APPOINTMENT_NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(AppointmentConflictException.class)
    public ResponseEntity<AppointmentResponse> handleAppointmentConflict(AppointmentConflictException ex) {
        AppointmentResponse response = AppointmentResponse.error(ex.getMessage(), "APPOINTMENT_CONFLICT");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<AppointmentResponse> handleFeignException(FeignException ex) {
        String errorMessage = "External service error: " + ex.getMessage();
        AppointmentResponse response = AppointmentResponse.error(errorMessage, "EXTERNAL_SERVICE_ERROR");

        log.error("Feign client error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AppointmentResponse> handleGenericException(Exception ex) {
        String errorMessage = "An unexpected error occurred: " + ex.getMessage();
        AppointmentResponse response = AppointmentResponse.error(errorMessage, "INTERNAL_SERVER_ERROR");

        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}