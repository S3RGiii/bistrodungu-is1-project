package com.bistrodungu.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events.
 * Events are the primary communication mechanism between modules in the monolith.
 */
public abstract class DomainEvent {
    private final UUID eventId;
    private final Instant occurredAt;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * Event name for serialization and identification
     */
    public String getEventName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return getEventName() + " {" +
                "eventId=" + eventId +
                ", occurredAt=" + occurredAt +
                '}';
    }
}
