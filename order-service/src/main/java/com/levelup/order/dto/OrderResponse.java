package com.levelup.order.dto;

import com.levelup.order.domain.Order;

public record OrderResponse(String orderId, String status) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getStatus()
        );
    }

}
