# Bob Agentic Resilience Checker V3 - Setup and Test Guide

## What Changed

The old rule-based workflow in [`workflows/resiliencecheck.yml`](workflows/resiliencecheck.yml) has been replaced with a **Bob-first V3 workflow** that is now adapted for a **Llama/Ollama-backed local agent API**.

This means:

- GitHub Actions prepares PR context and enforces the agent output
- your local Llama model performs the actual resilience analysis through [`llama-agent/server.js`](llama-agent/server.js)
- merge pass/fail comes from the generated JSON decision
- local helper scripts do not compute the resilience decision themselves

## Files Added for the Llama-backed Agent

- [`../.bob/modes/resilience-guardian-v3.md`](../.bob/modes/resilience-guardian-v3.md)
- [`../.bob/prompts/pr-review-agent-prompt.md`](../.bob/prompts/pr-review-agent-prompt.md)
- [`../.bob/prompts/critical-escalation-prompt.md`](../.bob/prompts/critical-escalation-prompt.md)
- [`../.bob/prompts/auto-fix-guardrails.md`](../.bob/prompts/auto-fix-guardrails.md)
- [`bob-agentic-config.yml`](bob-agentic-config.yml)
- [`llama-agent/package.json`](llama-agent/package.json)
- [`llama-agent/package-pr-for-bob.js`](llama-agent/package-pr-for-bob.js)
- [`llama-agent/review-bundle.js`](llama-agent/review-bundle.js)
- [`llama-agent/server.js`](llama-agent/server.js)
- [`llama-agent/validate-bob-review-result.js`](llama-agent/validate-bob-review-result.js)
- [`workflows/resiliencecheck.yml`](workflows/resiliencecheck.yml)

## Runtime Assumptions

This setup assumes:

- [`Ollama`](llama-agent/review-bundle.js) is installed on your laptop
- Node.js is installed
- a model such as `llama3.1:8b` is available in Ollama
- your laptop can expose an HTTP endpoint that GitHub Actions can call

## Local Agent Architecture

The flow is now:

1. GitHub Actions collects changed files and diff
2. GitHub Actions builds `.github/artifacts/bob-pr-review-bundle.json`
3. GitHub Actions sends that bundle to your HTTP endpoint exposed by [`llama-agent/server.js`](llama-agent/server.js)
4. [`llama-agent/server.js`](llama-agent/server.js) invokes [`llama-agent/review-bundle.js`](llama-agent/review-bundle.js)
5. [`llama-agent/review-bundle.js`](llama-agent/review-bundle.js) calls the local Ollama API
6. the agent writes:
   - `.github/artifacts/bob-review.md`
   - `.github/artifacts/bob-review.json`
7. GitHub Actions validates the JSON structure using [`llama-agent/validate-bob-review-result.js`](llama-agent/validate-bob-review-result.js)
8. the PR comment is posted and merge blocking is enforced from `block_merge`

## Step 1 - Ensure Ollama is Running

Test Ollama locally:

```powershell
ollama --version
ollama list
```

If your model is not present, pull one:

```powershell
ollama pull llama3.1:8b
```

Ollama normally serves its API at:

- `http://127.0.0.1:11434`

## Step 2 - Start the Local Llama Agent API

From the repository root [`vessel-operations-service`](../):

```powershell
cd .\vessel-operations-service
$env:OLLAMA_MODEL="llama3.1:8b"
$env:OLLAMA_BASE_URL="http://127.0.0.1:11434"
$env:LLAMA_AGENT_HOST="0.0.0.0"
$env:LLAMA_AGENT_PORT="8787"
$env:LLAMA_AGENT_REPO_ROOT=(Get-Location).Path
node .github/llama-agent/server.js
```

When healthy, the service exposes:

- `GET /health`
- `POST /review`

Example local health check:

```powershell
Invoke-RestMethod -Method Get -Uri http://127.0.0.1:8787/health
```

## Step 3 - Test the Review API Locally

Create a bundle locally or reuse one from workflow artifacts, then call the API.

Example request body shape:

```json
{
  "bundle": {
    "repository": "amverma/vessel-operations-service",
    "pull_request": {
      "number": "123",
      "title": "Test resilience review",
      "author": "amverma",
      "base_ref": "main",
      "head_ref": "test/branch"
    },
    "changed_files": [
      "src/main/java/com/freightforwarder/vesseloperations/infrastructure/kafka/KafkaEventConsumer.java"
    ],
    "diff": "diff content here",
    "policy_docs": {},
    "prompts": {
      "main": "prompt text",
      "critical": "critical text",
      "autofix": "autofix text"
    },
    "config": {},
    "supporting_v1": {
      "executed": false
    }
  }
}
```

