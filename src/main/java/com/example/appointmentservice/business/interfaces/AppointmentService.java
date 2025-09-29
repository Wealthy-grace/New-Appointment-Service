package com.example.appointmentservice.business.interfaces;

import com.example.appointmentservice.domain.dto.AppointmentDto;
import com.example.appointmentservice.domain.request.AppointmentRequest;
import com.example.appointmentservice.domain.response.AppointmentResponse;
import com.example.appointmentservice.persistence.model.AppointmentStatus;
import com.example.appointmentservice.persistence.model.AppointmentType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentService {

    /**
     * Create a new appointment
     */
    AppointmentResponse createAppointment(AppointmentRequest request);

    /**
     * Get appointment by ID
     */
    AppointmentResponse getAppointmentById(String appointmentId);

    /**
     * Get all appointments for a specific user (requester or provider)
     */
    AppointmentResponse getAppointmentsByUserId(String userId);

    /**
     * Get appointments by requester ID
     */
    AppointmentResponse getAppointmentsByRequesterId(String requesterId);

    /**
     * Get appointments by provider ID
     */
    AppointmentResponse getAppointmentsByProviderId(String providerId);

    /**
     * Get appointments by property ID
     */
    AppointmentResponse getAppointmentsByPropertyId(String propertyId);

    /**
     * Get appointments by status
     */
    AppointmentResponse getAppointmentsByStatus(AppointmentStatus status);

    /**
     * Get appointments by type
     */
    AppointmentResponse getAppointmentsByType(AppointmentType type);

    /**
     * Get appointments within a date range
     */
    AppointmentResponse getAppointmentsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get available time slots for a provider on a specific date
     */
    AppointmentResponse getAvailableSlots(String providerId, LocalDate date, Integer durationMinutes);

    /**
     * Confirm an appointment
     */
    AppointmentResponse confirmAppointment(String appointmentId);

    /**
     * Confirm appointment by token
     */
    AppointmentResponse confirmAppointmentByToken(String confirmationToken);

    /**
     * Cancel an appointment
     */
    AppointmentResponse cancelAppointment(String appointmentId, String cancellationReason);

    /**
     * Reschedule an appointment
     */
    AppointmentResponse rescheduleAppointment(String appointmentId, LocalDateTime newDateTime);

    /**
     * Mark appointment as completed
     */
    AppointmentResponse completeAppointment(String appointmentId);

    /**
     * Mark appointment as no-show
     */
    AppointmentResponse markAsNoShow(String appointmentId);

    /**
     * Update appointment details
     */
    AppointmentResponse updateAppointment(String appointmentId, AppointmentRequest request);

    /**
     * Delete an appointment
     */
    AppointmentResponse deleteAppointment(String appointmentId);

    /**
     * Check for appointment conflicts
     */
    boolean hasConflictingAppointment(String providerId, LocalDateTime startTime,
                                      LocalDateTime endTime, String excludeAppointmentId);

    /**
     * Get upcoming appointments for reminders
     */
    List<AppointmentDto> getUpcomingAppointmentsForReminders();

    /**
     * Mark reminder as sent
     */
    void markReminderSent(String appointmentId);

    /**
     * Get appointment statistics for a user
     */
    AppointmentResponse getAppointmentStatistics(Long userId);



    // Add the new methods I created
    AppointmentResponse getAllAppointmentsWithPropertyDetails();
    AppointmentResponse getAppointmentsByPropertyIdWithDetails(Long propertyId);
    AppointmentResponse getAppointmentsByRequesterIdWithDetails(Long requesterId);
}