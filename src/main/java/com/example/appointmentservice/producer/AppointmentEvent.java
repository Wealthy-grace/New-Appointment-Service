package com.example.appointmentservice.producer;

// Copy this file to BOTH services:
// 1. appointment-service/src/main/java/com/example/appointmentservice/messaging/event/AppointmentEvent.java
// 2. booking-service/src/main/java/com/example/bookingservice/messaging/event/AppointmentEvent.java



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event object for appointment-related messages
 * THIS CLASS MUST BE IDENTICAL IN BOTH SERVICES
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    // Event metadata
    private String eventType;
    private LocalDateTime eventTimestamp;
    private String eventId;

    // Appointment core details
    private String appointmentId;
    private String appointmentTitle;
    private String description;
    private LocalDateTime appointmentDateTime;
    private Integer durationMinutes;
    private String status;
    private String type;
    private String location;
    private String meetingLink;
    private String notes;

    // User details (Requester/Tenant)
    private Long requesterId;
    private String requesterUsername;
    private String requesterName;
    private String requesterEmail;
    private String requesterPhone;
    private String requesterFirstName;
    private String requesterLastName;
    private String requesterProfileImage;

    // User details (Provider/Landlord)
    private Long providerId;
    private String providerUsername;
    private String providerName;
    private String providerEmail;
    private String providerPhone;
    private String providerFirstName;
    private String providerLastName;
    private String providerProfileImage;

    // Property details
    private Long propertyId;
    private String propertyTitle;
    private String propertyAddress;
    private Boolean propertyIsRented;
    private String propertyImage;
    private String propertyImage2;
    private String propertyImage3;
    private String propertyImage4;
    private BigDecimal propertyRentAmount;
    private String propertyDescription;

    // Cancellation details
    private String cancellationReason;

    // Previous date (for rescheduling)
    private LocalDateTime previousDateTime;
}