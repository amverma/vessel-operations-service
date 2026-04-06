package com.freightforwarder.vesseloperations.application.port.output;

import com.freightforwarder.vesseloperations.domain.event.DomainEvent;

/**
 * Output port for publishing domain events to Kafka
 * This is a critical component for event-driven architecture
 */
public interface EventPublisher {
    
    /**
     * Publish a domain event to Kafka
     * CRITICAL: Events must be published reliably to prevent P1 incidents
     * 
     * @param event The domain event to publish
     * @throws EventPublishException if event publishing fails
     */
    void publish(DomainEvent event);
    
    /**
     * Publish event with retry mechanism
     * Used for critical events that must be delivered
     */
    void publishWithRetry(DomainEvent event);
}

// Made with Bob
