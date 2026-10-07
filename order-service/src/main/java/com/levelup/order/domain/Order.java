package com.levelup.order.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    private String orderId;

    private String customerId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;


    public Order() {
        // Required by JPA
    }

    public Order(String orderId, String customerId, OrderStatus status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void markInventoryReserved() {
        this.status = OrderStatus.INVENTORY_RESERVED;
    }

    public void markPaymentFailed() {
        this.status = OrderStatus.PAYMENT_FAILED;
    }

    public void markConfirmed() {
        this.status = OrderStatus.CONFIRMED;
    }
}
