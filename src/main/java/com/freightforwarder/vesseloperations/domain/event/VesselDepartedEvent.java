package com.freightforwarder.vesseloperations.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when a vessel departs from port
 * CRITICAL: This event marks successful completion of vessel operations
 * Triggers multiple downstream processes including customs, tracking, billing
 */
@Getter
@AllArgsConstructor
public class VesselDepartedEvent implements DomainEvent {
    
    private final String eventId;
    private final String vesselId;
    private final String vesselName;
    private final String imoNumber;
    private final String voyageNumber;
    private final String portOfLoading;
    private final String portOfDischarge;
    private final Integer totalContainersLoaded;
    private final Instant departureTime;
    private final Instant estimatedTimeOfArrival;
    private final Instant occurredAt;
    
    public VesselDepartedEvent(
            String vesselId,
            String vesselName,
            String imoNumber,
            String voyageNumber,
            String portOfLoading,
            String portOfDischarge,
            Integer totalContainersLoaded,
            Instant departureTime,
            Instant estimatedTimeOfArrival) {
        
        this.eventId = UUID.randomUUID().toString();
        this.vesselId = vesselId;
        this.vesselName = vesselName;
        this.imoNumber = imoNumber;
        this.voyageNumber = voyageNumber;
        this.portOfLoading = portOfLoading;
        this.portOfDischarge = portOfDischarge;
        this.totalContainersLoaded = totalContainersLoaded;
        this.departureTime = departureTime;
        this.estimatedTimeOfArrival = estimatedTimeOfArrival;
        this.occurredAt = Instant.now();
    }
    
    @Override
    public String getEventType() {
        return "VesselDeparted";
    }
    
    @Override
    public String getAggregateId() {
        return vesselId;
    }
}

// Made with Bob
