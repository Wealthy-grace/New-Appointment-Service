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

    List<AppointmentEntity> findByRequesterId(String requesterId);

    List<AppointmentEntity> findByProviderId(String providerId);

    List<AppointmentEntity> findByPropertyId(String propertyId);

    List<AppointmentEntity> findByStatus(AppointmentStatus status);

    List<AppointmentEntity> findByType(AppointmentType type);

    @Query("{ $or: [ { 'requesterId': ?0 }, { 'providerId': ?0 } ] }")
    List<AppointmentEntity> findByUserId(String userId);

    List<AppointmentEntity> findByAppointmentDateTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("{ 'providerId': ?0, 'appointmentDateTime': { $gte: ?1, $lte: ?2 }, 'status': { $nin: ['CANCELLED', 'NO_SHOW'] } }")
    List<AppointmentEntity> findProviderBookedSlots(String providerId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("{ 'propertyId': ?0, 'appointmentDateTime': { $gte: ?1, $lte: ?2 }, 'status': ?3 }")
    List<AppointmentEntity> findByPropertyIdAndDateRangeAndStatus(
            String propertyId, LocalDateTime startDate, LocalDateTime endDate, AppointmentStatus status);

    @Query("{ 'status': ?0, 'appointmentDateTime': { $lte: ?1 }, 'reminderSent': false }")
    List<AppointmentEntity> findPendingReminders(AppointmentStatus status, LocalDateTime dateTime);

    Optional<AppointmentEntity> findByConfirmationToken(String confirmationToken);

    @Query(value = "{ 'providerId': ?0, 'appointmentDateTime': { $gte: ?1, $lt: ?2 }, 'status': { $ne: ?3 } }",
            exists = true)
    boolean existsByProviderIdAndAppointmentDateTimeBetweenAndStatusNot(
            String providerId, LocalDateTime startDateTime, LocalDateTime endDateTime, AppointmentStatus status);

    @Query("{ 'providerId': ?0, " +
            "$and: [ " +
            "  { $or: [ " +
            "    { 'appointmentDateTime': { $lte: ?2 } }, " +
            "    { $expr: { $gt: [ { $add: ['$appointmentDateTime', { $multiply: ['$durationMinutes', 60000] }] }, ?1 ] } } " +
            "  ] }, " +
            "  { 'status': { $nin: ['CANCELLED', 'NO_SHOW'] } }, " +
            "  { $or: [ { '_id': { $exists: false } }, { '_id': { $ne: ?3 } } ] } " +
            "], " +
            "count: { $gt: 0 } }")
    boolean hasConflictingAppointment(String providerId, LocalDateTime startTime, LocalDateTime endTime, String excludeId);

    // Additional useful queries for MongoDB
    @Query("{ 'appointmentDateTime': { $gte: ?0 }, 'status': 'CONFIRMED' }")
    List<AppointmentEntity> findUpcomingConfirmedAppointments(LocalDateTime fromDate);

    @Query("{ 'requesterId': ?0, 'status': { $in: ?1 } }")
    List<AppointmentEntity> findByRequesterIdAndStatusIn(String requesterId, List<AppointmentStatus> statuses);

    @Query("{ 'providerId': ?0, 'status': { $in: ?1 } }")
    List<AppointmentEntity> findByProviderIdAndStatusIn(String providerId, List<AppointmentStatus> statuses);

    long countByRequesterIdAndStatus(String requesterId, AppointmentStatus status);

    long countByProviderIdAndStatus(String providerId, AppointmentStatus status);
}
