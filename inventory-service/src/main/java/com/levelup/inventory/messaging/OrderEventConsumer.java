package com.levelup.inventory.messaging;

import com.levelup.inventory.domain.InventoryReservation;
import com.levelup.inventory.domain.ProcessedEvent;
import com.levelup.inventory.entity.OutboxEvent;
import com.levelup.inventory.event.InventoryReservedEvent;
import com.levelup.inventory.repository.InventoryReservationRepository;
import com.levelup.inventory.repository.OutboxEventRepository;
import com.levelup.inventory.repository.ProcessedEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Component
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final OutboxEventRepository outboxEventRepository;

    public OrderEventConsumer(ObjectMapper objectMapper, ProcessedEventRepository processedEventRepository, InventoryReservationRepository inventoryReservationRepository, OutboxEventRepository outboxEventRepository) {
        this.objectMapper = objectMapper;
        this.processedEventRepository = processedEventRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @KafkaListener(
            topics = "order-events",
            groupId = "inventory-service"
    )
    @Transactional
    public void consume(String payload) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);

            // 1. Idempotency check
            if (processedEventRepository.existsById(event.eventId())) {
                System.out.println("Duplicate event received: " + event.eventId());
                return;
            }
            if (inventoryReservationRepository.existsById(event.orderId())) {
                processedEventRepository.save(new ProcessedEvent(event.eventId()));
                return;
            }

            InventoryReservation reservation = new InventoryReservation(event.orderId(), "RESERVED");
            inventoryReservationRepository.save(reservation);

            // 3. Create InventoryReservedEvent
            InventoryReservedEvent reservedEvent = new InventoryReservedEvent(UUID.randomUUID(), event.orderId(), event.customerId(), Instant.now(), 1);

            // 4. Serialize event
            String reservedPayload = objectMapper.writeValueAsString(reservedEvent);

            // 5. Save event to Outbox
            OutboxEvent outboxEvent = new OutboxEvent(reservedEvent.eventId(), "InventoryReserved", event.orderId(), reservedPayload);

            outboxEventRepository.save(outboxEvent);

            // 6. Mark original event as processed
            processedEventRepository.save(new ProcessedEvent(event.eventId()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize OrderCreatedEvent", e);
        }
    }

}
