package com.example.appointmentservice.persistence.model;


import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Document(collection = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentEntity {

    @Id
    private String id;

    @Field("appointment_title")
    @Indexed
    private String appointmentTitle;

    @Field("description")
    private String description;

    @Field("appointment_date_time")
    @Indexed
    private LocalDateTime appointmentDateTime;

    @Field("duration_minutes")
    private Integer durationMinutes;

    @Field("status")
    @Indexed
    private AppointmentStatus status;

    @Field("type")
    @Indexed
    private AppointmentType type;

    @Field("property_id")
    @Indexed
    private Long propertyId;

    @Field("requester_id")
    @Indexed
    private Long requesterId;

    @Field("provider_id")
    @Indexed
    private Long providerId;

    @Field("location")
    private String location;

    @Field("notes")
    private String notes;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("cancellation_reason")
    private String cancellationReason;

    @Field("is_recurring")
    private Boolean isRecurring = false;

    @Field("meeting_link")
    private String meetingLink;

    @Field("reminder_sent")
    private Boolean reminderSent = false;

    @Field("confirmation_token")
    @Indexed(unique = true)
    private String confirmationToken;

    // Additional fields for enhanced functionality
    @Field("requester_username")
    private String requesterUsername;

    @Field("provider_name")
    private String providerName;

    @Field("property_title")
    private String propertyTitle;

    @Field("property_address")
    private String propertyAddress;
}
