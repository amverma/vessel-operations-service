package com.freightforwarder.vesseloperations.infrastructure.persistence;

import com.freightforwarder.vesseloperations.domain.model.Vessel;
import com.freightforwarder.vesseloperations.domain.model.VesselId;
import com.freightforwarder.vesseloperations.domain.model.VesselStatus;
import com.freightforwarder.vesseloperations.domain.repository.VesselRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter that implements domain VesselRepository using JPA
 * Part of Clean Architecture - adapts infrastructure to domain
 */
@Component
@RequiredArgsConstructor
public class VesselRepositoryAdapter implements VesselRepository {
    
    private final JpaVesselRepository jpaVesselRepository;
    
    @Override
    public Vessel save(Vessel vessel) {
        return jpaVesselRepository.save(vessel);
    }
    
    @Override
    public Optional<Vessel> findById(VesselId vesselId) {
        return jpaVesselRepository.findByVesselId(vesselId);
    }
    
    @Override
    public Optional<Vessel> findByVoyageNumber(String voyageNumber) {
        return jpaVesselRepository.findByVoyageNumber(voyageNumber);
    }
    
    @Override
    public List<Vessel> findByStatus(VesselStatus status) {
        return jpaVesselRepository.findByStatus(status);
    }
    
    @Override
    public List<Vessel> findByPortOfLoading(String portOfLoading) {
        return jpaVesselRepository.findByPortOfLoading(portOfLoading);
    }
    
    @Override
    public List<Vessel> findReadyToDepart() {
        return jpaVesselRepository.findReadyToDepart();
    }
    
    @Override
    public boolean existsByVoyageNumber(String voyageNumber) {
        return jpaVesselRepository.existsByVoyageNumber(voyageNumber);
    }
}

// Made with Bob
