# Passed Evidence Feature - Resilience Checker Enhancement

## Overview

The resilience checker has been enhanced to show **why checks pass**, not just why they fail. This provides transparency and helps developers understand which files contain the required resilience patterns.

## What Changed

### Before
```json
{
  "id": "R5",
  "name": "Rate Limiting",
  "passed": true,
  "findings": [],
  "recommendation": "Add @RateLimiter annotation..."
}
```
**Problem:** No information about which files have rate limiting or why the check passed.

### After
```json
{
  "id": "R5",
  "name": "Rate Limiting",
  "passed": true,
  "findings": [],
  "passed_evidence": [
    {
      "file": "src/main/java/com/example/config/ResilienceConfig.java",
      "line": null,
      "message": "Rate limiting configured (found pattern: r'RateLimiterConfig')"
    },
    {
      "file": "src/main/java/com/example/api/UserController.java",
      "line": null,
      "message": "Rate limiting configured (found pattern: r'@RateLimiter')"
    }
  ],
  "recommendation": "Add @RateLimiter annotation..."
}
```
**Benefit:** Clear evidence of which files contain rate limiting and what patterns were found.

## Implementation

### Base Class Enhancement

Added `passed_evidence` list to track compliance:

```python
class ResilienceCheck:
    def __init__(self, ...):
        self.findings: List[Dict[str, Any]] = []           # Violations
        self.passed_evidence: List[Dict[str, Any]] = []    # Compliance evidence
        self.passed = False
    
    def add_finding(self, file: str, message: str, line: Optional[int] = None):
        """Add a finding (violation) to this check"""
        self.findings.append({
            "file": file,
            "line": line,
            "message": message
        })
    
    def add_passed_evidence(self, file: str, message: str, line: Optional[int] = None):
        """Add evidence of compliance (why check passed for this file)"""
        self.passed_evidence.append({
            "file": file,
            "line": line,
            "message": message
        })
```

### R5 Rate Limiting Example

```python
def check(self, file_path: str, content: str) -> bool:
    patterns = [
        r'@RateLimiter',
        r'RateLimiterConfig',
        r'rate-limit:',
        r'rateLimit',
        r'throttle'
    ]
    
    # Find which pattern matched
    matched_pattern = None
    for pattern in patterns:
        if re.search(pattern, content, re.IGNORECASE):
            matched_pattern = pattern
            break
    
    if matched_pattern:
        # Record evidence of compliance
        self.add_passed_evidence(
            file_path, 
            f"Rate limiting configured (found pattern: {matched_pattern})"
        )
        return True
    
    # Check if endpoints need rate limiting...
    if has_endpoints:
        self.add_finding(file_path, "API endpoints without rate limiting")
        self.passed = False
        return False
    
    return True
```

## Benefits

### 1. Transparency
Developers can see exactly which files contain resilience patterns and why checks pass.

### 2. Verification
Easy to verify that the checker is detecting patterns correctly.

### 3. Documentation
Passed evidence serves as documentation of where resilience patterns are implemented.

### 4. Debugging
Helps debug false positives - if a check passes but shouldn't, the evidence shows why.

### 5. Audit Trail
Provides an audit trail of compliance for security and governance reviews.

## Example Outputs

### Scenario 1: All Files Compliant

```json
{
  "id": "R5",
  "name": "Rate Limiting",
  "passed": true,
  "findings": [],
  "passed_evidence": [
    {
      "file": "src/config/ResilienceConfig.java",
      "message": "Rate limiting configured (found pattern: r'RateLimiterConfig')"
    },
    {
      "file": "src/api/OrderController.java",
      "message": "Rate limiting configured (found pattern: r'@RateLimiter')"
    },
    {
      "file": "src/api/UserController.java",
      "message": "Rate limiting configured (found pattern: r'@RateLimiter')"
    }
  ]
}
```

### Scenario 2: Mixed Compliance

```json
{
  "id": "R5",
  "name": "Rate Limiting",
  "passed": false,
  "findings": [
    {
      "file": "src/api/ProductController.java",
      "message": "API endpoints detected without rate limiting protection"
    }
  ],
  "passed_evidence": [
    {
      "file": "src/config/ResilienceConfig.java",
      "message": "Rate limiting configured (found pattern: r'RateLimiterConfig')"
    },
    {
      "file": "src/api/OrderController.java",
      "message": "Rate limiting configured (found pattern: r'@RateLimiter')"
    }
  ]
}
```
**Insight:** Two controllers have rate limiting, but ProductController is missing it.

