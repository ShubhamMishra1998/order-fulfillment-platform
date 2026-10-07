package com.levelup.order.outbox;

import com.levelup.order.messaging.InventoryCompensationEventPublisher;
import com.levelup.order.messaging.OrderEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final InventoryCompensationEventPublisher
            inventoryCompensationEventPublisher;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            OrderEventPublisher orderEventPublisher,
            InventoryCompensationEventPublisher
                    inventoryCompensationEventPublisher) {

        this.outboxEventRepository = outboxEventRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.inventoryCompensationEventPublisher =
                inventoryCompensationEventPublisher;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        var events = outboxEventRepository.findByPublishedFalse();
        for (OutboxEvent event : events) {
            switch (event.getEventType()) {
                case "OrderCreated" ->
                        orderEventPublisher.publish(event.getAggregateId(), event.getPayload());
                case "InventoryReleaseRequested" ->
                        inventoryCompensationEventPublisher.publish(
                                event.getAggregateId(),
                                event.getPayload());
                default ->
                        throw new IllegalStateException(
                                "Unsupported outbox event type: "
                                        + event.getEventType());
            }
            event.markAsPublished();
            outboxEventRepository.save(event);
        }
    }
}
