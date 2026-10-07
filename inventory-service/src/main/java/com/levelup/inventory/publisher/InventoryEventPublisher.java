package com.levelup.inventory.publisher;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class InventoryEventPublisher {
    private static final String INVENTORY_EVENTS_TOPIC = "inventory-events";
    private final KafkaTemplate<String, String> kafkaTemplate;

    public InventoryEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String orderId, String payload) {
        try {
            kafkaTemplate.send(INVENTORY_EVENTS_TOPIC, orderId, payload).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Kafka publishing interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to publish inventory event", e);
        }
    }
}