### Scenario 3: No Relevant Files

```json
{
  "id": "R5",
  "name": "Rate Limiting",
  "passed": true,
  "findings": [],
  "passed_evidence": []
}
```
**Insight:** No API controllers found, so rate limiting not required.

## When to Use

### Use `add_passed_evidence()` when:
- ✅ A file contains the required pattern
- ✅ A file demonstrates compliance
- ✅ You want to show why a check passed for specific files

### Use `add_finding()` when:
- ❌ A file violates the check
- ❌ A required pattern is missing
- ❌ You want to show why a check failed for specific files

## Best Practices

### 1. Be Specific
```python
# Good - Shows what was found
self.add_passed_evidence(file, "Rate limiting configured (found pattern: r'@RateLimiter')")

# Bad - Too generic
self.add_passed_evidence(file, "Passed")
```

### 2. Include Pattern Information
```python
# Good - Shows which pattern matched
matched_pattern = r'@CircuitBreaker'
self.add_passed_evidence(file, f"Circuit breaker found (pattern: {matched_pattern})")

# Bad - No context
self.add_passed_evidence(file, "Circuit breaker found")
```

### 3. Only Add Evidence for Relevant Files
```python
# Good - Only add evidence when pattern is actually found
if has_rate_limiting:
    self.add_passed_evidence(file, "Rate limiting configured")
    return True

# Bad - Adding evidence for every file
self.add_passed_evidence(file, "Checked")  # Don't do this
```

### 4. Don't Add Evidence for Non-Applicable Files
```python
# Good - Only relevant files
if has_endpoints and has_rate_limiting:
    self.add_passed_evidence(file, "Rate limiting on endpoints")
elif has_endpoints:
    self.add_finding(file, "Endpoints without rate limiting")
# No evidence for files without endpoints

# Bad - Evidence for every file
if not has_endpoints:
    self.add_passed_evidence(file, "No endpoints, not applicable")  # Don't do this
```

## Extending to Other Checks

Other checks can be enhanced similarly:

### Circuit Breaker (R1)
```python
if has_circuit_breaker:
    self.add_passed_evidence(
        file_path,
        f"Circuit breaker configured (found: {matched_pattern})"
    )
```

### Health Checks (R6)
```python
if has_health_checks:
    self.add_passed_evidence(
        file_path,
        f"Health checks configured (found: {matched_pattern})"
    )
```

### Retry Logic (R2)
```python
if has_retry:
    self.add_passed_evidence(
        file_path,
        f"Retry logic configured (found: {matched_pattern})"
    )
```

## Output Format

The `passed_evidence` field is:
- **Optional:** Only included if there is evidence
- **Array:** Can contain multiple evidence entries
- **Structured:** Same format as findings (file, line, message)

```typescript
interface CheckResult {
  id: string;
  name: string;
  passed: boolean;
  findings: Finding[];
  passed_evidence?: Finding[];  // Optional
  recommendation: string;
}

interface Finding {
  file: string;
  line?: number;
  message: string;
}
```

## Migration Guide

To add passed evidence to existing checks:

1. **Identify where patterns are found**
   ```python
   if re.search(pattern, content):
       # Pattern found
   ```

2. **Add evidence recording**
   ```python
   if re.search(pattern, content):
       self.add_passed_evidence(file_path, f"Pattern found: {pattern}")
       return True
   ```

3. **Test the output**
   ```bash
   python .github/scripts/resilience_checker.py --repo-path . --output-format json
   ```

4. **Verify passed_evidence appears in output**

## Conclusion

The passed evidence feature provides transparency into why checks pass, making the resilience checker more informative and useful for developers. It helps with:

- ✅ Understanding compliance
- ✅ Verifying checker accuracy
- ✅ Documenting resilience patterns
- ✅ Debugging false positives
- ✅ Audit and governance

This enhancement makes the checker not just a validator, but also a documentation tool that shows where resilience patterns are implemented in the codebase.

---

**Feature Added:** 2026-04-17  
**Status:** ✅ Implemented in R5 Rate Limiting check  
**Next Steps:** Can be extended to other checks (R1-R4, R6-R23)