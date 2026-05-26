package com.bistrodungu.shared.domain.aggregate;

import com.bistrodungu.shared.domain.event.DomainEvent;
import java.util.*;

/**
 * Base class for all aggregate roots.
 * Aggregates maintain consistency boundaries and publish domain events.
 *
 * @param <ID> The type of the aggregate's identity
 */
public abstract class AggregateRoot<ID> {
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * Get the aggregate's unique identifier
     */
    public abstract ID getId();

    /**
     * Register a domain event that will be published when the aggregate is persisted
     */
    protected void registerEvent(DomainEvent event) {
        domainEvents.add(Objects.requireNonNull(event, "Domain event cannot be null"));
    }

    /**
     * Get all unpublished domain events
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * Clear all registered events (called after persistence)
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
