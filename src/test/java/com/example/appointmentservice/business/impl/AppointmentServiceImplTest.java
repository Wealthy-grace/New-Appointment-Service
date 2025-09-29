package com.example.appointmentservice.business.impl;

import static org.junit.jupiter.api.Assertions.*;


import com.example.appointmentservice.business.client.PropertyServiceClient;
import com.example.appointmentservice.business.client.UserServiceClient;
import com.example.appointmentservice.business.mapper.AppointmentMapperDto;
import com.example.appointmentservice.domain.dto.AppointmentDto;
import com.example.appointmentservice.domain.dto.PropertyDto;
import com.example.appointmentservice.domain.response.PropertyServiceResponse;
import com.example.appointmentservice.domain.dto.UserDto;
import com.example.appointmentservice.domain.request.AppointmentRequest;
import com.example.appointmentservice.domain.response.AppointmentResponse;
import com.example.appointmentservice.persistence.model.AppointmentEntity;
import com.example.appointmentservice.persistence.model.AppointmentStatus;
import com.example.appointmentservice.persistence.model.AppointmentType;
import com.example.appointmentservice.persistence.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentMapperDto appointmentMapper;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private PropertyServiceClient propertyServiceClient;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private AppointmentRequest appointmentRequest;
    private AppointmentEntity appointmentEntity;
    private AppointmentDto appointmentDto;
    private UserDto userDto;
    private PropertyDto propertyDto;
    private PropertyServiceResponse propertyServiceResponse;

    @BeforeEach
    void setUp() {
        // Setup appointment request
        appointmentRequest = AppointmentRequest.builder()
                .appointmentTitle("Property Viewing")
                .description("Viewing appointment for apartment")
                .appointmentDateTime(LocalDateTime.now().plusDays(1))
                .durationMinutes(60)
                .type(AppointmentType.PROPERTY_VIEWING)
                .propertyId(1L)
                .requesterId(1L)
                .providerId(2L)
                .RequesterUsername("testuser")
                .location("123 Test Street")
                .notes("Please bring ID")
                .isRecurring(false)
                .build();

        // Setup appointment entity
        appointmentEntity = AppointmentEntity.builder()
                .id("appointment123")
                .appointmentTitle("Property Viewing")
                .description("Viewing appointment for apartment")
                .appointmentDateTime(LocalDateTime.now().plusDays(1))
                .durationMinutes(60)
                .status(AppointmentStatus.PENDING)
                .type(AppointmentType.PROPERTY_VIEWING)
                .propertyId(1L)
                .requesterId(1L)
                .providerId(2L)
                .location("123 Test Street")
                .notes("Please bring ID")
                .isRecurring(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .confirmationToken("token123")
                .reminderSent(false)
                .build();

        // Setup appointment DTO
        appointmentDto = AppointmentDto.builder()
                .id("appointment123")
                .appointmentTitle("Property Viewing")
                .description("Viewing appointment for apartment")
                .appointmentDateTime(LocalDateTime.now().plusDays(1))
                .durationMinutes(60)
                .status(AppointmentStatus.PENDING)
                .type(AppointmentType.PROPERTY_VIEWING)
                .propertyId(1L)
                .requesterId(1L)
                .providerId(2L)
                .location("123 Test Street")
                .notes("Please bring ID")
                .isRecurring(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .confirmationToken("token123")
                .reminderSent(false)
                .build();

        // Setup user DTO
        userDto = UserDto.builder()
                .id(1L)
                .username("testuser")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        // Setup property DTO
        propertyDto = PropertyDto.builder()
                .id(1L)
                .title("Test Property")
                .description("Test Description")
                .address("123 Test Street")
                .rentAmount(BigDecimal.valueOf(1500.00))
                .image("image1.jpg")
                .image2("image2.jpg")
                .image3("image3.jpg")
                .build();

        // Setup property service response
        propertyServiceResponse = PropertyServiceResponse.builder()
                .success(true)
                .propertyId(1L)
                .title("Test Property")
                .description("Test Description")
                .rentAmount(BigDecimal.valueOf(1500.00))
                .image("image1.jpg")
                .image2("image2.jpg")
                .image3("image3.jpg")
                .build();
    }

    @Test
    void createAppointment_Success() {
        // Arrange
        when(userServiceClient.getUserByUsername("testuser")).thenReturn(userDto);
        when(propertyServiceClient.getPropertyById(1L)).thenReturn(propertyServiceResponse);
        when(appointmentRepository.existsByRequesterIdAndProviderIdAndPropertyIdAndAppointmentDateTime(
                anyLong(), anyLong(), anyLong(), any(LocalDateTime.class))).thenReturn(false);
        when(appointmentRepository.hasConflictingAppointment(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), isNull())).thenReturn(false);
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenReturn(appointmentEntity);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.createAppointment(appointmentRequest);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment created successfully", response.getMessage());
        assertNotNull(response.getAppointment());
        assertEquals("Property Viewing", response.getAppointment().getAppointmentTitle());

        verify(appointmentRepository).save(any(AppointmentEntity.class));
        verify(appointmentMapper).toDto(appointmentEntity);
    }

    @Test
    void createAppointment_PastDateTime_ReturnsError() {
        // Arrange
        appointmentRequest.setAppointmentDateTime(LocalDateTime.now().minusHours(1));

        // Act
        AppointmentResponse response = appointmentService.createAppointment(appointmentRequest);

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Appointment time cannot be in the past", response.getMessage());
        assertEquals("INVALID_TIME", response.getErrorCode());

        verify(appointmentRepository, never()).save(any(AppointmentEntity.class));
    }

    @Test
    void createAppointment_DuplicateAppointment_ReturnsError() {
        // Arrange
        when(userServiceClient.getUserByUsername("testuser")).thenReturn(userDto);
        when(propertyServiceClient.getPropertyById(1L)).thenReturn(propertyServiceResponse);
        when(appointmentRepository.existsByRequesterIdAndProviderIdAndPropertyIdAndAppointmentDateTime(
                anyLong(), anyLong(), anyLong(), any(LocalDateTime.class))).thenReturn(true);

        // Act
        AppointmentResponse response = appointmentService.createAppointment(appointmentRequest);

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Duplicate appointment already exists", response.getMessage());
        assertEquals("DUPLICATE_APPOINTMENT", response.getErrorCode());

        verify(appointmentRepository, never()).save(any(AppointmentEntity.class));
    }

    @Test
    void createAppointment_ConflictingAppointment_ReturnsError() {
        // Arrange
        when(userServiceClient.getUserByUsername("testuser")).thenReturn(userDto);
        when(propertyServiceClient.getPropertyById(1L)).thenReturn(propertyServiceResponse);
        when(appointmentRepository.existsByRequesterIdAndProviderIdAndPropertyIdAndAppointmentDateTime(
                anyLong(), anyLong(), anyLong(), any(LocalDateTime.class))).thenReturn(false);
        when(appointmentRepository.hasConflictingAppointment(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), isNull())).thenReturn(true);

        // Act
        AppointmentResponse response = appointmentService.createAppointment(appointmentRequest);

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Provider has a conflicting appointment at this time", response.getMessage());
        assertEquals("TIME_CONFLICT", response.getErrorCode());

        verify(appointmentRepository, never()).save(any(AppointmentEntity.class));
    }

    @Test
    void getAppointmentById_Success() {
        // Arrange
        when(appointmentRepository.findById("appointment123")).thenReturn(Optional.of(appointmentEntity));
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);
        when(userServiceClient.getUserById(1L)).thenReturn(userDto);
        when(userServiceClient.getUserById(2L)).thenReturn(userDto);
        when(propertyServiceClient.getPropertyById(1L)).thenReturn(propertyServiceResponse);

        // Act
        AppointmentResponse response = appointmentService.getAppointmentById("appointment123");

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment retrieved successfully", response.getMessage());
        assertNotNull(response.getAppointment());
        assertEquals("appointment123", response.getAppointment().getId());

        verify(appointmentRepository).findById("appointment123");
        verify(appointmentMapper).toDto(appointmentEntity);
    }

    @Test
    void getAppointmentById_NotFound_ReturnsError() {
        // Arrange
        when(appointmentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act
        AppointmentResponse response = appointmentService.getAppointmentById("nonexistent");

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Appointment not found", response.getMessage());
        assertEquals("APPOINTMENT_NOT_FOUND", response.getErrorCode());

        verify(appointmentRepository).findById("nonexistent");
    }

    @Test
    void getAppointmentsByUserId_Success() {
        // Arrange
        List<AppointmentEntity> appointments = Arrays.asList(appointmentEntity);
        when(appointmentRepository.findByUserId(1L)).thenReturn(appointments);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.getAppointmentsByUserId("1");

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Retrieved 1 appointments for user", response.getMessage());
        assertNotNull(response.getAppointments());
        assertEquals(1, response.getAppointments().size());

        verify(appointmentRepository).findByUserId(1L);
    }

    @Test
    void getAppointmentsByRequesterId_Success() {
        // Arrange
        List<AppointmentEntity> appointments = Arrays.asList(appointmentEntity);
        when(appointmentRepository.findByRequesterId(1L)).thenReturn(appointments);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.getAppointmentsByRequesterId("1");

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Retrieved 1 appointments for requester", response.getMessage());
        assertNotNull(response.getAppointments());
        assertEquals(1, response.getAppointments().size());

        verify(appointmentRepository).findByRequesterId(1L);
    }

    @Test
    void getAppointmentsByProviderId_Success() {
        // Arrange
        List<AppointmentEntity> appointments = Arrays.asList(appointmentEntity);
        when(appointmentRepository.findByProviderId(2L)).thenReturn(appointments);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.getAppointmentsByProviderId("2");

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Retrieved 1 appointments for provider", response.getMessage());
        assertNotNull(response.getAppointments());
        assertEquals(1, response.getAppointments().size());

        verify(appointmentRepository).findByProviderId(2L);
    }

    @Test
    void getAppointmentsByPropertyId_Success() {
        // Arrange
        List<AppointmentEntity> appointments = Arrays.asList(appointmentEntity);
        when(appointmentRepository.findByPropertyId(1L)).thenReturn(appointments);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.getAppointmentsByPropertyId("1");

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Retrieved 1 appointments for property", response.getMessage());
        assertNotNull(response.getAppointments());
        assertEquals(1, response.getAppointments().size());

        verify(appointmentRepository).findByPropertyId(1L);
    }

    @Test
    void getAppointmentsByStatus_Success() {
        // Arrange
        List<AppointmentEntity> appointments = Arrays.asList(appointmentEntity);
        when(appointmentRepository.findByStatus(AppointmentStatus.PENDING)).thenReturn(appointments);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.getAppointmentsByStatus(AppointmentStatus.PENDING);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Retrieved 1 appointments with status PENDING", response.getMessage());
        assertNotNull(response.getAppointments());
        assertEquals(1, response.getAppointments().size());

        verify(appointmentRepository).findByStatus(AppointmentStatus.PENDING);
    }

    @Test
    void getAppointmentsByType_Success() {
        // Arrange
        List<AppointmentEntity> appointments = Arrays.asList(appointmentEntity);
        when(appointmentRepository.findByType(AppointmentType.PROPERTY_VIEWING)).thenReturn(appointments);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.getAppointmentsByType(AppointmentType.PROPERTY_VIEWING);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Retrieved 1 appointments of type PROPERTY_VIEWING", response.getMessage());
        assertNotNull(response.getAppointments());
        assertEquals(1, response.getAppointments().size());

        verify(appointmentRepository).findByType(AppointmentType.PROPERTY_VIEWING);
    }

    @Test
    void confirmAppointment_Success() {
        // Arrange
        when(appointmentRepository.findById("appointment123")).thenReturn(Optional.of(appointmentEntity));
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenReturn(appointmentEntity);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.confirmAppointment("appointment123");

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment confirmed successfully", response.getMessage());

        verify(appointmentRepository).findById("appointment123");
        verify(appointmentRepository).save(any(AppointmentEntity.class));
    }

    @Test
    void confirmAppointment_NotPending_ReturnsError() {
        // Arrange
        appointmentEntity.setStatus(AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById("appointment123")).thenReturn(Optional.of(appointmentEntity));

        // Act
        AppointmentResponse response = appointmentService.confirmAppointment("appointment123");

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Only pending appointments can be confirmed", response.getMessage());
        assertEquals("INVALID_STATUS", response.getErrorCode());

        verify(appointmentRepository, never()).save(any(AppointmentEntity.class));
    }

    @Test
    void confirmAppointmentByToken_Success() {
        // Arrange
        when(appointmentRepository.findByConfirmationToken("token123")).thenReturn(Optional.of(appointmentEntity));
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenReturn(appointmentEntity);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.confirmAppointmentByToken("token123");

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment confirmed successfully", response.getMessage());

        verify(appointmentRepository).findByConfirmationToken("token123");
        verify(appointmentRepository).save(any(AppointmentEntity.class));
    }

    @Test
    void confirmAppointmentByToken_InvalidToken_ReturnsError() {
        // Arrange
        when(appointmentRepository.findByConfirmationToken("invalid")).thenReturn(Optional.empty());

        // Act
        AppointmentResponse response = appointmentService.confirmAppointmentByToken("invalid");

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Invalid confirmation token", response.getMessage());
        assertEquals("INVALID_TOKEN", response.getErrorCode());

        verify(appointmentRepository).findByConfirmationToken("invalid");
        verify(appointmentRepository, never()).save(any(AppointmentEntity.class));
    }

    @Test
    void cancelAppointment_Success() {
        // Arrange
        when(appointmentRepository.findById("appointment123")).thenReturn(Optional.of(appointmentEntity));
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenReturn(appointmentEntity);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.cancelAppointment("appointment123", "User cancelled");

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment cancelled successfully", response.getMessage());

        verify(appointmentRepository).findById("appointment123");
        verify(appointmentRepository).save(any(AppointmentEntity.class));
    }

    @Test
    void cancelAppointment_AlreadyCancelled_ReturnsError() {
        // Arrange
        appointmentEntity.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById("appointment123")).thenReturn(Optional.of(appointmentEntity));

        // Act
        AppointmentResponse response = appointmentService.cancelAppointment("appointment123", "User cancelled");

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Cannot cancel completed or already cancelled appointment", response.getMessage());
        assertEquals("INVALID_STATUS", response.getErrorCode());

        verify(appointmentRepository, never()).save(any(AppointmentEntity.class));
    }

    @Test
    void rescheduleAppointment_Success() {
        // Arrange
        LocalDateTime newDateTime = LocalDateTime.now().plusDays(2);
        when(appointmentRepository.findById("appointment123")).thenReturn(Optional.of(appointmentEntity));
        when(appointmentRepository.hasConflictingAppointment(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), eq("appointment123"))).thenReturn(false);
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenReturn(appointmentEntity);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.rescheduleAppointment("appointment123", newDateTime);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment rescheduled successfully", response.getMessage());

        verify(appointmentRepository).findById("appointment123");
        verify(appointmentRepository).save(any(AppointmentEntity.class));
    }

    @Test
    void rescheduleAppointment_PastDateTime_ReturnsError() {
        // Arrange
        LocalDateTime pastDateTime = LocalDateTime.now().minusHours(1);
        when(appointmentRepository.findById("appointment123")).thenReturn(Optional.of(appointmentEntity));

        // Act
        AppointmentResponse response = appointmentService.rescheduleAppointment("appointment123", pastDateTime);

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("New appointment time cannot be in the past", response.getMessage());
        assertEquals("INVALID_TIME", response.getErrorCode());

        verify(appointmentRepository, never()).save(any(AppointmentEntity.class));
    }

    @Test
    void completeAppointment_Success() {
        // Arrange
        appointmentEntity.setStatus(AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById("appointment123")).thenReturn(Optional.of(appointmentEntity));
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenReturn(appointmentEntity);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.completeAppointment("appointment123");

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment marked as completed", response.getMessage());

        verify(appointmentRepository).findById("appointment123");
        verify(appointmentRepository).save(any(AppointmentEntity.class));
    }

    @Test
    void completeAppointment_InvalidStatus_ReturnsError() {
        // Arrange
        appointmentEntity.setStatus(AppointmentStatus.PENDING);
        when(appointmentRepository.findById("appointment123")).thenReturn(Optional.of(appointmentEntity));

        // Act
        AppointmentResponse response = appointmentService.completeAppointment("appointment123");

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Only confirmed/rescheduled appointments can be marked as completed", response.getMessage());
        assertEquals("INVALID_STATUS", response.getErrorCode());

        verify(appointmentRepository, never()).save(any(AppointmentEntity.class));
    }

    @Test
    void markAsNoShow_Success() {
        // Arrange
        when(appointmentRepository.findById("appointment123")).thenReturn(Optional.of(appointmentEntity));
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenReturn(appointmentEntity);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.markAsNoShow("appointment123");

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment marked as no-show", response.getMessage());

        verify(appointmentRepository).findById("appointment123");
        verify(appointmentRepository).save(any(AppointmentEntity.class));
    }

    @Test
    void updateAppointment_Success() {
        // Arrange
        when(appointmentRepository.findById("appointment123")).thenReturn(Optional.of(appointmentEntity));
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenReturn(appointmentEntity);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        AppointmentRequest updateRequest = AppointmentRequest.builder()
                .appointmentTitle("Updated Title")
                .description("Updated Description")
                .location("Updated Location")
                .notes("Updated Notes")
                .meetingLink("https://updated-meeting.com")
                .build();

        // Act
        AppointmentResponse response = appointmentService.updateAppointment("appointment123", updateRequest);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment updated successfully", response.getMessage());

        verify(appointmentRepository).findById("appointment123");
        verify(appointmentRepository).save(any(AppointmentEntity.class));
    }

    @Test
    void deleteAppointment_Success() {
        // Arrange
        when(appointmentRepository.findById("appointment123")).thenReturn(Optional.of(appointmentEntity));

        // Act
        AppointmentResponse response = appointmentService.deleteAppointment("appointment123");

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment deleted successfully", response.getMessage());

        verify(appointmentRepository).findById("appointment123");
        verify(appointmentRepository).delete(appointmentEntity);
    }

    @Test
    void hasConflictingAppointment_True() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusHours(1);
        when(appointmentRepository.hasConflictingAppointment(2L, startTime, endTime, null)).thenReturn(true);

        // Act
        boolean hasConflict = appointmentService.hasConflictingAppointment("2", startTime, endTime, null);

        // Assert
        assertTrue(hasConflict);
        verify(appointmentRepository).hasConflictingAppointment(2L, startTime, endTime, null);
    }

    @Test
    void hasConflictingAppointment_False() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusHours(1);
        when(appointmentRepository.hasConflictingAppointment(2L, startTime, endTime, null)).thenReturn(false);

        // Act
        boolean hasConflict = appointmentService.hasConflictingAppointment("2", startTime, endTime, null);

        // Assert
        assertFalse(hasConflict);
        verify(appointmentRepository).hasConflictingAppointment(2L, startTime, endTime, null);
    }

    @Test
    void getAvailableSlots_Success() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(1);
        List<AppointmentEntity> existingAppointments = Collections.emptyList();
        when(appointmentRepository.findProviderBookedSlots(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(existingAppointments);

        // Act
        AppointmentResponse response = appointmentService.getAvailableSlots("2", date, 60);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getAppointments());
        assertTrue(response.getMessage().contains("available slots"));

        verify(appointmentRepository).findProviderBookedSlots(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getUpcomingAppointmentsForReminders_Success() {
        // Arrange
        List<AppointmentEntity> appointments = Arrays.asList(appointmentEntity);
        when(appointmentRepository.findPendingReminders(eq(AppointmentStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(appointments);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        List<AppointmentDto> result = appointmentService.getUpcomingAppointmentsForReminders();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(appointmentRepository).findPendingReminders(eq(AppointmentStatus.CONFIRMED), any(LocalDateTime.class));
    }

    @Test
    void markReminderSent_Success() {
        // Arrange
        when(appointmentRepository.findById("appointment123")).thenReturn(Optional.of(appointmentEntity));
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenReturn(appointmentEntity);

        // Act
        appointmentService.markReminderSent("appointment123");

        // Assert
        verify(appointmentRepository).findById("appointment123");
        verify(appointmentRepository).save(any(AppointmentEntity.class));
    }

    @Test
    void getAppointmentStatistics_Success() {
        // Arrange
        AppointmentEntity completedAppointment = AppointmentEntity.builder()
                .status(AppointmentStatus.COMPLETED)
                .appointmentDateTime(LocalDateTime.now().minusDays(1))
                .build();

        AppointmentEntity cancelledAppointment = AppointmentEntity.builder()
                .status(AppointmentStatus.CANCELLED)
                .appointmentDateTime(LocalDateTime.now().plusDays(1))
                .build();

        AppointmentEntity upcomingAppointment = AppointmentEntity.builder()
                .status(AppointmentStatus.CONFIRMED)
                .appointmentDateTime(LocalDateTime.now().plusDays(2))
                .build();

        List<AppointmentEntity> userAppointments = Arrays.asList(
                completedAppointment, cancelledAppointment, upcomingAppointment);

        when(appointmentRepository.findByUserId(1L)).thenReturn(userAppointments);

        // Act
        AppointmentResponse response = appointmentService.getAppointmentStatistics(1L);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Statistics retrieved successfully", response.getMessage());
        assertNotNull(response.getAppointment());
        assertTrue(response.getAppointment().getNotes().contains("Total: 3"));
        assertTrue(response.getAppointment().getNotes().contains("Completed: 1"));
        assertTrue(response.getAppointment().getNotes().contains("Cancelled: 1"));
        assertTrue(response.getAppointment().getNotes().contains("Upcoming: 1"));

        verify(appointmentRepository).findByUserId(1L);
    }

    @Test
    void getAppointmentsByDateRange_Success() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.now().plusDays(7);
        List<AppointmentEntity> appointments = Arrays.asList(appointmentEntity);

        when(appointmentRepository.findByAppointmentDateTimeBetween(startDate, endDate)).thenReturn(appointments);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.getAppointmentsByDateRange(startDate, endDate);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Retrieved 1 appointments in date range", response.getMessage());
        assertNotNull(response.getAppointments());
        assertEquals(1, response.getAppointments().size());

        verify(appointmentRepository).findByAppointmentDateTimeBetween(startDate, endDate);
    }

    @Test
    void getAllAppointmentsWithPropertyDetails_Success() {
        // Arrange
        List<AppointmentEntity> appointments = Arrays.asList(appointmentEntity);
        when(appointmentRepository.findAll()).thenReturn(appointments);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);
        when(propertyServiceClient.getPropertyById(1L)).thenReturn(propertyServiceResponse);

        // Act
        AppointmentResponse response = appointmentService.getAllAppointmentsWithPropertyDetails();

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Retrieved 1 appointments with property details", response.getMessage());
        assertNotNull(response.getAppointments());
        assertEquals(1, response.getAppointments().size());

        verify(appointmentRepository).findAll();
    }

    @Test
    void getAppointmentsByPropertyIdWithDetails_Success() {
        // Arrange
        List<AppointmentEntity> appointments = Arrays.asList(appointmentEntity);
        when(appointmentRepository.findByPropertyId(1L)).thenReturn(appointments);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);
        when(propertyServiceClient.getPropertyById(1L)).thenReturn(propertyServiceResponse);

        // Act
        AppointmentResponse response = appointmentService.getAppointmentsByPropertyIdWithDetails(1L);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Retrieved 1 appointments for property with details", response.getMessage());
        assertNotNull(response.getAppointments());
        assertEquals(1, response.getAppointments().size());

        verify(appointmentRepository).findByPropertyId(1L);
        verify(propertyServiceClient).getPropertyById(1L);
    }

    @Test
    void getAppointmentsByRequesterIdWithDetails_Success() {
        // Arrange
        List<AppointmentEntity> appointments = Arrays.asList(appointmentEntity);
        when(appointmentRepository.findByRequesterId(1L)).thenReturn(appointments);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.getAppointmentsByRequesterIdWithDetails(1L);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Retrieved 1 appointments for requester with details", response.getMessage());
        assertNotNull(response.getAppointments());
        assertEquals(1, response.getAppointments().size());

        verify(appointmentRepository).findByRequesterId(1L);
    }

    // Test error handling scenarios
    @Test
    void createAppointment_UserServiceException_UsesFallback() {
        // Arrange
        when(userServiceClient.getUserByUsername("testuser")).thenThrow(new RuntimeException("Service unavailable"));
        when(propertyServiceClient.getPropertyById(1L)).thenReturn(propertyServiceResponse);
        when(appointmentRepository.existsByRequesterIdAndProviderIdAndPropertyIdAndAppointmentDateTime(
                anyLong(), anyLong(), anyLong(), any(LocalDateTime.class))).thenReturn(false);
        when(appointmentRepository.hasConflictingAppointment(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), isNull())).thenReturn(false);
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenReturn(appointmentEntity);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.createAppointment(appointmentRequest);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment created successfully", response.getMessage());

        verify(appointmentRepository).save(any(AppointmentEntity.class));
    }

    @Test
    void createAppointment_PropertyServiceException_UsesFallback() {
        // Arrange
        when(userServiceClient.getUserByUsername("testuser")).thenReturn(userDto);
        when(propertyServiceClient.getPropertyById(1L)).thenThrow(new RuntimeException("Service unavailable"));
        when(appointmentRepository.existsByRequesterIdAndProviderIdAndPropertyIdAndAppointmentDateTime(
                anyLong(), anyLong(), anyLong(), any(LocalDateTime.class))).thenReturn(false);
        when(appointmentRepository.hasConflictingAppointment(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), isNull())).thenReturn(false);
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenReturn(appointmentEntity);
        when(appointmentMapper.toDto(appointmentEntity)).thenReturn(appointmentDto);

        // Act
        AppointmentResponse response = appointmentService.createAppointment(appointmentRequest);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment created successfully", response.getMessage());

        verify(appointmentRepository).save(any(AppointmentEntity.class));
    }

    @Test
    void rescheduleAppointment_ConflictingTime_ReturnsError() {
        // Arrange
        LocalDateTime newDateTime = LocalDateTime.now().plusDays(2);
        when(appointmentRepository.findById("appointment123")).thenReturn(Optional.of(appointmentEntity));
        when(appointmentRepository.hasConflictingAppointment(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), eq("appointment123"))).thenReturn(true);

        // Act
        AppointmentResponse response = appointmentService.rescheduleAppointment("appointment123", newDateTime);

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Provider has a conflicting appointment at the new time", response.getMessage());
        assertEquals("TIME_CONFLICT", response.getErrorCode());

        verify(appointmentRepository, never()).save(any(AppointmentEntity.class));
    }

    @Test
    void updateAppointment_CompletedAppointment_ReturnsError() {
        // Arrange
        appointmentEntity.setStatus(AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById("appointment123")).thenReturn(Optional.of(appointmentEntity));

        AppointmentRequest updateRequest = AppointmentRequest.builder()
                .appointmentTitle("Updated Title")
                .build();

        // Act
        AppointmentResponse response = appointmentService.updateAppointment("appointment123", updateRequest);

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Cannot update completed or cancelled appointment", response.getMessage());
        assertEquals("INVALID_STATUS", response.getErrorCode());

        verify(appointmentRepository, never()).save(any(AppointmentEntity.class));
    }
}