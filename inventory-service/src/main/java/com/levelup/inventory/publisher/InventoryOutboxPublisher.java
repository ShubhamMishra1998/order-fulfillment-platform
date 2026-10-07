package com.levelup.inventory.publisher;

import com.levelup.inventory.entity.OutboxEvent;
import com.levelup.inventory.repository.OutboxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InventoryOutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final InventoryEventPublisher inventoryEventPublisher;

    public InventoryOutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            InventoryEventPublisher inventoryEventPublisher) {

        this.outboxEventRepository = outboxEventRepository;
        this.inventoryEventPublisher = inventoryEventPublisher;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        var events = outboxEventRepository.findByPublishedFalse();
        for (OutboxEvent event : events) {
            inventoryEventPublisher.publish(event.getAggregateId(), event.getPayload());
            event.markAsPublished();
            outboxEventRepository.save(event);
        }
    }
}
