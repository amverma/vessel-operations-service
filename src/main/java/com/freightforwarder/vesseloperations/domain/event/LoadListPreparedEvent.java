package com.freightforwarder.vesseloperations.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when load list is prepared and shared with terminal
 * One of the critical conditions for vessel departure
 */
@Getter
@AllArgsConstructor
public class LoadListPreparedEvent implements DomainEvent {
    
    private final String eventId;
    private final String vesselId;
    private final String vesselName;
    private final String voyageNumber;
    private final String portOfLoading;
    private final Integer totalContainersPlanned;
    private final Instant sharedAt;
    private final Instant occurredAt;
    
    public LoadListPreparedEvent(
            String vesselId,
            String vesselName,
            String voyageNumber,
            String portOfLoading,
            Integer totalContainersPlanned,
            Instant sharedAt) {
        
        this.eventId = UUID.randomUUID().toString();
        this.vesselId = vesselId;
        this.vesselName = vesselName;
        this.voyageNumber = voyageNumber;
        this.portOfLoading = portOfLoading;
        this.totalContainersPlanned = totalContainersPlanned;
        this.sharedAt = sharedAt;
        this.occurredAt = Instant.now();
    }
    
    @Override
    public String getEventType() {
        return "LoadListPrepared";
    }
    
    @Override
    public String getAggregateId() {
        return vesselId;
    }
}

// Made with Bob
