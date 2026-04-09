# Bob Agentic Resilience Checker V3 - Setup and Test Guide

## What Changed

The old rule-based workflow in [`workflows/resiliencecheck.yml`](workflows/resiliencecheck.yml) has been replaced with a **Bob-first V3 workflow**.

This means:

- GitHub Actions now prepares PR context and enforces Bob’s output
- Bob is intended to perform the actual resilience analysis
- merge pass/fail must come from Bob’s JSON decision
- local scripts no longer compute resilience findings for V3 enforcement

## Files Added for V3

- [`../.bob/modes/resilience-guardian-v3.md`](../.bob/modes/resilience-guardian-v3.md)
- [`../.bob/prompts/pr-review-agent-prompt.md`](../.bob/prompts/pr-review-agent-prompt.md)
- [`../.bob/prompts/critical-escalation-prompt.md`](../.bob/prompts/critical-escalation-prompt.md)
- [`../.bob/prompts/auto-fix-guardrails.md`](../.bob/prompts/auto-fix-guardrails.md)
- [`bob-agentic-config.yml`](bob-agentic-config.yml)
- [`scripts/package_pr_for_bob.py`](scripts/package_pr_for_bob.py)
- [`scripts/validate_bob_review_result.py`](scripts/validate_bob_review_result.py)
- [`workflows/resiliencecheck.yml`](workflows/resiliencecheck.yml)

## Important Reality

This workflow is now **purely agentic in design**, but you still need a real Bob execution mechanism in CI.

The current workflow prepares:
- `.github/artifacts/bob-pr-review-bundle.json`

And expects Bob to create:
- `.github/artifacts/bob-review.md`
- `.github/artifacts/bob-review.json`

## Where to Plug in Bob

Open [`workflows/resiliencecheck.yml`](workflows/resiliencecheck.yml) and find the step:

- `Await Bob review output`

Replace that step with your real Bob invocation.

## Example Integration Options

### Option 1: Bob CLI
If your runner has a Bob CLI:

```bash
bob review-pr \
  --mode resilience-guardian-v3 \
  --input .github/artifacts/bob-pr-review-bundle.json \
  --markdown-output .github/artifacts/bob-review.md \
  --json-output .github/artifacts/bob-review.json
```

### Option 2: Bob API
If Bob is exposed as an API:

```bash
curl -X POST https://YOUR_BOB_ENDPOINT/review \
  -H "Authorization: Bearer $BOB_API_KEY" \
  -H "Content-Type: application/json" \
  --data @.github/artifacts/bob-pr-review-bundle.json
```

Your integration logic must write:
- `.github/artifacts/bob-review.md`
- `.github/artifacts/bob-review.json`

## How to Test the New Workflow

### Test 1: Trigger the workflow
Create a PR that changes a monitored path such as:
- `src/**`
- `config/**`
- `application.yml`

Example:

```bash
git checkout -b test/bob-agentic-v3
echo "// trigger bob review" >> src/main/java/com/freightforwarder/vesseloperations/VesselOperationsServiceApplication.java
git add .
git commit -m "test: trigger bob agentic resilience review"
git push origin test/bob-agentic-v3
```

Then open a PR.

### Test 2: Confirm bundle creation
In GitHub Actions artifacts, verify:
- `.github/artifacts/bob-pr-review-bundle.json`
- `.github/artifacts/changed-files-filtered.txt`
- `.github/artifacts/pull-request.diff`

This proves GitHub Actions is only preparing context.

### Test 3: Confirm Bob is the decision source
After wiring Bob in, confirm Bob produces:
- `.github/artifacts/bob-review.md`
- `.github/artifacts/bob-review.json`

The validator in [`scripts/validate_bob_review_result.py`](scripts/validate_bob_review_result.py) will fail if those files are missing or malformed.

### Test 4: PASS case
Use a Bob JSON result like:

```json
{
  "status": "PASS",
  "confidence": "High",
  "summary": "No merge-blocking resilience issues found.",
  "findings": [],
  "positives": ["Operational resilience looks acceptable for the changed paths."],
  "block_merge": false
}
```

Expected result:
- workflow passes
- PR comment is posted
- merge is allowed if other branch rules pass

### Test 5: FAIL case
Use a Bob JSON result like:

```json
{
  "status": "FAIL",
  "confidence": "High",
  "summary": "Kafka handling changes introduce a credible message loss risk.",
  "findings": [
    {
      "id": "R14",
      "title": "Kafka delivery guarantees are insufficient",
      "severity": "CRITICAL",
      "files": ["src/main/java/com/freightforwarder/vesseloperations/infrastructure/kafka/KafkaEventConsumer.java"],
      "why_it_matters": "Offset handling and failure recovery are not explicit.",
      "recommendation": "Use explicit acknowledgment and DLQ/retry handling.",
      "auto_fix_feasible": false
    }
  ],
  "positives": [],
  "block_merge": true
}
```

Expected result:
- workflow fails
- PR gets Bob comment
- merge is blocked

## How to Know It Is Truly Pure Agentic AI

Your workflow is truly Bob-agentic when all of these are true:

1. [`package_pr_for_bob.py`](scripts/package_pr_for_bob.py) only prepares context
2. [`validate_bob_review_result.py`](scripts/validate_bob_review_result.py) only validates Bob output structure
3. No local script computes resilience findings for the merge decision
4. Bob generates the Markdown and JSON review artifacts
5. GitHub Actions only enforces Bob’s `block_merge` field

## Branch Protection

After the workflow has run successfully at least once:

1. Go to repository Settings → Branches
2. Add or edit the protection rule for `main`
3. Enable:
   - Require a pull request before merging
   - Require status checks to pass before merging
4. Select the new workflow check:
   - `Bob Agentic Resilience Review`
5. Save the rule

## Final Note

I have configured this repository structure for the V3 Bob-first flow.
The one remaining step is to connect the actual Bob runtime or API in the workflow step that currently waits for Bob output.
