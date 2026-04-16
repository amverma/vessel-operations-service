# Branch Protection Setup for R1-R20 Resilience Checker

This guide explains how to configure GitHub branch protection rules to make the R1-R20 Resilience Checker a **required status check** that blocks PR merging when checks fail.

## Prerequisites

- You must have **admin** or **maintain** permissions on the repository
- The workflow must have run at least once to appear in the status checks list

## Step-by-Step Instructions

### 1. Navigate to Branch Protection Settings

1. Go to your repository: https://github.com/amverma/vessel-operations-service
2. Click on **Settings** (top navigation bar)
3. In the left sidebar, click **Branches** (under "Code and automation")

### 2. Add or Edit Branch Protection Rule

1. Click **Add branch protection rule** (or edit existing rule for `main`)
2. In the **Branch name pattern** field, enter: `main`

### 3. Configure Required Status Checks

1. Check the box: ☑️ **Require status checks to pass before merging**
2. Check the box: ☑️ **Require branches to be up to date before merging** (recommended)
3. In the search box under "Status checks that are required", search for:
   - `R1-R20 Guardrail Check`
   
   **Note:** The status check will only appear in the list after the workflow has run at least once. If you don't see it:
   - Create a test PR first to trigger the workflow
   - Wait for the workflow to complete
   - Return to this settings page and refresh
   - The status check should now appear in the search results

4. Click on `R1-R20 Guardrail Check` to add it as a required check

### 4. Additional Recommended Settings

For maximum protection, also enable these settings:

- ☑️ **Require a pull request before merging**
  - ☑️ Require approvals: `1` (or more)
  - ☑️ Dismiss stale pull request approvals when new commits are pushed
  
- ☑️ **Require conversation resolution before merging**

- ☑️ **Do not allow bypassing the above settings**
  - This prevents even admins from bypassing the checks
  - Leave unchecked if you want admins to have override capability

### 5. Save Changes

1. Scroll to the bottom of the page
2. Click **Create** (or **Save changes** if editing existing rule)

## Verification

To verify the protection is working:

1. Create a new test branch
2. Make a change that will fail the resilience checks (e.g., add a hardcoded password)
3. Create a PR to `main`
4. Wait for the workflow to run and fail
5. Try to merge the PR - you should see:
   - ❌ The merge button is disabled or shows "Merging is blocked"
   - A message: "Required status check 'R1-R20 Guardrail Check' has not succeeded"

## Workflow Behavior

The R1-R20 Resilience Checker workflow will:

- ✅ **PASS**: Allow merging when all CRITICAL checks pass (based on severity threshold)
- ❌ **FAIL**: Block merging when any CRITICAL checks fail
- 📊 Post detailed results as a PR comment
- 📦 Upload artifacts with full reports

## Bypass Mechanism (Emergency Use Only)

If you need to bypass the check in an emergency:

1. Add the label `bypass-resilience-check` to the PR
2. This will trigger a notification to the Architecture Board
3. An issue will be created requiring:
   - Architecture Board approval
   - ADR (Architecture Decision Record) documentation
   - Minimum 2 approvals

**Important:** Bypasses should be rare and well-documented. All exceptions must follow governance policies.

## Troubleshooting

### Status check doesn't appear in the list

**Solution:** 
- The workflow must run at least once before it appears
- Create a test PR to trigger the workflow
- Wait for completion, then return to settings

### Workflow runs but doesn't block merging

**Solution:**
- Verify the status check name matches exactly: `R1-R20 Guardrail Check`
- Ensure "Require status checks to pass before merging" is checked
- Check that the workflow job name matches (line 40 in resiliencecheck.yml)

### Want to change severity threshold

**Solution:**
- Edit `.github/resilience-checker-config.yml`
- Change `severity_threshold` from `CRITICAL` to `HIGH`, `MEDIUM`, or `LOW`
- Commit and push changes

## Repository Information

- **Repository:** vessel-operations-service
- **Owner:** amverma
- **Workflow File:** `.github/workflows/resiliencecheck.yml`
- **Config File:** `.github/resilience-checker-config.yml`
- **Script:** `.github/scripts/resilience_checker.py`

## Support

For questions or issues:
- Review the workflow logs in GitHub Actions
- Check the resilience report artifacts
- Consult the Architecture Board for bypass requests

---

**Last Updated:** 2026-04-16  
**Workflow Version:** 1.0