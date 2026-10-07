package com.levelup.payment.messaging;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        String orderId,
        UUID paymentId,
        Instant occurredAt,
        String reason,
        int version
) {
}