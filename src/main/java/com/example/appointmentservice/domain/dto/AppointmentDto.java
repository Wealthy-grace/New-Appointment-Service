package com.example.appointmentservice.domain.dto;


import com.example.appointmentservice.persistence.model.AppointmentStatus;
import com.example.appointmentservice.persistence.model.AppointmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    private Boolean propertyIsRented;
    private String propertyImage;
    private String propertyImage2;
    private String propertyImage3;
    private String propertyImage4;
    private LocalDateTime endDateTime;
    private Boolean canCancel;
    private Boolean canReschedule;
    private Integer daysUntilAppointment;


     //Requester fields
    private String requesterEmail;
    private String requesterPhone;
    private String requesterFirstName;
    private String requesterLastName;
    private String requesterProfileImage;

    // Provider fields
    private String providerUsername;
    private String providerEmail;
    private String providerPhone;
    private String providerFirstName;
    private String providerLastName;
    private String providerProfileImage;

    // Additional property fields (if not already present)
    private String propertyDescription;
    private BigDecimal propertyRentAmount;
}
