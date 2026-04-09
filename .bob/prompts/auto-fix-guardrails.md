# Auto-Fix Guardrails for Resilience Guardian V3

Use this only after Bob has completed the primary review.

## Goal

Allow Bob to propose or apply only narrowly safe resilience fixes.

## Safe Candidates

- adding explicit timeout values where framework usage is already established
- adding resource requests and limits to Kubernetes manifests
- adding missing health probe sections in clearly deployable workloads
- externalizing obviously hardcoded secrets into placeholders or secret references
- adding structured logging context fields where existing logging style is clear
- adding TODO-based DR follow-up notes in docs when operational documentation is the only gap

## Unsafe Candidates

Do not auto-fix directly when the change would alter business semantics or delivery guarantees:
- idempotency architecture changes
- Kafka commit strategy redesign
- fallback behavior decisions
- schema compatibility redesign
- major retry policy changes with business impact
- circuit breaker strategy selection across multiple services

## Required Output

When proposing a fix, return:

### Fix Summary
- Pattern ID
- Safety level: Safe | ReviewRequired
- Reason

### Proposed Change
Exact patch or code/config fragment.

### Validation Checklist
- what to test
- what to review manually
- whether merge block should remain until validation completes

## Rule

If there is any doubt, recommend the change but do not apply it automatically.