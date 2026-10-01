package com.levelup.order.service;

import com.levelup.order.domain.Order;
import com.levelup.order.exception.OrderNotFoundException;
import com.levelup.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order createOrder(String customerId) {
        String orderId = "ORD-" + UUID.randomUUID();
        Order order = new Order(
                orderId,
                customerId,
                "PENDING"
        );
        return orderRepository.save(order);
    }

    public Order getOrder(String orderId) {

        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
