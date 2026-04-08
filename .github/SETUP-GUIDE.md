# R1-R20 Resilience Guardrail Checker - Complete Setup Guide

This guide provides step-by-step instructions to set up the R1-R20 Resilience Guardrail Checker with branch protection rules that **block merges** until all checks pass.

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start](#quick-start)
3. [Detailed Setup Instructions](#detailed-setup-instructions)
4. [Verification Steps](#verification-steps)
5. [Troubleshooting](#troubleshooting)
6. [Configuration Options](#configuration-options)

---

## Prerequisites

✅ **Repository Requirements:**
- GitHub repository (public or private with GitHub Team/Enterprise)
- Admin access to repository settings
- Main branch already exists

✅ **Important Note for Private Repositories:**
- Branch protection with required status checks requires **GitHub Team** ($4/user/month) or **Enterprise**
- For **FREE** enforcement, make your repository **public**
- Private repos on free tier will show checks but won't block merges

---

## Quick Start

### Step 1: Ensure Repository is Public (For Free Tier)

1. Go to **Settings** → **General**
2. Scroll to "Danger Zone"
3. Click **Change visibility** → **Change to public**
4. Confirm the change

> **Skip this step** if you have GitHub Team/Enterprise or don't mind manual enforcement.

### Step 2: Trigger the Workflow Once

The status check must run at least once before it appears in branch protection settings.

**Option A: Manual Trigger (Fastest)**
1. Go to **Actions** tab
2. Select **R1-R20 Resilience Guardrail Checker**
3. Click **Run workflow** → Select `main` branch → **Run workflow**
4. Wait 1-2 minutes for completion

**Option B: Create a Test PR**
```bash
git checkout -b test-resilience-check
echo "# Test" >> TEST.md
git add TEST.md
git commit -m "Test: Trigger resilience check"
git push -u origin test-resilience-check
```
Then create a PR from this branch to `main`.

### Step 3: Configure Branch Protection

1. Go to: `https://github.com/YOUR_USERNAME/vessel-operations-service/settings/branch_protection_rules/new`

2. **Branch name pattern:** `main`

3. **Enable these settings:**

   ✅ **Require a pull request before merging**
   - Optional: Set "Required number of approvals" to 1 or 2
   
   ✅ **Require status checks to pass before merging**
   - ✅ Check: **Require branches to be up to date before merging**
   - In the search box, type: `R1-R20 Guardrail Check`
   - Click on it to add it to required checks
   
   ✅ **Do not allow bypassing the above settings** (Recommended)
   - This prevents admins from bypassing the rules

4. Click **Create** at the bottom

### Step 4: Verify It Works

Create a test PR to verify the merge button is blocked:

```bash
git checkout main
git pull origin main
git checkout -b verify-branch-protection
echo "# Verification test" >> VERIFY.md
git add VERIFY.md
git commit -m "Test: Verify branch protection works"
git push -u origin verify-branch-protection
```

Create the PR and observe:
- ⏳ Merge button disabled while checks run
- ❌ "Merging is blocked" if checks fail
- ✅ Merge button enabled only when checks pass

---

## Detailed Setup Instructions

### Understanding the Components

The R1-R20 Resilience Checker consists of:

1. **Workflow File** (`.github/workflows/resiliencecheck.yml`)
   - Runs automatically on pull requests
   - Executes the resilience checker script
   - Posts results as PR comments
   - Reports status to GitHub

2. **Checker Script** (`.github/scripts/resilience_checker.py`)
   - Python script that analyzes code
   - Checks for R1-R20 resilience patterns
   - Generates detailed reports

3. **Configuration File** (`.github/resilience-checker-config.yml`)
   - Configures which checks to run
   - Sets severity thresholds
   - Defines exclusions and paths

### Workflow Triggers

The workflow runs automatically when:
- A pull request is opened
- New commits are pushed to an open PR
- A PR is reopened
- Changes are made to monitored paths (`src/`, `k8s/`, `helm/`, etc.)

You can also trigger it manually:
- Go to **Actions** → **R1-R20 Resilience Guardrail Checker** → **Run workflow**

### Branch Protection Behavior

Once configured, branch protection enforces:

| Check Status | Merge Button | Message |
|--------------|--------------|---------|
| ⏳ Running | Disabled | "Waiting for status checks" |
| ❌ Failed | Disabled | "Merging is blocked - Required status check 'R1-R20 Guardrail Check' must pass" |
| ✅ Passed | Enabled | "All checks have passed" |

---

## Verification Steps

### 1. Verify Workflow is Active

```bash
# Check if workflow file exists
ls -la .github/workflows/resiliencecheck.yml

# Check if script exists
ls -la .github/scripts/resilience_checker.py
```

### 2. Verify Branch Protection is Configured

1. Go to: `Settings` → `Branches`
2. You should see a rule for `main` branch
3. Click **Edit** to review settings
4. Verify "R1-R20 Guardrail Check" is in the required checks list

### 3. Test with a Real PR

Create a PR that modifies code in `src/`:

```bash
git checkout -b feature/test-real-check
# Make a change to a Java file
echo "// Test comment" >> src/main/java/com/freightforwarder/vesseloperations/VesselOperationsServiceApplication.java
git add .
git commit -m "Test: Real resilience check"
git push -u origin feature/test-real-check
```

Expected behavior:
1. Workflow runs automatically
2. PR comment appears with check results
3. Merge button is disabled until checks pass
4. Status shows in PR checks section

---

## Troubleshooting

### Issue: "R1-R20 Guardrail Check" doesn't appear in branch protection search

**Cause:** The workflow hasn't run yet, so GitHub doesn't know about this status check.

**Solution:**
1. Trigger the workflow manually (Actions → Run workflow)
2. OR create a test PR to trigger it
3. Wait for workflow to complete
4. Go back to branch protection settings
5. The status check should now appear

### Issue: Merge button is still enabled despite failed checks

**Cause:** Branch protection is not configured or not enforced.

**Solutions:**

**For Private Repositories (Free Tier):**
- GitHub Free doesn't enforce branch protection on private repos
- Either upgrade to GitHub Team or make repository public

**For Public Repositories:**
- Verify branch protection rule exists
- Check that "R1-R20 Guardrail Check" is in required checks
- Ensure "Do not allow bypassing" is enabled

### Issue: Workflow fails with "No file matched to requirements.txt"

**Cause:** Python setup trying to use pip cache without requirements file.

**Solution:** This has been fixed in the current workflow. If you see this error:
1. Pull the latest changes
2. The workflow now installs dependencies without caching

### Issue: Workflow runs but doesn't post PR comment

**Cause:** Missing permissions or GitHub token issues.

**Solution:**
1. Check workflow has `pull-requests: write` permission
2. Verify `GITHUB_TOKEN` is available
3. Check PR comments section for any errors

### Issue: Branch protection rule not saving

**Cause:** Insufficient permissions or repository settings.

**Solution:**
1. Verify you have admin access to the repository
2. Check if organization policies allow branch protection
3. Try using the direct URL: `https://github.com/YOUR_USERNAME/REPO/settings/branch_protection_rules/new`

---

## Configuration Options

### Adjusting Severity Threshold

Edit `.github/resilience-checker-config.yml`:

```yaml
# Only CRITICAL failures block merge
severity_threshold: CRITICAL

# HIGH and above block merge
severity_threshold: HIGH

# MEDIUM and above block merge
severity_threshold: MEDIUM

# All failures block merge
severity_threshold: LOW
```

### Disabling Specific Checks

Edit `.github/resilience-checker-config.yml`:

```yaml
checks:
  R1:
    enabled: false  # Disable circuit breaker check
    severity: CRITICAL
    name: "Circuit Breaker Pattern"
```

⚠️ **Warning:** Disabling checks requires Architecture Board approval.

### Adding Path Exclusions

Edit `.github/resilience-checker-config.yml`:

```yaml
exclusions:
  paths:
    - "**/test/**"
    - "**/experimental/**"
    - "**/legacy/**"
```

### Customizing Monitored Paths

Edit `.github/workflows/resiliencecheck.yml`:

```yaml
on:
  pull_request:
    paths:
      - 'src/**'
      - 'k8s/**'
      - 'your-custom-path/**'  # Add your path
```

---

## Advanced Configuration

### Running Checks on Specific Branches

Edit `.github/workflows/resiliencecheck.yml`:

```yaml
on:
  pull_request:
    branches:
      - main
      - develop
      - release/*
```

### Manual Workflow Dispatch with Custom Threshold

1. Go to **Actions** → **R1-R20 Resilience Guardrail Checker**
2. Click **Run workflow**
3. Select severity threshold (CRITICAL, HIGH, MEDIUM, LOW)
4. Click **Run workflow**

### Bypass Mechanism (Requires Approval)

To bypass checks in emergency situations:

1. Add label `bypass-resilience-check` to PR
2. An issue is automatically created for Architecture Board
3. Requires 2 approvals (configurable)
4. Must document reason in ADR format

---

## Best Practices

### 1. Always Review Check Results

Even if checks pass, review the PR comment for:
- Warnings about potential issues
- Recommendations for improvements
- Summary of what was checked

### 2. Don't Disable Checks Without Approval

All R1-R20 checks are there for a reason:
- Document why a check needs to be disabled
- Get Architecture Board approval
- Create an ADR (Architecture Decision Record)

### 3. Keep Configuration in Sync

When updating the checker:
- Update workflow file
- Update configuration file
- Update this documentation
- Test with a PR before merging

### 4. Monitor Workflow Performance

Check workflow execution times:
- Typical run: 1-2 minutes
- If slower: Check for large file changes
- Optimize by adjusting monitored paths

---

## Support and Maintenance

### Updating the Checker

To update to a new version:

```bash
# Pull latest changes
git pull origin main

# Review changes
git log --oneline .github/

# Test with a PR
git checkout -b test-updated-checker
# Make a small change and create PR
```

### Getting Help

If you encounter issues:

1. Check this guide's [Troubleshooting](#troubleshooting) section
2. Review workflow logs in Actions tab
3. Check PR comments for error details
4. Review configuration files for syntax errors

### Reporting Issues

When reporting issues, include:
- Workflow run URL
- Error messages from logs
- Configuration files (sanitized)
- Steps to reproduce

---

## Summary Checklist

Use this checklist to verify your setup:

- [ ] Repository is public (or have GitHub Team/Enterprise)
- [ ] Workflow files are present in `.github/`
- [ ] Workflow has run at least once successfully
- [ ] Branch protection rule created for `main` branch
- [ ] "R1-R20 Guardrail Check" added to required checks
- [ ] "Require branches to be up to date" is enabled
- [ ] "Do not allow bypassing" is enabled (optional but recommended)
- [ ] Test PR created and merge button is blocked during checks
- [ ] Merge button enables only when checks pass
- [ ] PR comments show detailed check results

---

## Quick Reference

### Key URLs

- **Branch Protection:** `Settings` → `Branches`
- **Workflow Runs:** `Actions` → `R1-R20 Resilience Guardrail Checker`
- **Configuration:** `.github/resilience-checker-config.yml`
- **Workflow:** `.github/workflows/resiliencecheck.yml`

### Key Commands

```bash
# Create test branch
git checkout -b test-branch

# Trigger workflow
# (Make changes to files in src/, k8s/, etc.)

# Push and create PR
git push -u origin test-branch
```

### Status Check Name

The exact name that appears in branch protection:
```
R1-R20 Guardrail Check
```

---

**Last Updated:** 2026-04-08  
**Version:** 1.0  
**Repository:** https://github.com/amverma/vessel-operations-service