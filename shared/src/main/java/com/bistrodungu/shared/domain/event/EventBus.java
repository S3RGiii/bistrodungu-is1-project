package com.bistrodungu.shared.domain.event;

import java.util.List;

/**
 * Port for publishing domain events.
 * Implementation depends on deployment: SpringApplicationEventBus for monolith,
 * Kafka/RabbitMQ for microservices.
 */
public interface EventBus {
    void publish(DomainEvent event);

    void publishAll(List<DomainEvent> events);
}
