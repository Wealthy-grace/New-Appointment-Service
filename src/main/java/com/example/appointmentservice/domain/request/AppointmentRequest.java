//package com.example.appointmentservice.domain.request;
//
//
//import com.example.appointmentservice.persistence.model.AppointmentType;
//import jakarta.validation.constraints.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class AppointmentRequest {
//
//    @NotBlank(message = "Appointment title is required")
//    @Size(max = 255, message = "Appointment title must not exceed 255 characters")
//    private String appointmentTitle;
//
//    @Size(max = 1000, message = "Description must not exceed 1000 characters")
//    private String description;
//
//    @NotNull(message = "Appointment date and time is required")
//    @Future(message = "Appointment date and time must be in the future")
//    private LocalDateTime appointmentDateTime;
//
//    @NotNull(message = "Duration is required")
//    @Min(value = 15, message = "Appointment duration must be at least 15 minutes")
//    @Max(value = 480, message = "Appointment duration cannot exceed 8 hours")
//    private Integer durationMinutes;
//
//    @NotNull(message = "Appointment type is required")
//    private AppointmentType type;
//
//    @NotNull(message = "Property ID is required")
//    @Positive(message = "Property ID must be positive")
//    private String propertyId;
//
//    @NotNull(message = "Requester ID is required")
//    @Positive(message = "Requester ID must be positive")
//    private String requesterId;
//
//    @NotNull(message = "Provider ID is required")
//    @Positive(message = "Provider ID must be positive")
//    private String providerId;
//
//    @Size(max = 500, message = "Location must not exceed 500 characters")
//    private String location;
//
//    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
//    private String notes;
//
//    private Boolean isRecurring = false;
//
//    @Pattern(regexp = "^(https?://).*", message = "Meeting link must be a valid URL")
//    @Size(max = 1000, message = "Meeting link must not exceed 1000 characters")
//    private String meetingLink;
//}


// new update

package com.example.appointmentservice.domain.request;

import com.example.appointmentservice.persistence.model.AppointmentType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRequest {

    @NotBlank(message = "Appointment title is required")
    @Size(max = 255, message = "Appointment title must not exceed 255 characters")
    private String appointmentTitle;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

//
//    @NotBlank(message = "Property address is required")
//    @Size(max = 255, message = "Property address must not exceed 255 characters")
//    private String propertyAddress;

    @NotNull(message = "Appointment date and time is required")
    @Future(message = "Appointment date and time must be in the future")
    private LocalDateTime appointmentDateTime;

    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Appointment duration must be at least 15 minutes")
    @Max(value = 480, message = "Appointment duration cannot exceed 8 hours")
    private Integer durationMinutes;

    @NotNull(message = "Appointment type is required")
    private AppointmentType type;

    @NotNull(message = "Property ID is required")
    private Long propertyId;

    @NotNull(message = "Requester ID is required")
    private Long requesterId;

    @NotNull(message = "Provider ID is required")
    private Long providerId;

    @Size(max = 500, message = "Location must not exceed 500 characters")
    private String location;

    @NotBlank(message = "Requester username is required")
    private String RequesterUsername;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    private Boolean isRecurring = false;

    @Pattern(regexp = "^$|^(https?://).*", message = "Meeting link must be a valid URL or empty")
    @Size(max = 1000, message = "Meeting link must not exceed 1000 characters")
    private String meetingLink;
}