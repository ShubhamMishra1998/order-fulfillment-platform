package com.levelup.designpatterns.templatemethod;

public class OnlineOrderProcessor extends OrderProcessor {

    @Override
    protected void makePayment(String orderId) {
        System.out.println("2. Charging credit card online for order " + orderId);
    }

    @Override
    protected void deliver(String orderId) {
        System.out.println("3. Shipping order " + orderId + " to customer's home address");
    }
}
