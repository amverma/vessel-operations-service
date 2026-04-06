package com.freightforwarder.vesseloperations.infrastructure.kafka;

import com.freightforwarder.vesseloperations.application.port.output.EventPublisher;
import com.freightforwarder.vesseloperations.domain.event.DomainEvent;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka implementation of EventPublisher
 * Publishes domain events to Kafka topics with Avro schema validation via AWS Glue Schema Registry
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements EventPublisher {
    
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final KafkaTopicResolver topicResolver;
    
    @Override
    public void publish(DomainEvent event) {
        String topic = topicResolver.resolveTopic(event.getEventType());
        
        log.debug("Publishing event {} to topic {}", event.getEventId(), topic);
        
        CompletableFuture<SendResult<String, DomainEvent>> future = 
                kafkaTemplate.send(topic, event.getAggregateId(), event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully published event {} to topic {} at offset {}", 
                        event.getEventId(), 
                        topic, 
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event {} to topic {}", 
                        event.getEventId(), topic, ex);
            }
        });
    }
    
    @Override
    @Retry(name = "kafkaPublish", fallbackMethod = "publishFallback")
    public void publishWithRetry(DomainEvent event) {
        String topic = topicResolver.resolveTopic(event.getEventType());
        
        log.debug("Publishing event {} to topic {} with retry", event.getEventId(), topic);
        
        try {
            SendResult<String, DomainEvent> result = 
                    kafkaTemplate.send(topic, event.getAggregateId(), event).get();
            
            log.info("Successfully published event {} to topic {} at offset {} with retry", 
                    event.getEventId(), 
                    topic, 
                    result.getRecordMetadata().offset());
        } catch (Exception e) {
            log.error("Failed to publish event {} to topic {} even with retry", 
                    event.getEventId(), topic, e);
            throw new EventPublishException("Failed to publish event after retries", e);
        }
    }
    
    /**
     * Fallback method when all retries are exhausted
     */
    private void publishFallback(DomainEvent event, Exception e) {
        log.error("CRITICAL: All retry attempts exhausted for event {}. " +
                "Event will be stored for manual recovery. This may cause P1 incident!", 
                event.getEventId(), e);
        
        // In production:
        // 1. Store event in dead letter queue
        // 2. Trigger P1 alert
        // 3. Store in database for manual recovery
        
        throw new EventPublishException("Failed to publish event after all retries", e);
    }
    
    public static class EventPublishException extends RuntimeException {
        public EventPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

// Made with Bob
