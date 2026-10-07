package com.levelup.payment.messaging;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID eventId,
        String orderId,
        UUID paymentId,
        Instant occurredAt,
        int version
) {
}