package com.freightforwarder.vesseloperations.domain.model;

import com.freightforwarder.vesseloperations.domain.event.DomainEvent;
import com.freightforwarder.vesseloperations.domain.event.VesselDepartedEvent;
import com.freightforwarder.vesseloperations.domain.event.VesselReconciledEvent;
import com.freightforwarder.vesseloperations.domain.event.ContainerLoadedOnVesselEvent;
import com.freightforwarder.vesseloperations.domain.event.ETAUpdatedEvent;
import com.freightforwarder.vesseloperations.domain.event.LoadListPreparedEvent;
import com.freightforwarder.vesseloperations.domain.event.StowagePlanCompletedEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Vessel Aggregate Root
 * Manages vessel loading operations and departure coordination
 * This is a mission-critical entity (P1) - vessel unable to depart halts operations
 */
@Entity
@Table(name = "vessels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vessel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "vessel_id", nullable = false, unique = true))
    private VesselId vesselId;
    
    @Column(name = "vessel_name", nullable = false)
    private String vesselName;
    
    @Column(name = "imo_number", unique = true)
    private String imoNumber;
    
    @Column(name = "voyage_number", nullable = false)
    private String voyageNumber;
    
    @Column(name = "port_of_loading", nullable = false)
    private String portOfLoading;
    
    @Column(name = "port_of_discharge")
    private String portOfDischarge;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VesselStatus status;
    
    @Column(name = "eta")
    private Instant estimatedTimeOfArrival;
    
    @Column(name = "etd")
    private Instant estimatedTimeOfDeparture;
    
    @Column(name = "actual_departure_time")
    private Instant actualDepartureTime;
    
    @Column(name = "load_list_prepared")
    private boolean loadListPrepared;
    
    @Column(name = "load_list_shared_at")
    private Instant loadListSharedAt;
    
    @Column(name = "stowage_plan_complete")
    private boolean stowagePlanComplete;
    
    @Column(name = "stowage_plan_completed_at")
    private Instant stowagePlanCompletedAt;
    
    @Column(name = "reconciled")
    private boolean reconciled;
    
    @Column(name = "reconciled_at")
    private Instant reconciledAt;
    
    @Column(name = "total_containers_planned")
    private Integer totalContainersPlanned;
    
    @Column(name = "total_containers_loaded")
    private Integer totalContainersLoaded;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    // Factory method
    public static Vessel create(
            VesselId vesselId,
            String vesselName,
            String imoNumber,
            String voyageNumber,
            String portOfLoading,
            String portOfDischarge,
            Instant eta,
            Instant etd,
            Integer totalContainersPlanned) {
        
        Vessel vessel = new Vessel();
        vessel.vesselId = vesselId;
        vessel.vesselName = vesselName;
        vessel.imoNumber = imoNumber;
        vessel.voyageNumber = voyageNumber;
        vessel.portOfLoading = portOfLoading;
        vessel.portOfDischarge = portOfDischarge;
        vessel.status = VesselStatus.SCHEDULED;
        vessel.estimatedTimeOfArrival = eta;
        vessel.estimatedTimeOfDeparture = etd;
        vessel.totalContainersPlanned = totalContainersPlanned;
        vessel.totalContainersLoaded = 0;
        vessel.loadListPrepared = false;
        vessel.stowagePlanComplete = false;
        vessel.reconciled = false;
        vessel.createdAt = Instant.now();
        vessel.updatedAt = Instant.now();
        
        return vessel;
    }
    
    /**
     * Mark vessel as arrived at port
     */
    public void markArrived() {
        if (this.status != VesselStatus.SCHEDULED) {
            throw new IllegalStateException("Vessel must be in SCHEDULED status to mark as arrived");
        }
        this.status = VesselStatus.ARRIVED;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Prepare and share load list with terminal
     * This is one of the critical conditions for vessel departure
     */
    public void prepareLoadList() {
        if (this.status == VesselStatus.DEPARTED) {
            throw new IllegalStateException("Cannot prepare load list for departed vessel");
        }
        
        this.loadListPrepared = true;
        this.loadListSharedAt = Instant.now();
        this.status = VesselStatus.LOAD_LIST_PREPARED;
        this.updatedAt = Instant.now();
        
        // Emit domain event
        this.domainEvents.add(new LoadListPreparedEvent(
                this.vesselId.getId(),
                this.vesselName,
                this.voyageNumber,
                this.portOfLoading,
                this.totalContainersPlanned,
                this.loadListSharedAt
        ));
    }
    
    /**
     * Complete stowage plan and load instructions
     * This is one of the critical conditions for vessel departure
     */
    public void completeStowagePlan() {
        if (!this.loadListPrepared) {
            throw new IllegalStateException("Load list must be prepared before completing stowage plan");
        }
        
        this.stowagePlanComplete = true;
        this.stowagePlanCompletedAt = Instant.now();
        this.status = VesselStatus.STOWAGE_PLAN_COMPLETE;
        this.updatedAt = Instant.now();
        
        // Emit domain event
        this.domainEvents.add(new StowagePlanCompletedEvent(
                this.vesselId.getId(),
                this.vesselName,
                this.voyageNumber,
                this.stowagePlanCompletedAt
        ));
    }
    
    /**
     * Start loading operations
     */
    public void startLoading() {
        if (!this.stowagePlanComplete) {
            throw new IllegalStateException("Stowage plan must be complete before starting loading");
        }
        
        this.status = VesselStatus.LOADING_IN_PROGRESS;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Load a container onto the vessel
     * This is a critical operation in the vessel loading process
     */
    public void loadContainer(
            ContainerId containerId,
            String bookingReference,
            String stowagePosition,
            Double weightKg,
            String containerType) {
        
        if (this.status == VesselStatus.DEPARTED) {
            throw new IllegalStateException("Cannot load container on departed vessel");
        }
        
        if (!this.stowagePlanComplete) {
            throw new IllegalStateException("Stowage plan must be complete before loading containers");
        }
        
        // Update loading status
        if (this.status != VesselStatus.LOADING_IN_PROGRESS) {
            this.status = VesselStatus.LOADING_IN_PROGRESS;
        }
        
        this.totalContainersLoaded++;
        this.updatedAt = Instant.now();
        
        // Check if loading is complete
        if (this.totalContainersLoaded.equals(this.totalContainersPlanned)) {
            this.status = VesselStatus.LOADING_COMPLETE;
        }
        
        // Emit domain event - CRITICAL for tracking
        this.domainEvents.add(new ContainerLoadedOnVesselEvent(
                containerId.getId(),
                this.vesselId.getId(),
                this.vesselName,
                this.voyageNumber,
                bookingReference,
                stowagePosition,
                weightKg,
                containerType,
                this.portOfLoading,
                this.portOfDischarge,
                Instant.now()
        ));
    }
    
    /**
     * Reconcile vessel - verify all containers are loaded and accounted for
     * This is one of the critical conditions for vessel departure
     */
    public void reconcile() {
        if (!this.stowagePlanComplete) {
            throw new IllegalStateException("Stowage plan must be complete before reconciliation");
        }
        
        if (!this.totalContainersLoaded.equals(this.totalContainersPlanned)) {
            throw new IllegalStateException(
                    String.format("Cannot reconcile: Expected %d containers, but %d loaded",
                            this.totalContainersPlanned, this.totalContainersLoaded)
            );
        }
        
        this.reconciled = true;
        this.reconciledAt = Instant.now();
        this.status = VesselStatus.RECONCILED;
        this.updatedAt = Instant.now();
        
        // Emit domain event
        this.domainEvents.add(new VesselReconciledEvent(
                this.vesselId.getId(),
                this.vesselName,
                this.voyageNumber,
                this.totalContainersLoaded,
                this.reconciledAt
        ));
    }
    
    /**
     * Depart vessel - all conditions must be met
     * CRITICAL: Vessel unable to depart is P1 incident
     */
    public void depart() {
        // Validate all departure conditions
        if (!canDepart()) {
            throw new IllegalStateException(
                    "Vessel cannot depart. Conditions not met: " + getDepartureBlockers()
            );
        }
        
        this.status = VesselStatus.DEPARTED;
        this.actualDepartureTime = Instant.now();
        this.updatedAt = Instant.now();
        
        // Emit domain event - CRITICAL
        this.domainEvents.add(new VesselDepartedEvent(
                this.vesselId.getId(),
                this.vesselName,
                this.imoNumber,
                this.voyageNumber,
                this.portOfLoading,
                this.portOfDischarge,
                this.totalContainersLoaded,
                this.actualDepartureTime,
                this.estimatedTimeOfArrival
        ));
    }
    
    /**
     * Update ETA
     */
    public void updateETA(Instant newETA, String reason) {
        Instant previousETA = this.estimatedTimeOfArrival;
        this.estimatedTimeOfArrival = newETA;
        this.updatedAt = Instant.now();
        
        // Emit domain event
        this.domainEvents.add(new ETAUpdatedEvent(
                this.vesselId.getId(),
                this.vesselName,
                this.voyageNumber,
                previousETA,
                newETA,
                reason,
                Instant.now()
        ));
    }
    
    /**
     * Check if vessel can depart
     * All conditions must be met for departure
     */
    public boolean canDepart() {
        return this.loadListPrepared
                && this.stowagePlanComplete
                && this.totalContainersLoaded.equals(this.totalContainersPlanned)
                && this.reconciled
                && this.status != VesselStatus.DEPARTED
                && this.status != VesselStatus.BLOCKED;
    }
    
    /**
     * Get list of departure blockers
     */
    public String getDepartureBlockers() {
        List<String> blockers = new ArrayList<>();
        
        if (!this.loadListPrepared) {
            blockers.add("Load list not prepared");
        }
        if (!this.stowagePlanComplete) {
            blockers.add("Stowage plan not complete");
        }
        if (!this.totalContainersLoaded.equals(this.totalContainersPlanned)) {
            blockers.add(String.format("Container mismatch: %d loaded, %d planned",
                    this.totalContainersLoaded, this.totalContainersPlanned));
        }
        if (!this.reconciled) {
            blockers.add("Vessel not reconciled");
        }
        if (this.status == VesselStatus.BLOCKED) {
            blockers.add("Vessel is blocked");
        }
        
        return String.join(", ", blockers);
    }
    
    /**
     * Block vessel operations due to issues
     */
    public void block(String reason) {
        this.status = VesselStatus.BLOCKED;
        this.updatedAt = Instant.now();
        // In production, this should trigger P1 alert
    }
    
    /**
     * Get and clear domain events
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(new ArrayList<>(domainEvents));
    }
    
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}

// Made with Bob
