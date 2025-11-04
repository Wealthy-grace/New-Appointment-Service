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


    AppointmentResponse createAppointment(AppointmentRequest request);


    AppointmentResponse getAppointmentById(String appointmentId);


    AppointmentResponse getAppointmentsByUserId(String userId);


    AppointmentResponse getAppointmentsByRequesterId(String requesterId);


    AppointmentResponse getAppointmentsByProviderId(String providerId);


    AppointmentResponse getAppointmentsByPropertyId(String propertyId);


    AppointmentResponse getAppointmentsByStatus(AppointmentStatus status);

    AppointmentResponse getAppointmentsByType(AppointmentType type);


    AppointmentResponse getAppointmentsByDateRange(LocalDateTime startDate, LocalDateTime endDate);


    AppointmentResponse getAvailableSlots(String providerId, LocalDate date, Integer durationMinutes);


    AppointmentResponse confirmAppointment(String appointmentId);


    AppointmentResponse confirmAppointmentByToken(String confirmationToken);


    AppointmentResponse cancelAppointment(String appointmentId, String cancellationReason);


    AppointmentResponse rescheduleAppointment(String appointmentId, LocalDateTime newDateTime);


    AppointmentResponse completeAppointment(String appointmentId);


    AppointmentResponse markAsNoShow(String appointmentId);


    AppointmentResponse updateAppointment(String appointmentId, AppointmentRequest request);


    AppointmentResponse deleteAppointment(String appointmentId);


    boolean hasConflictingAppointment(String providerId, LocalDateTime startTime,
                                      LocalDateTime endTime, String excludeAppointmentId);

    List<AppointmentDto> getUpcomingAppointmentsForReminders();


    void markReminderSent(String appointmentId);


    AppointmentResponse getAppointmentStatistics(Long userId);



    // Add the new methods I created
    AppointmentResponse getAllAppointmentsWithPropertyDetails();
    AppointmentResponse getAppointmentsByPropertyIdWithDetails(Long propertyId);
    AppointmentResponse getAppointmentsByRequesterIdWithDetails(Long requesterId);
}