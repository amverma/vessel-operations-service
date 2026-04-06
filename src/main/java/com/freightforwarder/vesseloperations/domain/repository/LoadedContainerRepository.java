package com.freightforwarder.vesseloperations.domain.repository;

import com.freightforwarder.vesseloperations.domain.model.ContainerId;
import com.freightforwarder.vesseloperations.domain.model.LoadedContainer;
import com.freightforwarder.vesseloperations.domain.model.VesselId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for LoadedContainer entity
 */
public interface LoadedContainerRepository {
    
    /**
     * Save a loaded container
     */
    LoadedContainer save(LoadedContainer container);
    
    /**
     * Find container by ID
     */
    Optional<LoadedContainer> findByContainerId(ContainerId containerId);
    
    /**
     * Find all containers loaded on a vessel
     */
    List<LoadedContainer> findByVesselId(VesselId vesselId);
    
    /**
     * Count containers loaded on a vessel
     */
    Integer countByVesselId(VesselId vesselId);
    
    /**
     * Check if container is loaded on any vessel
     */
    boolean existsByContainerId(ContainerId containerId);
}

// Made with Bob
