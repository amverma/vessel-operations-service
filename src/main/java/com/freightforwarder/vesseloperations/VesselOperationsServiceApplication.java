package com.freightforwarder.vesseloperations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for Vessel Operations Service
 * 
 * Mission-Critical Service (P1)
 * - RTO/RPO: 2 minutes
 * - Manages vessel loading and departure operations
 * - Vessel unable to depart is considered highest severity incident
 */
@SpringBootApplication
@EnableKafka
@EnableTransactionManagement
public class VesselOperationsServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(VesselOperationsServiceApplication.class, args);
    }
}

// Made with Bob
// Resilence Check Test
// Resilence Check Test-1
