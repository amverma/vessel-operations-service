package com.freightforwarder.vesseloperations.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for circuit breaker and retry patterns
 * Critical for mission-critical service with RTO/RPO of 2 minutes
 */
@Configuration
public class ResilienceConfig {
    
    /**
     * Circuit breaker configuration for vessel operations
     * Prevents cascading failures in mission-critical operations
     */
    @Bean
    public CircuitBreakerConfig vesselOperationsCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit if 50% of calls fail
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before trying again
                .slidingWindowSize(10) // Consider last 10 calls
                .minimumNumberOfCalls(5) // Need at least 5 calls before calculating failure rate
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 test calls in half-open state
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }
    
    /**
     * Retry configuration for Kafka operations
     * Ensures reliable event publishing for mission-critical events
     */
    @Bean
    public RetryConfig kafkaPublishRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3) // Retry up to 3 times
                .waitDuration(Duration.ofMillis(500)) // Wait 500ms between retries
                .retryExceptions(Exception.class) // Retry on any exception
                .build();
    }
    
    /**
     * Retry configuration for vessel operations
     * Handles transient failures in critical operations
     */
    @Bean
    public RetryConfig vesselOperationsRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(2) // Retry once
                .waitDuration(Duration.ofMillis(200)) // Wait 200ms before retry
                .retryExceptions(Exception.class)
                .build();
    }
}

// Made with Bob
