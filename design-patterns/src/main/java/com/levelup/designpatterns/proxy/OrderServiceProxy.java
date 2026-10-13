package com.levelup.designpatterns.proxy;

/**
 * The PROXY - stands in front of the real object and controls access to it.
 * It adds extra behavior without changing RealOrderService:
 *   1. Access control - only ADMIN can cancel orders.
 *   2. Lazy creation  - the real object is created only when actually needed.
 *   3. Logging        - logs every call.
 */
public class OrderServiceProxy implements OrderService {

    private final String userRole;
    private RealOrderService realService;

    public OrderServiceProxy(String userRole) {
        this.userRole = userRole;
    }

    @Override
    public void cancelOrder(String orderId) {
        System.out.println("   [Proxy] " + userRole + " requested to cancel order " + orderId);

        if (!"ADMIN".equals(userRole)) {
            System.out.println("   [Proxy] Access DENIED - only ADMIN can cancel orders");
            return;
        }

        if (realService == null) {
            realService = new RealOrderService();
        }
        realService.cancelOrder(orderId);
    }
}
