package com.freightforwarder.vesseloperations.infrastructure.persistence;

import com.freightforwarder.vesseloperations.domain.model.ContainerId;
import com.freightforwarder.vesseloperations.domain.model.LoadedContainer;
import com.freightforwarder.vesseloperations.domain.model.VesselId;
import com.freightforwarder.vesseloperations.domain.repository.LoadedContainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter that implements domain LoadedContainerRepository using JPA
 */
@Component
@RequiredArgsConstructor
public class LoadedContainerRepositoryAdapter implements LoadedContainerRepository {
    
    private final JpaLoadedContainerRepository jpaLoadedContainerRepository;
    
    @Override
    public LoadedContainer save(LoadedContainer container) {
        return jpaLoadedContainerRepository.save(container);
    }
    
    @Override
    public Optional<LoadedContainer> findByContainerId(ContainerId containerId) {
        return jpaLoadedContainerRepository.findByContainerId(containerId);
    }
    
    @Override
    public List<LoadedContainer> findByVesselId(VesselId vesselId) {
        return jpaLoadedContainerRepository.findByVesselId(vesselId);
    }
    
    @Override
    public Integer countByVesselId(VesselId vesselId) {
        return jpaLoadedContainerRepository.countByVesselId(vesselId);
    }
    
    @Override
    public boolean existsByContainerId(ContainerId containerId) {
        return jpaLoadedContainerRepository.existsByContainerId(containerId);
    }
}

// Made with Bob
