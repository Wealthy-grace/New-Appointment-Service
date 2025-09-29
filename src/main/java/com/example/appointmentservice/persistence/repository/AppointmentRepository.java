package com.example.appointmentservice.persistence.repository;
import com.example.appointmentservice.persistence.model.AppointmentEntity;
import com.example.appointmentservice.persistence.model.AppointmentStatus;
import com.example.appointmentservice.persistence.model.AppointmentType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends MongoRepository<AppointmentEntity, String> {

    List<AppointmentEntity> findByStatus(AppointmentStatus status);

    List<AppointmentEntity> findByType(AppointmentType type);

    List<AppointmentEntity> findByAppointmentDateTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    Optional<AppointmentEntity> findByConfirmationToken(String confirmationToken);

    @Query("{ 'requester_id': ?0 }")
    List<AppointmentEntity> findByRequesterId(Long requesterId);

    @Query("{ 'provider_id': ?0 }")
    List<AppointmentEntity> findByProviderId(Long providerId);

    @Query("{ 'property_id': ?0 }")
    List<AppointmentEntity> findByPropertyId(Long propertyId);

    @Query("{ $or: [{ 'requester_id': ?0 }, { 'provider_id': ?0 }] }")
    List<AppointmentEntity> findByUserId(Long userId);

    // FIXED: Added exists = true
    @Query(value = "{ 'requester_id': ?0, 'provider_id': ?1, 'property_id': ?2, 'appointment_date_time': ?3 }",
            exists = true)
    boolean existsByRequesterIdAndProviderIdAndPropertyIdAndAppointmentDateTime(
            Long requesterId, Long providerId, Long propertyId, LocalDateTime appointmentDateTime);

    @Query("{ 'status': ?0, 'reminder_sent': false, 'appointment_date_time': { $lte: ?1 } }")
    List<AppointmentEntity> findPendingReminders(AppointmentStatus status, LocalDateTime reminderTime);

    @Query("{ 'provider_id': ?0, 'appointment_date_time': { $gte: ?1, $lte: ?2 }, 'status': { $nin: ['CANCELLED', 'NO_SHOW'] } }")
    List<AppointmentEntity> findProviderBookedSlots(Long providerId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    @Query(value = "{ " +
            "'provider_id': ?0, " +
            "'status': { $nin: ['CANCELLED', 'NO_SHOW'] }, " +
            "'_id': { $ne: ?3 }, " +
            "$or: [ " +
            "  { $and: [ " +
            "    { 'appointment_date_time': { $lt: ?2 } }, " +
            "    { $expr: { $gt: [ { $add: ['$appointment_date_time', { $multiply: ['$duration_minutes', 60000] }] }, ?1 ] } } " +
            "  ] }, " +
            "  { $and: [ " +
            "    { 'appointment_date_time': { $gte: ?1 } }, " +
            "    { 'appointment_date_time': { $lt: ?2 } } " +
            "  ] } " +
            "] " +
            "}",
            exists = true)
    boolean hasConflictingAppointment(Long providerId, LocalDateTime startTime, LocalDateTime endTime, String excludeAppointmentId);
}