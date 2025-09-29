package com.example.appointmentservice.domain.response;



import com.example.appointmentservice.domain.dto.AppointmentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {

    private boolean success;
    private String message;
    private AppointmentDto appointment;
    private List<AppointmentDto> appointments;
    private String errorCode;
    private LocalDateTime timestamp;
    private Integer totalCount;
    private Integer pageNumber;
    private Integer pageSize;

    // Success response with single appointment
    public static AppointmentResponse success(String message, AppointmentDto appointment) {
        AppointmentResponse response = new AppointmentResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setAppointment(appointment);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    // Success response with list of appointments
    public static AppointmentResponse success(String message, List<AppointmentDto> appointments) {
        AppointmentResponse response = new AppointmentResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setAppointments(appointments);
        response.setTotalCount(appointments.size());
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    // Success response with pagination
    public static AppointmentResponse success(String message, List<AppointmentDto> appointments,
                                              Integer totalCount, Integer pageNumber, Integer pageSize) {
        AppointmentResponse response = new AppointmentResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setAppointments(appointments);
        response.setTotalCount(totalCount);
        response.setPageNumber(pageNumber);
        response.setPageSize(pageSize);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    // Success response with just message
    public static AppointmentResponse success(String message) {
        AppointmentResponse response = new AppointmentResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    // Error response
    public static AppointmentResponse error(String message, String errorCode) {
        AppointmentResponse response = new AppointmentResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorCode(errorCode);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    // Error response with just message
    public static AppointmentResponse error(String message) {
        return error(message, "GENERAL_ERROR");
    }
}