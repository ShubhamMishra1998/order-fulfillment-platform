package com.levelup.order;

import com.levelup.order.domain.Order;
import com.levelup.order.domain.OrderStatus;
import com.levelup.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class OrderPersistenceIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldPersistAndRetrieveOrder() {

        Order order = new Order(
                "ORD-TEST-001",
                "CUST-100",
                OrderStatus.PENDING
        );

        orderRepository.save(order);

        Optional<Order> result =
                orderRepository.findById("ORD-TEST-001");

        assertTrue(result.isPresent());
        assertEquals("CUST-100",
                result.get().getCustomerId());
        assertEquals(OrderStatus.PENDING,
                result.get().getStatus());
    }
}
