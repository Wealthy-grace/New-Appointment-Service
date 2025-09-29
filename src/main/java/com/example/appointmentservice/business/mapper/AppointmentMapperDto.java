package com.example.appointmentservice.business.mapper;


import com.example.appointmentservice.domain.dto.AppointmentDto;
import com.example.appointmentservice.persistence.model.AppointmentEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class AppointmentMapperDto {

    public AppointmentDto toDto(AppointmentEntity entity) {
        if (entity == null) {
            return null;
        }

        AppointmentDto dto = new AppointmentDto();
        dto.setId(entity.getId());
        dto.setAppointmentTitle(entity.getAppointmentTitle());
        dto.setDescription(entity.getDescription());
        dto.setAppointmentDateTime(entity.getAppointmentDateTime());
        dto.setDurationMinutes(entity.getDurationMinutes());
        dto.setStatus(entity.getStatus());
        dto.setType(entity.getType());
        dto.setPropertyId(entity.getPropertyId());
        dto.setRequesterId(entity.getRequesterId());
        dto.setProviderId(entity.getProviderId());
        dto.setLocation(entity.getLocation());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCancellationReason(entity.getCancellationReason());
        dto.setIsRecurring(entity.getIsRecurring());
        dto.setMeetingLink(entity.getMeetingLink());
        dto.setReminderSent(entity.getReminderSent());
        dto.setRequesterUsername(entity.getRequesterUsername());
        dto.setProviderName(entity.getProviderName());
        dto.setPropertyTitle(entity.getPropertyTitle());
        dto.setPropertyAddress(entity.getPropertyAddress());
        dto.setConfirmationToken(entity.getConfirmationToken());

        // Calculate additional fields
        if (entity.getAppointmentDateTime() != null && entity.getDurationMinutes() != null) {
            dto.setEndDateTime(entity.getAppointmentDateTime().plusMinutes(entity.getDurationMinutes()));
        }

        // Calculate days until appointment
        if (entity.getAppointmentDateTime() != null) {
            long daysUntil = ChronoUnit.DAYS.between(LocalDateTime.now(), entity.getAppointmentDateTime());
            dto.setDaysUntilAppointment((int) daysUntil);
        }

        // Set action permissions based on status and time
        dto.setCanCancel(canCancelAppointment(entity));
        dto.setCanReschedule(canRescheduleAppointment(entity));

        return dto;
    }

    public AppointmentEntity toEntity(AppointmentDto dto) {
        if (dto == null) {
            return null;
        }

        AppointmentEntity entity = new AppointmentEntity();
        entity.setId(dto.getId());
        entity.setAppointmentTitle(dto.getAppointmentTitle());
        entity.setDescription(dto.getDescription());
        entity.setAppointmentDateTime(dto.getAppointmentDateTime());
        entity.setDurationMinutes(dto.getDurationMinutes());
        entity.setStatus(dto.getStatus());
        entity.setType(dto.getType());
        entity.setPropertyId(dto.getPropertyId());
        entity.setRequesterId(dto.getRequesterId());
        entity.setProviderId(dto.getProviderId());
        entity.setLocation(dto.getLocation());
        entity.setNotes(dto.getNotes());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        entity.setCancellationReason(dto.getCancellationReason());
        entity.setIsRecurring(dto.getIsRecurring());
        entity.setMeetingLink(dto.getMeetingLink());
        entity.setReminderSent(dto.getReminderSent());
        entity.setRequesterUsername(dto.getRequesterUsername());
        entity.setProviderName(dto.getProviderName());
        entity.setPropertyTitle(dto.getPropertyTitle());
        entity.setPropertyAddress(dto.getPropertyAddress());
        entity.setConfirmationToken(dto.getConfirmationToken());

        return entity;
    }

    private boolean canCancelAppointment(AppointmentEntity entity) {
        // Can cancel if not completed, not cancelled, and at least 2 hours before appointment
        if (entity.getStatus() == null || entity.getAppointmentDateTime() == null) {
            return false;
        }

        return !entity.getStatus().name().equals("COMPLETED") &&
                !entity.getStatus().name().equals("CANCELLED") &&
                entity.getAppointmentDateTime().isAfter(LocalDateTime.now().plusHours(2));
    }

    private boolean canRescheduleAppointment(AppointmentEntity entity) {
        // Can reschedule if not completed, not cancelled, and at least 4 hours before appointment
        if (entity.getStatus() == null || entity.getAppointmentDateTime() == null) {
            return false;
        }

        return !entity.getStatus().name().equals("COMPLETED") &&
                !entity.getStatus().name().equals("CANCELLED") &&
                entity.getAppointmentDateTime().isAfter(LocalDateTime.now().plusHours(4));
    }
}