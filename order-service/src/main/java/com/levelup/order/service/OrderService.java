package com.levelup.order.service;

import com.levelup.order.domain.Order;
import com.levelup.order.domain.OrderStatus;
import com.levelup.order.exception.OrderNotFoundException;
import com.levelup.order.messaging.InventoryReleaseRequestedEvent;
import com.levelup.order.messaging.OrderCreatedEvent;
import com.levelup.order.messaging.PaymentCompletedEvent;
import com.levelup.order.messaging.PaymentFailedEvent;
import com.levelup.order.outbox.OutboxEvent;
import com.levelup.order.outbox.OutboxEventRepository;
import com.levelup.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order createOrder(String customerId) {
        String orderId = "ORD-" + UUID.randomUUID();
        Order order = new Order(
                orderId,
                customerId,
                OrderStatus.PENDING
        );

        orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                orderId,
                customerId,
                Instant.now(),
                1
        );

        String payload;

        payload = objectMapper.writeValueAsString(event);

        OutboxEvent outboxEvent = new OutboxEvent(
                event.eventId(),
                "OrderCreated",
                orderId,
                payload
        );

        outboxEventRepository.save(outboxEvent);

        return order;
    }

    @Transactional
    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {

        Order order = getOrder(event.orderId());

        order.markConfirmed();

        orderRepository.save(order);
    }

    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {

        Order order = getOrder(event.orderId());

        order.markPaymentFailed();

        orderRepository.save(order);

        InventoryReleaseRequestedEvent releaseEvent = new InventoryReleaseRequestedEvent(UUID.randomUUID(), event.orderId(), Instant.now(), "Payment failed", 1);
        String payload = objectMapper.writeValueAsString(releaseEvent);
        outboxEventRepository.save(new OutboxEvent(releaseEvent.eventId(), "InventoryReleaseRequested", event.orderId(), payload));

    }
}
