package com.freightforwarder.vesseloperations.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when a vessel is reconciled
 * Indicates all containers have been verified and vessel is ready for departure
 */
@Getter
@AllArgsConstructor
public class VesselReconciledEvent implements DomainEvent {
    
    private final String eventId;
    private final String vesselId;
    private final String vesselName;
    private final String voyageNumber;
    private final Integer totalContainersLoaded;
    private final Instant reconciledAt;
    private final Instant occurredAt;
    
    public VesselReconciledEvent(
            String vesselId,
            String vesselName,
            String voyageNumber,
            Integer totalContainersLoaded,
            Instant reconciledAt) {
        
        this.eventId = UUID.randomUUID().toString();
        this.vesselId = vesselId;
        this.vesselName = vesselName;
        this.voyageNumber = voyageNumber;
        this.totalContainersLoaded = totalContainersLoaded;
        this.reconciledAt = reconciledAt;
        this.occurredAt = Instant.now();
    }
    
    @Override
    public String getEventType() {
        return "VesselReconciled";
    }
    
    @Override
    public String getAggregateId() {
        return vesselId;
    }
}

// Made with Bob
