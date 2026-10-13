package com.levelup.designpatterns.templatemethod;

/**
 * Template Method pattern: the base class defines the fixed skeleton of an algorithm,
 * and subclasses fill in (override) only the steps that differ.
 */
public abstract class OrderProcessor {

    // The TEMPLATE METHOD - final so subclasses cannot change the order of steps.
    public final void processOrder(String orderId) {
        System.out.println("=== Processing order " + orderId + " (" + getClass().getSimpleName() + ") ===");
        validateOrder(orderId);
        makePayment(orderId);
        deliver(orderId);
        if (shouldSendNotification()) {
            sendNotification(orderId);
        }
        System.out.println();
    }

    // Common step - same for every order type.
    private void validateOrder(String orderId) {
        System.out.println("1. Validating order " + orderId);
    }

    // Steps that each subclass MUST implement.
    protected abstract void makePayment(String orderId);

    protected abstract void deliver(String orderId);

    // Hook - optional step with a default behaviour that subclasses MAY override.
    protected boolean shouldSendNotification() {
        return true;
    }

    private void sendNotification(String orderId) {
        System.out.println("4. Sending confirmation email for order " + orderId);
    }
}
