//package com.example.appointmentservice;
//
//import com.example.appointmentservice.persistence.model.AppointmentEntity;
//import com.example.appointmentservice.persistence.model.AppointmentStatus;
//import com.example.appointmentservice.persistence.model.AppointmentType;
//import com.example.appointmentservice.persistence.repository.AppointmentRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.MongoDBContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DataMongoTest
//@ActiveProfiles("test")
//@Testcontainers
//public class AppointmentRepositoryTest {
//
//    @Container
//    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0.0");
//
//    @DynamicPropertySource
//    static void configureProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
//    }
//
//    @Autowired
//    private AppointmentRepository appointmentRepository;
//
//    private AppointmentEntity testAppointment;
//
//    @BeforeEach
//    void setUp() {
//        appointmentRepository.deleteAll();
//
//        testAppointment = new AppointmentEntity();
//        testAppointment.setAppointmentTitle("Test Appointment");
//        testAppointment.setDescription("Test Description");
//        testAppointment.setAppointmentDateTime(LocalDateTime.now().plusDays(1));
//        testAppointment.setDurationMinutes(60);
//        testAppointment.setStatus(AppointmentStatus.PENDING);
//        testAppointment.setType(AppointmentType.PROPERTY_VIEWING);
//        testAppointment.setPropertyId(Long.valueOf("property123"));
//        testAppointment.setRequesterId(Long.valueOf("requester123"));
//        testAppointment.setProviderId(Long.valueOf("provider123"));
//        testAppointment.setIsRecurring(false);
//        testAppointment.setReminderSent(false);
//    }
//
//    @Test
//    void shouldSaveAndFindAppointment() {
//        // Given
//        AppointmentEntity savedAppointment = appointmentRepository.save(testAppointment);
//
//        // When
//        AppointmentEntity foundAppointment = appointmentRepository.findById(savedAppointment.getId()).orElse(null);
//
//        // Then
//        assertThat(foundAppointment).isNotNull();
//        assertThat(foundAppointment.getAppointmentTitle()).isEqualTo("Test Appointment");
//        assertThat(foundAppointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
//    }
//
//    @Test
//    void shouldFindAppointmentsByRequesterId() {
//        // Given
//        appointmentRepository.save(testAppointment);
//
//        // When
//        List<AppointmentEntity> appointments = appointmentRepository.findByRequesterId(Long.valueOf("requester123"));
//
//        // Then
//        assertThat(appointments).hasSize(1);
//        assertThat(appointments.get(0).getRequesterId()).isEqualTo("requester123");
//    }
//
//    @Test
//    void shouldFindAppointmentsByStatus() {
//        // Given
//        appointmentRepository.save(testAppointment);
//
//        // When
//        List<AppointmentEntity> pendingAppointments = appointmentRepository.findByStatus(AppointmentStatus.PENDING);
//
//        // Then
//        assertThat(pendingAppointments).hasSize(1);
//        assertThat(pendingAppointments.get(0).getStatus()).isEqualTo(AppointmentStatus.PENDING);
//    }
//
//    @Test
//    void shouldFindAppointmentsByUserId() {
//        // Given
//        appointmentRepository.save(testAppointment);
//
//        // When - searching by requester ID
//        List<AppointmentEntity> requesterAppointments = appointmentRepository.findByUserId(Long.valueOf("requester123"));
//
//        // When - searching by provider ID
//        List<AppointmentEntity> providerAppointments = appointmentRepository.findByUserId(Long.valueOf("provider123"));
//
//        // Then
//        assertThat(requesterAppointments).hasSize(1);
//        assertThat(providerAppointments).hasSize(1);
//        assertThat(requesterAppointments.get(0).getId()).isEqualTo(providerAppointments.get(0).getId());
//    }
//}
//
//// Alternative simpler test without Testcontainers (using @DataMongoTest defaults)
//// If you want to avoid Docker dependency entirely:
//
///*
//@DataMongoTest
//@ActiveProfiles("test")
//class AppointmentRepositorySimpleTest {
//
//    @Autowired
//    private AppointmentRepository appointmentRepository;
//
//    @Test
//    void contextLoads() {
//        // Simple test to verify repository injection works
//        assertThat(appointmentRepository).isNotNull();
//    }
//
//}
//*/