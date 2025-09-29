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

    @Override
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        try {
            log.info("Creating appointment: {} for user: {} and property: {}",
                    request.getAppointmentTitle(), request.getRequesterUsername(), request.getPropertyId());


            // Validate appointment time is not in the past
            if (request.getAppointmentDateTime().isBefore(LocalDateTime.now())) {
                return AppointmentResponse.error("Appointment time cannot be in the past", "INVALID_TIME");
            }

            // Validate and get user data with fallback
            UserDto userDto = validateAndGetUser(request.getRequesterUsername());

            // Validate and get property data with fallback
            PropertyDto propertyDto = validateAndGetProperty(request.getPropertyId());

            // Check for duplicate appointments
            if (isDuplicateAppointment(request)) {
                return AppointmentResponse.error("Duplicate appointment already exists", "DUPLICATE_APPOINTMENT");
            }

            // Check for provider time conflicts
            LocalDateTime endTime = request.getAppointmentDateTime().plusMinutes(request.getDurationMinutes());
            if (hasConflictingAppointment(String.valueOf(request.getProviderId()), request.getAppointmentDateTime(), endTime, null)) {
                return AppointmentResponse.error("Provider has a conflicting appointment at this time", "TIME_CONFLICT");
            }

            // Create and save appointment
            AppointmentEntity appointment = createAppointmentEntity(request);
            AppointmentEntity savedAppointment = appointmentRepository.save(appointment);

            // Convert to DTO and enrich with external data
            AppointmentDto appointmentDto = appointmentMapper.toDto(savedAppointment);
            enrichAppointmentDto(appointmentDto, userDto, propertyDto);

            log.info("Successfully created appointment ID: {} for property: {} by user: {}",
                    savedAppointment.getId(), request.getPropertyId(), request.getRequesterUsername());

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

            // Try to enrich with external data
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

            appointment.setAppointmentDateTime(newDateTime);
            appointment.setStatus(AppointmentStatus.RESCHEDULED);
            appointment.setUpdatedAt(LocalDateTime.now());
            AppointmentEntity savedAppointment = appointmentRepository.save(appointment);
            AppointmentDto appointmentDto = appointmentMapper.toDto(savedAppointment);

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

            // Update modifiable fields
            appointment.setAppointmentTitle(request.getAppointmentTitle());
            //appointment.setPropertyAddress(request.getPropertyAddress());
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

            // Convert Long to String for repository method compatibility
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

            // Convert Long to String for repository method compatibility
            String propertyIdStr = String.valueOf(propertyId);
            List<AppointmentEntity> appointments = appointmentRepository.findByPropertyId(Long.valueOf(propertyIdStr));

            // Get property details once for efficiency
            PropertyDto propertyDto = validateAndGetPropertyById(propertyId);

            List<AppointmentDto> enrichedAppointments = appointments.stream()
                    .map(appointmentMapper::toDto)
                    .peek(dto -> enrichAppointmentDto(dto, null, propertyDto))
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

            // Convert Long to String for repository method compatibility
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
                return user;
            }
        } catch (Exception e) {
            log.warn("Could not retrieve user data for {}: {} - using fallback", username, e.getMessage());
        }

        // Create fallback user data
        UserDto fallbackUser = new UserDto();
        fallbackUser.setUsername(username);
        fallbackUser.setFirstName(fallbackUser.getFullName().split(" ")[0]);
        fallbackUser.setLastName("User");
        return fallbackUser;
    }

    private PropertyDto validateAndGetProperty(Long propertyId) {
        try {
            Long propertyIdLong = Long.parseLong(String.valueOf(propertyId));
            return validateAndGetPropertyById(propertyIdLong);
        } catch (NumberFormatException e) {
            log.warn("Invalid property ID format: {} - using fallback", propertyId);
            return createFallbackProperty(propertyId);
        }
    }

    private PropertyDto validateAndGetPropertyById(Long propertyId) {
        try {
            PropertyServiceResponse response = propertyServiceClient.getPropertyById(propertyId);
            if (response != null && response.isSuccess()) {
                log.info("Successfully retrieved property data for ID: {}", propertyId);

                PropertyDto propertyDto = new PropertyDto();
                propertyDto.setId(response.getPropertyId());
                propertyDto.setTitle(response.getTitle());
                propertyDto.setDescription(response.getDescription());
                propertyDto.setRentAmount(response.getRentAmount());
                propertyDto.setAddress(response.getAddress()); // PropertyService doesn't return address
                propertyDto.setImage(response.getImage());
                propertyDto.setImage2(response.getImage2());
                propertyDto.setImage3(response.getImage3());
                return propertyDto;
            }
        } catch (Exception e) {
            log.warn("Could not retrieve property data for {}: {} - using fallback", propertyId, e.getMessage());
        }

        // Create fallback property data
        PropertyDto fallbackProperty = new PropertyDto();
        fallbackProperty.setId(propertyId);
        fallbackProperty.setTitle("Property #" + propertyId);
        fallbackProperty.setAddress("Address  available" + propertyId);
        fallbackProperty.setImage("image-" + propertyId);
        fallbackProperty.setImage2("image2-" + propertyId);
        fallbackProperty.setImage3("image3-" + propertyId);

        return fallbackProperty;
    }

    private PropertyDto createFallbackProperty(Long propertyId) {
        PropertyDto fallbackProperty = new PropertyDto();
        try {
            fallbackProperty.setId(propertyId);
        } catch (NumberFormatException e) {
            fallbackProperty.setId(0L);
        }
        fallbackProperty.setTitle("Property #" + propertyId);
        fallbackProperty.setAddress("Property #" + propertyId);
        fallbackProperty.setImage("Property #" + propertyId);
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
       // appointment.setPropertyAddress(request.getPropertyAddress());
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

    private void enrichAppointmentDto(AppointmentDto dto, UserDto userDto, PropertyDto propertyDto) {
        // Set user information
        if (userDto != null) {
            dto.setRequesterName(userDto.getFullName());
        }

        // Set property information
        if (propertyDto != null) {
            dto.setPropertyTitle(propertyDto.getTitle());

            // Use appointment location as address if property address is null
            String address = propertyDto.getAddress();
            if (address == null || address.trim().isEmpty()) {
                address = dto.getLocation(); // Use "123 Main Street, Apartment 4B"
            }
            dto.setPropertyAddress(address);

            // Set property images
            dto.setPropertyImage(propertyDto.getImage());
            dto.setPropertyImage2(propertyDto.getImage2());
            dto.setPropertyImage3(propertyDto.getImage3());


        // Set calculated fields
        if (dto.getAppointmentDateTime() != null && dto.getDurationMinutes() != null) {
            dto.setEndDateTime(dto.getAppointmentDateTime().plusMinutes(dto.getDurationMinutes()));
            long daysUntil = ChronoUnit.DAYS.between(LocalDateTime.now(), dto.getAppointmentDateTime());
            dto.setDaysUntilAppointment((int) daysUntil);
        }

        // Set business logic flags
        dto.setCanCancel(canCancelAppointment(dto.getStatus()));
        dto.setCanReschedule(canRescheduleAppointment(dto.getStatus()));
    }

        // Set calculated fields
        if (dto.getAppointmentDateTime() != null && dto.getDurationMinutes() != null) {
            dto.setEndDateTime(dto.getAppointmentDateTime().plusMinutes(dto.getDurationMinutes()));
            long daysUntil = ChronoUnit.DAYS.between(LocalDateTime.now(), dto.getAppointmentDateTime());
            dto.setDaysUntilAppointment((int) daysUntil);
        }

        // Set business logic flags
        dto.setCanCancel(canCancelAppointment(dto.getStatus()));
        dto.setCanReschedule(canRescheduleAppointment(dto.getStatus()));
    }

    private void enrichAppointmentDtoSafely(AppointmentDto dto) {
        try {
            // Try to get user data if we have a requesterId
            UserDto userDto = null;
            if (dto.getRequesterId() != null) {
                try {
                    // Convert requesterId to Long and fetch user by ID
                    Long requesterIdLong = Long.parseLong(String.valueOf(dto.getRequesterId()));
                    userDto = userServiceClient.getUserById(requesterIdLong);
                    log.debug("Successfully retrieved user data for ID: {}", dto.getRequesterId());
                } catch (Exception e) {
                    log.debug("Could not fetch user data by ID for enrichment: {}", e.getMessage());
                }
            }

            // Try to get provider data if we have a providerId
            UserDto providerDto = null;
            if (dto.getProviderId() != null) {
                try {
                    // Convert providerId to Long and fetch provider by ID
                    Long providerIdLong = Long.parseLong(String.valueOf(dto.getProviderId()));
                    providerDto = userServiceClient.getUserById(providerIdLong);
                    if (providerDto != null) {
                        dto.setProviderName(providerDto.getFullName());
                    }
                    log.debug("Successfully retrieved provider data for ID: {}", dto.getProviderId());
                } catch (Exception e) {
                    log.debug("Could not fetch provider data by ID for enrichment: {}", e.getMessage());
                }
            }

            // Try to get property data
            PropertyDto propertyDto = null;
            if (dto.getPropertyId() != null) {
                try {
                    propertyDto = validateAndGetProperty(Long.valueOf(String.valueOf(dto.getPropertyId())));
                } catch (Exception e) {
                    log.debug("Could not fetch property data for enrichment: {}", e.getMessage());
                }
            }

            // Enrich the DTO with available data
            enrichAppointmentDto(dto, userDto, propertyDto);

        } catch (Exception e) {
            log.warn("Error during safe enrichment of appointment DTO: {}", e.getMessage());
        }
    }

    private void enrichAppointmentDtoWithFullDetails(AppointmentDto dto) {
        try {
            // Get property details
            PropertyDto propertyDto = null;
            if (dto.getPropertyId() != null) {
                propertyDto = validateAndGetProperty(Long.valueOf(String.valueOf(dto.getPropertyId())));
            }

            // For user details, we'd need the username or a getUserById method
            // For now, we'll skip user enrichment in bulk operations
            UserDto userDto = null;

            enrichAppointmentDto(dto, userDto, propertyDto);

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
        dto.setStatus(AppointmentStatus.PENDING); // Available slot
        return dto;
    }

    private List<LocalDateTime> generateAvailableSlots(LocalDate date,
                                                       List<AppointmentEntity> existingAppointments,
                                                       Integer durationMinutes) {
        List<LocalDateTime> availableSlots = new ArrayList<>();

        // Business hours configuration
        LocalDateTime startTime = date.atTime(9, 0);  // 9:00 AM
        LocalDateTime endTime = date.atTime(17, 0);   // 5:00 PM
        int slotIntervalMinutes = 30; // 30-minute intervals

        LocalDateTime currentSlot = startTime;

        while (currentSlot.plusMinutes(durationMinutes).isBefore(endTime) ||
                currentSlot.plusMinutes(durationMinutes).isEqual(endTime)) {

            LocalDateTime slotEndTime = currentSlot.plusMinutes(durationMinutes);
            boolean isAvailable = true;

            // Check if this slot conflicts with any existing appointment
            for (AppointmentEntity appointment : existingAppointments) {
                // Skip cancelled appointments
                if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                    continue;
                }

                LocalDateTime appointmentEnd = appointment.getAppointmentDateTime()
                        .plusMinutes(appointment.getDurationMinutes());

                // Check for overlap: slots overlap if they don't end before the appointment starts
                // and don't start after the appointment ends
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
}