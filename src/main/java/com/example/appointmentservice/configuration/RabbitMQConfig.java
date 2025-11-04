package com.example.appointmentservice.configuration;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "app-exchange";
    public static final String APPOINTMENT_QUEUE = "appointment-queue";
    public static final String BOOKING_QUEUE = "booking-queue";
    public static final String APPOINTMENT_ROUTING_KEY = "appointment.key";
    public static final String BOOKING_ROUTING_KEY = "booking.key";

    @Bean
    public DirectExchange appExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue appointmentQueue() {
        return new Queue(APPOINTMENT_QUEUE);
    }

    @Bean
    public Queue bookingQueue() {
        return new Queue(BOOKING_QUEUE);
    }

    @Bean
    public Binding appointmentBinding(Queue appointmentQueue, DirectExchange appExchange) {
        return BindingBuilder.bind(appointmentQueue).to(appExchange).with(APPOINTMENT_ROUTING_KEY);
    }

    @Bean
    public Binding bookingBinding(Queue bookingQueue, DirectExchange appExchange) {
        return BindingBuilder.bind(bookingQueue).to(appExchange).with(BOOKING_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
