# R1-R20 Guardrail Check - Quick Start Guide

A simple, step-by-step guide to set up and test the R1-R20 Resilience Guardrail Checker.

---

## What is R1-R20 Guardrail Check?

A GitHub Actions workflow that automatically checks your code for 20 critical resilience patterns (R1-R20) whenever you create a pull request. It ensures your code follows best practices for:
- Circuit breakers
- Retry logic
- Timeouts
- Health checks
- Error handling
- And 15 more resilience patterns

**Key Benefit:** Blocks merging of PRs that fail critical resilience checks, ensuring code quality.

---

## How It Works Behind the Scenes

### Components:

1. **GitHub Actions Workflow** (`.github/workflows/resiliencecheck.yml`)
   - Automatically runs when you create or update a PR
   - Executes a Python script that analyzes your code
   - Reports results back to GitHub
   - Creates a "status check" that GitHub can use to block merges

2. **Python Checker Script** (`.github/scripts/resilience_checker.py`)
   - Scans your code files for resilience patterns
   - Checks against R1-R20 requirements
   - Generates a detailed report

3. **Configuration File** (`.github/resilience-checker-config.yml`)
   - Defines which checks to run
   - Sets severity levels (CRITICAL, HIGH, MEDIUM, LOW)
   - Specifies which files to check

### Workflow Process:

```
PR Created → Workflow Triggers → Python Script Runs → 
Checks Code → Generates Report → Posts PR Comment → 
Reports Status to GitHub → GitHub Blocks/Allows Merge
```

---

## Setup Steps (5 Minutes)

### Step 1: Merge the Workflow Files

1. Go to your repository: https://github.com/amverma/vessel-operations-service
2. You should see a PR with the workflow files
3. Review and merge the PR
4. The workflow is now installed but not yet enforced

### Step 2: Trigger the Workflow Once

**Why?** GitHub only shows status checks that have run at least once.

**How to trigger:**

1. Go to **Actions** tab in your repository
2. Click on **R1-R20 Resilience Guardrail Checker** in the left sidebar
3. Click the **Run workflow** button (top right)
4. Select branch: `main`
5. Click **Run workflow**
6. Wait 1-2 minutes for it to complete (you'll see a green checkmark)

### Step 3: Set Up Branch Protection

Now that the workflow has run once, we can add it to branch protection:

1. Go to **Settings** → **Branches**
2. Click **Add branch protection rule**
3. Fill in:
   - **Branch name pattern:** `main`
4. Enable these checkboxes:
   - ✅ **Require a pull request before merging**
   - ✅ **Require status checks to pass before merging**
     - ✅ **Require branches to be up to date before merging**
     - In the search box below, type: `R1-R20 Guardrail Check`
     - Click on it when it appears to add it
   - ✅ **Do not allow bypassing the above settings** (recommended)
5. Click **Create** at the bottom

**Done!** The checker is now active and will block merges.

### Step 4: Test It Works

Let's verify the merge button gets blocked:

```bash
# Create a test branch
git checkout main
git pull origin main
git checkout -b test-branch-protection

# Make a small change
echo "# Test file" > TEST.md
git add TEST.md
git commit -m "Test: Verify branch protection"
git push -u origin test-branch-protection
```

Then:
1. Go to your repository on GitHub
2. Click **Compare & pull request**
3. Create the PR
4. Watch the workflow run (takes 1-2 minutes)
5. **Verify:** The merge button should be disabled with message "Merging is blocked"
6. Once checks pass, the merge button will be enabled

---

## What You'll See

### During PR Review:

1. **Status Check in PR:**
   - ⏳ Yellow dot = Checks running
   - ✅ Green checkmark = All checks passed
   - ❌ Red X = Checks failed

2. **Automated PR Comment:**
   - Summary table showing passed/failed checks
   - Detailed list of any failures
   - Recommendations for fixing issues

3. **Merge Button Behavior:**
   - **Disabled** while checks run or if checks fail
   - **Enabled** only when all checks pass

### Example PR Comment:

```
🛡️ R1-R20 Resilience Guardrail Check v1.0

Status: ✅ PASSED
Checked: 2026-04-08 10:30:00

Summary
| Severity | Passed | Failed | Total |
|----------|--------|--------|-------|
| CRITICAL | 8      | 0      | 8     |
| HIGH     | 10     | 0      | 10    |
| MEDIUM   | 2      | 0      | 2     |

✅ Passed Checks (20)
- R1: Circuit Breaker Pattern
- R2: Retry with Exponential Backoff
- R3: Timeout Configuration
...
```

---

## Common Issues & Solutions

### Issue 1: "R1-R20 Guardrail Check" doesn't appear in search

**Cause:** Workflow hasn't run yet.

**Solution:** Complete Step 2 above (trigger the workflow once).

### Issue 2: Merge button still enabled despite failed checks

**Cause:** Branch protection not configured or repository is private on free tier.

**Solution:**
- Verify Step 3 is completed correctly
- For private repos: Upgrade to GitHub Team or make repository public

### Issue 3: Workflow fails with error

**Cause:** Various reasons (permissions, syntax, etc.)

**Solution:**
1. Go to **Actions** tab
2. Click on the failed workflow run
3. Review the error logs
4. Common fixes:
   - Ensure Python dependencies are correct
   - Check file paths in configuration
   - Verify workflow syntax

---

## Configuration (Optional)

### Change Severity Threshold

Edit `.github/resilience-checker-config.yml`:

```yaml
# Only block on CRITICAL failures (default)
severity_threshold: CRITICAL

# Block on HIGH and above
severity_threshold: HIGH
```

### Disable Specific Checks

Edit `.github/resilience-checker-config.yml`:

```yaml
checks:
  R1:
    enabled: false  # Disable this check
```

### Add Path Exclusions

Edit `.github/resilience-checker-config.yml`:

```yaml
exclusions:
  paths:
    - "**/test/**"
    - "**/experimental/**"
```

---

## Quick Reference

### Key Files:
- Workflow: `.github/workflows/resiliencecheck.yml`
- Script: `.github/scripts/resilience_checker.py`
- Config: `.github/resilience-checker-config.yml`

### Key URLs:
- Actions: `https://github.com/YOUR_USERNAME/vessel-operations-service/actions`
- Branch Protection: `https://github.com/YOUR_USERNAME/vessel-operations-service/settings/branches`

### Status Check Name:
```
R1-R20 Guardrail Check
```
(Use this exact name when setting up branch protection)

### Typical Workflow Time:
- 1-2 minutes per PR

---

## Need Help?

1. Check the detailed guide: `.github/SETUP-GUIDE.md`
2. Review workflow logs in Actions tab
3. Check PR comments for specific error details

---

**Last Updated:** 2026-04-08  
**Repository:** https://github.com/amverma/vessel-operations-service