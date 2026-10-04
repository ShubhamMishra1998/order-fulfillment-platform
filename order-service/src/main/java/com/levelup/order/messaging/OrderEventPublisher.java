package com.levelup.order.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class OrderEventPublisher {

    private static final String ORDER_EVENTS_TOPIC = "order-events";
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String orderId, String payload) {
        try {
            kafkaTemplate.send(ORDER_EVENTS_TOPIC, orderId, payload).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Kafka publishing interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to publish event to Kafka", e);
        }
    }
}
