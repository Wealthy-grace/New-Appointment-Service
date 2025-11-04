package com.example.appointmentservice.business.impl;

import java.time.temporal.ChronoUnit;
import com.example.appointmentservice.AppointmentServiceApplication;
import com.example.appointmentservice.business.client.PropertyServiceClient;
import com.example.appointmentservice.business.client.UserServiceClient;
import com.example.appointmentservice.domain.response.PropertyServiceResponse;
import com.example.appointmentservice.domain.dto.UserDto;
import com.example.appointmentservice.domain.request.AppointmentRequest;
import com.example.appointmentservice.domain.response.AppointmentResponse;
import com.example.appointmentservice.persistence.model.AppointmentEntity;
import com.example.appointmentservice.persistence.model.AppointmentStatus;
import com.example.appointmentservice.persistence.model.AppointmentType;
import com.example.appointmentservice.persistence.repository.AppointmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = AppointmentServiceApplication.class)
@ActiveProfiles("test")
public class AppointmentServiceIntegrationTest {

    @Autowired
    private AppointmentServiceImpl appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @MockitoBean
    private PropertyServiceClient propertyServiceClient;

    private AppointmentEntity existingAppointment;
    private UserDto testUser;
    private PropertyServiceResponse testProperty;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();

        testUser = UserDto.builder()
                .id(1L)
                .username("testuser")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        testProperty = PropertyServiceResponse.builder()
                .success(true)
                .propertyId(1L)
                .title("Test Property")
                .description("Test Description")
                .rentAmount(BigDecimal.valueOf(1500.00))
                .image("image1.jpg")
                .image2("image2.jpg")
                .image3("image3.jpg")
                .build();

