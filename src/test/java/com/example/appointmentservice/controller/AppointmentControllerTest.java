package com.example.appointmentservice.controller;

import com.example.appointmentservice.business.client.PropertyServiceClient;
import com.example.appointmentservice.business.client.UserServiceClient;
import com.example.appointmentservice.business.interfaces.AppointmentService;
import com.example.appointmentservice.domain.dto.AppointmentDto;
import com.example.appointmentservice.domain.request.AppointmentRequest;
import com.example.appointmentservice.domain.response.AppointmentResponse;
import com.example.appointmentservice.persistence.model.AppointmentStatus;
import com.example.appointmentservice.persistence.model.AppointmentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//@WebMvcTest(controllers = AppointmentController.class,
//        excludeAutoConfiguration = {
//                MongoAutoConfiguration.class,
//                MongoDataAutoConfiguration.class,
//                MongoRepositoriesAutoConfiguration.class
//        })
//@ActiveProfiles("test")
@WebMvcTest(controllers = AppointmentController.class)
@AutoConfigureMockMvc(addFilters = false)  // Add this to bypass security
@ActiveProfiles("test")
public class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @MockitoBean
    private PropertyServiceClient propertyServiceClient;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @Autowired
    private ObjectMapper objectMapper;
    private AppointmentRequest appointmentRequest;
    private AppointmentResponse successResponse;
    private AppointmentResponse errorResponse;
    private AppointmentDto appointmentDto;

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

        // Setup success response
        successResponse = AppointmentResponse.builder()
                .success(true)
                .message("Appointment created successfully")
                .appointment(appointmentDto)
                .build();

        // Setup error response
        errorResponse = AppointmentResponse.builder()
                .success(false)
                .message("Appointment not found")
                .errorCode("APPOINTMENT_NOT_FOUND")
                .build();
    }

    @Test
    void createAppointment_Success() throws Exception {
        // Arrange
        when(appointmentService.createAppointment(any(AppointmentRequest.class))).thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appointmentRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Appointment created successfully"))
                .andExpect(jsonPath("$.appointment.id").value("appointment123"))
                .andExpect(jsonPath("$.appointment.appointmentTitle").value("Property Viewing"));

        verify(appointmentService).createAppointment(any(AppointmentRequest.class));
    }

    @Test
    void createAppointment_ServiceError_BadRequest() throws Exception {
        // Arrange
        AppointmentResponse errorResponse = AppointmentResponse.builder()
                .success(false)
                .message("Appointment time cannot be in the past")
                .errorCode("INVALID_TIME")
                .build();

        when(appointmentService.createAppointment(any(AppointmentRequest.class))).thenReturn(errorResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appointmentRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Appointment time cannot be in the past"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_TIME"));

        verify(appointmentService).createAppointment(any(AppointmentRequest.class));
    }

    @Test
    void getAppointmentById_Success() throws Exception {
        // Arrange
        when(appointmentService.getAppointmentById("appointment123")).thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/appointment123"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Appointment created successfully"))
                .andExpect(jsonPath("$.appointment.id").value("appointment123"));

        verify(appointmentService).getAppointmentById("appointment123");
    }

    @Test
    void getAppointmentById_NotFound() throws Exception {
        // Arrange
        when(appointmentService.getAppointmentById("nonexistent")).thenReturn(errorResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/nonexistent"))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(appointmentService).getAppointmentById("nonexistent");
    }

    @Test
    void getAppointmentsByUserId_Success() throws Exception {
        // Arrange
        List<AppointmentDto> appointments = Arrays.asList(appointmentDto);
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Retrieved 1 appointments for user")
                .appointments(appointments)
                .build();

        when(appointmentService.getAppointmentsByUserId("1")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/user/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Retrieved 1 appointments for user"))
                .andExpect(jsonPath("$.appointments").isArray())
                .andExpect(jsonPath("$.appointments.length()").value(1));

        verify(appointmentService).getAppointmentsByUserId("1");
    }

    @Test
    void getAppointmentsByRequesterId_Success() throws Exception {
        // Arrange
        List<AppointmentDto> appointments = Arrays.asList(appointmentDto);
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Retrieved 1 appointments for requester")
                .appointments(appointments)
                .build();

        when(appointmentService.getAppointmentsByRequesterId("1")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/requester/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Retrieved 1 appointments for requester"))
                .andExpect(jsonPath("$.appointments").isArray())
                .andExpect(jsonPath("$.appointments.length()").value(1));

        verify(appointmentService).getAppointmentsByRequesterId("1");
    }

    @Test
    void getAppointmentsByProviderId_Success() throws Exception {
        // Arrange
        List<AppointmentDto> appointments = Arrays.asList(appointmentDto);
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Retrieved 1 appointments for provider")
                .appointments(appointments)
                .build();

        when(appointmentService.getAppointmentsByProviderId("2")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/provider/2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Retrieved 1 appointments for provider"))
                .andExpect(jsonPath("$.appointments").isArray())
                .andExpect(jsonPath("$.appointments.length()").value(1));

        verify(appointmentService).getAppointmentsByProviderId("2");
    }

    @Test
    void getAppointmentsByPropertyId_Success() throws Exception {
        // Arrange
        List<AppointmentDto> appointments = Arrays.asList(appointmentDto);
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Retrieved 1 appointments for property")
                .appointments(appointments)
                .build();

        when(appointmentService.getAppointmentsByPropertyId("1")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/property/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Retrieved 1 appointments for property"))
                .andExpect(jsonPath("$.appointments").isArray())
                .andExpect(jsonPath("$.appointments.length()").value(1));

        verify(appointmentService).getAppointmentsByPropertyId("1");
    }

    @Test
    void getAppointmentsByStatus_Success() throws Exception {
        // Arrange
        List<AppointmentDto> appointments = Arrays.asList(appointmentDto);
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Retrieved 1 appointments with status PENDING")
                .appointments(appointments)
                .build();

        when(appointmentService.getAppointmentsByStatus(AppointmentStatus.PENDING)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/status/PENDING"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Retrieved 1 appointments with status PENDING"))
                .andExpect(jsonPath("$.appointments").isArray())
                .andExpect(jsonPath("$.appointments.length()").value(1));

        verify(appointmentService).getAppointmentsByStatus(AppointmentStatus.PENDING);
    }

    @Test
    void getAppointmentsByType_Success() throws Exception {
        // Arrange
        List<AppointmentDto> appointments = Arrays.asList(appointmentDto);
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Retrieved 1 appointments of type PROPERTY_VIEWING")
                .appointments(appointments)
                .build();

        when(appointmentService.getAppointmentsByType(AppointmentType.PROPERTY_VIEWING)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/type/PROPERTY_VIEWING"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Retrieved 1 appointments of type PROPERTY_VIEWING"))
                .andExpect(jsonPath("$.appointments").isArray())
                .andExpect(jsonPath("$.appointments.length()").value(1));

        verify(appointmentService).getAppointmentsByType(AppointmentType.PROPERTY_VIEWING);
    }

    @Test
    void getAvailableSlots_Success() throws Exception {
        // Arrange
        List<AppointmentDto> slots = Arrays.asList(appointmentDto);
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Retrieved 16 available slots")
                .appointments(slots)
                .build();

        when(appointmentService.getAvailableSlots(eq("2"), any(LocalDate.class), eq(60)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/available-slots")
                        .param("providerId", "2")
                        .param("date", LocalDate.now().plusDays(1).toString())
                        .param("durationMinutes", "60"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Retrieved 16 available slots"))
                .andExpect(jsonPath("$.appointments").isArray());

        verify(appointmentService).getAvailableSlots(eq("2"), any(LocalDate.class), eq(60));
    }

    @Test
    void confirmAppointment_Success() throws Exception {
        // Arrange
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Appointment confirmed successfully")
                .appointment(appointmentDto)
                .build();

        when(appointmentService.confirmAppointment("appointment123")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/appointments/appointment123/confirm"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Appointment confirmed successfully"));

        verify(appointmentService).confirmAppointment("appointment123");
    }

    @Test
    void confirmAppointment_InvalidStatus_BadRequest() throws Exception {
        // Arrange
        AppointmentResponse response = AppointmentResponse.builder()
                .success(false)
                .message("Only pending appointments can be confirmed")
                .errorCode("INVALID_STATUS")
                .build();

        when(appointmentService.confirmAppointment("appointment123")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/appointments/appointment123/confirm"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Only pending appointments can be confirmed"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS"));

        verify(appointmentService).confirmAppointment("appointment123");
    }

    @Test
    void confirmAppointmentByToken_Success() throws Exception {
        // Arrange
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Appointment confirmed successfully")
                .appointment(appointmentDto)
                .build();

        when(appointmentService.confirmAppointmentByToken("token123")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/appointments/confirm-by-token/token123"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Appointment confirmed successfully"));

        verify(appointmentService).confirmAppointmentByToken("token123");
    }

    @Test
    void cancelAppointment_Success() throws Exception {
        // Arrange
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Appointment cancelled successfully")
                .appointment(appointmentDto)
                .build();

        when(appointmentService.cancelAppointment("appointment123", "User cancelled"))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/appointments/appointment123/cancel")
                        .param("cancellationReason", "User cancelled"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Appointment cancelled successfully"));

        verify(appointmentService).cancelAppointment("appointment123", "User cancelled");
    }

    @Test
    void cancelAppointment_WithoutReason_Success() throws Exception {
        // Arrange
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Appointment cancelled successfully")
                .appointment(appointmentDto)
                .build();

        when(appointmentService.cancelAppointment("appointment123", null)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/appointments/appointment123/cancel"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Appointment cancelled successfully"));

        verify(appointmentService).cancelAppointment("appointment123", null);
    }

    @Test
    void rescheduleAppointment_Success() throws Exception {
        // Arrange
        LocalDateTime newDateTime = LocalDateTime.now().plusDays(2);
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Appointment rescheduled successfully")
                .appointment(appointmentDto)
                .build();

        when(appointmentService.rescheduleAppointment("appointment123", newDateTime))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/appointments/appointment123/reschedule")
                        .param("newDateTime", newDateTime.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Appointment rescheduled successfully"));

        verify(appointmentService).rescheduleAppointment("appointment123", newDateTime);
    }

    @Test
    void rescheduleAppointment_PastDateTime_BadRequest() throws Exception {
        // Arrange
        LocalDateTime pastDateTime = LocalDateTime.now().minusHours(1);
        AppointmentResponse response = AppointmentResponse.builder()
                .success(false)
                .message("New appointment time cannot be in the past")
                .errorCode("INVALID_TIME")
                .build();

        when(appointmentService.rescheduleAppointment("appointment123", pastDateTime))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/appointments/appointment123/reschedule")
                        .param("newDateTime", pastDateTime.toString()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("New appointment time cannot be in the past"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_TIME"));

        verify(appointmentService).rescheduleAppointment("appointment123", pastDateTime);
    }

    @Test
    void completeAppointment_Success() throws Exception {
        // Arrange
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Appointment marked as completed")
                .appointment(appointmentDto)
                .build();

        when(appointmentService.completeAppointment("appointment123")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/appointments/appointment123/complete"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Appointment marked as completed"));

        verify(appointmentService).completeAppointment("appointment123");
    }

    @Test
    void completeAppointment_InvalidStatus_BadRequest() throws Exception {
        // Arrange
        AppointmentResponse response = AppointmentResponse.builder()
                .success(false)
                .message("Only confirmed/rescheduled appointments can be marked as completed")
                .errorCode("INVALID_STATUS")
                .build();

        when(appointmentService.completeAppointment("appointment123")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/appointments/appointment123/complete"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Only confirmed/rescheduled appointments can be marked as completed"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS"));

        verify(appointmentService).completeAppointment("appointment123");
    }

    @Test
    void markAsNoShow_Success() throws Exception {
        // Arrange
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Appointment marked as no-show")
                .appointment(appointmentDto)
                .build();

        when(appointmentService.markAsNoShow("appointment123")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/appointments/appointment123/no-show"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Appointment marked as no-show"));

        verify(appointmentService).markAsNoShow("appointment123");
    }

    @Test
    void updateAppointment_Success() throws Exception {
        // Arrange - Include all required fields for validation
        AppointmentRequest updateRequest = AppointmentRequest.builder()
                .appointmentTitle("Updated Appointment")
                .description("Updated description")
                .location("Updated location")
                .notes("Updated notes")
                // Add required fields
                .appointmentDateTime(LocalDateTime.now().plusDays(1))
                .durationMinutes(60)
                .type(AppointmentType.PROPERTY_VIEWING)
                .propertyId(1L)
                .requesterId(1L)
                .providerId(2L)
                .RequesterUsername("testuser")
                .isRecurring(false)
                .build();

        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Appointment updated successfully")
                .appointment(appointmentDto)
                .build();

        when(appointmentService.updateAppointment(eq("appointment123"), any(AppointmentRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/appointments/appointment123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Appointment updated successfully"));

        verify(appointmentService).updateAppointment(eq("appointment123"), any(AppointmentRequest.class));
    }
    @Test
    void deleteAppointment_Success() throws Exception {
        // Arrange
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Appointment deleted successfully")
                .build();

        when(appointmentService.deleteAppointment("appointment123")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/appointments/appointment123"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Appointment deleted successfully"));

        verify(appointmentService).deleteAppointment("appointment123");
    }

    @Test
    void deleteAppointment_NotFound_BadRequest() throws Exception {
        // Arrange
        AppointmentResponse response = AppointmentResponse.builder()
                .success(false)
                .message("Appointment not found")
                .errorCode("APPOINTMENT_NOT_FOUND")
                .build();

        when(appointmentService.deleteAppointment("nonexistent")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/appointments/nonexistent"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Appointment not found"))
                .andExpect(jsonPath("$.errorCode").value("APPOINTMENT_NOT_FOUND"));

        verify(appointmentService).deleteAppointment("nonexistent");
    }

    @Test
    void getAppointmentStatistics_Success() throws Exception {
        // Arrange
        AppointmentDto statisticsDto = AppointmentDto.builder()
                .id("1")
                .notes("Total: 10, Completed: 5, Cancelled: 2, Upcoming: 3")
                .build();

        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Statistics retrieved successfully")
                .appointment(statisticsDto)
                .build();

        when(appointmentService.getAppointmentStatistics(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/1/statistics"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Statistics retrieved successfully"))
                .andExpect(jsonPath("$.appointment.notes").value("Total: 10, Completed: 5, Cancelled: 2, Upcoming: 3"));

        verify(appointmentService).getAppointmentStatistics(1L);
    }

    @Test
    void checkAppointmentConflict_HasConflict() throws Exception {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusHours(1);

        when(appointmentService.hasConflictingAppointment("2", startTime, endTime, null))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/conflict-check")
                        .param("providerId", "2")
                        .param("startTime", startTime.toString())
                        .param("endTime", endTime.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(appointmentService).hasConflictingAppointment("2", startTime, endTime, null);
    }

    @Test
    void checkAppointmentConflict_NoConflict() throws Exception {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusHours(1);

        when(appointmentService.hasConflictingAppointment("2", startTime, endTime, "exclude123"))
                .thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/conflict-check")
                        .param("providerId", "2")
                        .param("startTime", startTime.toString())
                        .param("endTime", endTime.toString())
                        .param("excludeAppointmentId", "exclude123"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(appointmentService).hasConflictingAppointment("2", startTime, endTime, "exclude123");
    }

    @Test
    void getAppointmentsByDateRange_Success() throws Exception {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.now().plusDays(7);

        List<AppointmentDto> appointments = Arrays.asList(appointmentDto);
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Retrieved 1 appointments in date range")
                .appointments(appointments)
                .build();

        when(appointmentService.getAppointmentsByDateRange(startDate, endDate)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/date-range")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Retrieved 1 appointments in date range"))
                .andExpect(jsonPath("$.appointments").isArray())
                .andExpect(jsonPath("$.appointments.length()").value(1));

        verify(appointmentService).getAppointmentsByDateRange(startDate, endDate);
    }

    @Test
    void getAppointmentsByDateRange_InvalidDateRange_BadRequest() throws Exception {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().plusDays(7);
        LocalDateTime endDate = LocalDateTime.now(); // End date before start date

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/date-range")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Start date must be before end date"));

        verify(appointmentService, never()).getAppointmentsByDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    // Edge case tests
    @Test
    void createAppointment_MissingRequiredFields_BadRequest() throws Exception {
        // Arrange
        AppointmentRequest invalidRequest = AppointmentRequest.builder()
                .description("Test Description")
                // Missing required fields like appointmentTitle, appointmentDateTime, etc.
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(appointmentService, never()).createAppointment(any(AppointmentRequest.class));
    }

    @Test
    void getAppointmentsByUserId_EmptyList_Success() throws Exception {
        // Arrange
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Retrieved 0 appointments for user")
                .appointments(Collections.emptyList())
                .build();

        when(appointmentService.getAppointmentsByUserId("999")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/user/999"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Retrieved 0 appointments for user"))
                .andExpect(jsonPath("$.appointments").isArray())
                .andExpect(jsonPath("$.appointments.length()").value(0));

        verify(appointmentService).getAppointmentsByUserId("999");
    }

    @Test
    void getAvailableSlots_DefaultDuration_Success() throws Exception {
        // Arrange
        List<AppointmentDto> slots = Arrays.asList(appointmentDto);
        AppointmentResponse response = AppointmentResponse.builder()
                .success(true)
                .message("Retrieved 16 available slots")
                .appointments(slots)
                .build();

        when(appointmentService.getAvailableSlots(eq("2"), any(LocalDate.class), eq(60)))
                .thenReturn(response);

        // Act & Assert - Test default duration parameter
        mockMvc.perform(get("/api/v1/appointments/available-slots")
                        .param("providerId", "2")
                        .param("date", LocalDate.now().plusDays(1).toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(appointmentService).getAvailableSlots(eq("2"), any(LocalDate.class), eq(60));
    }
}