package com.levelup.order.messaging;

import java.time.Instant;
import java.util.UUID;

public record InventoryReleaseRequestedEvent(
        UUID eventId,
        String orderId,
        Instant occurredAt,
        String reason,
        int version
) {
}