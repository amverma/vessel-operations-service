package com.freightforwarder.vesseloperations.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when vessel ETA is updated
 * Triggers customer alerts, SLA updates, and tracking notifications
 */
@Getter
@AllArgsConstructor
public class ETAUpdatedEvent implements DomainEvent {
    
    private final String eventId;
    private final String vesselId;
    private final String vesselName;
    private final String voyageNumber;
    private final Instant previousETA;
    private final Instant newETA;
    private final String reason;
    private final Instant occurredAt;
    
    public ETAUpdatedEvent(
            String vesselId,
            String vesselName,
            String voyageNumber,
            Instant previousETA,
            Instant newETA,
            String reason,
            Instant occurredAt) {
        
        this.eventId = UUID.randomUUID().toString();
        this.vesselId = vesselId;
        this.vesselName = vesselName;
        this.voyageNumber = voyageNumber;
        this.previousETA = previousETA;
        this.newETA = newETA;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }
    
    @Override
    public String getEventType() {
        return "ETAUpdated";
    }
    
    @Override
    public String getAggregateId() {
        return vesselId;
    }
}

// Made with Bob
