package com.freightforwarder.vesseloperations.infrastructure.persistence;

import com.freightforwarder.vesseloperations.domain.model.ContainerId;
import com.freightforwarder.vesseloperations.domain.model.LoadedContainer;
import com.freightforwarder.vesseloperations.domain.model.VesselId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for LoadedContainer entity
 */
@Repository
public interface JpaLoadedContainerRepository extends JpaRepository<LoadedContainer, Long> {
    
    Optional<LoadedContainer> findByContainerId(ContainerId containerId);
    
    List<LoadedContainer> findByVesselId(VesselId vesselId);
    
    Integer countByVesselId(VesselId vesselId);
    
    boolean existsByContainerId(ContainerId containerId);
}

// Made with Bob
