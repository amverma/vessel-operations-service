package com.freightforwarder.vesseloperations.application.usecase;

import com.freightforwarder.vesseloperations.application.port.output.EventPublisher;
import com.freightforwarder.vesseloperations.domain.event.DomainEvent;
import com.freightforwarder.vesseloperations.domain.model.Vessel;
import com.freightforwarder.vesseloperations.domain.model.VesselId;
import com.freightforwarder.vesseloperations.domain.repository.VesselRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Use case for vessel departure
 * CRITICAL: Vessel unable to depart is a P1 incident
 * This use case validates all departure conditions before allowing departure
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepartVesselUseCase {
    
    private final VesselRepository vesselRepository;
    private final EventPublisher eventPublisher;
    
    /**
     * Depart a vessel after validating all conditions
     * CRITICAL: All conditions must be met for departure
     */
    @Transactional
    @CircuitBreaker(name = "vesselOperations", fallbackMethod = "departVesselFallback")
    @Retry(name = "vesselOperations")
    public DepartVesselResult execute(DepartVesselCommand command) {
        log.info("Attempting to depart vessel: {}", command.getVesselId());
        
        // Find vessel
        Vessel vessel = vesselRepository.findById(VesselId.of(command.getVesselId()))
                .orElseThrow(() -> new VesselNotFoundException(
                        "Vessel not found: " + command.getVesselId()));
        
        // Check if vessel can depart
        if (!vessel.canDepart()) {
            String blockers = vessel.getDepartureBlockers();
            log.error("CRITICAL: Vessel {} cannot depart. Blockers: {}. " +
                    "This is a P1 incident!", command.getVesselId(), blockers);
            throw new VesselCannotDepartException(
                    "Vessel cannot depart. Conditions not met: " + blockers);
        }
        
        // Depart vessel (domain logic)
        vessel.depart();
        
        // Persist changes
        vesselRepository.save(vessel);
        
        // Publish domain events - CRITICAL
        for (DomainEvent event : vessel.getDomainEvents()) {
            try {
                eventPublisher.publishWithRetry(event);
                log.info("Published event: {} for vessel {}", 
                        event.getEventType(), command.getVesselId());
            } catch (Exception e) {
                log.error("CRITICAL: Failed to publish VesselDeparted event for vessel {}. " +
                        "This is a P1 incident! Downstream services will not be notified!", 
                        command.getVesselId(), e);
                // Trigger P1 alert
                throw new EventPublishException("Failed to publish critical departure event", e);
            }
        }
        vessel.clearDomainEvents();
        
        log.info("Successfully departed vessel {}. Departure time: {}", 
                command.getVesselId(), vessel.getActualDepartureTime());
        
        return new DepartVesselResult(
                vessel.getVesselId().getId(),
                vessel.getVesselName(),
                vessel.getVoyageNumber(),
                vessel.getActualDepartureTime(),
                vessel.getTotalContainersLoaded(),
                vessel.getStatus().name()
        );
    }
    
    /**
     * Fallback method for circuit breaker
     */
    private DepartVesselResult departVesselFallback(
            DepartVesselCommand command, Exception e) {
        log.error("CRITICAL: Circuit breaker activated for vessel departure {}. " +
                "This is a P1 incident! Operations are halted!", command.getVesselId(), e);
        // Trigger P1 alert
        throw new VesselOperationFailedException(
                "Failed to depart vessel due to system failure", e);
    }
    
    // Command and Result DTOs
    @lombok.Value
    public static class DepartVesselCommand {
        String vesselId;
    }
    
    @lombok.Value
    public static class DepartVesselResult {
        String vesselId;
        String vesselName;
        String voyageNumber;
        Instant departureTime;
        Integer totalContainersLoaded;
        String vesselStatus;
    }
    
    // Exceptions
    public static class VesselNotFoundException extends RuntimeException {
        public VesselNotFoundException(String message) {
            super(message);
        }
    }
    
    public static class VesselCannotDepartException extends RuntimeException {
        public VesselCannotDepartException(String message) {
            super(message);
        }
    }
    
    public static class EventPublishException extends RuntimeException {
        public EventPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class VesselOperationFailedException extends RuntimeException {
        public VesselOperationFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

// Made with Bob
// Added a comment to test Resilience Checker script with PR
