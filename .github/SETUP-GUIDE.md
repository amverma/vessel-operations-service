# R1-R23 Resilience Checker - Setup Guide

Complete guide to set up the R1-R23 Resilience Checker with automated merge protection.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start](#quick-start)
3. [Detailed Setup](#detailed-setup)
4. [Branch Protection Configuration](#branch-protection-configuration)
5. [Configuration Options](#configuration-options)
6. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Access
- GitHub repository (public or private with GitHub Team/Enterprise)
- **Admin** permissions on the repository
- Main branch exists

### Important: Private Repository Limitations
- Branch protection with required status checks requires **GitHub Team** ($4/user/month) or **Enterprise**
- For **FREE** enforcement, make your repository **public**
- Private repos on free tier will show checks but won't block merges

---

## Quick Start

### Step 1: Make Repository Public (Free Tier Only)

**Skip this if you have GitHub Team/Enterprise**

1. Go to **Settings** → **General**
2. Scroll to "Danger Zone"
3. Click **Change visibility** → **Change to public**

### Step 2: Run Workflow Once

The workflow must run at least once before it appears in branch protection settings.

**Option A: Manual Trigger (Recommended)**
1. Go to **Actions** tab
2. Select **R1-R23 Resilience Guardrail Checker**
3. Click **Run workflow** → Select `main` → **Run workflow**
4. Wait 1-2 minutes ✅

**Option B: Create Test PR**
```bash
git checkout -b test-resilience-check
echo "# Test" >> TEST.md
git add TEST.md
git commit -m "Test: Trigger resilience check"
git push -u origin test-resilience-check
```
Then create PR via GitHub UI.

### Step 3: Verify Workflow Ran Successfully

1. Go to **Actions** tab
2. Check that the workflow completed successfully
3. Note: The status check name is `R1-R20 Guardrail Check`

### Step 4: Configure Branch Protection

See [Branch Protection Configuration](#branch-protection-configuration) section below for detailed steps.

### Step 5: Test It Works

```bash
git checkout main
git pull origin main
git checkout -b verify-protection
echo "# Verification" >> VERIFY.md
git add VERIFY.md
git commit -m "Test: Verify protection"
git push -u origin verify-protection
```

Create PR and verify:
- ⏳ Merge button disabled while checks run
- ✅ Merge enabled when checks pass
- ❌ Merge blocked when checks fail

---

## Detailed Setup

### What Gets Installed

The R1-R20 Resilience Checker includes:

1. **Workflow** (`.github/workflows/resiliencecheck.yml`)
   - Runs on PR creation/updates
   - Posts results as PR comments
   - Blocks merge on failures

2. **Checker Script** (`.github/scripts/resilience_checker.py`)
   - Analyzes code for R1-R20 patterns
   - Smart, context-aware detection
   - Generates detailed reports

3. **Configuration** (`.github/resilience-checker-config.yml`)
   - Controls which checks run
   - Sets severity thresholds
   - Defines exclusions

### When Workflow Runs

Automatically triggers when:
- Pull request opened
- New commits pushed to PR
- PR reopened
- Changes in monitored paths:
  - `src/**`, `k8s/**`, `helm/**`, `schemas/**`
  - `infrastructure/**`, `specs/**`, `config/**`
  - `application.yml`, `pom.xml`, `build.gradle`

Manual trigger:
- **Actions** → **R1-R20 Resilience Guardrail Checker** → **Run workflow**

---

## Branch Protection Configuration

### Step-by-Step Instructions

#### 1. Navigate to Branch Protection

- **Direct URL:** `https://github.com/YOUR_USERNAME/vessel-operations-service/settings/branches`
- **Or via UI:** Settings → Branches (left sidebar)

#### 2. Create Protection Rule

- Click **Add branch protection rule** (or edit existing for `main`)
- **Branch name pattern:** `main`

#### 3. Configure Required Settings

**Enable these checkboxes:**

☑️ **Require a pull request before merging**
- Set "Required number of approvals": 1 or 2 (optional)
- ☑️ Dismiss stale pull request approvals when new commits are pushed

☑️ **Require status checks to pass before merging** ⚠️ CRITICAL
- ☑️ **Require branches to be up to date before merging**
- In search box "Status checks that are required":
  - Type: `R1-R20 Guardrail Check`
  - Click to add it
  - **Note:** If not visible, workflow hasn't run yet (go back to Quick Start Step 2)

☑️ **Require conversation resolution before merging** (Recommended)

☑️ **Do not allow bypassing the above settings** (Recommended)
- Prevents admins from bypassing checks
- Leave unchecked if you want admin override capability

#### 4. Save

- Scroll to bottom
- Click **Create** (or **Save changes**)

### How It Works

| Check Status | Merge Button | Message |
|--------------|--------------|---------|
| ⏳ Running | Disabled | "Waiting for status checks" |
| ✅ Passed | Enabled | "All checks passed" |
| ❌ Failed | Disabled | "Merging is blocked" |

### Workflow Behavior

- ✅ **PASS**: Allows merge when CRITICAL checks pass
- ❌ **FAIL**: Blocks merge when CRITICAL checks fail
- 📊 Posts detailed PR comment with results
- 📦 Uploads report artifacts

### Emergency Bypass

**Use only in emergencies:**

1. Add label `bypass-resilience-check` to PR
2. Architecture Board gets notified
3. Issue created requiring:
   - Architecture Board approval
   - ADR documentation
   - Minimum 2 approvals

---

## Configuration Options

### Adjust Severity Threshold

Edit `.github/resilience-checker-config.yml`:

```yaml
# Only CRITICAL failures block merge (default)
severity_threshold: CRITICAL

# HIGH and above block merge
severity_threshold: HIGH

# MEDIUM and above block merge
severity_threshold: MEDIUM

# All failures block merge
severity_threshold: LOW
```

### Disable Specific Checks

Edit `.github/resilience-checker-config.yml`:

```yaml
checks:
  R1:
    enabled: false  # Disable circuit breaker check
    severity: CRITICAL
```

⚠️ Requires Architecture Board approval

### Add Path Exclusions

Edit `.github/resilience-checker-config.yml`:

```yaml
exclusions:
  paths:
    - "**/test/**"
    - "**/experimental/**"
```

### Customize Monitored Paths

Edit `.github/workflows/resiliencecheck.yml`:

```yaml
on:
  pull_request:
    paths:
      - 'src/**'
      - 'your-custom-path/**'
```

---

## Troubleshooting

### Status Check Doesn't Appear in Branch Protection

**Cause:** Workflow hasn't run yet

**Solution:**
1. Go to **Actions** → **Run workflow**
2. Wait for completion
3. Return to branch protection settings
4. Status check should now appear

### Merge Button Still Enabled Despite Failed Checks

**Cause:** Branch protection not configured or not enforced

**Solution:**

**For Private Repos (Free Tier):**
- Upgrade to GitHub Team or make repository public

**For Public Repos:**
- Verify branch protection rule exists
- Check "R1-R20 Guardrail Check" is in required checks
- Ensure "Require status checks to pass" is enabled

### Workflow Fails with "No file matched to requirements.txt"

**Solution:** Pull latest changes - this has been fixed

### Workflow Runs But Doesn't Post PR Comment

**Solution:**
1. Check workflow has `pull-requests: write` permission
2. Review workflow logs in Actions tab

### Branch Protection Rule Not Saving

**Solution:**
1. Verify you have admin access
2. Check organization policies
3. Try direct URL: `https://github.com/YOUR_USERNAME/REPO/settings/branch_protection_rules/new`

---

## Verification Checklist

Use this to verify your setup:

**Initial Setup:**
- [ ] Repository is public (or have GitHub Team/Enterprise)
- [ ] Workflow files exist in `.github/`
- [ ] Workflow has run successfully at least once

**Branch Protection:**
- [ ] Branch protection rule created for `main`
- [ ] "Require status checks to pass" is enabled
- [ ] "R1-R20 Guardrail Check" is in required checks
- [ ] "Require branches to be up to date" is enabled

**Testing:**
- [ ] Test PR created
- [ ] Workflow runs automatically
- [ ] PR comment shows results
- [ ] Merge button behavior correct (disabled when failing, enabled when passing)

---

## Quick Reference

### Key URLs
- **Settings:** `https://github.com/YOUR_USERNAME/vessel-operations-service/settings`
- **Branch Protection:** Settings → Branches
- **Workflow Runs:** Actions → R1-R20 Resilience Guardrail Checker

### Status Check Name
```
R1-R20 Guardrail Check
```

### Important Files
```
.github/
├── workflows/resiliencecheck.yml          # Workflow
├── scripts/resilience_checker.py          # Checker
├── resilience-checker-config.yml          # Config
├── R1-R20-RESILIENCE-CHECKLIST.md        # Pattern guide
└── RESILIENCE-CHECKER-TECHNICAL-GUIDE.md # Technical docs
```

### Test Commands
```bash
# Create test branch
git checkout -b test-branch

# Make changes (modify files in src/, k8s/, etc.)

# Push and create PR
git add .
git commit -m "Your message"
git push -u origin test-branch
```

---

## Additional Resources

- 📚 [R1-R20 Resilience Checklist](.github/R1-R20-RESILIENCE-CHECKLIST.md) - Pattern guide
- 🔧 [Technical Guide](.github/RESILIENCE-CHECKER-TECHNICAL-GUIDE.md) - How it works
- 📖 [README](.github/README-RESILIENCE-CHECKER.md) - Overview

---

**Last Updated:** 2026-04-16  
**Version:** 2.1  
**Repository:** https://github.com/amverma/vessel-operations-service