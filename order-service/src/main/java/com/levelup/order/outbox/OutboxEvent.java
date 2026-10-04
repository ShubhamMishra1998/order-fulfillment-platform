package com.levelup.order.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    private UUID eventId;

    private String eventType;

    private String aggregateId;

    private String payload;

    private boolean published;

    protected OutboxEvent() {
    }

    public OutboxEvent(
            UUID eventId,
            String eventType,
            String aggregateId,
            String payload) {

        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.published = false;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public void markAsPublished() {
        this.published = true;
    }
}
