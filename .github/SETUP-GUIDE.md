# R1-R20 Resilience Checker - Complete Setup Guide

This comprehensive guide provides step-by-step instructions to set up the R1-R20 Resilience Checker with branch protection rules that **automatically block PR merges** until all critical checks pass.

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start (5 Minutes)](#quick-start-5-minutes)
3. [Detailed Setup Instructions](#detailed-setup-instructions)
4. [Branch Protection Configuration](#branch-protection-configuration)
5. [Verification & Testing](#verification--testing)
6. [Configuration Options](#configuration-options)
7. [Troubleshooting](#troubleshooting)
8. [Best Practices](#best-practices)

---

## Prerequisites

### Repository Requirements

✅ **Access & Permissions:**
- GitHub repository (public or private with GitHub Team/Enterprise)
- **Admin** or **Maintain** permissions on the repository
- Main branch already exists

✅ **Important Note for Private Repositories:**
- Branch protection with required status checks requires **GitHub Team** ($4/user/month) or **Enterprise**
- For **FREE** enforcement, make your repository **public**
- Private repos on free tier will show checks but won't block merges

### What You'll Get

After setup, the system will:
- ✅ Automatically run R1-R20 resilience checks on every PR
- ✅ Post detailed results as PR comments
- ✅ Block merge button when CRITICAL checks fail
- ✅ Generate downloadable reports and badges
- ✅ Notify Architecture Board on bypass attempts

---

## Quick Start (5 Minutes)

Follow these steps to get up and running quickly:

### Step 1: Make Repository Public (For Free Tier)

**Skip this if you have GitHub Team/Enterprise**

1. Go to **Settings** → **General**
2. Scroll to "Danger Zone"
3. Click **Change visibility** → **Change to public**
4. Confirm the change

### Step 2: Trigger the Workflow Once

The status check must run at least once before it appears in branch protection settings.

**Option A: Manual Trigger (Fastest - Recommended)**

1. Go to **Actions** tab in your repository
2. Select **R1-R20 Resilience Guardrail Checker** from the left sidebar
3. Click **Run workflow** button (top right)
4. Select `main` branch from dropdown
5. Click **Run workflow** button
6. Wait 1-2 minutes for completion ✅

**Option B: Create a Test PR**

```bash
git checkout -b test-resilience-check
echo "# Test" >> TEST.md
git add TEST.md
git commit -m "Test: Trigger resilience check"
git push -u origin test-resilience-check
```

Then create a PR from this branch to `main` via GitHub UI.

### Step 3: Configure Branch Protection

1. **Navigate to Settings:**
   - Go to: `https://github.com/YOUR_USERNAME/vessel-operations-service/settings/branches`
   - Or: **Settings** → **Branches** (left sidebar under "Code and automation")

2. **Create Protection Rule:**
   - Click **Add branch protection rule** (or edit existing rule for `main`)
   - **Branch name pattern:** `main`

3. **Enable Required Settings:**

   ☑️ **Require a pull request before merging**
   - Optional: Set "Required number of approvals" to 1 or 2
   - ☑️ Dismiss stale pull request approvals when new commits are pushed
   
   ☑️ **Require status checks to pass before merging**
   - ☑️ **Require branches to be up to date before merging**
   - In the search box under "Status checks that are required":
     - Type: `R1-R20 Guardrail Check`
     - Click on it to add it to required checks
     - **Note:** If you don't see it, the workflow hasn't run yet - go back to Step 2
   
   ☑️ **Require conversation resolution before merging** (Recommended)
   
   ☑️ **Do not allow bypassing the above settings** (Recommended)
   - This prevents even admins from bypassing the checks
   - Leave unchecked if you want admins to have override capability

4. **Save:**
   - Scroll to bottom
   - Click **Create** (or **Save changes** if editing)

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

Create the PR via GitHub UI and observe:
- ⏳ Merge button disabled while checks run
- ✅ Merge button enabled when checks pass
- ❌ "Merging is blocked" message if checks fail

**🎉 Setup Complete!** Your repository now has automated resilience checking with merge protection.

---

## Detailed Setup Instructions

### Understanding the Components

The R1-R20 Resilience Checker consists of:

#### 1. Workflow File (`.github/workflows/resiliencecheck.yml`)
- Runs automatically on pull requests
- Executes the resilience checker script
- Posts results as PR comments
- Reports status to GitHub
- Uploads artifacts (reports, badges)

#### 2. Checker Script (`.github/scripts/resilience_checker.py`)
- Python script that analyzes code
- Checks for R1-R20 resilience patterns
- Uses smart, context-aware detection
- Generates detailed reports

#### 3. Configuration File (`.github/resilience-checker-config.yml`)
- Configures which checks to run
- Sets severity thresholds
- Defines exclusions and monitored paths
- Customizes behavior

#### 4. Dependencies (`.requirements.txt`)
- Python dependencies for the checker
- Automatically installed by workflow

### Workflow Triggers

The workflow runs automatically when:
- ✅ A pull request is opened
- ✅ New commits are pushed to an open PR
- ✅ A PR is reopened
- ✅ Changes are made to monitored paths:
  - `src/**` - Source code
  - `k8s/**` - Kubernetes manifests
  - `helm/**` - Helm charts
  - `schemas/**` - Avro schemas
  - `infrastructure/**` - Infrastructure code
  - `specs/**` - API specifications
  - `config/**` - Configuration files
  - `application.yml` / `application.yaml`
  - `pom.xml` / `build.gradle`

You can also trigger it manually:
- Go to **Actions** → **R1-R20 Resilience Guardrail Checker** → **Run workflow**

---

## Branch Protection Configuration

### How Branch Protection Works

Once configured, branch protection enforces these rules:

| Check Status | Merge Button | Message |
|--------------|--------------|---------|
| ⏳ Running | Disabled | "Waiting for status checks to complete" |
| ❌ Failed | Disabled | "Merging is blocked - Required status check 'R1-R20 Guardrail Check' must pass" |
| ✅ Passed | Enabled | "All checks have passed - Ready to merge" |

### Workflow Behavior

The R1-R20 Resilience Checker workflow will:

- ✅ **PASS**: Allow merging when all CRITICAL checks pass (based on severity threshold)
- ❌ **FAIL**: Block merging when any CRITICAL checks fail
- 📊 Post detailed results as a PR comment with:
  - Summary table by severity
  - Failed checks with recommendations
  - Passed checks (expandable)
  - Links to documentation
- 📦 Upload artifacts with full reports
- 🏷️ Generate status badge

### Bypass Mechanism (Emergency Use Only)

If you need to bypass the check in an emergency:

1. Add the label `bypass-resilience-check` to the PR
2. This will trigger a notification to the Architecture Board
3. An issue will be automatically created requiring:
   - Architecture Board approval
   - ADR (Architecture Decision Record) documentation
   - Minimum 2 approvals (configurable)
4. All exceptions must follow governance policies

**⚠️ Important:** Bypasses should be rare and well-documented.

---

## Verification & Testing

### 1. Verify Workflow is Active

```bash
# Check if workflow file exists
ls -la .github/workflows/resiliencecheck.yml

# Check if script exists
ls -la .github/scripts/resilience_checker.py

# Check if config exists
ls -la .github/resilience-checker-config.yml
```

### 2. Verify Branch Protection is Configured

1. Go to: **Settings** → **Branches**
2. You should see a rule for `main` branch
3. Click **Edit** to review settings
4. Verify "R1-R20 Guardrail Check" is in the required checks list
5. Verify "Require status checks to pass before merging" is checked

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
1. ✅ Workflow runs automatically
2. ✅ PR comment appears with check results
3. ✅ Merge button is disabled until checks pass
4. ✅ Status shows in PR checks section
5. ✅ Artifacts are uploaded

### 4. Test Failure Scenario

Create a PR with a hardcoded secret to test failure:

```bash
git checkout -b test-failure
echo 'String password = "hardcoded123";' >> src/main/java/Test.java
git add .
git commit -m "Test: Should fail R19 (Secrets Management)"
git push -u origin test-failure
```

Expected behavior:
1. ❌ Workflow runs and fails
2. ❌ PR comment shows R19 failure
3. ❌ Merge button is disabled with "Merging is blocked" message
4. ❌ Status check shows as failed

---

## Configuration Options

### Adjusting Severity Threshold

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

**Recommendation:** Keep at `CRITICAL` for production, use `HIGH` for stricter enforcement.

### Disabling Specific Checks

Edit `.github/resilience-checker-config.yml`:

```yaml
checks:
  R1:
    enabled: false  # Disable circuit breaker check
    severity: CRITICAL
    name: "Circuit Breaker Pattern"
  R2:
    enabled: true   # Keep retry check enabled
    severity: HIGH
    name: "Retry with Exponential Backoff"
```

⚠️ **Warning:** Disabling checks requires Architecture Board approval and ADR documentation.

### Adding Path Exclusions

Edit `.github/resilience-checker-config.yml`:

```yaml
exclusions:
  paths:
    - "**/test/**"
    - "**/tests/**"
    - "**/*Test.java"
    - "**/experimental/**"
    - "**/legacy/**"
  files:
    - "README.md"
    - "LICENSE"
    - ".gitignore"
```

### Customizing Monitored Paths

Edit `.github/workflows/resiliencecheck.yml`:

```yaml
on:
  pull_request:
    paths:
      - 'src/**'
      - 'k8s/**'
      - 'helm/**'
      - 'your-custom-path/**'  # Add your path
```

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
4. Select target branch (optional)
5. Click **Run workflow**

---

## Troubleshooting

### Issue: "R1-R20 Guardrail Check" doesn't appear in branch protection search

**Cause:** The workflow hasn't run yet, so GitHub doesn't know about this status check.

**Solution:**
1. Trigger the workflow manually: **Actions** → **Run workflow**
2. OR create a test PR to trigger it
3. Wait for workflow to complete (1-2 minutes)
4. Go back to branch protection settings and refresh
5. The status check should now appear in search results

### Issue: Merge button is still enabled despite failed checks

**Cause:** Branch protection is not configured or not enforced.

**Solutions:**

**For Private Repositories (Free Tier):**
- GitHub Free doesn't enforce branch protection on private repos
- Either upgrade to GitHub Team ($4/user/month) or make repository public
- Verify your plan at: **Settings** → **Billing and plans**

**For Public Repositories:**
- Verify branch protection rule exists: **Settings** → **Branches**
- Check that "R1-R20 Guardrail Check" is in required checks list
- Ensure "Require status checks to pass before merging" is checked
- Verify "Do not allow bypassing" is enabled (if you want strict enforcement)

### Issue: Workflow fails with "No file matched to requirements.txt"

**Cause:** Python setup trying to use pip cache without requirements file.

**Solution:** This has been fixed in the current workflow. If you still see this error:
1. Pull the latest changes: `git pull origin main`
2. The workflow now installs dependencies without caching
3. Verify `requirements.txt` exists in repository root

### Issue: Workflow runs but doesn't post PR comment

**Cause:** Missing permissions or GitHub token issues.

**Solution:**
1. Check workflow has `pull-requests: write` permission (line 44-46 in workflow file)
2. Verify `GITHUB_TOKEN` is available (automatically provided by GitHub)
3. Check PR comments section for any errors
4. Review workflow logs: **Actions** → Select run → View logs

### Issue: Branch protection rule not saving

**Cause:** Insufficient permissions or repository settings.

**Solution:**
1. Verify you have **admin** access to the repository
2. Check if organization policies allow branch protection
3. Try using the direct URL: `https://github.com/YOUR_USERNAME/REPO/settings/branch_protection_rules/new`
4. Contact repository owner if you don't have admin access

### Issue: Workflow runs but status check name doesn't match

**Cause:** Job name in workflow doesn't match the status check name.

**Solution:**
1. Open `.github/workflows/resiliencecheck.yml`
2. Find the job name on line 40: `name: R1-R20 Guardrail Check`
3. Ensure it matches exactly (case-sensitive)
4. If changed, update branch protection to use new name

### Issue: Checks pass but merge is still blocked

**Cause:** Other required checks or branch protection rules.

**Solution:**
1. Check PR for other failing status checks
2. Review all branch protection requirements
3. Ensure branch is up to date with base branch
4. Check for required reviews or conversation resolution

---

## Best Practices

### 1. Always Review Check Results

Even if checks pass, review the PR comment for:
- ⚠️ Warnings about potential issues
- 💡 Recommendations for improvements
- 📊 Summary of what was checked
- 📋 List of passed checks

### 2. Don't Disable Checks Without Approval

All R1-R20 checks are there for a reason:
- 📝 Document why a check needs to be disabled
- ✅ Get Architecture Board approval
- 📄 Create an ADR (Architecture Decision Record)
- ⏰ Set a timeline for re-enabling the check

### 3. Keep Configuration in Sync

When updating the checker:
- ✅ Update workflow file
- ✅ Update configuration file
- ✅ Update documentation
- ✅ Test with a PR before merging to main
- ✅ Communicate changes to team

### 4. Monitor Workflow Performance

Check workflow execution times:
- ✅ Typical run: 1-2 minutes
- ⚠️ If slower (>5 minutes): Check for large file changes
- 🔧 Optimize by adjusting monitored paths
- 📊 Review workflow logs for bottlenecks

### 5. Regular Maintenance

- 🔄 Update checker to latest version quarterly
- 📊 Review failed checks trends
- 🔍 Audit bypass requests
- 📚 Keep documentation current
- 🎓 Train team on resilience patterns

---

## Summary Checklist

Use this checklist to verify your setup:

**Initial Setup:**
- [ ] Repository is public (or have GitHub Team/Enterprise)
- [ ] Workflow files are present in `.github/`
- [ ] Python dependencies file (`requirements.txt`) exists
- [ ] Configuration file (`.github/resilience-checker-config.yml`) exists

**Workflow Verification:**
- [ ] Workflow has run at least once successfully
- [ ] Workflow logs show no errors
- [ ] PR comment was posted with results
- [ ] Artifacts were uploaded

**Branch Protection:**
- [ ] Branch protection rule created for `main` branch
- [ ] "Require status checks to pass before merging" is enabled
- [ ] "R1-R20 Guardrail Check" added to required checks
- [ ] "Require branches to be up to date" is enabled
- [ ] "Do not allow bypassing" is enabled (optional but recommended)

**Testing:**
- [ ] Test PR created with passing checks
- [ ] Merge button enables when checks pass
- [ ] Test PR created with failing checks
- [ ] Merge button is blocked when checks fail
- [ ] PR comments show detailed check results

---

## Quick Reference

### Key URLs

- **Repository Settings:** `https://github.com/YOUR_USERNAME/vessel-operations-service/settings`
- **Branch Protection:** `Settings` → `Branches`
- **Workflow Runs:** `Actions` → `R1-R20 Resilience Guardrail Checker`
- **Configuration:** `.github/resilience-checker-config.yml`
- **Workflow:** `.github/workflows/resiliencecheck.yml`

### Key Commands

```bash
# Create test branch
git checkout -b test-branch

# Make changes to trigger workflow
# (Modify files in src/, k8s/, helm/, etc.)

# Push and create PR
git add .
git commit -m "Your commit message"
git push -u origin test-branch
```

### Status Check Name

The exact name that appears in branch protection:
```
R1-R20 Guardrail Check
```

### Important Files

```
.github/
├── workflows/
│   └── resiliencecheck.yml          # Main workflow
├── scripts/
│   └── resilience_checker.py        # Checker script
├── resilience-checker-config.yml    # Configuration
├── R1-R20-RESILIENCE-CHECKLIST.md  # Pattern guide
└── RESILIENCE-CHECKER-TECHNICAL-GUIDE.md  # Technical docs
```

---

## Support and Maintenance

### Getting Help

If you encounter issues:

1. ✅ Check this guide's [Troubleshooting](#troubleshooting) section
2. ✅ Review workflow logs in **Actions** tab
3. ✅ Check PR comments for error details
4. ✅ Review configuration files for syntax errors
5. ✅ Consult [Technical Guide](.github/RESILIENCE-CHECKER-TECHNICAL-GUIDE.md)
6. ✅ Review [R1-R20 Checklist](.github/R1-R20-RESILIENCE-CHECKLIST.md)

### Reporting Issues

When reporting issues, include:
- 📎 Workflow run URL
- 📋 Error messages from logs
- 📄 Configuration files (sanitized - remove secrets)
- 🔄 Steps to reproduce
- 🖼️ Screenshots if applicable

### Updating the Checker

To update to a new version:

```bash
# Pull latest changes
git pull origin main

# Review changes
git log --oneline .github/

# Test with a PR
git checkout -b test-updated-checker
echo "# Test update" >> TEST_UPDATE.md
git add TEST_UPDATE.md
git commit -m "Test: Updated checker"
git push -u origin test-updated-checker
# Create PR and verify workflow runs correctly
```

---

## Additional Resources

- 📚 [R1-R20 Resilience Checklist](.github/R1-R20-RESILIENCE-CHECKLIST.md) - Complete pattern guide
- 🔧 [Technical Implementation Guide](.github/RESILIENCE-CHECKER-TECHNICAL-GUIDE.md) - How the checker works
- 📖 [README](.github/README-RESILIENCE-CHECKER.md) - Overview and introduction

---

**Last Updated:** 2026-04-16  
**Version:** 2.0  
**Repository:** https://github.com/amverma/vessel-operations-service  
**Workflow Version:** 1.0