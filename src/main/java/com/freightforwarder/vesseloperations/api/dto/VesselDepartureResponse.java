package com.freightforwarder.vesseloperations.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for vessel departure operation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VesselDepartureResponse {
    
    private String vesselId;
    private String vesselName;
    private String voyageNumber;
    private Instant departureTime;
    private Integer totalContainersLoaded;
    private String vesselStatus;
    private String message;
}

// Made with Bob
