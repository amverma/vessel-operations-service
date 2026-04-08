# R1-R20 Resilience Checker - Quick Reference

**Repository:** vessel-operations-service | **IBM GHE:** https://github.ibm.com/Amit-Kumar-Verma/vessel-operations-service.git

## 🚀 Quick Start

The resilience checker runs automatically on every PR. No action needed unless issues are found.

## ✅ Passing the Check

### Most Common Issues & Quick Fixes

#### 1. Circuit Breaker Missing (R1) - CRITICAL

```java
// ❌ Before
@Service
public class ExternalService {
    public Data callApi() {
        return restTemplate.getForObject(url, Data.class);
    }
}

// ✅ After
@Service
public class ExternalService {
    @CircuitBreaker(name = "externalApi", fallbackMethod = "fallback")
    public Data callApi() {
        return restTemplate.getForObject(url, Data.class);
    }
    
    private Data fallback(Exception e) {
        return Data.empty();
    }
}
```

#### 2. Timeout Not Configured (R3) - CRITICAL

```yaml
# application.yml
resilience4j:
  timelimiter:
    instances:
      externalApi:
        timeout-duration: 3s
        cancel-running-future: true
```

#### 3. Health Checks Missing (R6) - CRITICAL

```yaml
# Kubernetes deployment
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 20
  periodSeconds: 5
```

#### 4. Graceful Shutdown Missing (R7) - CRITICAL

```java
@Component
public class KafkaConsumerService {
    
    @KafkaListener(topics = "my-topic")
    public void consume(String message) {
        // Process message
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down gracefully...");
        // Cleanup resources
    }
}
```

#### 5. Idempotency Not Implemented (R8) - CRITICAL

```java
// ❌ Before
@PostMapping("/orders")
public Order createOrder(@RequestBody OrderRequest request) {
    return orderService.create(request);
}

// ✅ After
@PostMapping("/orders")
public Order createOrder(
    @RequestBody OrderRequest request,
    @RequestHeader("Idempotency-Key") String idempotencyKey) {
    
    return orderService.createIdempotent(request, idempotencyKey);
}
```

#### 6. Kafka Consumer Without DLQ (R9) - HIGH

```yaml
# application.yml
spring:
  kafka:
    consumer:
      properties:
        spring.kafka.listener.dead-letter-publishing-recoverer.enabled: true
    listener:
      error-handler: dead-letter-publishing-recoverer
```

#### 7. Metrics Missing (R12) - CRITICAL

```java
@Service
public class OrderService {
    private final MeterRegistry meterRegistry;
    
    @Timed(value = "order.create", description = "Time to create order")
    public Order create(OrderRequest request) {
        meterRegistry.counter("orders.created").increment();
        // Create order
    }
}
```

#### 8. Schema Evolution Not Configured (R13) - CRITICAL

```yaml
# application.yml
spring:
  kafka:
    properties:
      schema.registry.url: ${SCHEMA_REGISTRY_URL}
      value.subject.name.strategy: io.confluent.kafka.serializers.subject.TopicRecordNameStrategy
      auto.register.schemas: false
      use.latest.version: true
```

#### 9. Kafka Manual Commit Missing (R14) - CRITICAL

```java
@KafkaListener(
    topics = "my-topic",
    containerFactory = "kafkaListenerContainerFactory"
)
public void consume(
    @Payload String message,
    Acknowledgment acknowledgment) {
    
    try {
        processMessage(message);
        acknowledgment.acknowledge(); // Manual commit
    } catch (Exception e) {
        // Handle error
    }
}
```

#### 10. K8s Resource Limits Missing (R16) - CRITICAL

```yaml
# deployment.yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

#### 11. Hardcoded Secrets (R19) - CRITICAL

```java
// ❌ Never do this
String password = "myPassword123";
String apiKey = "sk-1234567890abcdef";

// ✅ Always use environment variables or secrets
@Value("${db.password}")
private String password;

@Value("${api.key}")
private String apiKey;
```

## 🔧 Configuration Files

### Resilience4j Configuration Template

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      externalApi:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
        
  retry:
    instances:
      externalApi:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
        
  timelimiter:
    instances:
      externalApi:
        timeout-duration: 3s
        
  bulkhead:
    instances:
      externalApi:
        max-concurrent-calls: 10
        max-wait-duration: 0
```

### Kafka Consumer Configuration Template

```yaml
spring:
  kafka:
    consumer:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
      group-id: ${CONSUMER_GROUP_ID}
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
      properties:
        schema.registry.url: ${SCHEMA_REGISTRY_URL}
        specific.avro.reader: true
    listener:
      ack-mode: manual
      concurrency: 3
```

### Kubernetes Deployment Template

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: vessel-operations-service
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: app
        image: vessel-operations-service:latest
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
        lifecycle:
          preStop:
            exec:
              command: ["/bin/sh", "-c", "sleep 15"]
        env:
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: password
---
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: vessel-operations-service-pdb
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: vessel-operations-service
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: vessel-operations-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: vessel-operations-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

## 🚫 Bypassing Checks

**⚠️ Requires Architecture Board Approval**

1. Add label: `bypass-resilience-check`
2. Create ADR in `docs/adr/`
3. Wait for Architecture Board review
4. Minimum 2 approvals required

### ADR Template

```markdown
# ADR-XXX: Bypass Resilience Check for [Feature/Issue]

## Status
Proposed

## Context
[Explain why bypass is needed]

## Decision
[What check(s) to bypass and why]

## Consequences
[Impact and mitigation plan]

## Alternatives Considered
[Other options evaluated]
```

## 📊 Severity Levels

| Level | Impact | Action |
|-------|--------|--------|
| 🔴 CRITICAL | Blocks PR merge | Must fix |
| 🟠 HIGH | Warning | Should fix |
| 🟡 MEDIUM | Info | Recommended |
| 🟢 LOW | Info | Nice to have |

## 🔍 Local Testing

```bash
# Install dependencies
pip install pyyaml requests jinja2 gitpython

# Run checker locally
python .github/scripts/resilience_checker.py \
  --repo-path . \
  --output-format json \
  --output-file resilience-report.json \
  --severity-threshold CRITICAL

# View results
cat resilience-report.json | jq '.failed_checks'
```

## 📚 Full Documentation

- **Complete Guide**: [README-RESILIENCE-CHECKER.md](README-RESILIENCE-CHECKER.md)
- **Setup Instructions**: [SETUP-GUIDE.md](SETUP-GUIDE.md)
- **R1-R20 Checklist**: `docs/R1-R20-RESILIENCE-CHECKLIST.md`

## 💬 Getting Help

- **Questions**: Contact @architecture-board
- **Issues**: Create issue with label `resilience-checker`
- **IBM GHE Support**: IBM GitHub Enterprise support team

## 🎯 Pro Tips

1. **Run locally first** - Catch issues before pushing
2. **Fix incrementally** - Don't try to fix everything at once
3. **Ask for help** - Architecture Board is here to help
4. **Document exceptions** - Always create ADR for bypasses
5. **Learn patterns** - Understanding R1-R20 makes you a better developer

---

**Made with Bob** 🤖