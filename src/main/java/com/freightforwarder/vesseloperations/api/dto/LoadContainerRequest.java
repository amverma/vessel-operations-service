package com.freightforwarder.vesseloperations.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for loading a container onto a vessel
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoadContainerRequest {
    
    @NotBlank(message = "Container ID is required")
    private String containerId;
    
    @NotBlank(message = "Vessel ID is required")
    private String vesselId;
    
    @NotBlank(message = "Booking reference is required")
    private String bookingReference;
    
    @NotBlank(message = "Stowage position is required")
    private String stowagePosition;
    
    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    private Double weightKg;
    
    @NotBlank(message = "Container type is required")
    private String containerType;
}

// Made with Bob
