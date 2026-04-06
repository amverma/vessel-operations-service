package com.freightforwarder.vesseloperations.infrastructure.kafka;

import com.freightforwarder.vesseloperations.application.usecase.HandleContainerGatedInEventUseCase;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka event consumer for handling events from other services
 * Part of the process choreography pattern
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventConsumer {
    
    private final HandleContainerGatedInEventUseCase handleContainerGatedInEventUseCase;
    
    /**
     * Consume ContainerGatedIn events from Port Operations Service
     * This is part of the vessel loading choreography
     */
    @KafkaListener(
            topics = "${kafka.topics.container-gated-in}",
            groupId = "${kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @CircuitBreaker(name = "kafkaConsumer")
    public void consumeContainerGatedInEvent(
            @Payload HandleContainerGatedInEventUseCase.ContainerGatedInEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        log.info("Received ContainerGatedIn event for container {} from topic {} " +
                "partition {} offset {}", 
                event.getContainerId(), topic, partition, offset);
        
        try {
            handleContainerGatedInEventUseCase.execute(event);
            
            // Acknowledge message after successful processing
            acknowledgment.acknowledge();
            
            log.info("Successfully processed ContainerGatedIn event for container {}", 
                    event.getContainerId());
            
        } catch (Exception e) {
            log.error("Failed to process ContainerGatedIn event for container {}. " +
                    "Event will be retried.", event.getContainerId(), e);
            // Don't acknowledge - message will be redelivered
            throw e;
        }
    }
    
    /**
     * Fallback method for circuit breaker
     */
    public void consumeContainerGatedInEventFallback(
            HandleContainerGatedInEventUseCase.ContainerGatedInEvent event,
            String topic,
            int partition,
            long offset,
            Acknowledgment acknowledgment,
            Exception e) {
        
        log.error("CRITICAL: Circuit breaker activated for consuming ContainerGatedIn event. " +
                "Container {} may not be loaded on vessel!", event.getContainerId(), e);
        
        // In production:
        // 1. Store event in dead letter queue
        // 2. Trigger alert
        // 3. Manual intervention may be required
    }
}

// Made with Bob
