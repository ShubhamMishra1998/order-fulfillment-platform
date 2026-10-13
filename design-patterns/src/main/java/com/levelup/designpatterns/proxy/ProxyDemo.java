package com.levelup.designpatterns.proxy;

public class ProxyDemo {

    public static void main(String[] args) {
        System.out.println("=== Customer tries to cancel ===");
        OrderService customerService = new OrderServiceProxy("CUSTOMER");
        customerService.cancelOrder("ORD-101");

        System.out.println();
        System.out.println("=== Admin cancels two orders ===");
        OrderService adminService = new OrderServiceProxy("ADMIN");
        adminService.cancelOrder("ORD-202");
        adminService.cancelOrder("ORD-303");
    }
}
