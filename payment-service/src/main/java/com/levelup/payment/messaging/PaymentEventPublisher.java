package com.levelup.payment.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class PaymentEventPublisher {
    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String orderId, String payload) {
        try {
            kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, orderId, payload).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Kafka publishing interrupted", e);

        } catch (ExecutionException e) {
            throw new RuntimeException(
                    "Failed to publish payment event", e);
        }
    }
}
