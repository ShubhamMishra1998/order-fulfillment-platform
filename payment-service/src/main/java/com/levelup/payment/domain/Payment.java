package com.levelup.payment.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_order_id", columnNames = "order_id")
        }
)
public class Payment {
    @Id
    private UUID paymentId;

    @Column(nullable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    protected Payment() {
        // Required by JPA
    }

    public Payment(
            UUID paymentId,
            String orderId,
            PaymentStatus status) {

        this.paymentId = paymentId;
        this.orderId = orderId;
        this.status = status;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void markSuccess() {
        this.status = PaymentStatus.SUCCESS;
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }
}
