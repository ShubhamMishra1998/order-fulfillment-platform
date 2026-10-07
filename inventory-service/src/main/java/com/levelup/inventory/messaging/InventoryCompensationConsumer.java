package com.levelup.inventory.messaging;

import com.levelup.inventory.domain.InventoryReservation;
import com.levelup.inventory.domain.ProcessedEvent;
import com.levelup.inventory.event.InventoryReleaseRequestedEvent;
import com.levelup.inventory.repository.InventoryReservationRepository;
import com.levelup.inventory.repository.ProcessedEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class InventoryCompensationConsumer {
    private final ObjectMapper objectMapper;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final ProcessedEventRepository processedEventRepository;

    public InventoryCompensationConsumer(
            ObjectMapper objectMapper,
            InventoryReservationRepository inventoryReservationRepository,
            ProcessedEventRepository processedEventRepository) {

        this.objectMapper = objectMapper;
        this.inventoryReservationRepository =
                inventoryReservationRepository;
        this.processedEventRepository =
                processedEventRepository;
    }

    @KafkaListener(
            topics = "inventory-compensation-events",
            groupId = "inventory-service")
    @Transactional
    public void consume(String payload) {

        System.out.println(
                "Received InventoryReleaseRequested: "
                        + payload
        );

        try {
            InventoryReleaseRequestedEvent event = objectMapper.readValue(payload, InventoryReleaseRequestedEvent.class);
            // Idempotency
            if (processedEventRepository.existsById(event.eventId())) {
                return;
            }

            InventoryReservation reservation = inventoryReservationRepository.findById(event.orderId()).orElseThrow(() ->
                                    new IllegalStateException(
                                            "Inventory reservation not found for order "
                                                    + event.orderId()));

            reservation.updateStatus("RELEASED");

            inventoryReservationRepository.save(reservation);

            processedEventRepository.save(new ProcessedEvent(event.eventId()));

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to process InventoryReleaseRequestedEvent",
                    e);
        }
    }
}
