# R1-R23 Resilience Checklist

This document provides a comprehensive guide to the R1-R23 resilience patterns that are automatically checked by the R1-R23 Resilience Guardrail Checker workflow.

## Overview

The R1-R23 checklist represents 23 critical resilience patterns that every microservice must implement to ensure high availability, fault tolerance, disaster recovery capabilities, and infrastructure best practices. These patterns are based on industry best practices and lessons learned from production incidents.

**Pattern Categories:**
- **R1-R3:** Fault Tolerance (Circuit Breaker, Retry, Timeout)
- **R4-R5:** Resource Management (Bulkhead, Rate Limiting)
- **R6-R7:** Availability (Health Checks, Graceful Shutdown)
- **R8-R9:** Data Integrity (Idempotency, Dead Letter Queue)
- **R10-R12:** Observability (Logging, Tracing, Metrics)
- **R13-R14:** Message Processing (Schema Evolution, Kafka Best Practices)
- **R15:** Database (Connection Pooling)
- **R16-R18:** Kubernetes (Resource Limits, HPA, PDB)
- **R19:** Security (Secrets Management)
- **R20:** Disaster Recovery (DR Testing)
- **R21-R23:** Infrastructure as Code (Terraform, OpenAPI, Helm)

---

## Fault Tolerance Patterns

### R1: Circuit Breaker Pattern
**Severity:** CRITICAL  
**Category:** Fault Tolerance

**Description:**  
Implement circuit breakers for all external service calls to prevent cascading failures. When a service becomes unavailable, the circuit breaker trips and prevents further calls, allowing the failing service to recover.

**Implementation:**
- Use `@CircuitBreaker` annotation from Resilience4j
- Configure failure threshold, wait duration, and permitted calls in half-open state
- Apply to all REST calls, database connections, and message broker interactions

**Example:**
```java
@CircuitBreaker(name = "externalService", fallbackMethod = "fallbackMethod")
public ResponseEntity<Data> callExternalService() {
    // External service call
}
```

**Recommendation:**  
Add @CircuitBreaker annotation from Resilience4j or configure circuit breaker in configuration files.

---

### R2: Retry with Exponential Backoff
**Severity:** HIGH  
**Category:** Fault Tolerance

**Description:**  
Implement retry logic with exponential backoff for transient failures. This prevents overwhelming a recovering service with immediate retries.

**Implementation:**
- Use `@Retry` annotation with exponential backoff configuration
- Set maximum retry attempts (typically 3-5)
- Configure backoff multiplier (e.g., 2x: 1s, 2s, 4s, 8s)
- Implement jitter to prevent thundering herd

**Example:**
```java
@Retry(name = "database", fallbackMethod = "fallbackMethod")
public Data queryDatabase() {
    // Database query
}
```

**Recommendation:**  
Add @Retry annotation with exponential backoff configuration.

---

### R3: Timeout Configuration
**Severity:** CRITICAL  
**Category:** Fault Tolerance

**Description:**  
All external calls must have explicit timeout configurations to prevent thread exhaustion and resource leaks.

**Implementation:**
- Configure connection timeout (time to establish connection)
- Configure read timeout (time to receive response)
- Configure write timeout (time to send request)
- Set appropriate values based on SLA (typically 5-30 seconds)

**Example:**
```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 5000
            readTimeout: 10000
```

**Recommendation:**  
Configure connection-timeout, read-timeout, and write-timeout for all external calls.

---

## Resource Management Patterns

### R4: Bulkhead Pattern
**Severity:** HIGH  
**Category:** Resource Isolation

**Description:**  
Isolate resources to prevent resource exhaustion from affecting the entire system. Use separate thread pools for different operations.

**Implementation:**
- Use `@Bulkhead` annotation to limit concurrent calls
- Configure separate thread pools for different services
- Set appropriate pool sizes based on load testing

**Example:**
```java
@Bulkhead(name = "externalService", type = Bulkhead.Type.THREADPOOL)
public CompletableFuture<Data> callExternalService() {
    // Async external service call
}
```

**Recommendation:**  
Configure @Bulkhead annotation or separate thread pools for different operations.

---

### R5: Rate Limiting
**Severity:** HIGH  
**Category:** Resource Protection

**Description:**  
Implement rate limiting to protect services from overload and ensure fair resource allocation.

