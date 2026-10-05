package com.levelup.inventory.messaging;

import com.levelup.inventory.domain.InventoryReservation;
import com.levelup.inventory.domain.ProcessedEvent;
import com.levelup.inventory.repository.InventoryReservationRepository;
import com.levelup.inventory.repository.ProcessedEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final InventoryReservationRepository inventoryReservationRepository;

    public OrderEventConsumer(ObjectMapper objectMapper, ProcessedEventRepository processedEventRepository, InventoryReservationRepository inventoryReservationRepository) {
        this.objectMapper = objectMapper;
        this.processedEventRepository = processedEventRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
    }

    @KafkaListener(
            topics = "order-events",
            groupId = "inventory-service"
    )
    @Transactional
    public void consume(String payload) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);

            if (processedEventRepository.existsById(event.eventId())) {
                System.out.println("Duplicate event received: " + event.eventId());
                return;
            }

            System.out.println("Received OrderCreated: " + event.orderId());
            InventoryReservation reservation = new InventoryReservation(event.orderId(), "RESERVED");
            inventoryReservationRepository.save(reservation);
            processedEventRepository.save(new ProcessedEvent(event.eventId()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize OrderCreatedEvent", e);
        }
    }

}
