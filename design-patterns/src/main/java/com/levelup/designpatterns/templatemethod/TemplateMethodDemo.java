package com.levelup.designpatterns.templatemethod;

public class TemplateMethodDemo {

    public static void main(String[] args) {
        OrderProcessor online = new OnlineOrderProcessor();
        online.processOrder("ORD-101");

        OrderProcessor pickup = new StorePickupOrderProcessor();
        pickup.processOrder("ORD-202");
    }
}
