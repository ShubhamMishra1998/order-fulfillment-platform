package com.levelup.order.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class InventoryCompensationEventPublisher {

    private static final String TOPIC = "inventory-compensation-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public InventoryCompensationEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String orderId, String payload) {
        try {
            kafkaTemplate.send(TOPIC, orderId, payload).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Kafka publishing interrupted", e);

        } catch (ExecutionException e) {
            throw new RuntimeException(
                    "Failed to publish inventory compensation event",
                    e);
        }
    }
}