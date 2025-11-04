package com.example.appointmentservice.controller;

import com.example.appointmentservice.business.client.AppointmentSecurityService;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final AppointmentSecurityService securityService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
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
    @PreAuthorize("hasRole('STUDENT') or hasRole('PROPERTY_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> getAppointmentById(
            @PathVariable @NotBlank(message = "Appointment ID cannot be blank") String appointmentId) {
        log.info("Received request to get appointment by ID: {}", appointmentId);

        
        if (!securityService.canAccessAppointment(appointmentId)) {
            log.warn("User denied access to appointment: {}", appointmentId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AppointmentResponse.error("Access denied to this appointment", "ACCESS_DENIED"));
        }

        AppointmentResponse response = appointmentService.getAppointmentById(appointmentId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('PROPERTY_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> getAppointmentsByUserId(
            @PathVariable @NotBlank(message = "User ID cannot be blank") String userId) {
        log.info("Received request to get appointments for user: {}", userId);

        // Check if user can access appointments for this user ID
        if (!securityService.canAccessUserAppointments(userId)) {
            log.warn("User denied access to appointments for user: {}", userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AppointmentResponse.error("Access denied to these appointments", "ACCESS_DENIED"));
        }

        AppointmentResponse response = appointmentService.getAppointmentsByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/requester/{requesterId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('PROPERTY_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> getAppointmentsByRequesterId(
            @PathVariable @NotBlank(message = "Requester ID cannot be blank") String requesterId) {
        log.info("Received request to get appointments for requester: {}", requesterId);

        // Check if user can access appointments for this requester
        if (!securityService.canAccessRequesterAppointments(requesterId)) {
            log.warn("User denied access to appointments for requester: {}", requesterId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AppointmentResponse.error("Access denied to these appointments", "ACCESS_DENIED"));
        }

        AppointmentResponse response = appointmentService.getAppointmentsByRequesterId(requesterId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/provider/{providerId}")
    @PreAuthorize("hasRole('PROPERTY_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> getAppointmentsByProviderId(
            @PathVariable @NotBlank(message = "Provider ID cannot be blank") String providerId) {
        log.info("Received request to get appointments for provider: {}", providerId);


        if (!securityService.canAccessProviderAppointments(providerId)) {
            log.warn("User denied access to appointments for provider: {}", providerId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AppointmentResponse.error("Access denied to these appointments", "ACCESS_DENIED"));
        }

        AppointmentResponse response = appointmentService.getAppointmentsByProviderId(providerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("hasRole('PROPERTY_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> getAppointmentsByPropertyId(
            @PathVariable @NotBlank(message = "Property ID cannot be blank") String propertyId) {
        log.info("Received request to get appointments for property: {}", propertyId);
        AppointmentResponse response = appointmentService.getAppointmentsByPropertyId(propertyId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('PROPERTY_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> getAppointmentsByStatus(@PathVariable AppointmentStatus status) {
        log.info("Received request to get appointments by status: {}", status);
        AppointmentResponse response = appointmentService.getAppointmentsByStatus(status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('PROPERTY_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> getAppointmentsByType(@PathVariable AppointmentType type) {
        log.info("Received request to get appointments by type: {}", type);
        AppointmentResponse response = appointmentService.getAppointmentsByType(type);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available-slots")
    @PreAuthorize("hasRole('STUDENT') or hasRole('PROPERTY_MANAGER') or hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('PROPERTY_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> confirmAppointment(
            @PathVariable @NotBlank(message = "Appointment ID cannot be blank") String appointmentId) {
        log.info("Received request to confirm appointment: {}", appointmentId);


        if (!securityService.canModifyAppointment(appointmentId)) {
            log.warn("User denied permission to confirm appointment: {}", appointmentId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AppointmentResponse.error("Access denied to modify this appointment", "ACCESS_DENIED"));
        }

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
        // Token-based confirmation doesn't require authentication
        AppointmentResponse response = appointmentService.confirmAppointmentByToken(confirmationToken);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasRole('STUDENT') or hasRole('PROPERTY_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable @NotBlank(message = "Appointment ID cannot be blank") String appointmentId,
            @RequestParam(required = false) String cancellationReason) {
        log.info("Received request to cancel appointment: {}", appointmentId);


        if (!securityService.canModifyAppointment(appointmentId)) {
            log.warn("User denied permission to cancel appointment: {}", appointmentId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AppointmentResponse.error("Access denied to modify this appointment", "ACCESS_DENIED"));
        }

        AppointmentResponse response = appointmentService.cancelAppointment(appointmentId, cancellationReason);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{appointmentId}/reschedule")
    @PreAuthorize("hasRole('STUDENT') or hasRole('PROPERTY_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            @PathVariable @NotBlank(message = "Appointment ID cannot be blank") String appointmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDateTime) {
        log.info("Received request to reschedule appointment {} to {}", appointmentId, newDateTime);

        // Check if user can modify this appointment
        if (!securityService.canModifyAppointment(appointmentId)) {
            log.warn("User denied permission to reschedule appointment: {}", appointmentId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AppointmentResponse.error("Access denied to modify this appointment", "ACCESS_DENIED"));
        }

        AppointmentResponse response = appointmentService.rescheduleAppointment(appointmentId, newDateTime);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{appointmentId}/complete")
    @PreAuthorize("hasRole('PROPERTY_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> completeAppointment(
            @PathVariable @NotBlank(message = "Appointment ID cannot be blank") String appointmentId) {
        log.info("Received request to complete appointment: {}", appointmentId);

        // Only admin or the provider can mark as complete
        if (!securityService.canAccessAppointment(appointmentId)) {
            log.warn("User denied permission to complete appointment: {}", appointmentId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AppointmentResponse.error("Access denied to modify this appointment", "ACCESS_DENIED"));
        }

        AppointmentResponse response = appointmentService.completeAppointment(appointmentId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{appointmentId}/no-show")
    @PreAuthorize("hasRole('PROPERTY_MANAGER') or hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AppointmentResponse> updateAppointment(
            @PathVariable @NotBlank(message = "Appointment ID cannot be blank") String appointmentId,
            @Valid @RequestBody AppointmentRequest request) {
        log.info("Received request to update appointment: {}", appointmentId);

        // Check if user can modify this appointment (students can only modify their own)
        if (!securityService.canModifyAppointment(appointmentId)) {
            log.warn("User denied permission to update appointment: {}", appointmentId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AppointmentResponse.error("Access denied to modify this appointment", "ACCESS_DENIED"));
        }

        AppointmentResponse response = appointmentService.updateAppointment(appointmentId, request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }


    @DeleteMapping("/{appointmentId}")
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('STUDENT') or hasRole('PROPERTY_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> getAppointmentStatistics(
            @PathVariable @NotBlank(message = "User ID cannot be blank") String userId) {
        log.info("Received request to get appointment statistics for user: {}", userId);

        // Check if user can access this user's statistics
        if (!securityService.canAccessUserAppointments(userId)) {
            log.warn("User denied access to statistics for user: {}", userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AppointmentResponse.error("Access denied to these statistics", "ACCESS_DENIED"));
        }

        AppointmentResponse response = appointmentService.getAppointmentStatistics(Long.valueOf(userId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conflict-check")
    @PreAuthorize("hasRole('STUDENT') or hasRole('PROPERTY_MANAGER') or hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('PROPERTY_MANAGER') or hasRole('ADMIN')")
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