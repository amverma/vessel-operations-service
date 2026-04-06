package com.freightforwarder.vesseloperations.application.usecase;

import com.freightforwarder.vesseloperations.application.port.output.EventPublisher;
import com.freightforwarder.vesseloperations.domain.event.DomainEvent;
import com.freightforwarder.vesseloperations.domain.model.ContainerId;
import com.freightforwarder.vesseloperations.domain.model.LoadedContainer;
import com.freightforwarder.vesseloperations.domain.model.Vessel;
import com.freightforwarder.vesseloperations.domain.model.VesselId;
import com.freightforwarder.vesseloperations.domain.repository.LoadedContainerRepository;
import com.freightforwarder.vesseloperations.domain.repository.VesselRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for loading a container onto a vessel
 * CRITICAL: This operation must be reliable as it affects vessel departure
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoadContainerOnVesselUseCase {
    
    private final VesselRepository vesselRepository;
    private final LoadedContainerRepository loadedContainerRepository;
    private final EventPublisher eventPublisher;
    
    /**
     * Load a container onto a vessel
     * This is a critical operation with circuit breaker and retry patterns
     */
    @Transactional
    @CircuitBreaker(name = "vesselOperations", fallbackMethod = "loadContainerFallback")
    @Retry(name = "vesselOperations")
    public LoadContainerResult execute(LoadContainerCommand command) {
        log.info("Loading container {} onto vessel {}", 
                command.getContainerId(), command.getVesselId());
        
        // Validate vessel exists
        Vessel vessel = vesselRepository.findById(VesselId.of(command.getVesselId()))
                .orElseThrow(() -> new VesselNotFoundException(
                        "Vessel not found: " + command.getVesselId()));
        
        // Check if container is already loaded
        ContainerId containerId = ContainerId.of(command.getContainerId());
        if (loadedContainerRepository.existsByContainerId(containerId)) {
            throw new ContainerAlreadyLoadedException(
                    "Container already loaded: " + command.getContainerId());
        }
        
        // Load container on vessel (domain logic)
        vessel.loadContainer(
                containerId,
                command.getBookingReference(),
                command.getStowagePosition(),
                command.getWeightKg(),
                command.getContainerType()
        );
        
        // Create loaded container entity
        LoadedContainer loadedContainer = LoadedContainer.create(
                containerId,
                vessel.getVesselId(),
                command.getBookingReference(),
                command.getStowagePosition(),
                command.getWeightKg(),
                command.getContainerType()
        );
        
        // Persist changes
        vesselRepository.save(vessel);
        loadedContainerRepository.save(loadedContainer);
        
        // Publish domain events - CRITICAL
        for (DomainEvent event : vessel.getDomainEvents()) {
            try {
                eventPublisher.publishWithRetry(event);
                log.info("Published event: {} for container {}", 
                        event.getEventType(), command.getContainerId());
            } catch (Exception e) {
                log.error("CRITICAL: Failed to publish event {} for container {}. " +
                        "This may result in P1 incident!", 
                        event.getEventType(), command.getContainerId(), e);
                // In production, trigger alert here
                throw new EventPublishException("Failed to publish critical event", e);
            }
        }
        vessel.clearDomainEvents();
        
        log.info("Successfully loaded container {} onto vessel {}. " +
                "Total loaded: {}/{}", 
                command.getContainerId(), 
                command.getVesselId(),
                vessel.getTotalContainersLoaded(),
                vessel.getTotalContainersPlanned());
        
        return new LoadContainerResult(
                loadedContainer.getId(),
                vessel.getVesselId().getId(),
                vessel.getTotalContainersLoaded(),
                vessel.getTotalContainersPlanned(),
                vessel.getStatus().name()
        );
    }
    
    /**
     * Fallback method for circuit breaker
     */
    private LoadContainerResult loadContainerFallback(
            LoadContainerCommand command, Exception e) {
        log.error("CRITICAL: Circuit breaker activated for loading container {} onto vessel {}. " +
                "This is a P1 incident!", command.getContainerId(), command.getVesselId(), e);
        // Trigger P1 alert
        throw new VesselOperationFailedException(
                "Failed to load container due to system failure", e);
    }
    
    // Command and Result DTOs
    @lombok.Value
    public static class LoadContainerCommand {
        String containerId;
        String vesselId;
        String bookingReference;
        String stowagePosition;
        Double weightKg;
        String containerType;
    }
    
    @lombok.Value
    public static class LoadContainerResult {
        Long loadedContainerId;
        String vesselId;
        Integer totalContainersLoaded;
        Integer totalContainersPlanned;
        String vesselStatus;
    }
    
    // Exceptions
    public static class VesselNotFoundException extends RuntimeException {
        public VesselNotFoundException(String message) {
            super(message);
        }
    }
    
    public static class ContainerAlreadyLoadedException extends RuntimeException {
        public ContainerAlreadyLoadedException(String message) {
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
