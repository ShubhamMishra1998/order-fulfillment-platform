package com.levelup.order.repository;

import com.levelup.order.domain.Order;

import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(String orderId);
}
