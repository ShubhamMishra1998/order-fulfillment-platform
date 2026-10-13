package com.levelup.designpatterns.proxy;

// The REAL object - does the actual work (imagine DB calls, refunds, etc.).
public class RealOrderService implements OrderService {

    public RealOrderService() {
        System.out.println("   [RealOrderService] Created (expensive: connecting to DB...)");
    }

    @Override
    public void cancelOrder(String orderId) {
        System.out.println("   [RealOrderService] Order " + orderId + " cancelled successfully");
    }
}
