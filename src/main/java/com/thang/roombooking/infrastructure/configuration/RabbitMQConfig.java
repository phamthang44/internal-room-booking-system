package com.thang.roombooking.infrastructure.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    private final RoomBookingRabbitMQProperties properties;

    // ==================== 1. EXCHANGE ====================
    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(properties.getExchange());
    }

    // ==================== 2. EMAIL QUEUES (FAST/SLOW LANE) ====================

    // --- FAST LANE: Security-critical emails (OTP, Reset Password) ---
    @Bean
    public Queue emailPriorityQueue() {
        return new Queue(properties.getQueues().getEmailPriority(), true);
    }

    @Bean
    public Binding bindingPriorityEmail(Queue emailPriorityQueue, TopicExchange eventExchange) {
        // Enforce: notification.email.security.* -> email-priority
        return BindingBuilder.bind(emailPriorityQueue)
                .to(eventExchange)
                .with(properties.getRoutingKeys().getPatternEmailPriority());
    }

    // --- SLOW LANE: Business and non-critical emails ---
    @Bean
    public Queue emailNormalQueue() {
        return new Queue(properties.getQueues().getEmailNormal(), true);
    }

    @Bean
    public Binding bindingNormalEmailOrder(Queue emailNormalQueue, TopicExchange eventExchange) {
        // Enforce: notification.email.* -> email-normal
        return BindingBuilder.bind(emailNormalQueue)
                .to(eventExchange)
                .with(properties.getRoutingKeys().getPatternEmailNormal());
    }

    // --- BOOKING LANE: booking lifecycle emails (create/approve/reject/cancel) ---
    @Bean
    public Queue emailBookingQueue() {
        return new Queue(properties.getQueues().getEmailBooking(), true);
    }

    @Bean
    public Binding bindingBookingEmail(Queue emailBookingQueue, TopicExchange eventExchange) {
        // Enforce: notification.email.booking.* -> email-booking
        return BindingBuilder.bind(emailBookingQueue)
                .to(eventExchange)
                .with(properties.getRoutingKeys().getPatternEmailBooking());
    }

    // ==================== 3. INFRASTRUCTURE ====================
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}