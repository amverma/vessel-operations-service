package com.freightforwarder.vesseloperations.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when a container is loaded onto a vessel
 * CRITICAL: This event is essential for tracking container location
 * Failure to emit this event can result in P1 incident (vessel unable to depart)
 */
@Getter
@AllArgsConstructor
public class ContainerLoadedOnVesselEvent implements DomainEvent {
    
    private final String eventId;
    private final String containerId;
    private final String vesselId;
    private final String vesselName;
    private final String voyageNumber;
    private final String bookingReference;
    private final String stowagePosition;
    private final Double weightKg;
    private final String containerType;
    private final String portOfLoading;
    private final String portOfDischarge;
    private final Instant loadedAt;
    private final Instant occurredAt;
    
    public ContainerLoadedOnVesselEvent(
            String containerId,
            String vesselId,
            String vesselName,
            String voyageNumber,
            String bookingReference,
            String stowagePosition,
            Double weightKg,
            String containerType,
            String portOfLoading,
            String portOfDischarge,
            Instant loadedAt) {
        
        this.eventId = UUID.randomUUID().toString();
        this.containerId = containerId;
        this.vesselId = vesselId;
        this.vesselName = vesselName;
        this.voyageNumber = voyageNumber;
        this.bookingReference = bookingReference;
        this.stowagePosition = stowagePosition;
        this.weightKg = weightKg;
        this.containerType = containerType;
        this.portOfLoading = portOfLoading;
        this.portOfDischarge = portOfDischarge;
        this.loadedAt = loadedAt;
        this.occurredAt = Instant.now();
    }
    
    @Override
    public String getEventType() {
        return "ContainerLoadedOnVessel";
    }
    
    @Override
    public String getAggregateId() {
        return vesselId;
    }
}

// Made with Bob
