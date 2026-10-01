package com.levelup.order.domain;

public class Order {
    private final String orderId;
    private final String customerId;
    private final String status;

    public Order(String orderId, String customerId, String status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getStatus() {
        return status;
    }
}
