package com.freightforwarder.vesseloperations.infrastructure.kafka;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves Kafka topic names based on event types
 */
@Component
public class KafkaTopicResolver {
    
    private static final Map<String, String> EVENT_TOPIC_MAP = new HashMap<>();
    
    static {
        // Map event types to Kafka topics
        EVENT_TOPIC_MAP.put("ContainerLoadedOnVessel", "vessel-operations.container-loaded");
        EVENT_TOPIC_MAP.put("VesselDeparted", "vessel-operations.vessel-departed");
        EVENT_TOPIC_MAP.put("VesselReconciled", "vessel-operations.vessel-reconciled");
        EVENT_TOPIC_MAP.put("ETAUpdated", "vessel-operations.eta-updated");
        EVENT_TOPIC_MAP.put("LoadListPrepared", "vessel-operations.load-list-prepared");
        EVENT_TOPIC_MAP.put("StowagePlanCompleted", "vessel-operations.stowage-plan-completed");
    }
    
    public String resolveTopic(String eventType) {
        return EVENT_TOPIC_MAP.getOrDefault(eventType, "vessel-operations.domain-events");
    }
}

// Made with Bob
