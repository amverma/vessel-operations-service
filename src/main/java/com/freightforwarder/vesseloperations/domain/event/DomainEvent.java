package com.freightforwarder.vesseloperations.domain.event;

import java.time.Instant;

/**
 * Base interface for all domain events
 * Domain events represent something that happened in the domain
 */
public interface DomainEvent {
    
    /**
     * Get the unique identifier for this event
     */
    String getEventId();
    
    /**
     * Get the type of this event
     */
    String getEventType();
    
    /**
     * Get the timestamp when this event occurred
     */
    Instant getOccurredAt();
    
    /**
     * Get the aggregate ID that this event relates to
     */
    String getAggregateId();
}

// Made with Bob
