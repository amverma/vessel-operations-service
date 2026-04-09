# Critical Escalation Prompt for Resilience Guardian V3

Use this prompt when a pull request appears to introduce potentially merge-blocking resilience risk.

## Purpose

Drive a second-pass deep review before setting `block_merge: true`.

## Review Areas

### R1 Circuit Breaker
Check whether outbound dependencies could fail in a way that causes:
- cascading latency
- thread exhaustion
- widespread request failure

### R3 Timeout Configuration
Check whether external interactions are explicitly bounded and operationally sane.

### R6 Health Checks
Check whether runtime deployments remain observable and orchestratable.

### R7 Graceful Shutdown
Check whether in-flight work can be lost during shutdown or rollout.

### R8 Idempotency
Check whether replay, retry, or duplicate delivery could create duplicate business effects.

### R12 Metrics and Monitoring
Check whether critical changes remain visible to operators.

### R13 Schema Evolution
Check whether rolling deployment or downstream compatibility could break.

### R14 Kafka Best Practices
Check whether message handling is reliable under failure, retry, rebalance, and poison-message conditions.

### R16 Kubernetes Resource Limits
Check whether missing limits or requests could destabilize runtime scheduling or node health.

### R19 Secrets Management
Check whether the change exposes secrets directly or through insecure defaults.

### R20 Disaster Recovery Testing
Check whether the change alters recovery assumptions without corresponding operational updates.

## Required Decision Questions

Before blocking merge, answer:
1. What concrete failure mode do I believe is credible?
2. What is the likely blast radius?
3. Is the evidence strong enough to block, or only to warn?
4. What exact remediation should the team apply?

## Output Additions

If you block merge, say:
- what failed
- why this is not safe to merge
- what minimum change would remove the block

## Caution

Do not block on vague discomfort.
Block only on evidence-backed resilience risk.