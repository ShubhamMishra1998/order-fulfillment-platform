package com.levelup.order.service;

import com.levelup.order.domain.Order;
import com.levelup.order.domain.OrderStatus;
import com.levelup.order.exception.OrderNotFoundException;
import com.levelup.order.outbox.OutboxEventRepository;
import com.levelup.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldCreateOrder() {

        Order savedOrder = new Order(
                "ORD-123",
                "CUST-100",
                OrderStatus.PENDING
        );

        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        Order result = orderService.createOrder("CUST-100");

        assertNotNull(result);
        assertEquals("CUST-100", result.getCustomerId());
        assertEquals(OrderStatus.PENDING, result.getStatus());

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldReturnOrderWhenOrderExists() {

        Order order = new Order(
                "ORD-123",
                "CUST-100",
                OrderStatus.PENDING
        );

        when(orderRepository.findById("ORD-123"))
                .thenReturn(Optional.of(order));

        Order result = orderService.getOrder("ORD-123");

        assertNotNull(result);
        assertEquals("ORD-123", result.getOrderId());
        assertEquals("CUST-100", result.getCustomerId());

        verify(orderRepository).findById("ORD-123");
    }

    @Test
    void shouldThrowExceptionWhenOrderDoesNotExist() {

        when(orderRepository.findById("ORD-999"))
                .thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> orderService.getOrder("ORD-999")
        );

        verify(orderRepository).findById("ORD-999");
    }

}