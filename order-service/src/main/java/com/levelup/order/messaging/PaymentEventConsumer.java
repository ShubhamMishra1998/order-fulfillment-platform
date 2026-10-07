package com.levelup.order.messaging;

import com.levelup.order.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    public PaymentEventConsumer(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @KafkaListener(
            topics = "payment-events",
            groupId = "order-service"
    )
    public void consume(String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            String eventType = json.has("reason")
                            ? "PaymentFailed"
                            : "PaymentCompleted";

            if ("PaymentFailed".equals(eventType)) {
                PaymentFailedEvent event = objectMapper.readValue(payload, PaymentFailedEvent.class);
                orderService.handlePaymentFailed(event);
            } else {
                PaymentCompletedEvent event = objectMapper.readValue(payload, PaymentCompletedEvent.class);
                orderService.handlePaymentCompleted(event);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process payment event", e);
        }
    }
}
