package com.levelup.payment.service;

import com.levelup.payment.domain.Payment;
import com.levelup.payment.event.InventoryReservedEvent;
import com.levelup.payment.messaging.PaymentCompletedEvent;
import com.levelup.payment.messaging.PaymentFailedEvent;
import com.levelup.payment.outbox.OutboxEvent;
import com.levelup.payment.outbox.OutboxEventRepository;
import com.levelup.payment.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processPayment(InventoryReservedEvent event, Payment payment) {

        if ("CUST-FAIL".equals(event.customerId())) {
            payment.markFailed();
            paymentRepository.save(payment);
            PaymentFailedEvent failedEvent = new PaymentFailedEvent(UUID.randomUUID(), event.orderId(), payment.getPaymentId(), Instant.now(), "Payment declined", 1);
            String payload = objectMapper.writeValueAsString(failedEvent);
            outboxEventRepository.save(new OutboxEvent(failedEvent.eventId(), "PaymentFailed", event.orderId(), payload));
            return;
        }

        payment.markSuccess();
        paymentRepository.save(payment);
        PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(UUID.randomUUID(), event.orderId(), payment.getPaymentId(), Instant.now(), 1);
        String payload = objectMapper.writeValueAsString(completedEvent);
        outboxEventRepository.save(new OutboxEvent(completedEvent.eventId(), "PaymentCompleted", event.orderId(), payload));
    }
}
