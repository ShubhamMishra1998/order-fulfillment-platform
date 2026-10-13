package com.levelup.designpatterns.templatemethod;

public class StorePickupOrderProcessor extends OrderProcessor {

    @Override
    protected void makePayment(String orderId) {
        System.out.println("2. Customer pays cash at the store counter for order " + orderId);
    }

    @Override
    protected void deliver(String orderId) {
        System.out.println("3. Customer picks up order " + orderId + " from the store");
    }

    // Overriding the hook: customer is already at the store, no email needed.
    @Override
    protected boolean shouldSendNotification() {
        return false;
    }
}
