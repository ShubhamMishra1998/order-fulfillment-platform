package com.levelup.payment.messaging;

import com.levelup.payment.domain.Payment;
import com.levelup.payment.domain.PaymentStatus;
import com.levelup.payment.event.InventoryReservedEvent;
import com.levelup.payment.repository.ProcessedEvent;
import com.levelup.payment.repository.ProcessedEventRepository;
import com.levelup.payment.service.PaymentService;
import jakarta.transaction.Transactional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final PaymentService paymentService;

    public PaymentEventConsumer(
            ObjectMapper objectMapper,
            ProcessedEventRepository processedEventRepository,
            PaymentService paymentService) {

        this.objectMapper = objectMapper;
        this.processedEventRepository = processedEventRepository;
        this.paymentService = paymentService;
    }

    @KafkaListener(
            topics = "inventory-events",
            groupId = "payment-service"
    )
    @Transactional
    public void consume(String payload) {
        InventoryReservedEvent event = objectMapper.readValue(payload, InventoryReservedEvent.class);
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }
        Payment payment = new Payment(
                UUID.randomUUID(),
                event.orderId(),
                PaymentStatus.PENDING
        );
        paymentService.processPayment(event, payment);
        processedEventRepository.save(new ProcessedEvent(event.eventId()));
    }
}
