package com.bistrodungu.shared.infrastructure.event;

import com.bistrodungu.shared.domain.event.DomainEvent;
import com.bistrodungu.shared.domain.event.EventBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementation of EventBus using Spring ApplicationEventPublisher.
 * In a microservices migration, this would be replaced by Kafka/RabbitMQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringApplicationEventBus implements EventBus {
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(DomainEvent event) {
        log.debug("Publishing domain event: {}", event.getEventName());
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
