package com.levelup.inventory.messaging;

import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        String orderId,
        String customerId,
        Instant occurredAt,
        int version
) {
}
