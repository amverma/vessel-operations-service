package com.freightforwarder.vesseloperations.domain.repository;

import com.freightforwarder.vesseloperations.domain.model.Vessel;
import com.freightforwarder.vesseloperations.domain.model.VesselId;
import com.freightforwarder.vesseloperations.domain.model.VesselStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Vessel aggregate
 * Defines the contract for vessel persistence operations
 */
public interface VesselRepository {
    
    /**
     * Save a vessel
     */
    Vessel save(Vessel vessel);
    
    /**
     * Find vessel by ID
     */
    Optional<Vessel> findById(VesselId vesselId);
    
    /**
     * Find vessel by voyage number
     */
    Optional<Vessel> findByVoyageNumber(String voyageNumber);
    
    /**
     * Find vessels by status
     */
    List<Vessel> findByStatus(VesselStatus status);
    
    /**
     * Find vessels by port of loading
     */
    List<Vessel> findByPortOfLoading(String portOfLoading);
    
    /**
     * Find all vessels that can depart
     */
    List<Vessel> findReadyToDepart();
    
    /**
     * Check if vessel exists
     */
    boolean existsByVoyageNumber(String voyageNumber);
}

// Made with Bob
