package com.levelup.inventory.event;
import java.time.Instant;
import java.util.UUID;

public record InventoryReservedEvent(
        UUID eventId,
        String orderId,
        String customerId,
        Instant occurredAt,
        int version
) {}