**Implementation:**
- Use `@RateLimiter` annotation
- Configure requests per time window
- Implement token bucket or sliding window algorithm
- Return 429 (Too Many Requests) when limit exceeded

**Example:**
```java
@RateLimiter(name = "api")
public ResponseEntity<Data> handleRequest() {
    // Request handling
}
```

**Recommendation:**  
Add @RateLimiter annotation or configure rate limiting middleware.

---

### R15: Database Connection Pooling
**Severity:** HIGH  
**Category:** Resource Management

**Description:**  
Proper database connection pool configuration to prevent connection exhaustion and optimize performance.

**Implementation:**
- Use HikariCP (default in Spring Boot)
- Configure maximum pool size (typically 10-50)
- Set connection timeout and idle timeout
- Configure max lifetime to prevent stale connections

**Example:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**Recommendation:**  
Configure HikariCP with appropriate pool size and timeouts.

---

## Observability Patterns

### R6: Health Checks
**Severity:** CRITICAL  
**Category:** Observability

**Description:**  
Comprehensive health checks for all dependencies including databases, message brokers, and external services.

**Implementation:**
- Implement liveness probe (is the service running?)
- Implement readiness probe (is the service ready to accept traffic?)
- Implement startup probe (has the service started successfully?)
- Create custom health indicators for each dependency

**Example:**
```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // Check database connectivity
        return Health.up().build();
    }
}
```

**Recommendation:**  
Implement liveness, readiness, and startup probes with health indicators for all dependencies.

---

### R10: Structured Logging
**Severity:** HIGH  
**Category:** Observability

**Description:**  
All logs must be structured with correlation IDs for distributed tracing and debugging.

**Implementation:**
- Use JSON logging format
- Include correlation ID in MDC (Mapped Diagnostic Context)
- Log request/response for all external calls
- Include timestamp, service name, and log level

**Example:**
```java
MDC.put("correlationId", UUID.randomUUID().toString());
log.info("Processing request", kv("userId", userId), kv("action", "create"));
```

**Recommendation:**  
Use JSON logging with MDC for correlation IDs.

---

### R11: Distributed Tracing
**Severity:** HIGH  
**Category:** Observability

**Description:**  
Implement distributed tracing for request flow visibility across microservices.

**Implementation:**
- Integrate OpenTelemetry or Spring Cloud Sleuth
- Configure trace sampling rate
- Export traces to Zipkin or Jaeger
- Include trace ID in all logs

**Recommendation:**  
Integrate OpenTelemetry or Spring Cloud Sleuth for distributed tracing.

---

### R12: Metrics and Monitoring
**Severity:** CRITICAL  
**Category:** Observability

**Description:**  
Expose comprehensive metrics for monitoring service health and performance.

**Implementation:**
- Use Micrometer with Prometheus
- Expose /actuator/metrics endpoint
- Track custom business metrics
- Monitor JVM metrics, HTTP metrics, and database metrics

**Example:**
```java
@Timed(value = "api.requests", description = "API request duration")
public ResponseEntity<Data> handleRequest() {
    // Request handling
}
```

**Recommendation:**  
Use Micrometer with Prometheus for metrics collection.

---

## Availability Patterns

### R7: Graceful Shutdown
**Severity:** CRITICAL  
**Category:** Availability

**Description:**  
Services must shutdown gracefully without losing in-flight requests or messages.

**Implementation:**
- Implement `@PreDestroy` hooks
- Stop accepting new requests
- Complete in-flight requests
- Close connections and release resources
- Configure graceful shutdown timeout

**Example:**
```java
@PreDestroy
public void onShutdown() {
    log.info("Shutting down gracefully");
    // Close connections, flush buffers, etc.
}
```

**Recommendation:**  
Implement @PreDestroy hooks and configure graceful shutdown timeout.

---

## Data Integrity Patterns

### R8: Idempotency
**Severity:** CRITICAL  
**Category:** Data Integrity

**Description:**  
All state-changing operations must be idempotent to handle duplicate requests safely.

**Implementation:**
- Use idempotency keys for POST/PUT/DELETE operations
- Implement duplicate detection using unique constraints
- Store processed message IDs to prevent reprocessing
- Return same result for duplicate requests

**Example:**
```java
@Transactional
public void processPayment(String idempotencyKey, PaymentRequest request) {
    if (paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
        return; // Already processed
    }
    // Process payment
}
```

**Recommendation:**  
Implement idempotency keys and duplicate detection mechanisms.

---

