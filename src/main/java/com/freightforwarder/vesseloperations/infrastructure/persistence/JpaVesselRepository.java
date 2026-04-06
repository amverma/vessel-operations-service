package com.freightforwarder.vesseloperations.infrastructure.persistence;

import com.freightforwarder.vesseloperations.domain.model.Vessel;
import com.freightforwarder.vesseloperations.domain.model.VesselId;
import com.freightforwarder.vesseloperations.domain.model.VesselStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for Vessel entity
 * Extends Spring Data JPA for database operations
 */
@Repository
public interface JpaVesselRepository extends JpaRepository<Vessel, Long> {
    
    Optional<Vessel> findByVesselId(VesselId vesselId);
    
    Optional<Vessel> findByVoyageNumber(String voyageNumber);
    
    List<Vessel> findByStatus(VesselStatus status);
    
    List<Vessel> findByPortOfLoading(String portOfLoading);
    
    @Query("SELECT v FROM Vessel v WHERE v.loadListPrepared = true " +
           "AND v.stowagePlanComplete = true " +
           "AND v.reconciled = true " +
           "AND v.status = 'RECONCILED'")
    List<Vessel> findReadyToDepart();
    
    boolean existsByVoyageNumber(String voyageNumber);
}

// Made with Bob
