package com.parkease.notification_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String BOOKING_QUEUE   = "notification.booking.queue";
    public static final String PAYMENT_QUEUE   = "notification.payment.queue";
    public static final String CHECKIN_QUEUE   = "notification.checkin.queue";
    public static final String CHECKOUT_QUEUE  = "notification.checkout.queue";
    public static final String EXPIRY_QUEUE    = "notification.expiry.queue";
    public static final String ADMIN_BROADCAST_QUEUE = "notification.admin.broadcast.queue";
    public static final String ADMIN_WARN_QUEUE      = "notification.admin.warn.queue";

    public static final String EXCHANGE        = "notification.exchange";

    public static final String BOOKING_KEY     = "notification.booking";
    public static final String PAYMENT_KEY     = "notification.payment";
    public static final String CHECKIN_KEY     = "notification.checkin";
    public static final String CHECKOUT_KEY    = "notification.checkout";
    public static final String EXPIRY_KEY      = "notification.expiry";
    public static final String ADMIN_BROADCAST_KEY = "notification.admin.broadcast";
    public static final String ADMIN_WARN_KEY      = "notification.admin.warn";

    @Bean
    public Queue bookingQueue()  {
        return new Queue(BOOKING_QUEUE,  true);
    }

    @Bean
    public Queue paymentQueue()  {
        return new Queue(PAYMENT_QUEUE,  true);
    }

    @Bean
    public Queue checkinQueue()  {
        return new Queue(CHECKIN_QUEUE,  true);
    }

    @Bean
    public Queue checkoutQueue() {
        return new Queue(CHECKOUT_QUEUE, true);
    }

    @Bean
    public Queue expiryQueue()   {
        return new Queue(EXPIRY_QUEUE,   true);
    }

    @Bean
    public Queue adminBroadcastQueue() {
        return new Queue(ADMIN_BROADCAST_QUEUE, true);
    }

    @Bean
    public Queue adminWarnQueue() {
        return new Queue(ADMIN_WARN_QUEUE, true);
    }


    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding bookingBinding() {
        return BindingBuilder.bind(bookingQueue())
                .to(notificationExchange()).with(BOOKING_KEY);
    }

    @Bean
    public Binding paymentBinding() {
        return BindingBuilder.bind(paymentQueue())
                .to(notificationExchange()).with(PAYMENT_KEY);
    }

    @Bean
    public Binding checkinBinding() {
        return BindingBuilder.bind(checkinQueue())
                .to(notificationExchange()).with(CHECKIN_KEY);
    }

    @Bean
    public Binding checkoutBinding() {
        return BindingBuilder.bind(checkoutQueue())
                .to(notificationExchange()).with(CHECKOUT_KEY);
    }

    @Bean
    public Binding expiryBinding() {
        return BindingBuilder.bind(expiryQueue())
                .to(notificationExchange()).with(EXPIRY_KEY);
    }

    @Bean
    public Binding adminBroadcastBinding() {
        return BindingBuilder.bind(adminBroadcastQueue())
                .to(notificationExchange()).with(ADMIN_BROADCAST_KEY);
    }

    @Bean
    public Binding adminWarnBinding() {
        return BindingBuilder.bind(adminWarnQueue())
                .to(notificationExchange()).with(ADMIN_WARN_KEY);
    }

    // Converts Java objects to JSON automatically
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

}