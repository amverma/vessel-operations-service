package com.freightforwarder.vesseloperations.application.usecase;

import com.freightforwarder.vesseloperations.domain.model.VesselId;
import com.freightforwarder.vesseloperations.domain.repository.VesselRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for handling ContainerGatedIn events from Port Operations Service
 * Part of the process choreography
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HandleContainerGatedInEventUseCase {
    
    private final VesselRepository vesselRepository;
    
    /**
     * Handle ContainerGatedIn event
     * This event indicates a container has arrived at the port and is ready for loading
     */
    @Transactional
    public void execute(ContainerGatedInEvent event) {
        log.info("Handling ContainerGatedIn event for container {} on vessel {}", 
                event.getContainerId(), event.getVesselId());
        
        // Verify vessel exists and is ready for loading
        vesselRepository.findById(VesselId.of(event.getVesselId()))
                .ifPresentOrElse(
                    vessel -> {
                        log.info("Container {} is ready for loading on vessel {}. " +
                                "Current status: {}", 
                                event.getContainerId(), 
                                event.getVesselId(),
                                vessel.getStatus());
                        
                        // In a real implementation, this might trigger:
                        // - Update vessel loading queue
                        // - Notify terminal operations
                        // - Update stowage plan
                    },
                    () -> log.warn("Vessel {} not found for container {}", 
                            event.getVesselId(), event.getContainerId())
                );
    }
    
    // Event DTO
    @lombok.Value
    public static class ContainerGatedInEvent {
        String containerId;
        String vesselId;
        String bookingReference;
        String portCode;
        String gatedInTime;
    }
}

// Made with Bob
