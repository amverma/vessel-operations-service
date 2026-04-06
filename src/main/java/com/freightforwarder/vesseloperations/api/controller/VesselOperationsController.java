package com.freightforwarder.vesseloperations.api.controller;

import com.freightforwarder.vesseloperations.api.dto.LoadContainerRequest;
import com.freightforwarder.vesseloperations.api.dto.LoadContainerResponse;
import com.freightforwarder.vesseloperations.api.dto.VesselDepartureResponse;
import com.freightforwarder.vesseloperations.application.usecase.DepartVesselUseCase;
import com.freightforwarder.vesseloperations.application.usecase.LoadContainerOnVesselUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for vessel operations
 * Provides endpoints for loading containers and departing vessels
 */
@RestController
@RequestMapping("/api/v1/vessel-operations")
@RequiredArgsConstructor
@Slf4j
public class VesselOperationsController {
    
    private final LoadContainerOnVesselUseCase loadContainerOnVesselUseCase;
    private final DepartVesselUseCase departVesselUseCase;
    
    /**
     * Load a container onto a vessel
     * POST /api/v1/vessel-operations/containers/load
     */
    @PostMapping("/containers/load")
    public ResponseEntity<LoadContainerResponse> loadContainer(
            @Valid @RequestBody LoadContainerRequest request) {
        
        log.info("Received request to load container {} onto vessel {}", 
                request.getContainerId(), request.getVesselId());
        
        try {
            LoadContainerOnVesselUseCase.LoadContainerCommand command = 
                    new LoadContainerOnVesselUseCase.LoadContainerCommand(
                            request.getContainerId(),
                            request.getVesselId(),
                            request.getBookingReference(),
                            request.getStowagePosition(),
                            request.getWeightKg(),
                            request.getContainerType()
                    );
            
            LoadContainerOnVesselUseCase.LoadContainerResult result = 
                    loadContainerOnVesselUseCase.execute(command);
            
            LoadContainerResponse response = new LoadContainerResponse(
                    result.getLoadedContainerId(),
                    result.getVesselId(),
                    request.getContainerId(),
                    result.getTotalContainersLoaded(),
                    result.getTotalContainersPlanned(),
                    result.getVesselStatus(),
                    "Container loaded successfully"
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (LoadContainerOnVesselUseCase.VesselNotFoundException e) {
            log.error("Vessel not found: {}", request.getVesselId(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            
        } catch (LoadContainerOnVesselUseCase.ContainerAlreadyLoadedException e) {
            log.error("Container already loaded: {}", request.getContainerId(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
            
        } catch (Exception e) {
            log.error("CRITICAL: Failed to load container {} onto vessel {}. " +
                    "This may result in P1 incident!", 
                    request.getContainerId(), request.getVesselId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Depart a vessel
     * POST /api/v1/vessel-operations/vessels/{vesselId}/depart
     */
    @PostMapping("/vessels/{vesselId}/depart")
    public ResponseEntity<VesselDepartureResponse> departVessel(
            @PathVariable String vesselId) {
        
        log.info("Received request to depart vessel {}", vesselId);
        
        try {
            DepartVesselUseCase.DepartVesselCommand command = 
                    new DepartVesselUseCase.DepartVesselCommand(vesselId);
            
            DepartVesselUseCase.DepartVesselResult result = 
                    departVesselUseCase.execute(command);
            
            VesselDepartureResponse response = new VesselDepartureResponse(
                    result.getVesselId(),
                    result.getVesselName(),
                    result.getVoyageNumber(),
                    result.getDepartureTime(),
                    result.getTotalContainersLoaded(),
                    result.getVesselStatus(),
                    "Vessel departed successfully"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (DepartVesselUseCase.VesselNotFoundException e) {
            log.error("Vessel not found: {}", vesselId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            
        } catch (DepartVesselUseCase.VesselCannotDepartException e) {
            log.error("CRITICAL: Vessel {} cannot depart. This is a P1 incident! " +
                    "Reason: {}", vesselId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                    .body(new VesselDepartureResponse(
                            vesselId, null, null, null, null, null, 
                            "Vessel cannot depart: " + e.getMessage()
                    ));
            
        } catch (Exception e) {
            log.error("CRITICAL: Failed to depart vessel {}. This is a P1 incident!", 
                    vesselId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Health check endpoint
     * GET /api/v1/vessel-operations/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Vessel Operations Service is running");
    }
}

// Made with Bob
