# R1-R20 Resilience Checker for vessel-operations-service

## Overview

This automated resilience checker validates that code changes in the `vessel-operations-service` repository comply with the R1-R20 resilience guardrails. It runs automatically on pull requests and can be triggered manually via workflow dispatch.

**Repository:** https://github.ibm.com/Amit-Kumar-Verma/vessel-operations-service.git

## What It Checks

The checker validates 20 critical resilience patterns across multiple categories:

### Fault Tolerance (R1-R3)
- **R1**: Circuit Breaker Pattern - Prevents cascading failures
- **R2**: Retry with Exponential Backoff - Handles transient failures
- **R3**: Timeout Configuration - Prevents indefinite waits

### Resource Management (R4-R5, R15-R16)
- **R4**: Bulkhead Pattern - Isolates resources (only flags files with concurrent/async operations)
- **R5**: Rate Limiting - Protects from overload
- **R15**: Database Connection Pooling - Efficient resource usage
- **R16**: Kubernetes Resource Limits - Prevents resource exhaustion

### Observability (R6, R10-R12)
- **R6**: Health Checks - Liveness, readiness, startup probes
- **R10**: Structured Logging - Correlation IDs and JSON logs
- **R11**: Distributed Tracing - Request flow visibility
- **R12**: Metrics and Monitoring - Prometheus/Micrometer integration

### Availability (R7, R17-R18)
- **R7**: Graceful Shutdown - No lost requests
- **R17**: Horizontal Pod Autoscaling (HPA) - Auto-scaling
- **R18**: Pod Disruption Budget (PDB) - Maintain availability

### Data Integrity (R8, R13-R14)
- **R8**: Idempotency - Safe retry of operations
- **R13**: Schema Evolution Strategy - Backward/forward compatibility
- **R14**: Kafka Consumer Best Practices - Reliable message processing

### Error Handling (R9)
- **R9**: Dead Letter Queue (DLQ) - Failed message handling

### Security & DR (R19-R20)
- **R19**: Secrets Management - No hardcoded credentials
- **R20**: Disaster Recovery Testing - DR drills and backups

## How It Works

### Automatic Triggers

The workflow runs automatically when:
1. A pull request is opened, synchronized, or reopened
2. Changes are made to monitored paths:
   - `src/**`
   - `k8s/**`
   - `helm/**`
   - `schemas/**`
   - `infrastructure/**`
   - `specs/**`
   - `config/**`
   - Application configuration files

### Manual Trigger

You can manually trigger the check via GitHub Actions:
1. Go to Actions → R1-R20 Resilience Guardrail Checker
2. Click "Run workflow"
3. Select severity threshold (CRITICAL, HIGH, MEDIUM, LOW)
4. Click "Run workflow"

## Configuration

### Main Configuration File

[`resilience-checker-config.yml`](resilience-checker-config.yml) contains:

- **Severity Threshold**: Default is `CRITICAL` (only critical failures block merge)
- **Monitored Paths**: Directories to scan
- **File Extensions**: Types of files to check
- **Check Configuration**: Enable/disable specific checks
- **Exclusions**: Paths/files to skip (test files, build artifacts)
- **Reporting**: PR comment settings, badge generation
- **Notifications**: Architecture Board alerts
- **IBM GHE Settings**: IBM-specific configurations

### Severity Levels

- **CRITICAL**: Must be fixed before merge (blocks PR)
- **HIGH**: Should be fixed (warning)
- **MEDIUM**: Recommended to fix (info)
- **LOW**: Nice to have (info)

## Workflow Output

### PR Comment

The checker posts a detailed comment on your PR with:
- ✅/❌ Overall status
- Summary table by severity
- Failed checks with details and recommendations
- Passed checks (collapsible)
- Links to documentation

### Artifacts

The workflow uploads:
- `resilience-report.json` - Full JSON report (30 days retention)
- `resilience-badge.md` - Status badge (30 days retention)

### Status Check

The workflow creates a GitHub status check that:
- ✅ Passes if no critical issues found
- ❌ Fails if critical issues found (blocks merge)

## Bypassing Checks

⚠️ **Bypassing checks requires Architecture Board approval**

### Process

1. Add label `bypass-resilience-check` to PR
2. Automated issue is created for Architecture Board
3. Create ADR documenting the exception
4. Minimum 2 Architecture Board approvals required
5. ADR must be placed in `docs/adr/` directory

### Bypass Notification

When bypass is attempted:
- Issue created with label `architecture-board`
- Stakeholder teams notified:
  - @architecture-board
  - @vessel-ops-team
  - @platform-engineering

## File Structure

```
.github/
├── workflows/
│   └── resiliencecheck.yml          # GitHub Actions workflow
├── scripts/
│   └── resilience_checker.py        # Python checker script
├── resilience-checker-config.yml    # Configuration file
└── README-RESILIENCE-CHECKER.md     # This file
```

## Local Testing

You can run the checker locally:

```bash
# Install dependencies
pip install pyyaml requests jinja2 gitpython

# Run checker
python .github/scripts/resilience_checker.py \
  --repo-path . \
  --output-format json \
  --output-file resilience-report.json \
  --severity-threshold CRITICAL
```

## Common Issues and Solutions

### Issue: External calls without circuit breaker (R1)

**Solution:**
```java
@CircuitBreaker(name = "externalService", fallbackMethod = "fallback")
public ResponseEntity<Data> callExternalService() {
    // Your code
}
```

### Issue: Missing timeout configuration (R3)

**Solution:**
```yaml
# application.yml
resilience4j:
  timelimiter:
    instances:
      externalService:
        timeout-duration: 3s
```

### Issue: Kafka consumer without DLQ (R9)

**Solution:**
```yaml
spring:
  kafka:
    consumer:
      properties:
        spring.kafka.listener.dead-letter-publishing-recoverer.enabled: true
```

### Issue: Kubernetes manifest without resource limits (R16)

**Solution:**
```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

### Issue: Hardcoded secrets (R19)

**Solution:**
```java
// ❌ Bad
String password = "myPassword123";

// ✅ Good
@Value("${db.password}")
private String password;
```

## Integration with IBM GHE

This checker is configured for IBM GitHub Enterprise:
- Repository: `vessel-operations-service`
- Owner: `Amit-Kumar-Verma`
- IBM-specific compliance checks enabled
- Security scan integration
- License check enforcement

## Support and Documentation

- **R1-R20 Checklist**: See `docs/R1-R20-RESILIENCE-CHECKLIST.md`
- **Architecture Decisions**: See `docs/adr/`
- **Issues**: Create issue with label `resilience-checker`
- **Questions**: Contact @architecture-board

## Version

**Current Version:** 1.0

## Maintenance

The checker is maintained by the Architecture Team. Updates and improvements are tracked in the repository's issue tracker.

---

**Made with Bob** 🤖