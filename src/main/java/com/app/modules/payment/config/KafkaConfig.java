package com.app.modules.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the payment-events topic. Spring's {@code KafkaAdmin} creates it on startup when a broker
 * is reachable; if not, startup still succeeds (the admin is not fatal by default) and the topic is
 * created once the broker is available.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic paymentEventsTopic(@Value("${payment.outbox.topic:payment-events}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
