package com.example.appointmentservice.producer;

import com.example.appointmentservice.configuration.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Producer for sending appointment events to RabbitMQ
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes appointment event to the exchange
     */
    public void publishAppointmentEvent(AppointmentEvent event) {
        try {
            log.info("Publishing appointment event: {} for appointment ID: {}",
                    event.getEventType(), event.getAppointmentId());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.APPOINTMENT_ROUTING_KEY,
                    event
            );

            log.info("Successfully published appointment event: {}", event.getEventType());
        } catch (Exception e) {
            log.error("Failed to publish appointment event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish event to RabbitMQ", e);
        }
    }

    /**
     * Convenience method for appointment creation events
     */
    public void publishAppointmentCreated(AppointmentEvent event) {
        event.setEventType("APPOINTMENT_CREATED");
        publishAppointmentEvent(event);
    }

    /**
     * Convenience method for appointment confirmation events
     */
    public void publishAppointmentConfirmed(AppointmentEvent event) {
        event.setEventType("APPOINTMENT_CONFIRMED");
        publishAppointmentEvent(event);
    }

    /**
     * Convenience method for appointment cancellation events
     */
    public void publishAppointmentCancelled(AppointmentEvent event) {
        event.setEventType("APPOINTMENT_CANCELLED");
        publishAppointmentEvent(event);
    }

    /**
     * Convenience method for appointment rescheduled events
     */
    public void publishAppointmentRescheduled(AppointmentEvent event) {
        event.setEventType("APPOINTMENT_RESCHEDULED");
        publishAppointmentEvent(event);
    }

    /**
     * Convenience method for appointment completed events
     */
    public void publishAppointmentCompleted(AppointmentEvent event) {
        event.setEventType("APPOINTMENT_COMPLETED");
        publishAppointmentEvent(event);
    }
}