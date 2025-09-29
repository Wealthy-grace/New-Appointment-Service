package com.example.appointmentservice.domain.dto;


import com.example.appointmentservice.persistence.model.AppointmentStatus;
import com.example.appointmentservice.persistence.model.AppointmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentDto {

    private String id;
    private String appointmentTitle;
    private String description;
    private LocalDateTime appointmentDateTime;
    private Integer durationMinutes;
    private AppointmentStatus status;
    private AppointmentType type;
     private String requesterName;
    private Long propertyId;
    private Long requesterId;
    private Long providerId;
    private String location;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String cancellationReason;
    private Boolean isRecurring;
    private String meetingLink;
    private Boolean reminderSent;
    private String confirmationToken;

    // Additional fields for enhanced response
    private String requesterUsername;
    private String providerName;
    private String propertyTitle;
    private String propertyAddress;
    private String propertyImage;
    private String propertyImage2;
    private String propertyImage3;
    private LocalDateTime endDateTime;
    private Boolean canCancel;
    private Boolean canReschedule;
    private Integer daysUntilAppointment;
}
