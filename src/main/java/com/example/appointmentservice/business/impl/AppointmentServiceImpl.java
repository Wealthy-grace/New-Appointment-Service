package com.example.appointmentservice.business.impl;

import com.example.appointmentservice.business.client.PropertyServiceClient;
import com.example.appointmentservice.business.client.UserServiceClient;
import com.example.appointmentservice.business.interfaces.AppointmentService;
import com.example.appointmentservice.business.mapper.AppointmentMapperDto;
import com.example.appointmentservice.domain.dto.AppointmentDto;
import com.example.appointmentservice.domain.dto.PropertyDto;
import com.example.appointmentservice.domain.response.PropertyServiceResponse;
import com.example.appointmentservice.domain.dto.UserDto;
import com.example.appointmentservice.domain.request.AppointmentRequest;
import com.example.appointmentservice.domain.response.AppointmentResponse;
import com.example.appointmentservice.exception.AppointmentNotFoundException;

import com.example.appointmentservice.persistence.model.AppointmentEntity;
import com.example.appointmentservice.persistence.model.AppointmentStatus;
import com.example.appointmentservice.persistence.model.AppointmentType;
import com.example.appointmentservice.persistence.repository.AppointmentRepository;
import com.example.appointmentservice.producer.AppointmentEvent;
import com.example.appointmentservice.producer.AppointmentEventProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapperDto appointmentMapper;
    private final UserServiceClient userServiceClient;
    private final PropertyServiceClient propertyServiceClient;
    private final AppointmentEventProducer appointmentEventProducer;



    // ========== CIRCUIT BREAKER METHODS ==========

    @CircuitBreaker(name = "propertyService", fallbackMethod = "getPropertyFallback")
    @Retry(name = "propertyService")
    public PropertyServiceResponse getPropertyWithCircuitBreaker(Long propertyId) {
        log.debug("🔍 [APPOINTMENT] Calling Property Service for ID: {}", propertyId);
        return propertyServiceClient.getPropertyById(propertyId);
    }

    private PropertyDto getPropertyFallback(Long propertyId, Exception ex) {
        log.error("⚠️ [APPOINTMENT] PROPERTY SERVICE CB ACTIVATED for ID: {}", propertyId);

        PropertyDto fallback = new PropertyDto();
        fallback.setId(propertyId);
        fallback.setTitle("Property Temporarily Unavailable");
        return fallback;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
    @Retry(name = "userService")
    public UserDto getUserWithCircuitBreaker(Long userId) {
        log.debug("🔍 [APPOINTMENT] Calling User Service for ID: {}", userId);
        return userServiceClient.getUserById(userId);
    }

    private UserDto getUserFallback(Long userId, Exception ex) {
        log.error("⚠️ [APPOINTMENT] USER SERVICE CB ACTIVATED for ID: {}", userId);

        UserDto fallback = new UserDto();
        fallback.setId(userId);
        fallback.setEmail("fallback@system.local");
        return fallback;
    }

    @Override
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        try {
            log.info("Creating appointment: {} for user: {} and property: {}",
                    request.getAppointmentTitle(), request.getRequesterUsername(), request.getPropertyId());

            if (request.getAppointmentDateTime().isBefore(LocalDateTime.now())) {
                return AppointmentResponse.error("Appointment time cannot be in the past", "INVALID_TIME");
            }

            UserDto requesterDto = validateAndGetUser(request.getRequesterUsername());
            UserDto providerDto = validateAndGetUserById(request.getProviderId());
            PropertyDto propertyDto = validateAndGetProperty(request.getPropertyId());

            if (isDuplicateAppointment(request)) {
                return AppointmentResponse.error("Duplicate appointment already exists", "DUPLICATE_APPOINTMENT");
            }

            LocalDateTime endTime = request.getAppointmentDateTime().plusMinutes(request.getDurationMinutes());
            if (hasConflictingAppointment(String.valueOf(request.getProviderId()), request.getAppointmentDateTime(), endTime, null)) {
                return AppointmentResponse.error("Provider has a conflicting appointment at this time", "TIME_CONFLICT");
            }

            AppointmentEntity appointment = createAppointmentEntity(request);
            AppointmentEntity savedAppointment = appointmentRepository.save(appointment);

            AppointmentDto appointmentDto = appointmentMapper.toDto(savedAppointment);
            enrichAppointmentDto(appointmentDto, requesterDto, providerDto, propertyDto);

            // ✅ PUBLISH EVENT TO RABBITMQ
            try {
                AppointmentEvent event = createAppointmentEvent(appointmentDto, "APPOINTMENT_CREATED");
                appointmentEventProducer.publishAppointmentCreated(event);
                log.info("Published APPOINTMENT_CREATED event for appointment ID: {}", savedAppointment.getId());
            } catch (Exception e) {
                log.warn("Failed to publish appointment created event: {}", e.getMessage());
                // Don't fail the entire operation if event publishing fails
            }

            log.info("Successfully created appointment ID: {}", savedAppointment.getId());
            return AppointmentResponse.success("Appointment created successfully", appointmentDto);

        } catch (Exception e) {
            log.error("Error creating appointment: {}", e.getMessage(), e);
            return AppointmentResponse.error("Failed to create appointment: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse getAppointmentById(String appointmentId) {
        try {
            log.info("Retrieving appointment with ID: {}", appointmentId);

            AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new AppointmentNotFoundException(appointmentId, "Appointment not found"));

            AppointmentDto appointmentDto = appointmentMapper.toDto(appointment);
            enrichAppointmentDtoSafely(appointmentDto);

            return AppointmentResponse.success("Appointment retrieved successfully", appointmentDto);

        } catch (AppointmentNotFoundException e) {
            log.warn("Appointment not found with ID: {}", appointmentId);
            return AppointmentResponse.error("Appointment not found", "APPOINTMENT_NOT_FOUND");
        } catch (Exception e) {
            log.error("Error retrieving appointment by ID {}: {}", appointmentId, e.getMessage());
            return AppointmentResponse.error("Failed to retrieve appointment: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse getAppointmentsByUserId(String userId) {
        try {
            log.info("Retrieving appointments for user ID: {}", userId);

            List<AppointmentEntity> appointments = appointmentRepository.findByUserId(Long.valueOf(userId));
            List<AppointmentDto> appointmentDtos = appointments.stream()
                    .map(appointmentMapper::toDto)
                    .peek(this::enrichAppointmentDtoSafely)
                    .collect(Collectors.toList());

            return AppointmentResponse.success(
                    "Retrieved " + appointmentDtos.size() + " appointments for user", appointmentDtos);

        } catch (Exception e) {
            log.error("Error retrieving appointments for user {}: {}", userId, e.getMessage());
            return AppointmentResponse.error("Failed to retrieve appointments: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse getAppointmentsByRequesterId(String requesterId) {
        try {
            log.info("Retrieving appointments for requester ID: {}", requesterId);

            List<AppointmentEntity> appointments = appointmentRepository.findByRequesterId(Long.valueOf(requesterId));
            List<AppointmentDto> appointmentDtos = appointments.stream()
                    .map(appointmentMapper::toDto)
                    .peek(this::enrichAppointmentDtoSafely)
                    .collect(Collectors.toList());

            return AppointmentResponse.success(
                    "Retrieved " + appointmentDtos.size() + " appointments for requester", appointmentDtos);

        } catch (Exception e) {
            log.error("Error retrieving appointments for requester {}: {}", requesterId, e.getMessage());
            return AppointmentResponse.error("Failed to retrieve appointments: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse getAppointmentsByProviderId(String providerId) {
        try {
            log.info("Retrieving appointments for provider ID: {}", providerId);

            List<AppointmentEntity> appointments = appointmentRepository.findByProviderId(Long.valueOf(providerId));
            List<AppointmentDto> appointmentDtos = appointments.stream()
                    .map(appointmentMapper::toDto)
                    .peek(this::enrichAppointmentDtoSafely)
                    .collect(Collectors.toList());

            return AppointmentResponse.success(
                    "Retrieved " + appointmentDtos.size() + " appointments for provider", appointmentDtos);

        } catch (Exception e) {
            log.error("Error retrieving appointments for provider {}: {}", providerId, e.getMessage());
            return AppointmentResponse.error("Failed to retrieve appointments: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse getAppointmentsByPropertyId(String propertyId) {
        try {
            log.info("Retrieving appointments for property ID: {}", propertyId);

            List<AppointmentEntity> appointments = appointmentRepository.findByPropertyId(Long.valueOf(propertyId));
            List<AppointmentDto> appointmentDtos = appointments.stream()
                    .map(appointmentMapper::toDto)
                    .peek(this::enrichAppointmentDtoSafely)
                    .collect(Collectors.toList());

            return AppointmentResponse.success(
                    "Retrieved " + appointmentDtos.size() + " appointments for property", appointmentDtos);

        } catch (Exception e) {
            log.error("Error retrieving appointments for property {}: {}", propertyId, e.getMessage());
            return AppointmentResponse.error("Failed to retrieve appointments: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse getAppointmentsByStatus(AppointmentStatus status) {
        try {
            log.info("Retrieving appointments with status: {}", status);

            List<AppointmentEntity> appointments = appointmentRepository.findByStatus(status);
            List<AppointmentDto> appointmentDtos = appointments.stream()
                    .map(appointmentMapper::toDto)
                    .collect(Collectors.toList());

            return AppointmentResponse.success(
                    "Retrieved " + appointmentDtos.size() + " appointments with status " + status, appointmentDtos);

        } catch (Exception e) {
            log.error("Error retrieving appointments with status {}: {}", status, e.getMessage());
            return AppointmentResponse.error("Failed to retrieve appointments: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse getAppointmentsByType(AppointmentType type) {
        try {
            log.info("Retrieving appointments with type: {}", type);

            List<AppointmentEntity> appointments = appointmentRepository.findByType(type);
            List<AppointmentDto> appointmentDtos = appointments.stream()
                    .map(appointmentMapper::toDto)
                    .collect(Collectors.toList());

            return AppointmentResponse.success(
                    "Retrieved " + appointmentDtos.size() + " appointments of type " + type, appointmentDtos);

        } catch (Exception e) {
            log.error("Error retrieving appointments of type {}: {}", type, e.getMessage());
            return AppointmentResponse.error("Failed to retrieve appointments: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse getAppointmentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            log.info("Retrieving appointments between {} and {}", startDate, endDate);

            List<AppointmentEntity> appointments = appointmentRepository
                    .findByAppointmentDateTimeBetween(startDate, endDate);
            List<AppointmentDto> appointmentDtos = appointments.stream()
                    .map(appointmentMapper::toDto)
                    .collect(Collectors.toList());

            return AppointmentResponse.success(
                    "Retrieved " + appointmentDtos.size() + " appointments in date range", appointmentDtos);

        } catch (Exception e) {
            log.error("Error retrieving appointments in date range: {}", e.getMessage());
            return AppointmentResponse.error("Failed to retrieve appointments: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse getAvailableSlots(String providerId, LocalDate date, Integer durationMinutes) {
        try {
            log.info("Finding available slots for provider {} on {} for {} minutes", providerId, date, durationMinutes);

            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

            List<AppointmentEntity> existingAppointments = appointmentRepository
                    .findProviderBookedSlots(Long.valueOf(providerId), startOfDay, endOfDay);

            List<LocalDateTime> availableSlots = generateAvailableSlots(date, existingAppointments, durationMinutes);

            List<AppointmentDto> slotDtos = availableSlots.stream()
                    .map(slot -> createSlotDto(slot, durationMinutes, providerId))
                    .collect(Collectors.toList());

            return AppointmentResponse.success(
                    "Retrieved " + slotDtos.size() + " available slots", slotDtos);

        } catch (Exception e) {
            log.error("Error retrieving available slots for provider {}: {}", providerId, e.getMessage());
            return AppointmentResponse.error("Failed to retrieve available slots: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse confirmAppointment(String appointmentId) {
        try {
            log.info("Confirming appointment: {}", appointmentId);

            AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new AppointmentNotFoundException(appointmentId, "Appointment not found"));

            if (appointment.getStatus() != AppointmentStatus.PENDING) {
                return AppointmentResponse.error("Only pending appointments can be confirmed", "INVALID_STATUS");
            }

            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointment.setUpdatedAt(LocalDateTime.now());
            AppointmentEntity savedAppointment = appointmentRepository.save(appointment);
            AppointmentDto appointmentDto = appointmentMapper.toDto(savedAppointment);
            enrichAppointmentDtoSafely(appointmentDto);

            // ✅ PUBLISH EVENT TO RABBITMQ
            try {
                AppointmentEvent event = createAppointmentEvent(appointmentDto, "APPOINTMENT_CONFIRMED");
                appointmentEventProducer.publishAppointmentConfirmed(event);
                log.info("Published APPOINTMENT_CONFIRMED event for appointment ID: {}", appointmentId);
            } catch (Exception e) {
                log.warn("Failed to publish appointment confirmed event: {}", e.getMessage());
            }

            log.info("Successfully confirmed appointment: {}", appointmentId);
            return AppointmentResponse.success("Appointment confirmed successfully", appointmentDto);

        } catch (AppointmentNotFoundException e) {
            return AppointmentResponse.error("Appointment not found", "APPOINTMENT_NOT_FOUND");
        } catch (Exception e) {
            log.error("Error confirming appointment {}: {}", appointmentId, e.getMessage());
            return AppointmentResponse.error("Failed to confirm appointment: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse confirmAppointmentByToken(String confirmationToken) {
        try {
            log.info("Confirming appointment by token");

            AppointmentEntity appointment = appointmentRepository.findByConfirmationToken(confirmationToken)
                    .orElseThrow(() -> new AppointmentNotFoundException("Invalid confirmation token", "Token not found"));

            if (appointment.getStatus() != AppointmentStatus.PENDING) {
                return AppointmentResponse.error("Appointment is already confirmed or cancelled", "INVALID_STATUS");
            }

            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointment.setUpdatedAt(LocalDateTime.now());
            AppointmentEntity savedAppointment = appointmentRepository.save(appointment);
            AppointmentDto appointmentDto = appointmentMapper.toDto(savedAppointment);
            enrichAppointmentDtoSafely(appointmentDto);

            // ✅ PUBLISH EVENT TO RABBITMQ
            try {
                AppointmentEvent event = createAppointmentEvent(appointmentDto, "APPOINTMENT_CONFIRMED");
                appointmentEventProducer.publishAppointmentConfirmed(event);
                log.info("Published APPOINTMENT_CONFIRMED event for token confirmation");
            } catch (Exception e) {
                log.warn("Failed to publish appointment confirmed event: {}", e.getMessage());
            }

            return AppointmentResponse.success("Appointment confirmed successfully", appointmentDto);

        } catch (AppointmentNotFoundException e) {
            return AppointmentResponse.error("Invalid confirmation token", "INVALID_TOKEN");
        } catch (Exception e) {
            log.error("Error confirming appointment by token: {}", e.getMessage());
            return AppointmentResponse.error("Failed to confirm appointment: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse cancelAppointment(String appointmentId, String cancellationReason) {
        try {
            log.info("Cancelling appointment: {} with reason: {}", appointmentId, cancellationReason);

            AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new AppointmentNotFoundException(appointmentId, "Appointment not found"));

            if (appointment.getStatus() == AppointmentStatus.CANCELLED ||
                    appointment.getStatus() == AppointmentStatus.COMPLETED) {
                return AppointmentResponse.error("Cannot cancel completed or already cancelled appointment", "INVALID_STATUS");
            }

            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setCancellationReason(cancellationReason);
            appointment.setUpdatedAt(LocalDateTime.now());
            AppointmentEntity savedAppointment = appointmentRepository.save(appointment);
            AppointmentDto appointmentDto = appointmentMapper.toDto(savedAppointment);
            enrichAppointmentDtoSafely(appointmentDto);

            // ✅ PUBLISH EVENT TO RABBITMQ
            try {
                AppointmentEvent event = createAppointmentEvent(appointmentDto, "APPOINTMENT_CANCELLED");
                event.setCancellationReason(cancellationReason);
                appointmentEventProducer.publishAppointmentCancelled(event);
                log.info("Published APPOINTMENT_CANCELLED event for appointment ID: {}", appointmentId);
            } catch (Exception e) {
                log.warn("Failed to publish appointment cancelled event: {}", e.getMessage());
            }

            log.info("Successfully cancelled appointment: {}", appointmentId);
            return AppointmentResponse.success("Appointment cancelled successfully", appointmentDto);

        } catch (AppointmentNotFoundException e) {
            return AppointmentResponse.error("Appointment not found", "APPOINTMENT_NOT_FOUND");
        } catch (Exception e) {
            log.error("Error cancelling appointment {}: {}", appointmentId, e.getMessage());
            return AppointmentResponse.error("Failed to cancel appointment: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse rescheduleAppointment(String appointmentId, LocalDateTime newDateTime) {
        try {
            log.info("Rescheduling appointment: {} to {}", appointmentId, newDateTime);

            AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new AppointmentNotFoundException(appointmentId, "Appointment not found"));

            if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
                    appointment.getStatus() == AppointmentStatus.CANCELLED) {
                return AppointmentResponse.error("Cannot reschedule completed or cancelled appointment", "INVALID_STATUS");
            }

            if (newDateTime.isBefore(LocalDateTime.now())) {
                return AppointmentResponse.error("New appointment time cannot be in the past", "INVALID_TIME");
            }

            LocalDateTime newEndTime = newDateTime.plusMinutes(appointment.getDurationMinutes());
            if (hasConflictingAppointment(String.valueOf(appointment.getProviderId()), newDateTime, newEndTime, appointmentId)) {
                return AppointmentResponse.error("Provider has a conflicting appointment at the new time", "TIME_CONFLICT");
            }

            LocalDateTime oldDateTime = appointment.getAppointmentDateTime();
            appointment.setAppointmentDateTime(newDateTime);
            appointment.setStatus(AppointmentStatus.RESCHEDULED);
            appointment.setUpdatedAt(LocalDateTime.now());
            AppointmentEntity savedAppointment = appointmentRepository.save(appointment);
            AppointmentDto appointmentDto = appointmentMapper.toDto(savedAppointment);
            enrichAppointmentDtoSafely(appointmentDto);

            // ✅ PUBLISH EVENT TO RABBITMQ
            try {
                AppointmentEvent event = createAppointmentEvent(appointmentDto, "APPOINTMENT_RESCHEDULED");
                event.setPreviousDateTime(oldDateTime);
                appointmentEventProducer.publishAppointmentRescheduled(event);
                log.info("Published APPOINTMENT_RESCHEDULED event for appointment ID: {}", appointmentId);
            } catch (Exception e) {
                log.warn("Failed to publish appointment rescheduled event: {}", e.getMessage());
            }

            log.info("Successfully rescheduled appointment: {} to {}", appointmentId, newDateTime);
            return AppointmentResponse.success("Appointment rescheduled successfully", appointmentDto);

        } catch (AppointmentNotFoundException e) {
            return AppointmentResponse.error("Appointment not found", "APPOINTMENT_NOT_FOUND");
        } catch (Exception e) {
            log.error("Error rescheduling appointment {}: {}", appointmentId, e.getMessage());
            return AppointmentResponse.error("Failed to reschedule appointment: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse completeAppointment(String appointmentId) {
        try {
            log.info("Completing appointment: {}", appointmentId);

            AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new AppointmentNotFoundException(appointmentId, "Appointment not found"));

            if (appointment.getStatus() != AppointmentStatus.CONFIRMED &&
                    appointment.getStatus() != AppointmentStatus.RESCHEDULED) {
                return AppointmentResponse.error("Only confirmed/rescheduled appointments can be marked as completed", "INVALID_STATUS");
            }

            appointment.setStatus(AppointmentStatus.COMPLETED);
            appointment.setUpdatedAt(LocalDateTime.now());
            AppointmentEntity savedAppointment = appointmentRepository.save(appointment);
            AppointmentDto appointmentDto = appointmentMapper.toDto(savedAppointment);
            enrichAppointmentDtoSafely(appointmentDto);

            // ✅ PUBLISH EVENT TO RABBITMQ
            try {
                AppointmentEvent event = createAppointmentEvent(appointmentDto, "APPOINTMENT_COMPLETED");
                appointmentEventProducer.publishAppointmentCompleted(event);
                log.info("Published APPOINTMENT_COMPLETED event for appointment ID: {}", appointmentId);
            } catch (Exception e) {
                log.warn("Failed to publish appointment completed event: {}", e.getMessage());
            }

            log.info("Successfully completed appointment: {}", appointmentId);
            return AppointmentResponse.success("Appointment marked as completed", appointmentDto);

        } catch (AppointmentNotFoundException e) {
            return AppointmentResponse.error("Appointment not found", "APPOINTMENT_NOT_FOUND");
        } catch (Exception e) {
            log.error("Error completing appointment {}: {}", appointmentId, e.getMessage());
            return AppointmentResponse.error("Failed to complete appointment: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse markAsNoShow(String appointmentId) {
        try {
            log.info("Marking appointment as no-show: {}", appointmentId);

            AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new AppointmentNotFoundException(appointmentId, "Appointment not found"));

            appointment.setStatus(AppointmentStatus.NO_SHOW);
            appointment.setUpdatedAt(LocalDateTime.now());
            AppointmentEntity savedAppointment = appointmentRepository.save(appointment);
            AppointmentDto appointmentDto = appointmentMapper.toDto(savedAppointment);

            log.info("Successfully marked appointment as no-show: {}", appointmentId);
            return AppointmentResponse.success("Appointment marked as no-show", appointmentDto);

        } catch (AppointmentNotFoundException e) {
            return AppointmentResponse.error("Appointment not found", "APPOINTMENT_NOT_FOUND");
        } catch (Exception e) {
            log.error("Error marking appointment {} as no-show: {}", appointmentId, e.getMessage());
            return AppointmentResponse.error("Failed to mark appointment as no-show: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse updateAppointment(String appointmentId, AppointmentRequest request) {
        try {
            log.info("Updating appointment: {}", appointmentId);

            AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new AppointmentNotFoundException(appointmentId, "Appointment not found"));

            if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
                    appointment.getStatus() == AppointmentStatus.CANCELLED) {
                return AppointmentResponse.error("Cannot update completed or cancelled appointment", "INVALID_STATUS");
            }

            appointment.setAppointmentTitle(request.getAppointmentTitle());
            appointment.setDescription(request.getDescription());
            appointment.setLocation(request.getLocation());
            appointment.setNotes(request.getNotes());
            appointment.setMeetingLink(request.getMeetingLink());
            appointment.setUpdatedAt(LocalDateTime.now());

            AppointmentEntity savedAppointment = appointmentRepository.save(appointment);
            AppointmentDto appointmentDto = appointmentMapper.toDto(savedAppointment);

            log.info("Successfully updated appointment: {}", appointmentId);
            return AppointmentResponse.success("Appointment updated successfully", appointmentDto);

        } catch (AppointmentNotFoundException e) {
            return AppointmentResponse.error("Appointment not found", "APPOINTMENT_NOT_FOUND");
        } catch (Exception e) {
            log.error("Error updating appointment {}: {}", appointmentId, e.getMessage());
            return AppointmentResponse.error("Failed to update appointment: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse deleteAppointment(String appointmentId) {
        try {
            log.info("Deleting appointment: {}", appointmentId);

            AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new AppointmentNotFoundException(appointmentId, "Appointment not found"));

            appointmentRepository.delete(appointment);

            log.info("Successfully deleted appointment: {}", appointmentId);
            return AppointmentResponse.success("Appointment deleted successfully");

        } catch (AppointmentNotFoundException e) {
            return AppointmentResponse.error("Appointment not found", "APPOINTMENT_NOT_FOUND");
        } catch (Exception e) {
            log.error("Error deleting appointment {}: {}", appointmentId, e.getMessage());
            return AppointmentResponse.error("Failed to delete appointment: " + e.getMessage());
        }
    }

    @Override
    public boolean hasConflictingAppointment(String providerId, LocalDateTime startTime,
                                             LocalDateTime endTime, String excludeAppointmentId) {
        try {
            return appointmentRepository.hasConflictingAppointment(Long.valueOf(providerId), startTime, endTime, excludeAppointmentId);
        } catch (Exception e) {
            log.warn("Error checking appointment conflicts for provider {}: {}", providerId, e.getMessage());
            return false;
        }
    }

    @Override
    public List<AppointmentDto> getUpcomingAppointmentsForReminders() {
        try {
            LocalDateTime reminderTime = LocalDateTime.now().plusHours(24);
            List<AppointmentEntity> appointments = appointmentRepository
                    .findPendingReminders(AppointmentStatus.CONFIRMED, reminderTime);

            return appointments.stream()
                    .map(appointmentMapper::toDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error retrieving upcoming appointments for reminders: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void markReminderSent(String appointmentId) {
        try {
            appointmentRepository.findById(appointmentId).ifPresent(appointment -> {
                appointment.setReminderSent(true);
                appointmentRepository.save(appointment);
                log.info("Marked reminder as sent for appointment: {}", appointmentId);
            });
        } catch (Exception e) {
            log.error("Error marking reminder as sent for appointment {}: {}", appointmentId, e.getMessage());
        }
    }

    @Override
    public AppointmentResponse getAppointmentStatistics(Long userId) {
        try {
            log.info("Retrieving statistics for user: {}", userId);

            String userIdStr = String.valueOf(userId);
            List<AppointmentEntity> userAppointments = appointmentRepository.findByUserId(Long.valueOf(userIdStr));

            long totalAppointments = userAppointments.size();
            long completedAppointments = userAppointments.stream()
                    .filter(apt -> apt.getStatus() == AppointmentStatus.COMPLETED)
                    .count();
            long cancelledAppointments = userAppointments.stream()
                    .filter(apt -> apt.getStatus() == AppointmentStatus.CANCELLED)
                    .count();
            long upcomingAppointments = userAppointments.stream()
                    .filter(apt -> apt.getAppointmentDateTime().isAfter(LocalDateTime.now()) &&
                            (apt.getStatus() == AppointmentStatus.CONFIRMED ||
                                    apt.getStatus() == AppointmentStatus.PENDING))
                    .count();

            AppointmentDto statsDto = new AppointmentDto();
            statsDto.setId(userIdStr);
            statsDto.setNotes(String.format("Total: %d, Completed: %d, Cancelled: %d, Upcoming: %d",
                    totalAppointments, completedAppointments, cancelledAppointments, upcomingAppointments));

            return AppointmentResponse.success("Statistics retrieved successfully", statsDto);

        } catch (Exception e) {
            log.error("Error retrieving statistics for user {}: {}", userId, e.getMessage());
            return AppointmentResponse.error("Failed to retrieve statistics: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse getAllAppointmentsWithPropertyDetails() {
        try {
            log.info("Retrieving all appointments with property details");

            List<AppointmentEntity> appointments = appointmentRepository.findAll();
            List<AppointmentDto> enrichedAppointments = appointments.stream()
                    .map(appointmentMapper::toDto)
                    .peek(this::enrichAppointmentDtoWithFullDetails)
                    .collect(Collectors.toList());

            return AppointmentResponse.success(
                    "Retrieved " + enrichedAppointments.size() + " appointments with property details",
                    enrichedAppointments);

        } catch (Exception e) {
            log.error("Error retrieving appointments with property details: {}", e.getMessage());
            return AppointmentResponse.error("Failed to retrieve appointments with details: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse getAppointmentsByPropertyIdWithDetails(Long propertyId) {
        try {
            log.info("Retrieving appointments for property {} with full details", propertyId);

            String propertyIdStr = String.valueOf(propertyId);
            List<AppointmentEntity> appointments = appointmentRepository.findByPropertyId(Long.valueOf(propertyIdStr));

            PropertyDto propertyDto = validateAndGetPropertyById(propertyId);

            List<AppointmentDto> enrichedAppointments = appointments.stream()
                    .map(appointmentMapper::toDto)
                    .peek(dto -> enrichAppointmentDto(dto, null, null, propertyDto))
                    .collect(Collectors.toList());

            return AppointmentResponse.success(
                    "Retrieved " + enrichedAppointments.size() + " appointments for property with details",
                    enrichedAppointments);

        } catch (Exception e) {
            log.error("Error retrieving appointments for property {} with details: {}", propertyId, e.getMessage());
            return AppointmentResponse.error("Failed to retrieve appointments with details: " + e.getMessage());
        }
    }

    @Override
    public AppointmentResponse getAppointmentsByRequesterIdWithDetails(Long requesterId) {
        try {
            log.info("Retrieving appointments for requester {} with full details", requesterId);

            String requesterIdStr = String.valueOf(requesterId);
            List<AppointmentEntity> appointments = appointmentRepository.findByRequesterId(Long.valueOf(requesterIdStr));
            List<AppointmentDto> enrichedAppointments = appointments.stream()
                    .map(appointmentMapper::toDto)
                    .peek(this::enrichAppointmentDtoWithFullDetails)
                    .collect(Collectors.toList());

            return AppointmentResponse.success(
                    "Retrieved " + enrichedAppointments.size() + " appointments for requester with details",
                    enrichedAppointments);

        } catch (Exception e) {
            log.error("Error retrieving appointments for requester {} with details: {}", requesterId, e.getMessage());
            return AppointmentResponse.error("Failed to retrieve appointments with details: " + e.getMessage());
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    private UserDto validateAndGetUser(String username) {
        try {
            UserDto user = userServiceClient.getUserByUsername(username);
            if (user != null) {
                log.info("Successfully retrieved user data for: {}", username);
                log.debug("User details - Email: {}, FirstName: {}, LastName: {}, Phone: {}",
                        user.getEmail(), user.getFirstName(), user.getLastName(), user.getPhoneNumber());
                return user;
            }
        } catch (Exception e) {
            log.warn("Could not retrieve user data for {}: {} - using fallback", username, e.getMessage());
        }

        return createFallbackUser(null, username);
    }

    private UserDto validateAndGetUserById(Long userId) {
        try {
            UserDto user = userServiceClient.getUserById(userId);
            if (user != null) {
                log.info("Successfully retrieved user data for ID: {}", userId);
                log.debug("User details - Email: {}, FirstName: {}, LastName: {}, Phone: {}",
                        user.getEmail(), user.getFirstName(), user.getLastName(), user.getPhoneNumber());
                return user;
            }
        } catch (Exception e) {
            log.warn("Could not retrieve user data for ID {}: {} - using fallback", userId, e.getMessage());
        }

        return createFallbackUser(userId, null);
    }

    private PropertyDto validateAndGetProperty(Long propertyId) {
        try {
            return validateAndGetPropertyById(propertyId);
        } catch (Exception e) {
            log.warn("Invalid property ID: {} - using fallback", propertyId);
            return createFallbackProperty(propertyId);
        }
    }

    private PropertyDto validateAndGetPropertyById(Long propertyId) {
        try {
            log.info("Attempting to fetch property data for ID: {}", propertyId);
            PropertyServiceResponse response = propertyServiceClient.getPropertyById(propertyId);

            if (response == null) {
                log.warn("PropertyServiceClient returned NULL response for property ID: {}", propertyId);
                return createFallbackProperty(propertyId);
            }

            log.info("PropertyServiceResponse received - Success: {}, PropertyId: {}, Title: {}",
                    response.isSuccess(), response.getPropertyId(), response.getTitle());

            if (response.isSuccess()) {
                log.info("Successfully retrieved property data for ID: {}", propertyId);

                PropertyDto propertyDto = new PropertyDto();
                propertyDto.setId(response.getPropertyId());
                propertyDto.setTitle(response.getTitle());
                propertyDto.setDescription(response.getDescription());
                propertyDto.setRentAmount(response.getRentAmount());
                propertyDto.setAddress(response.getAddress());
                propertyDto.setRented(response.isRented());
                propertyDto.setImage(response.getImage());
                propertyDto.setImage2(response.getImage2());
                propertyDto.setImage3(response.getImage3());
                propertyDto.setImage4(response.getImage4());

                log.info("Property DTO created - Title: {}, Image: {}", propertyDto.getTitle(), propertyDto.getImage());
                return propertyDto;
            } else {
                log.warn("PropertyServiceResponse returned success=false for property ID: {}", propertyId);
            }
        } catch (Exception e) {
            log.error("Exception while fetching property data for ID {}: {}", propertyId, e.getMessage(), e);
        }

        log.info("Using fallback property data for ID: {}", propertyId);
        return createFallbackProperty(propertyId);
    }

    private UserDto createFallbackUser(Long userId, String username) {
        UserDto fallbackUser = new UserDto();

        if (userId != null) {
            fallbackUser.setId(userId);
            fallbackUser.setUsername("User" + userId);
            fallbackUser.setFirstName("User");
            fallbackUser.setLastName(String.valueOf(userId));
            fallbackUser.setEmail("user" + userId + "@example.com");
        } else if (username != null) {
            fallbackUser.setUsername(username);
            fallbackUser.setFirstName(username);
            fallbackUser.setLastName("User");
            fallbackUser.setEmail(username + "@example.com");
        }

        return fallbackUser;
    }

    private PropertyDto createFallbackProperty(Long propertyId) {
        PropertyDto fallbackProperty = new PropertyDto();
        fallbackProperty.setId(propertyId);
        fallbackProperty.setTitle("Property #" + propertyId);
        fallbackProperty.setAddress("Address not available");
        fallbackProperty.setDescription("Description not available");
        fallbackProperty.setImage("default-property-image.jpg");
        fallbackProperty.setImage2("default-property-image-2.jpg");
        fallbackProperty.setImage3("default-property-image-3.jpg");
        return fallbackProperty;
    }

    private boolean isDuplicateAppointment(AppointmentRequest request) {
        try {
            return appointmentRepository.existsByRequesterIdAndProviderIdAndPropertyIdAndAppointmentDateTime(
                    request.getRequesterId(),
                    request.getProviderId(),
                    request.getPropertyId(),
                    request.getAppointmentDateTime()
            );
        } catch (Exception e) {
            log.warn("Error checking for duplicate appointments: {}", e.getMessage());
            return false;
        }
    }

    private AppointmentEntity createAppointmentEntity(AppointmentRequest request) {
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setAppointmentTitle(request.getAppointmentTitle());
        appointment.setDescription(request.getDescription());
        appointment.setAppointmentDateTime(request.getAppointmentDateTime());
        appointment.setDurationMinutes(request.getDurationMinutes());
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setType(request.getType());
        appointment.setPropertyId(request.getPropertyId());
        appointment.setRequesterId(request.getRequesterId());
        appointment.setProviderId(request.getProviderId());
        appointment.setLocation(request.getLocation());
        appointment.setNotes(request.getNotes());
        appointment.setIsRecurring(request.getIsRecurring());
        appointment.setMeetingLink(request.getMeetingLink());
        appointment.setConfirmationToken(UUID.randomUUID().toString());
        appointment.setReminderSent(false);
        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setUpdatedAt(LocalDateTime.now());
        return appointment;
    }

    private void enrichAppointmentDto(AppointmentDto dto, UserDto requesterDto, UserDto providerDto, PropertyDto propertyDto) {
        // Set requester information
        if (requesterDto != null) {
            String requesterFirstName = requesterDto.getFirstName();
            String requesterLastName = requesterDto.getLastName();

            if (requesterFirstName == null || requesterFirstName.trim().isEmpty()) {
                requesterFirstName = requesterDto.getUsername();
            }
            if (requesterLastName == null || requesterLastName.trim().isEmpty()) {
                requesterLastName = "";
            }

            dto.setRequesterName(requesterDto.getFullName() != null ?
                    requesterDto.getFullName() : (requesterFirstName + " " + requesterLastName).trim());
            dto.setRequesterUsername(requesterDto.getUsername());
            dto.setRequesterEmail(requesterDto.getEmail());
            dto.setRequesterPhone(requesterDto.getPhoneNumber());
            dto.setRequesterFirstName(requesterFirstName);
            dto.setRequesterLastName(requesterLastName);
            dto.setRequesterProfileImage(requesterDto.getProfileImage() != null ?
                    requesterDto.getProfileImage() : "default-avatar.png");
        }

        // Set provider information
        if (providerDto != null) {
            String providerFirstName = providerDto.getFirstName();
            String providerLastName = providerDto.getLastName();

            if (providerFirstName == null || providerFirstName.trim().isEmpty()) {
                providerFirstName = providerDto.getUsername();
            }
            if (providerLastName == null || providerLastName.trim().isEmpty()) {
                providerLastName = "";
            }

            dto.setProviderName(providerDto.getFullName() != null ?
                    providerDto.getFullName() : (providerFirstName + " " + providerLastName).trim());
            dto.setProviderUsername(providerDto.getUsername());
            dto.setProviderEmail(providerDto.getEmail());
            dto.setProviderPhone(providerDto.getPhoneNumber());
            dto.setProviderFirstName(providerFirstName);
            dto.setProviderLastName(providerLastName);
            dto.setProviderProfileImage(providerDto.getProfileImage() != null ?
                    providerDto.getProfileImage() : "default-avatar.png");
        }

        // Set property information
        if (propertyDto != null) {
            dto.setPropertyTitle(propertyDto.getTitle());

            String address = propertyDto.getAddress();
            if (address == null || address.trim().isEmpty() || address.equals("Address not available")) {
                address = dto.getLocation();
            }
            dto.setPropertyAddress(address);

            dto.setPropertyIsRented(propertyDto.isRented());
            dto.setPropertyImage(propertyDto.getImage());
            dto.setPropertyImage2(propertyDto.getImage2());
            dto.setPropertyImage3(propertyDto.getImage3());
            dto.setPropertyImage4(propertyDto.getImage4());
            dto.setPropertyDescription(propertyDto.getDescription());
            dto.setPropertyRentAmount(propertyDto.getRentAmount());
        }

        // Set calendar information
        if (dto.getAppointmentDateTime() != null && dto.getDurationMinutes() != null) {
            dto.setEndDateTime(dto.getAppointmentDateTime().plusMinutes(dto.getDurationMinutes()));
            long daysUntil = ChronoUnit.DAYS.between(LocalDateTime.now(), dto.getAppointmentDateTime());
            dto.setDaysUntilAppointment((int) daysUntil);
        }

        dto.setCanCancel(canCancelAppointment(dto.getStatus()));
        dto.setCanReschedule(canRescheduleAppointment(dto.getStatus()));
    }

    private void enrichAppointmentDtoSafely(AppointmentDto dto) {
        try {
            UserDto requesterDto = null;
            if (dto.getRequesterId() != null) {
                try {
                    Long requesterIdLong = Long.valueOf(String.valueOf(dto.getRequesterId()));
                    requesterDto = userServiceClient.getUserById(requesterIdLong);
                    log.debug("Successfully retrieved requester data for ID: {}", dto.getRequesterId());
                } catch (Exception e) {
                    log.debug("Could not fetch requester data by ID for enrichment: {}", e.getMessage());
                }
            }

            UserDto providerDto = null;
            if (dto.getProviderId() != null) {
                try {
                    Long providerIdLong = Long.valueOf(String.valueOf(dto.getProviderId()));
                    providerDto = userServiceClient.getUserById(providerIdLong);
                    log.debug("Successfully retrieved provider data for ID: {}", dto.getProviderId());
                } catch (Exception e) {
                    log.debug("Could not fetch provider data by ID for enrichment: {}", e.getMessage());
                }
            }

            PropertyDto propertyDto = null;
            if (dto.getPropertyId() != null) {
                try {
                    propertyDto = validateAndGetProperty(Long.valueOf(String.valueOf(dto.getPropertyId())));
                } catch (Exception e) {
                    log.debug("Could not fetch property data for enrichment: {}", e.getMessage());
                }
            }

            enrichAppointmentDto(dto, requesterDto, providerDto, propertyDto);

        } catch (Exception e) {
            log.warn("Error during safe enrichment of appointment DTO: {}", e.getMessage());
        }
    }

    private void enrichAppointmentDtoWithFullDetails(AppointmentDto dto) {
        try {
            PropertyDto propertyDto = null;
            if (dto.getPropertyId() != null) {
                propertyDto = validateAndGetProperty(Long.valueOf(String.valueOf(dto.getPropertyId())));
            }

            UserDto requesterDto = null;
            if (dto.getRequesterId() != null) {
                requesterDto = validateAndGetUserById(Long.valueOf(String.valueOf(dto.getRequesterId())));
            }

            UserDto providerDto = null;
            if (dto.getProviderId() != null) {
                providerDto = validateAndGetUserById(Long.valueOf(String.valueOf(dto.getProviderId())));
            }

            enrichAppointmentDto(dto, requesterDto, providerDto, propertyDto);

        } catch (Exception e) {
            log.warn("Error enriching appointment {} with full details: {}", dto.getId(), e.getMessage());
        }
    }

    private boolean canCancelAppointment(AppointmentStatus status) {
        return status != AppointmentStatus.CANCELLED &&
                status != AppointmentStatus.COMPLETED &&
                status != AppointmentStatus.NO_SHOW;
    }

    private boolean canRescheduleAppointment(AppointmentStatus status) {
        return status == AppointmentStatus.PENDING ||
                status == AppointmentStatus.CONFIRMED;
    }

    private AppointmentDto createSlotDto(LocalDateTime slot, Integer durationMinutes, String providerId) {
        AppointmentDto dto = new AppointmentDto();
        dto.setAppointmentDateTime(slot);
        dto.setDurationMinutes(durationMinutes);
        dto.setProviderId(Long.valueOf(providerId));
        dto.setEndDateTime(slot.plusMinutes(durationMinutes));
        dto.setStatus(AppointmentStatus.PENDING);
        return dto;
    }

    private List<LocalDateTime> generateAvailableSlots(LocalDate date,
                                                       List<AppointmentEntity> existingAppointments,
                                                       Integer durationMinutes) {
        List<LocalDateTime> availableSlots = new ArrayList<>();

        LocalDateTime startTime = date.atTime(9, 0);
        LocalDateTime endTime = date.atTime(17, 0);
        int slotIntervalMinutes = 30;

        LocalDateTime currentSlot = startTime;

        while (currentSlot.plusMinutes(durationMinutes).isBefore(endTime) ||
                currentSlot.plusMinutes(durationMinutes).isEqual(endTime)) {

            LocalDateTime slotEndTime = currentSlot.plusMinutes(durationMinutes);
            boolean isAvailable = true;

            for (AppointmentEntity appointment : existingAppointments) {
                if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                    continue;
                }

                LocalDateTime appointmentEnd = appointment.getAppointmentDateTime()
                        .plusMinutes(appointment.getDurationMinutes());

                if (!(slotEndTime.isBefore(appointment.getAppointmentDateTime()) ||
                        currentSlot.isAfter(appointmentEnd))) {
                    isAvailable = false;
                    break;
                }
            }

            if (isAvailable) {
                availableSlots.add(currentSlot);
            }

            currentSlot = currentSlot.plusMinutes(slotIntervalMinutes);
        }

        return availableSlots;
    }

    // ✅ NEW HELPER METHOD: Create AppointmentEvent from AppointmentDto
    private AppointmentEvent createAppointmentEvent(AppointmentDto dto, String eventType) {
        return AppointmentEvent.builder()
                .eventType(eventType)
                .eventTimestamp(LocalDateTime.now())
                .eventId(UUID.randomUUID().toString())
                .appointmentId(dto.getId())
                .appointmentTitle(dto.getAppointmentTitle())
                .description(dto.getDescription())
                .appointmentDateTime(dto.getAppointmentDateTime())
                .durationMinutes(dto.getDurationMinutes())
                .status(dto.getStatus() != null ? dto.getStatus().toString() : null)
                .type(dto.getType() != null ? dto.getType().toString() : null)
                .location(dto.getLocation())
                .meetingLink(dto.getMeetingLink())
                .notes(dto.getNotes())
                .requesterId(dto.getRequesterId())
                .requesterUsername(dto.getRequesterUsername())
                .requesterName(dto.getRequesterName())
                .requesterEmail(dto.getRequesterEmail())
                .requesterPhone(dto.getRequesterPhone())
                .requesterFirstName(dto.getRequesterFirstName())
                .requesterLastName(dto.getRequesterLastName())
                .requesterProfileImage(dto.getRequesterProfileImage())
                .providerId(dto.getProviderId())
                .providerUsername(dto.getProviderUsername())
                .providerName(dto.getProviderName())
                .providerEmail(dto.getProviderEmail())
                .providerPhone(dto.getProviderPhone())
                .providerFirstName(dto.getProviderFirstName())
                .providerLastName(dto.getProviderLastName())
                .providerProfileImage(dto.getProviderProfileImage())
                .propertyId(dto.getPropertyId())
                .propertyTitle(dto.getPropertyTitle())
                .propertyAddress(dto.getPropertyAddress())
                .propertyIsRented(dto.getPropertyIsRented())
                .propertyImage(dto.getPropertyImage())
                .propertyImage2(dto.getPropertyImage2())
                .propertyImage3(dto.getPropertyImage3())
                .propertyImage4(dto.getPropertyImage4())
                .propertyRentAmount(dto.getPropertyRentAmount())
                .propertyDescription(dto.getPropertyDescription())
                .build();
    }
}