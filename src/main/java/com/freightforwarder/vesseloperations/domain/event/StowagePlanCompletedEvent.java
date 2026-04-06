package com.freightforwarder.vesseloperations.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when stowage plan and load instructions are completed
 * One of the critical conditions for vessel departure
 */
@Getter
@AllArgsConstructor
public class StowagePlanCompletedEvent implements DomainEvent {
    
    private final String eventId;
    private final String vesselId;
    private final String vesselName;
    private final String voyageNumber;
    private final Instant completedAt;
    private final Instant occurredAt;
    
    public StowagePlanCompletedEvent(
            String vesselId,
            String vesselName,
            String voyageNumber,
            Instant completedAt) {
        
        this.eventId = UUID.randomUUID().toString();
        this.vesselId = vesselId;
        this.vesselName = vesselName;
        this.voyageNumber = voyageNumber;
        this.completedAt = completedAt;
        this.occurredAt = Instant.now();
    }
    
    @Override
    public String getEventType() {
        return "StowagePlanCompleted";
    }
    
    @Override
    public String getAggregateId() {
        return vesselId;
    }
}

// Made with Bob
