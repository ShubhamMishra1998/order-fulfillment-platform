package com.levelup.payment.outbox;

import com.levelup.payment.messaging.PaymentEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentOutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentOutboxPublisher(OutboxEventRepository outboxEventRepository, PaymentEventPublisher paymentEventPublisher) {
        this.outboxEventRepository = outboxEventRepository;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        var events = outboxEventRepository.findByPublishedFalse();
        for (OutboxEvent event : events) {
            paymentEventPublisher.publish(event.getAggregateId(), event.getPayload());
            event.markAsPublished();
            outboxEventRepository.save(event);
        }
    }
}
