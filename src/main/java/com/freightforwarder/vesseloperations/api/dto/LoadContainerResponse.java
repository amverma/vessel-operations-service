package com.freightforwarder.vesseloperations.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for container loading operation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoadContainerResponse {
    
    private Long loadedContainerId;
    private String vesselId;
    private String containerId;
    private Integer totalContainersLoaded;
    private Integer totalContainersPlanned;
    private String vesselStatus;
    private String message;
}

// Made with Bob