### R13: Schema Evolution Strategy
**Severity:** CRITICAL  
**Category:** Data Integrity

**Description:**  
Avro schemas must support backward/forward compatibility for zero-downtime deployments.

**Implementation:**
- Configure schema registry with compatibility mode
- Use BACKWARD, FORWARD, or FULL compatibility
- Add default values for new fields
- Never remove required fields

**Example:**
```json
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "id", "type": "string"},
    {"name": "email", "type": "string"},
    {"name": "phone", "type": ["null", "string"], "default": null}
  ]
}
```

**Recommendation:**  
Configure schema registry with appropriate compatibility mode.

---

## Error Handling Patterns

### R9: Dead Letter Queue (DLQ)
**Severity:** HIGH  
**Category:** Error Handling

**Description:**  
Failed messages must be routed to DLQ for analysis and replay.

**Implementation:**
- Configure DLQ for Kafka consumers
- Implement DeadLetterPublishingRecoverer
- Include error details and original message
- Set up monitoring and alerting for DLQ

**Example:**
```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
    return new DefaultErrorHandler(
        new DeadLetterPublishingRecoverer(template),
        new FixedBackOff(1000L, 3L)
    );
}
```

**Recommendation:**  
Configure DLQ for Kafka consumers and implement DLQ routing for failed messages.

---

## Message Processing Patterns

### R14: Kafka Consumer Best Practices
**Severity:** CRITICAL  
**Category:** Message Processing

**Description:**  
Kafka consumers must follow best practices for reliability and performance.

**Implementation:**
- Use manual offset commit (disable auto-commit)
- Implement proper exception handling
- Configure rebalance listeners
- Set appropriate session timeout and max poll interval
- Use consumer groups for scalability

**Example:**
```java
@KafkaListener(
    topics = "orders",
    groupId = "order-service",
    containerFactory = "kafkaListenerContainerFactory"
)
public void processOrder(@Payload Order order, Acknowledgment ack) {
    try {
        // Process order
        ack.acknowledge(); // Manual commit
    } catch (Exception e) {
        // Error handling
    }
}
```

**Recommendation:**  
Use manual offset commit, proper exception handling, and rebalance listeners.

---

## Kubernetes Patterns

### R16: Kubernetes Resource Limits
**Severity:** CRITICAL  
**Category:** Resource Management

**Description:**  
All pods must have resource requests and limits to prevent resource starvation.

**Implementation:**
- Define CPU and memory requests (guaranteed resources)
- Define CPU and memory limits (maximum resources)
- Set appropriate values based on load testing
- Monitor actual usage and adjust

**Example:**
```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

**Recommendation:**  
Define CPU and memory requests/limits in Kubernetes manifests.

---

### R17: Horizontal Pod Autoscaling (HPA)
**Severity:** HIGH  
**Category:** Scalability

**Description:**  
Configure HPA for automatic scaling based on load.

**Implementation:**
- Create HPA manifest
- Configure min/max replicas
- Set target CPU/memory utilization
- Use custom metrics if needed

**Example:**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: vessel-operations-service
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: vessel-operations-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

**Recommendation:**  
Create HPA manifest with appropriate scaling metrics.

---

### R18: Pod Disruption Budget (PDB)
**Severity:** HIGH  
**Category:** Availability

**Description:**  
Define PDB to ensure availability during disruptions like node maintenance.

**Implementation:**
- Create PDB manifest
- Set minAvailable or maxUnavailable
- Ensure at least one pod is always available
- Consider during rolling updates

**Example:**
```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: vessel-operations-service-pdb
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: vessel-operations-service
```

**Recommendation:**  
Create PDB manifest to maintain minimum available pods.

---

## Security Patterns

### R19: Secrets Management
**Severity:** CRITICAL  
**Category:** Security

**Description:**  
Secrets must never be hardcoded or committed to version control.

**Implementation:**
- Use Kubernetes Secrets or external secret management (Vault, AWS Secrets Manager)
- Use environment variables for configuration
- Rotate secrets regularly
- Encrypt secrets at rest

**Example:**
```yaml
env:
- name: DATABASE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: database-secret
      key: password
