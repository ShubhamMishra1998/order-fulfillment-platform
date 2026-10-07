package com.levelup.inventory.event;

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