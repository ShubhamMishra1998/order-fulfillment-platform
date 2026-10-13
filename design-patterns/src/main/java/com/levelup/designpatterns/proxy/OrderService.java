package com.levelup.designpatterns.proxy;

// Common interface - the client talks only to this, so it can't tell proxy from real object.
public interface OrderService {

    void cancelOrder(String orderId);
}