        existingAppointment = AppointmentEntity.builder()
                .appointmentTitle("Existing Appointment")
                .description("Existing Description")
                .appointmentDateTime(LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MILLIS))
                .durationMinutes(60)
                .status(AppointmentStatus.PENDING)
                .type(AppointmentType.PROPERTY_VIEWING)
                .propertyId(1L)
                .requesterId(1L)
                .providerId(2L)
                .location("123 Existing Street")
                .notes("Existing notes")
                .isRecurring(false)
                .confirmationToken("existing-token")
                .reminderSent(false)
                .createdAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .updatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .build();

        existingAppointment = appointmentRepository.save(existingAppointment);

        when(userServiceClient.getUserByUsername(anyString())).thenReturn(testUser);
        when(userServiceClient.getUserById(anyLong())).thenReturn(testUser);
        when(propertyServiceClient.getPropertyById(anyLong())).thenReturn(testProperty);
    }

    @AfterEach
    void tearDown() {
        appointmentRepository.deleteAll();
    }

    @Test
    void createAppointment_Success() {
        AppointmentRequest request = AppointmentRequest.builder()
                .appointmentTitle("New Appointment")
                .description("New Description")
                .appointmentDateTime(LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.MILLIS))
                .durationMinutes(90)
                .type(AppointmentType.PROPERTY_VIEWING)
                .propertyId(2L)
                .requesterId(2L)
                .providerId(3L)
                .RequesterUsername("newuser")
                .location("456 New Street")
                .notes("New notes")
                .isRecurring(false)
                .build();

        AppointmentResponse response = appointmentService.createAppointment(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment created successfully", response.getMessage());
        assertNotNull(response.getAppointment());
        assertEquals("New Appointment", response.getAppointment().getAppointmentTitle());
        assertEquals(AppointmentStatus.PENDING, response.getAppointment().getStatus());

        List<AppointmentEntity> appointments = appointmentRepository.findAll();
        assertEquals(2, appointments.size());

        AppointmentEntity savedAppointment = appointments.stream()
                .filter(apt -> "New Appointment".equals(apt.getAppointmentTitle()))
                .findFirst()
                .orElse(null);

        assertNotNull(savedAppointment);
        assertEquals("New Appointment", savedAppointment.getAppointmentTitle());
        assertEquals(90, savedAppointment.getDurationMinutes());
    }

    @Test
    void createAppointment_DuplicateAppointment_ThrowsException() {
        AppointmentRequest duplicateRequest = AppointmentRequest.builder()
                .appointmentTitle("Duplicate Appointment")
                .description("Duplicate Description")
                .appointmentDateTime(existingAppointment.getAppointmentDateTime())
                .durationMinutes(60)
                .type(AppointmentType.PROPERTY_VIEWING)
                .propertyId(existingAppointment.getPropertyId())
                .requesterId(existingAppointment.getRequesterId())
                .providerId(existingAppointment.getProviderId())
                .RequesterUsername("testuser")
                .location("123 Test Street")
                .notes("Duplicate notes")
                .isRecurring(false)
                .build();

        AppointmentResponse response = appointmentService.createAppointment(duplicateRequest);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Duplicate appointment already exists", response.getMessage());
        assertEquals("DUPLICATE_APPOINTMENT", response.getErrorCode());
        assertEquals(1, appointmentRepository.findAll().size());
    }

    @Test
    void createAppointment_ConflictingTime_ReturnsError() {
        AppointmentRequest conflictingRequest = AppointmentRequest.builder()
                .appointmentTitle("Conflicting Appointment")
                .description("Conflicting Description")
                .appointmentDateTime(existingAppointment.getAppointmentDateTime().plusMinutes(30))
                .durationMinutes(60)
                .type(AppointmentType.PROPERTY_VIEWING)
                .propertyId(2L)
                .requesterId(2L)
                .providerId(existingAppointment.getProviderId())
                .RequesterUsername("conflictuser")
                .location("789 Conflict Street")
                .notes("Conflict notes")
                .isRecurring(false)
                .build();

        AppointmentResponse response = appointmentService.createAppointment(conflictingRequest);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Provider has a conflicting appointment at this time", response.getMessage());
        assertEquals("TIME_CONFLICT", response.getErrorCode());
        assertEquals(1, appointmentRepository.findAll().size());
    }

    @Test
    void getAppointmentById_Success() {
        AppointmentResponse response = appointmentService.getAppointmentById(existingAppointment.getId());

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment retrieved successfully", response.getMessage());
        assertNotNull(response.getAppointment());
        assertEquals(existingAppointment.getId(), response.getAppointment().getId());
        assertEquals("Existing Appointment", response.getAppointment().getAppointmentTitle());
    }

    @Test
    void getAppointmentById_NotFound_ThrowsException() {
        AppointmentResponse response = appointmentService.getAppointmentById("nonexistent-id-123");

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Appointment not found", response.getMessage());
        assertEquals("APPOINTMENT_NOT_FOUND", response.getErrorCode());
    }

    @Test
    void confirmAppointment_Success() {
        AppointmentResponse response = appointmentService.confirmAppointment(existingAppointment.getId());

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment confirmed successfully", response.getMessage());

        AppointmentEntity updatedAppointment = appointmentRepository.findById(existingAppointment.getId()).orElse(null);
        assertNotNull(updatedAppointment);
        assertEquals(AppointmentStatus.CONFIRMED, updatedAppointment.getStatus());
    }

    @Test
    void confirmAppointment_AlreadyConfirmed_ReturnsError() {
        existingAppointment.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(existingAppointment);

        AppointmentResponse response = appointmentService.confirmAppointment(existingAppointment.getId());

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Only pending appointments can be confirmed", response.getMessage());
        assertEquals("INVALID_STATUS", response.getErrorCode());
    }

    @Test
    void confirmAppointmentByToken_Success() {
        AppointmentResponse response = appointmentService.confirmAppointmentByToken(existingAppointment.getConfirmationToken());

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment confirmed successfully", response.getMessage());

        AppointmentEntity updatedAppointment = appointmentRepository.findById(existingAppointment.getId()).orElse(null);
        assertNotNull(updatedAppointment);
        assertEquals(AppointmentStatus.CONFIRMED, updatedAppointment.getStatus());
    }

    @Test
    void confirmAppointmentByToken_InvalidToken_ReturnsError() {
        AppointmentResponse response = appointmentService.confirmAppointmentByToken("invalid-token");

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Invalid confirmation token", response.getMessage());
        assertEquals("INVALID_TOKEN", response.getErrorCode());
    }

    @Test
    void cancelAppointment_Success() {
        AppointmentResponse response = appointmentService.cancelAppointment(existingAppointment.getId(), "User cancelled");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment cancelled successfully", response.getMessage());

        AppointmentEntity updatedAppointment = appointmentRepository.findById(existingAppointment.getId()).orElse(null);
        assertNotNull(updatedAppointment);
        assertEquals(AppointmentStatus.CANCELLED, updatedAppointment.getStatus());
        assertEquals("User cancelled", updatedAppointment.getCancellationReason());
    }

    @Test
    void rescheduleAppointment_Success() {
        LocalDateTime newDateTime = LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.MILLIS);

        AppointmentResponse response = appointmentService.rescheduleAppointment(existingAppointment.getId(), newDateTime);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment rescheduled successfully", response.getMessage());

        AppointmentEntity updatedAppointment = appointmentRepository.findById(existingAppointment.getId()).orElse(null);
        assertNotNull(updatedAppointment);
        assertEquals(newDateTime, updatedAppointment.getAppointmentDateTime());
        assertEquals(AppointmentStatus.RESCHEDULED, updatedAppointment.getStatus());
    }

    @Test
    void rescheduleAppointment_PastDateTime_ReturnsError() {
        LocalDateTime pastDateTime = LocalDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MILLIS);

        AppointmentResponse response = appointmentService.rescheduleAppointment(existingAppointment.getId(), pastDateTime);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("New appointment time cannot be in the past", response.getMessage());
        assertEquals("INVALID_TIME", response.getErrorCode());

        AppointmentEntity unchangedAppointment = appointmentRepository.findById(existingAppointment.getId()).orElse(null);
        assertNotNull(unchangedAppointment);
        assertEquals(existingAppointment.getAppointmentDateTime().truncatedTo(ChronoUnit.MILLIS),
                unchangedAppointment.getAppointmentDateTime());
        assertEquals(AppointmentStatus.PENDING, unchangedAppointment.getStatus());
    }

    @Test
    void completeAppointment_Success() {
        existingAppointment.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(existingAppointment);

        AppointmentResponse response = appointmentService.completeAppointment(existingAppointment.getId());

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment marked as completed", response.getMessage());

        AppointmentEntity updatedAppointment = appointmentRepository.findById(existingAppointment.getId()).orElse(null);
        assertNotNull(updatedAppointment);
        assertEquals(AppointmentStatus.COMPLETED, updatedAppointment.getStatus());
    }

    @Test
    void completeAppointment_NotConfirmed_ReturnsError() {
        AppointmentResponse response = appointmentService.completeAppointment(existingAppointment.getId());

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Only confirmed/rescheduled appointments can be marked as completed", response.getMessage());
        assertEquals("INVALID_STATUS", response.getErrorCode());

        AppointmentEntity unchangedAppointment = appointmentRepository.findById(existingAppointment.getId()).orElse(null);
        assertNotNull(unchangedAppointment);
        assertEquals(AppointmentStatus.PENDING, unchangedAppointment.getStatus());
    }

    @Test
    void markAsNoShow_Success() {
        AppointmentResponse response = appointmentService.markAsNoShow(existingAppointment.getId());

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment marked as no-show", response.getMessage());

        AppointmentEntity updatedAppointment = appointmentRepository.findById(existingAppointment.getId()).orElse(null);
        assertNotNull(updatedAppointment);
        assertEquals(AppointmentStatus.NO_SHOW, updatedAppointment.getStatus());
    }

    @Test
    void updateAppointment_Success() {
        AppointmentRequest updateRequest = AppointmentRequest.builder()
                .appointmentTitle("Updated Title")
                .description("Updated Description")
                .location("Updated Location")
                .notes("Updated Notes")
                .meetingLink("https://updated-meeting.com")
                .build();

        AppointmentResponse response = appointmentService.updateAppointment(existingAppointment.getId(), updateRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment updated successfully", response.getMessage());

        AppointmentEntity updatedAppointment = appointmentRepository.findById(existingAppointment.getId()).orElse(null);
        assertNotNull(updatedAppointment);
        assertEquals("Updated Title", updatedAppointment.getAppointmentTitle());
        assertEquals("Updated Description", updatedAppointment.getDescription());
        assertEquals("Updated Location", updatedAppointment.getLocation());
        assertEquals("Updated Notes", updatedAppointment.getNotes());
        assertEquals("https://updated-meeting.com", updatedAppointment.getMeetingLink());
    }

    @Test
    void updateAppointment_CompletedAppointment_ReturnsError() {
        existingAppointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(existingAppointment);

        AppointmentRequest updateRequest = AppointmentRequest.builder()
                .appointmentTitle("Updated Title")
                .build();

        AppointmentResponse response = appointmentService.updateAppointment(existingAppointment.getId(), updateRequest);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Cannot update completed or cancelled appointment", response.getMessage());
        assertEquals("INVALID_STATUS", response.getErrorCode());

        AppointmentEntity unchangedAppointment = appointmentRepository.findById(existingAppointment.getId()).orElse(null);
        assertNotNull(unchangedAppointment);
        assertEquals("Existing Appointment", unchangedAppointment.getAppointmentTitle());
    }

    @Test
    void deleteAppointment_Success() {
        String appointmentId = existingAppointment.getId();
        assertTrue(appointmentRepository.existsById(appointmentId));

        AppointmentResponse response = appointmentService.deleteAppointment(appointmentId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment deleted successfully", response.getMessage());
        assertFalse(appointmentRepository.existsById(appointmentId));
    }

    @Test
    void deleteAppointment_NotFound_ReturnsError() {
        AppointmentResponse response = appointmentService.deleteAppointment("nonexistent-id-123");

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Appointment not found", response.getMessage());
        assertEquals("APPOINTMENT_NOT_FOUND", response.getErrorCode());
    }

    @Test
    void getAppointmentsByUserId_Success() {
        AppointmentEntity userAppointment2 = createTestAppointment("User Appointment 2",
                "Second appointment for user", LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.MILLIS),
                AppointmentStatus.CONFIRMED, 1L, 1L, 2L);
        appointmentRepository.save(userAppointment2);

        AppointmentResponse response = appointmentService.getAppointmentsByUserId("1");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("appointments for user"));
        assertNotNull(response.getAppointments());
        assertEquals(2, response.getAppointments().size());

        List<String> titles = response.getAppointments().stream()
                .map(dto -> dto.getAppointmentTitle())
                .collect(Collectors.toList());
        assertTrue(titles.contains("Existing Appointment"));
        assertTrue(titles.contains("User Appointment 2"));
    }

    @Test
    void getAppointmentsByRequesterId_Success() {
        AppointmentEntity requesterAppointment2 = createTestAppointment("Requester Appointment 2",
                "Second appointment for requester", LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.MILLIS),
                AppointmentStatus.PENDING, 2L, 1L, 4L);
        appointmentRepository.save(requesterAppointment2);

        AppointmentResponse response = appointmentService.getAppointmentsByRequesterId("1");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("appointments for requester"));
        assertNotNull(response.getAppointments());
        assertEquals(2, response.getAppointments().size());
    }

    @Test
    void getAppointmentsByProviderId_Success() {
        AppointmentEntity providerAppointment2 = createTestAppointment("Provider Appointment 2",
                "Second appointment for provider", LocalDateTime.now().plusDays(4).truncatedTo(ChronoUnit.MILLIS),
                AppointmentStatus.CONFIRMED, 3L, 3L, 2L);
        appointmentRepository.save(providerAppointment2);

        AppointmentResponse response = appointmentService.getAppointmentsByProviderId("2");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("appointments for provider"));
        assertNotNull(response.getAppointments());
        assertEquals(2, response.getAppointments().size());
    }

    @Test
    void getAppointmentsByPropertyId_Success() {
        AppointmentEntity propertyAppointment2 = createTestAppointment("Property Appointment 2",
                "Second appointment for property", LocalDateTime.now().plusDays(5).truncatedTo(ChronoUnit.MILLIS),
                AppointmentStatus.PENDING, 1L, 4L, 3L);
        appointmentRepository.save(propertyAppointment2);

        AppointmentResponse response = appointmentService.getAppointmentsByPropertyId("1");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("appointments for property"));
        assertNotNull(response.getAppointments());
        assertEquals(2, response.getAppointments().size());
    }

    @Test
    void getAppointmentsByStatus_Success() {
        AppointmentEntity confirmedAppointment = createTestAppointment("Confirmed Appointment",
                "A confirmed appointment", LocalDateTime.now().plusDays(6).truncatedTo(ChronoUnit.MILLIS),
                AppointmentStatus.CONFIRMED, 2L, 5L, 3L);
        appointmentRepository.save(confirmedAppointment);

        AppointmentEntity cancelledAppointment = createTestAppointment("Cancelled Appointment",
                "A cancelled appointment", LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.MILLIS),
                AppointmentStatus.CANCELLED, 3L, 6L, 4L);
        appointmentRepository.save(cancelledAppointment);

        AppointmentResponse response = appointmentService.getAppointmentsByStatus(AppointmentStatus.PENDING);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("appointments with status PENDING"));
        assertNotNull(response.getAppointments());
        assertTrue(response.getAppointments().stream()
                .anyMatch(a -> a.getAppointmentTitle().equals("Existing Appointment")));
    }

    @Test
    void getAppointmentsByType_Success() {
        AppointmentEntity maintenanceAppointment = createTestAppointment("Maintenance Appointment",
                "A maintenance appointment", LocalDateTime.now().plusDays(8).truncatedTo(ChronoUnit.MILLIS),
                AppointmentStatus.PENDING, 4L, 7L, 5L);
        maintenanceAppointment.setType(AppointmentType.MAINTENANCE_REQUEST);
        appointmentRepository.save(maintenanceAppointment);

        AppointmentResponse response = appointmentService.getAppointmentsByType(AppointmentType.PROPERTY_VIEWING);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("appointments of type PROPERTY_VIEWING"));
        assertNotNull(response.getAppointments());
        assertTrue(response.getAppointments().stream()
                .anyMatch(a -> a.getAppointmentTitle().equals("Existing Appointment")));
    }

    @Test
    void getAvailableSlots_Success() {
        LocalDate futureDate = LocalDate.now().plusDays(10);

        AppointmentResponse response = appointmentService.getAvailableSlots("2", futureDate, 60);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("available slots"));
        assertNotNull(response.getAppointments());
        assertTrue(response.getAppointments().size() > 0);
    }

    @Test
    void getAppointmentStatistics_Success() {
        AppointmentEntity completedAppointment = createTestAppointment("Completed Appointment",
                "A completed appointment", LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.MILLIS),
                AppointmentStatus.COMPLETED, 5L, 1L, 6L);
        appointmentRepository.save(completedAppointment);

        AppointmentEntity cancelledAppointment = createTestAppointment("Cancelled Appointment",
                "A cancelled appointment", LocalDateTime.now().plusDays(9).truncatedTo(ChronoUnit.MILLIS),
                AppointmentStatus.CANCELLED, 6L, 1L, 7L);
        appointmentRepository.save(cancelledAppointment);

        AppointmentEntity upcomingAppointment = createTestAppointment("Upcoming Appointment",
                "An upcoming appointment", LocalDateTime.now().plusDays(10).truncatedTo(ChronoUnit.MILLIS),
                AppointmentStatus.CONFIRMED, 7L, 1L, 8L);
        appointmentRepository.save(upcomingAppointment);

        AppointmentResponse response = appointmentService.getAppointmentStatistics(1L);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Statistics retrieved successfully", response.getMessage());
        assertNotNull(response.getAppointment());

        String notes = response.getAppointment().getNotes();

        assertTrue(notes.contains("Total: 4"));
        assertTrue(notes.contains("Completed: 1"));
        assertTrue(notes.contains("Cancelled: 1"));
        assertTrue(notes.contains("Upcoming: 2"));
    }

    @Test
    void hasConflictingAppointment_WithConflict_ReturnsTrue() {
        LocalDateTime conflictStart = existingAppointment.getAppointmentDateTime().plusMinutes(30);
        LocalDateTime conflictEnd = conflictStart.plusMinutes(60);

        boolean hasConflict = appointmentService.hasConflictingAppointment(
                String.valueOf(existingAppointment.getProviderId()), conflictStart, conflictEnd, null);

        assertTrue(hasConflict);
    }

    @Test
    void hasConflictingAppointment_NoConflict_ReturnsFalse() {
        LocalDateTime noConflictStart = existingAppointment.getAppointmentDateTime().plusHours(2);
        LocalDateTime noConflictEnd = noConflictStart.plusMinutes(60);

        boolean hasConflict = appointmentService.hasConflictingAppointment(
                String.valueOf(existingAppointment.getProviderId()), noConflictStart, noConflictEnd, null);

        assertFalse(hasConflict);
    }

    @Test
    void getAppointmentsByDateRange_Success() {
        LocalDateTime startDate = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endDate = LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.MILLIS);

        AppointmentEntity dateRangeAppointment = createTestAppointment("Date Range Appointment",
                "Appointment within date range", LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.MILLIS),
                AppointmentStatus.PENDING, 8L, 9L, 10L);
        appointmentRepository.save(dateRangeAppointment);

        AppointmentResponse response = appointmentService.getAppointmentsByDateRange(startDate, endDate);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().toLowerCase().contains("appointments in date range"));
        assertNotNull(response.getAppointments());
        assertTrue(response.getAppointments().size() >= 1);
    }

    @Test
    void createAppointment_UserServiceFailure_UsesFallback() {
        when(userServiceClient.getUserByUsername(anyString()))
                .thenThrow(new RuntimeException("Service unavailable"));

        AppointmentRequest request = AppointmentRequest.builder()
                .appointmentTitle("Fallback Test")
                .description("Test with service failure")
                .appointmentDateTime(LocalDateTime.now().plusDays(5).truncatedTo(ChronoUnit.MILLIS))
                .durationMinutes(60)
                .type(AppointmentType.PROPERTY_VIEWING)
                .propertyId(10L)
                .requesterId(10L)
                .providerId(10L)
                .RequesterUsername("fallbackuser")
                .location("Fallback Location")
                .notes("Fallback notes")
                .isRecurring(false)
                .build();

        AppointmentResponse response = appointmentService.createAppointment(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment created successfully", response.getMessage());
    }

    @Test
    void createAppointment_PropertyServiceFailure_UsesFallback() {
        when(propertyServiceClient.getPropertyById(anyLong()))
                .thenThrow(new RuntimeException("Service unavailable"));

        AppointmentRequest request = AppointmentRequest.builder()
                .appointmentTitle("Property Fallback Test")
                .description("Test with property service failure")
                .appointmentDateTime(LocalDateTime.now().plusDays(6).truncatedTo(ChronoUnit.MILLIS))
                .durationMinutes(60)
                .type(AppointmentType.PROPERTY_VIEWING)
                .propertyId(11L)
                .requesterId(11L)
                .providerId(11L)
                .RequesterUsername("propertyfallbackuser")
                .location("Property Fallback Location")
                .notes("Property fallback notes")
                .isRecurring(false)
                .build();

        AppointmentResponse response = appointmentService.createAppointment(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Appointment created successfully", response.getMessage());
    }

    private AppointmentEntity createTestAppointment(String title, String description,
                                                    LocalDateTime dateTime, AppointmentStatus status,
                                                    Long propertyId, Long requesterId, Long providerId) {
        return AppointmentEntity.builder()
                .appointmentTitle(title)
                .description(description)
                .appointmentDateTime(dateTime)
                .durationMinutes(60)
                .status(status)
                .type(AppointmentType.PROPERTY_VIEWING)
                .propertyId(propertyId)
                .requesterId(requesterId)
                .providerId(providerId)
                .location("Test Location")
                .notes("Test notes")
                .isRecurring(false)
                .confirmationToken("test-token-" + System.currentTimeMillis())
                .reminderSent(false)
                .createdAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .updatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .build();
    }
}