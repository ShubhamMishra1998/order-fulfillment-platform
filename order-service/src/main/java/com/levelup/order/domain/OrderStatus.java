package com.levelup.order.domain;

public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    CONFIRMED,
    PAYMENT_FAILED,
    OUT_OF_STOCK
}