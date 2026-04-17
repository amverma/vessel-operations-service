# R1-R20 Resilience Checker - Technical Guide

This document provides a comprehensive technical explanation of how the R1-R20 Resilience Checker works, including its architecture, detection logic, and individual pattern implementations.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Execution Flow](#execution-flow)
4. [Smart Context-Aware Detection](#smart-context-aware-detection)
5. [Individual Pattern Detection Logic](#individual-pattern-detection-logic)
6. [Configuration](#configuration)
7. [Extending the Checker](#extending-the-checker)

---

## Overview

The R1-R20 Resilience Checker is a **static code analysis tool** that automatically validates whether your codebase implements critical resilience patterns. Unlike simple linters, it uses **intelligent, context-aware detection** to only flag issues when patterns are actually needed.

### Key Features

- ✅ **Context-Aware**: Only flags missing patterns when the code actually needs them
- ✅ **Multi-Language**: Supports Java, Kotlin, Python, JavaScript, TypeScript, Go
- ✅ **Multi-Format**: Analyzes code, configuration files, Kubernetes manifests, and schemas
- ✅ **Configurable**: Severity thresholds and check enablement via configuration
- ✅ **CI/CD Integration**: Runs automatically on pull requests via GitHub Actions

---

## Architecture

### Component Structure

```
┌─────────────────────────────────────────────────────────────┐
│                    GitHub Actions Workflow                   │
│  (.github/workflows/resiliencecheck.yml)                    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              ResilienceChecker (Main Class)                  │
│  - Initializes all 20 checks                                │
│  - Scans monitored directories                              │
│  - Runs checks against each file                            │
│  - Generates report                                         │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│           Individual Check Classes (R1-R20)                  │
│  - CircuitBreakerCheck (R1)                                 │
│  - RetryCheck (R2)                                          │
│  - TimeoutCheck (R3)                                        │
│  - ... (17 more checks)                                     │
└─────────────────────────────────────────────────────────────┘
```

### Class Hierarchy

```python
ResilienceCheck (Base Class)
├── Properties:
│   ├── check_id: str (e.g., "R1")
│   ├── name: str (e.g., "Circuit Breaker Pattern")
│   ├── category: str (e.g., "Fault Tolerance")
│   ├── severity: str (CRITICAL, HIGH, MEDIUM, LOW)
│   ├── description: str
│   ├── findings: List[Dict]
│   ├── passed: bool
│   └── recommendation: str
│
└── Methods:
    ├── check(file_path, content) -> bool
    ├── add_finding(file, message, line)
    └── to_dict() -> Dict
```

---

## Execution Flow

### Step-by-Step Process

```
1. TRIGGER
   ├─ Pull Request created/updated
   └─ Files in monitored paths changed

2. CHECKOUT
   ├─ Clone repository
   └─ Fetch base branch for comparison

3. FILE DISCOVERY
   ├─ Scan monitored directories:
   │  ├─ src/**
   │  ├─ k8s/**
   │  ├─ helm/**
   │  ├─ schemas/**
   │  ├─ infrastructure/**
   │  └─ specs/**
   │
   └─ Filter by file extensions:
      ├─ Code: .java, .kt, .py, .js, .ts, .go
      ├─ Config: .yml, .yaml, .json, .properties, .xml
      ├─ Schema: .avsc, .avdl
      └─ Docs: .md, .txt, .adoc

4. CHECK EXECUTION
   ├─ For each file:
   │  ├─ Read file content
   │  └─ Run ALL 23 checks against content
   │
   └─ Each check:
      ├─ Phase 1: Look for pattern implementation
      ├─ Phase 2: Look for code that needs pattern
      └─ Update check state (passed/failed)

5. REPORT GENERATION
   ├─ Aggregate results from all checks
   ├─ Calculate summary by severity
   ├─ Determine overall status (PASS/FAIL)
   └─ Generate JSON report

6. PR COMMENT
   ├─ Format results as markdown
   ├─ Post/update PR comment
   └─ Upload artifacts

7. STATUS CHECK
   ├─ If CRITICAL checks fail: Exit code 1 (blocks merge)
   └─ If all CRITICAL checks pass: Exit code 0 (allows merge)
```

### Detailed Workflow

```python
def run_checks(self) -> Dict[str, Any]:
    """Main execution method"""
    
    # 1. Get all files to check
    files = self._get_files_to_check()
    
    # 2. Initialize all 20 checks
    checks = [
        CircuitBreakerCheck(),
        RetryCheck(),
        TimeoutCheck(),
        # ... 17 more checks
    ]
    
    # 3. Run checks against each file
    for file_path in files:
        content = read_file(file_path)
        
        for check in checks:
            # Each check decides if it's relevant
            check.check(file_path, content)
    
    # 4. Generate report
    return self._generate_report()
```

---

## Smart Context-Aware Detection

### Two-Phase Detection Strategy

Every check uses a **two-phase approach** to avoid false positives:

#### Phase 1: Pattern Implementation Detection
Look for evidence that the pattern is already implemented.

```python
# Example: Circuit Breaker Check
patterns = [
    r'@CircuitBreaker',           # Annotation
    r'CircuitBreakerConfig',      # Configuration class
    r'circuitBreaker\s*\(',       # Method call
    r'circuit-breaker:',          # YAML config
]

if any(re.search(p, content) for p in patterns):
    return PASS  # Pattern is implemented
```

#### Phase 2: Necessity Detection
If pattern not found, check if the code actually needs it.

```python
# Check if code makes external calls
external_call_patterns = [
    r'RestTemplate',
    r'WebClient',
    r'@FeignClient',
    r'HttpClient',
]

has_external_calls = any(re.search(p, content) for p in external_call_patterns)

if has_external_calls:
    return FAIL  # Pattern needed but missing
else:
    return PASS  # Pattern not needed
```

### File Type Awareness

Checks are **file-type specific** to avoid irrelevant checks:

```python
# R13: Schema Evolution - Only checks schema files
if not file_path.endswith(('.avsc', '.avdl')):
    self.passed = True
    return True

# R16-R18: Kubernetes - Only checks K8s manifests
if not (file_path.endswith(('.yml', '.yaml')) and 
        ('k8s' in file_path or 'helm' in file_path)):
    self.passed = True
    return True
```

### Example: Complete Check Logic

```python
class CircuitBreakerCheck(ResilienceCheck):
    def check(self, file_path: str, content: str) -> bool:
        # PHASE 1: Look for circuit breaker implementation
        cb_patterns = [
            r'@CircuitBreaker',
            r'CircuitBreakerConfig',
            r'circuitBreaker\s*\(',
        ]
        
        if any(re.search(p, content, re.IGNORECASE) for p in cb_patterns):
            self.passed = True
            return True  # ✅ Pattern implemented
        
        # PHASE 2: Check if pattern is needed
        external_call_patterns = [
            r'RestTemplate',
            r'WebClient',
            r'@FeignClient',
            r'HttpClient',
        ]
        
        has_external_calls = any(re.search(p, content) for p in external_call_patterns)
        
        if has_external_calls:
            # ❌ Pattern needed but missing
            self.add_finding(
                file_path,
                "External service calls detected without circuit breaker protection"
            )
            self.passed = False
            return False
        
        # ✅ Pattern not needed (no external calls)
        self.passed = True
        return True
```

---

## Individual Pattern Detection Logic

### R1: Circuit Breaker Pattern

**Severity:** CRITICAL  
**Category:** Fault Tolerance

**Detection Logic:**

```python
# Phase 1: Look for circuit breaker
patterns = [
    r'@CircuitBreaker',           # Resilience4j annotation
    r'CircuitBreakerConfig',      # Configuration class
    r'circuitBreaker\s*\(',       # Method call
    r'circuit-breaker:',          # YAML config
    r'circuitbreaker:',           # Alternative YAML
]

# Phase 2: Check if needed
external_call_patterns = [
    r'RestTemplate',              # Spring REST client
    r'WebClient',                 # Spring WebFlux client
    r'@FeignClient',              # Feign client
    r'HttpClient',                # Generic HTTP client
    r'kafkaTemplate\.send',       # Kafka producer
    r'jdbcTemplate\.',            # JDBC operations
]

# Decision
if has_circuit_breaker:
    return PASS
elif has_external_calls:
    return FAIL  # Missing circuit breaker for external calls
else:
    return PASS  # No external calls, not needed
```

**Example Scenarios:**

✅ **PASS**: Has circuit breaker
```java
@CircuitBreaker(name = "externalService")
public Data callService() {
    return restTemplate.getForObject(url, Data.class);
}
```

❌ **FAIL**: External call without circuit breaker
```java
public Data callService() {
    return restTemplate.getForObject(url, Data.class);  // Missing @CircuitBreaker
}
```

✅ **PASS**: No external calls
```java
public Data processData(Data input) {
    return input.transform();  // Pure logic, no external calls
}
```

---

### R2: Retry with Exponential Backoff

**Severity:** HIGH  
**Category:** Fault Tolerance

**Detection Logic:**

```python
# Phase 1: Look for retry implementation
patterns = [
    r'@Retry',                    # Resilience4j annotation
    r'RetryConfig',               # Configuration class
    r'retryTemplate',             # Spring Retry template
    r'exponentialBackoff',        # Backoff strategy
    r'backoff:',                  # YAML config
    r'retry-policy:',             # Alternative config
]

# Phase 2: Check if needed
retry_needed_patterns = [
    r'RestTemplate',
    r'WebClient',
    r'@FeignClient',
    r'kafkaTemplate\.send',
]

# Decision
if has_retry:
    return PASS
elif needs_retry:
    return FAIL  # Missing retry for transient failures
else:
    return PASS
```

---

### R3: Timeout Configuration

**Severity:** CRITICAL  
**Category:** Fault Tolerance

**Detection Logic:**

```python
# Phase 1: Look for timeout configuration
timeout_patterns = [
    r'@TimeLimiter',              # Resilience4j annotation
    r'timeout:',                  # YAML config
    r'connection-timeout:',       # Connection timeout
    r'read-timeout:',             # Read timeout
    r'connectTimeout',            # Java property
    r'readTimeout',               # Java property
    r'requestTimeout',            # Request timeout
    r'\.timeout\(',               # Method call
]

# Phase 2: Check if needed
external_call_patterns = [
    r'RestTemplate',
    r'WebClient',
    r'HttpClient',
    r'kafkaTemplate',
    r'@FeignClient',
]

# Decision
if has_timeout:
    return PASS
elif has_external_calls:
    return FAIL  # Missing timeout for external calls
else:
    return PASS
```

**Why Critical:**
Without timeouts, threads can hang indefinitely, leading to:
- Thread pool exhaustion
- Resource leaks
- Cascading failures
- System unresponsiveness

---

### R4: Bulkhead Pattern

**Severity:** HIGH  
**Category:** Resource Isolation

**Detection Logic:**

```python
# Phase 1: Look for bulkhead implementation
patterns = [
    r'@Bulkhead',                 # Resilience4j annotation
    r'BulkheadConfig',            # Configuration class
    r'ThreadPoolTaskExecutor',    # Spring thread pool
    r'thread-pool:',              # YAML config
    r'bulkhead:',                 # Bulkhead config
]

# Decision
if has_bulkhead:
    return PASS
else:
    # Bulkhead is recommended but not always required
    # Only flag if file is a service class
    if is_service_class(file_path):
        return FAIL
    else:
        return PASS
```

**Note:** This check is more lenient as bulkheads are not always necessary for every service.

---

### R5: Rate Limiting

**Severity:** HIGH  
**Category:** Resource Protection

**Detection Logic:**

```python
# Phase 1: Look for rate limiting
patterns = [
    r'@RateLimiter',              # Resilience4j annotation
    r'RateLimiterConfig',         # Configuration class
    r'rate-limit:',               # YAML config
    r'rateLimit',                 # Property
    r'throttle',                  # Throttling
]

# Decision
if has_rate_limiting:
    return PASS
else:
    return PASS  # Optional pattern, doesn't fail
```

**Note:** Rate limiting is recommended but not mandatory for all services.

---

### R6: Health Checks

**Severity:** CRITICAL  
**Category:** Observability

**Detection Logic:**

```python
# Phase 1: Look for health checks
health_patterns = [
    r'/actuator/health',          # Spring Boot Actuator
    r'@HealthIndicator',          # Custom health indicator
    r'HealthIndicator',           # Health indicator class
    r'livenessProbe:',            # K8s liveness probe
    r'readinessProbe:',           # K8s readiness probe
    r'startupProbe:',             # K8s startup probe
    r'health-check:',             # Generic health check
]

# Phase 2: File-specific checks
if is_kubernetes_manifest(file_path):
    # K8s manifests MUST have probes
    if not has_health_probes:
        return FAIL  # Missing health probes in K8s manifest
else:
    # Application code should have health indicators
    if has_health_checks:
        return PASS
    else:
        return PASS  # Not mandatory for all files
```

**Why Critical:**
Health checks are essential for:
- Kubernetes orchestration
- Load balancer routing
- Auto-healing
- Zero-downtime deployments

---

### R7: Graceful Shutdown

**Severity:** CRITICAL  
**Category:** Availability

**Detection Logic:**

```python
# Phase 1: Look for graceful shutdown
patterns = [
    r'@PreDestroy',               # Spring lifecycle hook
    r'shutdown\s*\(',             # Shutdown method
    r'close\s*\(',                # Close method
    r'graceful-shutdown:',        # YAML config
    r'preStop:',                  # K8s preStop hook
    r'terminationGracePeriodSeconds:',  # K8s grace period
]

# Phase 2: Check if needed for Kafka consumers
if '@KafkaListener' in content or 'KafkaConsumer' in content:
    if not has_graceful_shutdown:
        return FAIL  # Kafka consumers MUST shutdown gracefully
    else:
        return PASS
else:
    if has_graceful_shutdown:
        return PASS
    else:
        return PASS  # Not mandatory for all components
```

**Why Critical for Kafka:**
Without graceful shutdown, Kafka consumers can:
- Lose messages
- Cause duplicate processing
- Leave uncommitted offsets
- Trigger unnecessary rebalances

---

### R8: Idempotency

**Severity:** CRITICAL  
**Category:** Data Integrity

**Detection Logic:**

```python
# Phase 1: Look for idempotency implementation
patterns = [
    r'idempotency',               # Idempotency key
    r'idempotent',                # Idempotent flag
    r'deduplication',             # Deduplication logic
    r'unique.*constraint',        # Database constraint
    r'UNIQUE',                    # SQL unique constraint
]

# Phase 2: Check if needed for state-changing operations
state_change_patterns = [
    r'@PostMapping',              # HTTP POST
    r'@PutMapping',               # HTTP PUT
    r'@DeleteMapping',            # HTTP DELETE
    r'@KafkaListener',            # Message consumer
    r'\.save\(',                  # Database save
    r'\.update\(',                # Database update
    r'\.delete\(',                # Database delete
]

# Decision
if has_idempotency:
    return PASS
elif has_state_changes:
    return FAIL  # State-changing operations without idempotency
else:
    return PASS  # Read-only operations don't need idempotency
```

**Why Critical:**
Without idempotency:
- Duplicate requests cause duplicate data
- Retries can corrupt state
- Message replay causes inconsistencies
- Financial transactions can be duplicated

---

### R9: Dead Letter Queue (DLQ)

**Severity:** HIGH  
**Category:** Error Handling

**Detection Logic:**

```python
# Phase 1: Look for DLQ configuration
patterns = [
    r'dead.*letter',              # Dead letter reference
    r'dlq',                       # DLQ abbreviation
    r'DLQ',                       # DLQ uppercase
    r'error.*topic',              # Error topic
    r'failed.*topic',             # Failed topic
    r'DeadLetterPublishingRecoverer',  # Spring Kafka DLQ
]

# Phase 2: Check if needed for Kafka consumers
if '@KafkaListener' in content or 'KafkaConsumer' in content:
    if not has_dlq:
        return FAIL  # Kafka consumers MUST have DLQ
    else:
        return PASS
else:
    return PASS  # Not applicable to non-Kafka code
```

**Why Important:**
DLQ enables:
- Failed message analysis
- Message replay after fixes
- Preventing message loss
- Debugging production issues

---

### R10: Structured Logging

**Severity:** HIGH  
**Category:** Observability

**Detection Logic:**

```python
# Phase 1: Look for structured logging
patterns = [
    r'MDC\.',                     # Mapped Diagnostic Context
    r'correlation.*id',           # Correlation ID
    r'trace.*id',                 # Trace ID
    r'request.*id',               # Request ID
    r'LogstashEncoder',           # Logstash JSON encoder
    r'JsonLayout',                # JSON layout
]

# Decision
if has_structured_logging:
    return PASS
else:
    return PASS  # Recommended but not mandatory
```

---

### R11: Distributed Tracing

**Severity:** HIGH  
**Category:** Observability

**Detection Logic:**

```python
# Phase 1: Look for distributed tracing
patterns = [
    r'spring-cloud-sleuth',       # Spring Cloud Sleuth
    r'opentelemetry',             # OpenTelemetry
    r'@NewSpan',                  # Span annotation
    r'Tracer',                    # Tracer class
    r'zipkin',                    # Zipkin
    r'jaeger',                    # Jaeger
]

# Decision
if has_distributed_tracing:
    return PASS
else:
    return PASS  # Recommended but not mandatory
```

---

### R12: Metrics and Monitoring

**Severity:** CRITICAL  
**Category:** Observability

**Detection Logic:**

```python
# Phase 1: Look for metrics
patterns = [
    r'@Timed',                    # Micrometer @Timed
    r'@Counted',                  # Micrometer @Counted
    r'MeterRegistry',             # Micrometer registry
    r'micrometer',                # Micrometer library
    r'prometheus',                # Prometheus
    r'/actuator/metrics',         # Actuator metrics endpoint
    r'metrics:',                  # YAML metrics config
]

# Decision
if has_metrics:
    return PASS
else:
    return PASS  # Should be configured at application level
```

---

### R13: Schema Evolution Strategy

**Severity:** CRITICAL  
**Category:** Data Integrity

**Detection Logic:**

```python
# Phase 1: File type check
if not file_path.endswith(('.avsc', '.avdl')):
    return PASS  # Not a schema file

# Phase 2: Look for compatibility configuration
patterns = [
    r'schema.*registry',          # Schema registry reference
    r'compatibility',             # Compatibility mode
    r'BACKWARD',                  # Backward compatibility
    r'FORWARD',                   # Forward compatibility
    r'FULL',                      # Full compatibility
]

# Decision
if has_compatibility_config:
    return PASS
else:
    return FAIL  # Schema without compatibility configuration
```

**Why Critical:**
Schema evolution without compatibility:
- Breaks consumers during deployment
- Causes deserialization errors
- Prevents zero-downtime deployments
- Requires coordinated releases

---

### R14: Kafka Consumer Best Practices

**Severity:** CRITICAL  
**Category:** Message Processing

**Detection Logic:**

```python
# Phase 1: Check if file has Kafka consumers
if '@KafkaListener' not in content and 'KafkaConsumer' not in content:
    return PASS  # Not a Kafka consumer

# Phase 2: Check for manual commit
has_manual_commit = any(re.search(p, content) for p in [
    r'enable\.auto\.commit.*false',
    r'commitSync',
    r'commitAsync',
    r'AckMode\.MANUAL',
])

# Phase 3: Check for error handling
has_error_handling = any(re.search(p, content) for p in [
    r'@KafkaListener.*errorHandler',
    r'try.*catch',
    r'ErrorHandler',
    r'@RetryableTopic',
])

# Decision
if not has_manual_commit:
    return FAIL  # Auto-commit is dangerous
elif not has_error_handling:
    return FAIL  # Missing error handling
else:
    return PASS
```

**Why Critical:**
Auto-commit can cause:
- Message loss (commit before processing)
- Duplicate processing (crash before commit)
- Data inconsistencies
- Difficult debugging

---

### R15: Database Connection Pooling

**Severity:** HIGH  
**Category:** Resource Management

**Detection Logic:**

```python
# Phase 1: Look for connection pool configuration
patterns = [
    r'hikari',                    # HikariCP
    r'maximum-pool-size:',        # Pool size config
    r'connection-timeout:',       # Connection timeout
    r'idle-timeout:',             # Idle timeout
    r'max-lifetime:',             # Max lifetime
]

# Decision
if has_connection_pool_config:
    return PASS
else:
    return PASS  # Should be configured at application level
```

---

### R16: Kubernetes Resource Limits

**Severity:** CRITICAL  
**Category:** Resource Management

**Detection Logic:**

```python
# Phase 1: File type check
if not (file_path.endswith(('.yml', '.yaml')) and 
        ('k8s' in file_path or 'helm' in file_path)):
    return PASS  # Not a K8s manifest

# Phase 2: Check for resource configuration
has_requests = 'requests:' in content
has_limits = 'limits:' in content
has_cpu = 'cpu:' in content
has_memory = 'memory:' in content

# Decision
if has_requests and has_limits and has_cpu and has_memory:
    return PASS
else:
    return FAIL  # Missing resource requests/limits
```

**Why Critical:**
Without resource limits:
- Pods can starve other pods
- Node can run out of resources
- Unpredictable performance
- Difficult capacity planning

---

### R17: Horizontal Pod Autoscaling (HPA)

**Severity:** HIGH  
**Category:** Scalability

**Detection Logic:**

```python
# Phase 1: File type check
if not (file_path.endswith(('.yml', '.yaml')) and 
        ('k8s' in file_path or 'helm' in file_path)):
    return PASS  # Not a K8s manifest

# Phase 2: Look for HPA configuration
patterns = [
    r'kind:\s*HorizontalPodAutoscaler',
    r'autoscaling/v2',
    r'minReplicas:',
    r'maxReplicas:',
    r'targetCPUUtilizationPercentage:',
]

# Decision
if has_hpa:
    return PASS
else:
    return PASS  # Recommended but not mandatory
```

---

### R18: Pod Disruption Budget (PDB)

**Severity:** HIGH  
**Category:** Availability

**Detection Logic:**

```python
# Phase 1: File type check
if not (file_path.endswith(('.yml', '.yaml')) and 
        ('k8s' in file_path or 'helm' in file_path)):
    return PASS  # Not a K8s manifest

# Phase 2: Look for PDB configuration
patterns = [
    r'kind:\s*PodDisruptionBudget',
    r'minAvailable:',
    r'maxUnavailable:',
]

# Decision
if has_pdb:
    return PASS
else:
    return PASS  # Recommended but not mandatory
```

---

### R19: Secrets Management

**Severity:** CRITICAL  
**Category:** Security

**Detection Logic:**

```python
# Phase 1: Look for hardcoded secrets (applies to ALL files)
secret_patterns = [
    r'password\s*=\s*["\'][^"\']+["\']',
    r'api[_-]?key\s*=\s*["\'][^"\']+["\']',
    r'secret\s*=\s*["\'][^"\']+["\']',
    r'token\s*=\s*["\'][^"\']+["\']',
    r'jdbc:.*://.*:.*@',          # JDBC URL with credentials
]

# Decision
if has_hardcoded_secrets:
    return FAIL  # Hardcoded secrets detected
else:
    return PASS
```

**Why Critical:**
Hardcoded secrets:
- Are visible in version control
- Can be leaked in logs
- Cannot be rotated easily
- Violate security policies
- Expose systems to attacks

---

### R20: Disaster Recovery Testing

**Severity:** CRITICAL  
**Category:** Disaster Recovery

**Detection Logic:**

```python
# Phase 1: Check documentation files
if file_path.endswith(('.md', '.txt', '.adoc')):
    patterns = [
        r'disaster.*recovery',
        r'backup',
        r'RTO',                   # Recovery Time Objective
        r'RPO',                   # Recovery Point Objective
        r'failover',
        r'DR.*drill',
    ]
    
    if has_dr_documentation:
        return PASS
    else:
        return PASS  # Not all docs need DR info

# Phase 2: Check backup configurations
if file_path.endswith(('.yml', '.yaml')):
    patterns = [
        r'backup:',
        r'snapshot:',
        r'velero',                # Kubernetes backup tool
        r'backup-schedule:',
    ]
    
    if has_backup_config:
        return PASS
    else:
        return PASS  # Not all configs need backup

# Default
return PASS  # DR is typically documented separately
```

---

## Configuration

### Severity Threshold

Controls which failures block PR merging:

```yaml
# .github/resilience-checker-config.yml
severity_threshold: CRITICAL  # Options: CRITICAL, HIGH, MEDIUM, LOW
```

**Behavior:**
- `CRITICAL`: Only CRITICAL failures block merge
- `HIGH`: HIGH and CRITICAL failures block merge
- `MEDIUM`: MEDIUM, HIGH, and CRITICAL failures block merge
- `LOW`: All failures block merge

### Enabling/Disabling Checks

```yaml
checks:
  R1:
    enabled: true
    severity: CRITICAL
  R2:
    enabled: false  # Disable this check
    severity: HIGH
```

### Exclusions

```yaml
exclusions:
  paths:
    - "**/test/**"
    - "**/tests/**"
    - "**/*Test.java"
  files:
    - "README.md"
    - "LICENSE"
```

---

## Extending the Checker

### Adding a New Check

1. **Create Check Class**

```python
class MyNewCheck(ResilienceCheck):
    def __init__(self):
        super().__init__(
            "R21",
            "My New Pattern",
            "Category",
            "HIGH",
            "Description of the pattern"
        )
        self.recommendation = "How to implement it"
    
    def check(self, file_path: str, content: str) -> bool:
        # Phase 1: Look for pattern
        if pattern_exists(content):
            self.passed = True
            return True
        
        # Phase 2: Check if needed
        if pattern_needed(content):
            self.add_finding(file_path, "Pattern missing")
            self.passed = False
            return False
        
        self.passed = True
        return True
```

2. **Register Check**

```python
def _initialize_checks(self) -> List[ResilienceCheck]:
    return [
        # ... existing checks
        MyNewCheck(),
    ]
```

3. **Update Configuration**

```yaml
checks:
  R21:
    enabled: true
    severity: HIGH
    name: "My New Pattern"
```

---

## Summary

The R1-R20 Resilience Checker is a sophisticated static analysis tool that:

1. **Scans** all relevant files in monitored directories
2. **Runs** all 20 checks against each file
3. **Uses** intelligent, context-aware detection to avoid false positives
4. **Flags** only real issues where patterns are needed but missing
5. **Reports** results with detailed findings and recommendations
6. **Blocks** PR merging when CRITICAL checks fail

This approach ensures that:
- ✅ No false positives from irrelevant checks
- ✅ Real issues are caught early
- ✅ Developers get actionable feedback
- ✅ Code quality improves over time
- ✅ Production resilience is maintained

---

**Version:** 1.0  
**Last Updated:** 2026-04-16  
**Repository:** vessel-operations-service