```

**Recommendation:**  
Use Kubernetes Secrets, environment variables, or external secret management.

---

## Disaster Recovery Patterns

### R20: Disaster Recovery Testing
**Severity:** CRITICAL  
**Category:** Disaster Recovery

**Description:**  
Regular DR drills and backup verification to ensure recovery capabilities.

**Implementation:**
- Document backup strategy
- Define RTO (Recovery Time Objective) and RPO (Recovery Point Objective)
- Maintain DR runbook
- Conduct regular DR drills
- Test backup restoration

**Documentation Required:**
- Backup schedule and retention policy
- Recovery procedures
- Contact information for DR team
- Escalation procedures

**Recommendation:**  
Document backup strategy, RTO/RPO, and maintain DR runbook.

---

## Infrastructure as Code Patterns

### R21: Terraform State Backend Configuration
**Severity:** HIGH
**Category:** Infrastructure as Code

**Description:**
Terraform state must be stored in a remote backend with locking enabled to prevent state corruption and enable team collaboration.

**Implementation:**
- Configure remote backend (S3, Azure Storage, GCS, or Terraform Cloud)
- Enable state locking (DynamoDB for S3, built-in for Azure/GCS)
- Use separate state files for different environments
- Enable state encryption at rest

**Example:**
```hcl
terraform {
  backend "s3" {
    bucket         = "my-terraform-state"
    key            = "vessel-operations/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "terraform-state-lock"
    encrypt        = true
  }
}
```

**Recommendation:**
Configure S3/Azure Storage backend with state locking (DynamoDB/Azure Storage).

---

### R22: OpenAPI Specification Validation
**Severity:** MEDIUM
**Category:** API Design

**Description:**
API specifications must include comprehensive error responses, rate limiting information, and versioning to ensure robust API contracts.

**Implementation:**
- Define all error responses (4xx and 5xx)
- Document rate limiting headers (X-RateLimit-*)
- Include API versioning in path or headers
- Specify authentication and authorization requirements
- Document all request/response schemas

**Example:**
```yaml
paths:
  /api/v1/vessels:
    get:
      responses:
        '200':
          description: Success
        '400':
          description: Bad Request
        '429':
          description: Too Many Requests
          headers:
            X-RateLimit-Limit:
              schema:
                type: integer
            X-RateLimit-Remaining:
              schema:
                type: integer
        '500':
          description: Internal Server Error
```

**Recommendation:**
Include 4xx/5xx responses, rate limit headers, and API versioning in OpenAPI spec.

---

### R23: Helm Chart Best Practices
**Severity:** MEDIUM
**Category:** Deployment

**Description:**
Helm charts must follow best practices for production deployments including resource management, health checks, and proper configuration.

**Implementation:**
- Define resource requests and limits in values.yaml
- Configure liveness, readiness, and startup probes
- Set appropriate replica count
- Use proper labels and annotations
- Include security context settings
- Document all configurable values

**Example values.yaml:**
```yaml
replicaCount: 3

resources:
  limits:
    cpu: 1000m
    memory: 1Gi
  requests:
    cpu: 500m
    memory: 512Mi

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

**Recommendation:**
Include resource limits, health checks, and proper labels in Helm charts.

---

## Compliance and Enforcement

### Severity Levels

- **CRITICAL**: Must be implemented. Blocks PR merge if failing.
- **HIGH**: Strongly recommended. Should be implemented soon.
- **MEDIUM**: Recommended. Plan for implementation.
- **LOW**: Nice to have. Consider for future improvements.

### Bypass Process

If you need to bypass a check:

1. Add label `bypass-resilience-check` to the PR
2. Architecture Board will be notified automatically
3. Create an ADR (Architecture Decision Record) documenting:
   - Reason for bypass
   - Risk assessment
   - Mitigation plan
   - Timeline for remediation
4. Obtain minimum 2 approvals from Architecture Board
5. Merge only after approval

### Monitoring and Reporting

- All check results are posted as PR comments
- Detailed reports are uploaded as artifacts
- Failed checks are tracked and reported to Architecture Board
- Metrics are collected for compliance reporting

---

## Resources

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
- [Kafka Consumer Best Practices](https://kafka.apache.org/documentation/#consumerconfigs)

## Support

For questions or assistance:
- Review workflow logs in GitHub Actions
- Check resilience report artifacts
- Contact Architecture Board for bypass requests
- Refer to [Branch Protection Setup Guide](../.github/BRANCH-PROTECTION-SETUP.md)

---

**Repository:** vessel-operations-service  
**Workflow:** R1-R20 Resilience Guardrail Checker  
**Version:** 1.0  
**Last Updated:** 2026-04-16