Example PowerShell call:

```powershell
$body = Get-Content .github/artifacts/bob-pr-review-bundle.json -Raw
Invoke-RestMethod `
  -Method Post `
  -Uri http://127.0.0.1:8787/review `
  -ContentType "application/json" `
  -Body (@{ bundle = ($body | ConvertFrom-Json) } | ConvertTo-Json -Depth 100)
```

Expected API response fields:

- `review_json`
- `review_markdown`

## Step 4 - Expose the Local API to GitHub Actions

GitHub-hosted runners cannot directly call `127.0.0.1` on your laptop. You need a reachable URL.

Practical options:

### Option A - Use a tunnel
Use a tunnel such as:

- Cloudflare Tunnel
- ngrok
- Tailscale Funnel

Example concept:

- local agent listens on `http://0.0.0.0:8787`
- tunnel exposes `https://your-agent.example.com`
- GitHub Actions calls that URL

### Option B - Self-hosted GitHub runner on your laptop
Run a self-hosted runner on the same machine as Ollama and the local agent.

Then the workflow can call:

- `http://127.0.0.1:8787/review`

This is the simplest setup for a POC.

## Step 5 - Configure GitHub Secret

In the GitHub repository settings for [`https://github.com/amverma/vessel-operations-service.git`](https://github.com/amverma/vessel-operations-service.git), add:

- `LLAMA_AGENT_BASE_URL`

Set it to one of:

- `http://127.0.0.1:8787` for a self-hosted runner on your laptop
- `https://your-public-agent-endpoint` if using a tunnel or hosted service

The workflow already reads this secret in [`workflows/resiliencecheck.yml`](workflows/resiliencecheck.yml).

## Step 6 - How the Workflow Uses the API

[`workflows/resiliencecheck.yml`](workflows/resiliencecheck.yml) now does this:

1. collect changed files
2. capture PR diff
3. package PR context with [`llama-agent/package-pr-for-bob.js`](llama-agent/package-pr-for-bob.js)
4. call `${LLAMA_AGENT_BASE_URL}/review`
5. persist:
   - `.github/artifacts/bob-review.json`
   - `.github/artifacts/bob-review.md`
6. validate with [`llama-agent/validate-bob-review-result.js`](llama-agent/validate-bob-review-result.js)
7. comment on the PR
8. pass/fail based on `block_merge`

## Step 7 - Recommended POC Deployment Model

For your laptop-based setup, the easiest real-time integration is:

1. keep Ollama running locally
2. run [`llama-agent/server.js`](llama-agent/server.js) locally
3. configure a **self-hosted GitHub runner** on the same laptop
4. set `LLAMA_AGENT_BASE_URL=http://127.0.0.1:8787`
5. run PRs through the workflow

This avoids public exposure and avoids tunnel instability.

## Step 8 - Verify End-to-End

### Test A - Health check
Confirm the local service responds:

```powershell
Invoke-RestMethod -Method Get -Uri http://127.0.0.1:8787/health
```

### Test B - Local review generation
Call `/review` with a sample bundle and confirm it returns:

- `review_json`
- `review_markdown`

### Test C - GitHub workflow run
Create a PR that changes a monitored file such as:

- `src/**`
- `config/**`
- `application.yml`

Then verify in Actions artifacts:

- `.github/artifacts/bob-pr-review-bundle.json`
- `.github/artifacts/bob-review.json`
- `.github/artifacts/bob-review.md`

### Test D - PASS case
Confirm the agent returns:

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

Expected:
- workflow passes
- PR comment is posted
- merge is allowed

### Test E - FAIL case
Confirm the agent returns:

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

Expected:
- workflow fails
- PR comment is posted
- merge is blocked

## Real-Time GitHub Integration Summary

If you want this to work in real time from GitHub to your laptop-hosted Llama agent, you need:

- a reachable API URL for [`llama-agent/server.js`](llama-agent/server.js)
- either:
  - a self-hosted GitHub runner on your laptop
  - or a public tunnel/hosted endpoint
- the GitHub secret `LLAMA_AGENT_BASE_URL`
- Ollama running with the configured model

## Final Note

This repository is now prepared for a Llama/Ollama-backed agentic resilience checker. The remaining operational step is to run [`llama-agent/server.js`](llama-agent/server.js) continuously and make its `/review` endpoint reachable from the GitHub Actions runner that executes [`workflows/resiliencecheck.yml`](workflows/resiliencecheck.yml).
