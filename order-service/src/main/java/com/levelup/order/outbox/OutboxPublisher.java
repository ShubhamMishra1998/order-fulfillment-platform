package com.levelup.order.outbox;

import com.levelup.order.messaging.OrderEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, OrderEventPublisher orderEventPublisher) {
        this.outboxEventRepository = outboxEventRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        var events = outboxEventRepository.findByPublishedFalse();
        for (OutboxEvent event : events) {
            orderEventPublisher.publish(event.getAggregateId(), event.getEventId().toString(), event.getPayload());
            event.markAsPublished();
            outboxEventRepository.save(event);
        }
    }
}
