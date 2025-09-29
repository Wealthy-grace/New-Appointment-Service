package com.example.appointmentservice.controller;
import com.example.appointmentservice.business.interfaces.AppointmentService;
import com.example.appointmentservice.domain.request.AppointmentRequest;
import com.example.appointmentservice.domain.response.AppointmentResponse;
import com.example.appointmentservice.persistence.model.AppointmentStatus;
import com.example.appointmentservice.persistence.model.AppointmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Slf4j
@Validated
@CrossOrigin(origins = "*")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(@Valid @RequestBody AppointmentRequest request) {
        log.info("Received request to create appointment: {}", request.getAppointmentTitle());
        AppointmentResponse response = appointmentService.createAppointment(request);

        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(
            @PathVariable @NotBlank(message = "Appointment ID cannot be blank") String appointmentId) {
        log.info("Received request to get appointment by ID: {}", appointmentId);
        AppointmentResponse response = appointmentService.getAppointmentById(appointmentId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<AppointmentResponse> getAppointmentsByUserId(
            @PathVariable @NotBlank(message = "User ID cannot be blank") String userId) {
        log.info("Received request to get appointments for user: {}", userId);
        AppointmentResponse response = appointmentService.getAppointmentsByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/requester/{requesterId}")
    public ResponseEntity<AppointmentResponse> getAppointmentsByRequesterId(
            @PathVariable @NotBlank(message = "Requester ID cannot be blank") String requesterId) {
        log.info("Received request to get appointments for requester: {}", requesterId);
        AppointmentResponse response = appointmentService.getAppointmentsByRequesterId(requesterId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<AppointmentResponse> getAppointmentsByProviderId(
            @PathVariable @NotBlank(message = "Provider ID cannot be blank") String providerId) {
        log.info("Received request to get appointments for provider: {}", providerId);
        AppointmentResponse response = appointmentService.getAppointmentsByProviderId(providerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<AppointmentResponse> getAppointmentsByPropertyId(
            @PathVariable @NotBlank(message = "Property ID cannot be blank") String propertyId) {
        log.info("Received request to get appointments for property: {}", propertyId);
        AppointmentResponse response = appointmentService.getAppointmentsByPropertyId(propertyId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<AppointmentResponse> getAppointmentsByStatus(@PathVariable AppointmentStatus status) {
        log.info("Received request to get appointments by status: {}", status);
        AppointmentResponse response = appointmentService.getAppointmentsByStatus(status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<AppointmentResponse> getAppointmentsByType(@PathVariable AppointmentType type) {
        log.info("Received request to get appointments by type: {}", type);
        AppointmentResponse response = appointmentService.getAppointmentsByType(type);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available-slots")
    public ResponseEntity<AppointmentResponse> getAvailableSlots(
            @RequestParam @NotBlank(message = "Provider ID cannot be blank") String providerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "60") Integer durationMinutes) {
        log.info("Received request to get available slots for provider {} on {}", providerId, date);

        if (date.isBefore(LocalDate.now())) {
            AppointmentResponse errorResponse = AppointmentResponse.error("Date cannot be in the past");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        AppointmentResponse response = appointmentService.getAvailableSlots(providerId, date, durationMinutes);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{appointmentId}/confirm")
    public ResponseEntity<AppointmentResponse> confirmAppointment(
            @PathVariable @NotBlank(message = "Appointment ID cannot be blank") String appointmentId) {
        log.info("Received request to confirm appointment: {}", appointmentId);
        AppointmentResponse response = appointmentService.confirmAppointment(appointmentId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/confirm-by-token/{confirmationToken}")
    public ResponseEntity<AppointmentResponse> confirmAppointmentByToken(@PathVariable String confirmationToken) {
        log.info("Received request to confirm appointment by token");
        AppointmentResponse response = appointmentService.confirmAppointmentByToken(confirmationToken);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{appointmentId}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable @NotBlank(message = "Appointment ID cannot be blank") String appointmentId,
            @RequestParam(required = false) String cancellationReason) {
        log.info("Received request to cancel appointment: {}", appointmentId);
        AppointmentResponse response = appointmentService.cancelAppointment(appointmentId, cancellationReason);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{appointmentId}/reschedule")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            @PathVariable @NotBlank(message = "Appointment ID cannot be blank") String appointmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDateTime) {
        log.info("Received request to reschedule appointment {} to {}", appointmentId, newDateTime);
        AppointmentResponse response = appointmentService.rescheduleAppointment(appointmentId, newDateTime);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{appointmentId}/complete")
    public ResponseEntity<AppointmentResponse> completeAppointment(
            @PathVariable @NotBlank(message = "Appointment ID cannot be blank") String appointmentId) {
        log.info("Received request to complete appointment: {}", appointmentId);
        AppointmentResponse response = appointmentService.completeAppointment(appointmentId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{appointmentId}/no-show")
    public ResponseEntity<AppointmentResponse> markAsNoShow(
            @PathVariable @NotBlank(message = "Appointment ID cannot be blank") String appointmentId) {
        log.info("Received request to mark appointment as no-show: {}", appointmentId);
        AppointmentResponse response = appointmentService.markAsNoShow(appointmentId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{appointmentId}")
    public ResponseEntity<AppointmentResponse> updateAppointment(
            @PathVariable @NotBlank(message = "Appointment ID cannot be blank") String appointmentId,
            @Valid @RequestBody AppointmentRequest request) {
        log.info("Received request to update appointment: {}", appointmentId);
        AppointmentResponse response = appointmentService.updateAppointment(appointmentId, request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<AppointmentResponse> deleteAppointment(
            @PathVariable @NotBlank(message = "Appointment ID cannot be blank") String appointmentId) {
        log.info("Received request to delete appointment: {}", appointmentId);
        AppointmentResponse response = appointmentService.deleteAppointment(appointmentId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{userId}/statistics")
    public ResponseEntity<AppointmentResponse> getAppointmentStatistics(
            @PathVariable @NotBlank(message = "User ID cannot be blank") String userId) {
        log.info("Received request to get appointment statistics for user: {}", userId);
        AppointmentResponse response = appointmentService.getAppointmentStatistics(Long.valueOf(userId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conflict-check")
    public ResponseEntity<Boolean> checkAppointmentConflict(
            @RequestParam @NotBlank(message = "Provider ID cannot be blank") String providerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String excludeAppointmentId) {
        log.info("Checking appointment conflict for provider: {}", providerId);

        boolean hasConflict = appointmentService.hasConflictingAppointment(
                providerId, startTime, endTime, excludeAppointmentId);

        return ResponseEntity.ok(hasConflict);
    }

    @GetMapping("/date-range")
    public ResponseEntity<AppointmentResponse> getAppointmentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Received request to get appointments between {} and {}", startDate, endDate);

        if (startDate.isAfter(endDate)) {
            AppointmentResponse errorResponse = AppointmentResponse.error("Start date must be before end date");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        AppointmentResponse response = appointmentService.getAppointmentsByDateRange(startDate, endDate);
        return ResponseEntity.ok(response);
    }
}