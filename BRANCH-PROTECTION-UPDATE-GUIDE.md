# Branch Protection Update Guide for R1-R23

## Issue
The branch protection rules are still looking for the old status check name "R1-R20 Guardrail Check" but the workflow has been updated to "R1-R23 Guardrail Check".

## Root Cause
GitHub Actions uses the **job name** (not the workflow name) as the status check name. The job name has been updated from "R1-R20 Guardrail Check" to "R1-R23 Guardrail Check", but the new status check won't appear in branch protection settings until the workflow runs at least once from the main branch.

## Solution: 3 Steps

### Step 1: Trigger Manual Workflow Run

The workflow needs to run once from the main branch for the new status check to appear.

**Option A: Via GitHub UI (Recommended)**
1. Go to: https://github.com/amverma/vessel-operations-service/actions
2. Click on "R1-R23 Resilience Guardrail Checker" in the left sidebar
3. Click the "Run workflow" button (top right)
4. Select branch: `main`
5. Click "Run workflow" (green button)
6. Wait 1-2 minutes for the workflow to complete

**Option B: Create a Test Pull Request**
1. Create a new branch: `git checkout -b test-r1-r23-checker`
2. Make a small change to any monitored file (e.g., add a comment to a Java file)
3. Commit and push: `git commit -am "Test R1-R23 checker" && git push origin test-r1-r23-checker`
4. Create a pull request from `test-r1-r23-checker` to `main`
5. The workflow will run automatically
6. After it completes, the new status check will be available

### Step 2: Verify New Status Check Appears

After the workflow runs:
1. Go to: https://github.com/amverma/vessel-operations-service/settings/branches
2. Click "Edit" on the existing branch protection rule for `main`
3. Scroll to "Require status checks to pass before merging"
4. In the search box, type "R1-R23"
5. You should now see "R1-R23 Guardrail Check" in the dropdown

### Step 3: Update Branch Protection Rule

1. In the branch protection settings (from Step 2):
2. **Remove** the old status check:
   - Uncheck or remove "R1-R20 Guardrail Check"
3. **Add** the new status check:
   - Search for "R1-R23 Guardrail Check"
   - Click to select it
4. Ensure these settings are enabled:
   - ✅ Require status checks to pass before merging
   - ✅ Require branches to be up to date before merging
   - ✅ R1-R23 Guardrail Check (selected)
5. Click "Save changes" at the bottom

## Verification

After updating branch protection:
1. Create a test pull request
2. Verify that "R1-R23 Guardrail Check" appears as a required status check
3. Verify that the PR cannot be merged until the check passes

## Current Status Check Names

| Old Name | New Name |
|----------|----------|
| R1-R20 Guardrail Check | R1-R23 Guardrail Check |

## Workflow Details

- **Workflow File:** `.github/workflows/resiliencecheck.yml`
- **Workflow Name:** R1-R23 Resilience Guardrail Checker
- **Job Name:** R1-R23 Guardrail Check ← This is what appears in branch protection
- **Status Check Name:** R1-R23 Guardrail Check

## Troubleshooting

### "I don't see the new status check in the dropdown"
- **Solution:** The workflow hasn't run yet from the main branch. Follow Step 1 to trigger it.

### "The old status check is still required"
- **Solution:** You need to manually remove it and add the new one. GitHub doesn't automatically update status check names.

### "The workflow failed"
- **Solution:** This is expected if there are resilience issues. The status check will still appear in branch protection settings. You can configure it even if the check failed.

### "I see both old and new status checks"
- **Solution:** Remove the old "R1-R20 Guardrail Check" from branch protection. Only keep "R1-R23 Guardrail Check".

## Important Notes

1. **Both checks may appear temporarily:** Until you remove the old check, both "R1-R20" and "R1-R23" may be required. Remove the old one after adding the new one.

2. **Workflow must run from main:** The status check only appears after the workflow runs at least once from the base branch (main).

3. **Private repo limitations:** If this is a private repository on the free tier, branch protection with required status checks requires GitHub Team or Enterprise. For free enforcement, make the repository public.

## Quick Reference

```bash
# If you have GitHub CLI installed, you can trigger the workflow with:
gh workflow run resiliencecheck.yml --ref main

# Or create a test branch and PR:
git checkout -b test-r1-r23-checker
echo "# Test" >> README.md
git commit -am "Test R1-R23 checker"
git push origin test-r1-r23-checker
# Then create PR via GitHub UI
```

---

**Need Help?**
- See: [`.github/SETUP-GUIDE.md`](.github/SETUP-GUIDE.md)
- See: [`.github/R1-R20-RESILIENCE-CHECKLIST.md`](.github/R1-R20-RESILIENCE-CHECKLIST.md)
- See: [`RESILIENCE-CHECKER-UPDATES.md`](RESILIENCE-CHECKER-UPDATES.md)