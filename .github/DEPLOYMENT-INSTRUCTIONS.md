# Deployment Instructions for R1-R20 Resilience Checker

**Target Repository:** https://github.ibm.com/Amit-Kumar-Verma/vessel-operations-service.git

## 🎯 Choose Your Deployment Method

### Method 1: Automated Deployment (Recommended)

#### For Windows (PowerShell)

```powershell
# Run from the current directory
.\deploy-to-repo.ps1
```

#### For Linux/Mac (Bash)

```bash
# Make script executable
chmod +x deploy-to-repo.sh

# Run the script
./deploy-to-repo.sh
```

The script will:
1. ✅ Clone the repository
2. ✅ Create a feature branch
3. ✅ Copy all files
4. ✅ Commit changes
5. ✅ Push to IBM GHE
6. ✅ Provide PR creation instructions

---

### Method 2: Manual Deployment

If you prefer manual control or the automated script doesn't work:

#### Step 1: Clone the Repository

```bash
# Clone the vessel-operations-service repository
git clone https://github.ibm.com/Amit-Kumar-Verma/vessel-operations-service.git

# Navigate to the repository
cd vessel-operations-service
```

#### Step 2: Create Feature Branch

```bash
# Create and checkout new branch
git checkout -b feature/add-resilience-checker
```

#### Step 3: Create Directory Structure

```bash
# Create .github directories
mkdir -p .github/workflows
mkdir -p .github/scripts
```

#### Step 4: Copy Files

Copy the following files from the current directory to the repository:

**Workflow:**
```bash
cp /path/to/current/.github/workflows/resiliencecheck.yml .github/workflows/
```

**Script:**
```bash
cp /path/to/current/.github/scripts/resilience_checker.py .github/scripts/
chmod +x .github/scripts/resilience_checker.py
```

**Configuration:**
```bash
cp /path/to/current/.github/resilience-checker-config.yml .github/
```

**Documentation:**
```bash
cp /path/to/current/.github/INDEX.md .github/
cp /path/to/current/.github/README-RESILIENCE-CHECKER.md .github/
cp /path/to/current/.github/SETUP-GUIDE.md .github/
cp /path/to/current/.github/QUICK-REFERENCE.md .github/
```

#### Step 5: Verify Files

```bash
# Check the structure
tree .github/

# Should show:
# .github/
# ├── workflows/
# │   └── resiliencecheck.yml
# ├── scripts/
# │   └── resilience_checker.py
# ├── resilience-checker-config.yml
# ├── INDEX.md
# ├── README-RESILIENCE-CHECKER.md
# ├── SETUP-GUIDE.md
# └── QUICK-REFERENCE.md
```

#### Step 6: Commit Changes

```bash
# Stage all files
git add .github/

# Check what will be committed
git status

# Commit with descriptive message
git commit -m "feat: add R1-R20 resilience checker workflow

- Add GitHub Actions workflow for automated resilience checks
- Add Python checker script validating 20 resilience patterns
- Add configuration file for customization
- Add comprehensive documentation (INDEX, README, SETUP-GUIDE, QUICK-REFERENCE)
- Configure for IBM GitHub Enterprise
- Support for Java, Kotlin, Python, Go, JS/TS, YAML, Avro

This implements automated validation of:
- Fault tolerance patterns (Circuit Breaker, Retry, Timeout)
- Resource management (Bulkhead, Rate Limiting, K8s limits)
- Observability (Health checks, Logging, Tracing, Metrics)
- Data integrity (Idempotency, Schema evolution, Kafka best practices)
- Security (Secrets management)
- Disaster recovery"
```

#### Step 7: Push to Remote

```bash
# Push the branch to IBM GHE
git push -u origin feature/add-resilience-checker
```

---

## 📝 Create Pull Request

### Step 1: Navigate to IBM GHE

Open your browser and go to:
```
https://github.ibm.com/Amit-Kumar-Verma/vessel-operations-service/pulls
```

### Step 2: Create New PR

1. Click **"New Pull Request"**
2. Select base branch: `main` (or your default branch)
3. Select compare branch: `feature/add-resilience-checker`
4. Click **"Create Pull Request"**

### Step 3: Fill PR Details

**Title:**
```
feat: Add R1-R20 Resilience Checker
```

**Description:**
```markdown
## 🛡️ R1-R20 Resilience Checker

This PR adds automated resilience validation for all code changes.

### What's Included

- ✅ GitHub Actions workflow for automated checks
- ✅ Python checker validating 20 resilience patterns
- ✅ Customizable configuration file
- ✅ Comprehensive documentation

### Features

**Automated Validation:**
- Circuit Breaker, Retry, Timeout patterns
- Resource management (Bulkhead, Rate Limiting, K8s limits)
- Observability (Health checks, Logging, Tracing, Metrics)
- Data integrity (Idempotency, Schema evolution, Kafka best practices)
- Security (Secrets management)
- Disaster recovery testing

**Workflow Capabilities:**
- Runs on every PR automatically
- Posts detailed comments with findings
- Blocks merge on CRITICAL failures
- Supports manual workflow dispatch
- Architecture Board bypass workflow
- IBM GHE optimized

### Documentation

- 📋 [INDEX.md](.github/INDEX.md) - Documentation hub
- 📖 [README-RESILIENCE-CHECKER.md](.github/README-RESILIENCE-CHECKER.md) - Complete guide
- 🚀 [SETUP-GUIDE.md](.github/SETUP-GUIDE.md) - Setup instructions
- ⚡ [QUICK-REFERENCE.md](.github/QUICK-REFERENCE.md) - Developer quick reference

### Testing

This PR has been tested with:
- ✅ Workflow syntax validation
- ✅ Python script execution
- ✅ Configuration file validation
- ✅ Documentation review

### After Merge

Follow these steps to complete the setup:

1. **Configure GitHub Actions Permissions** (Step 3 in SETUP-GUIDE.md)
   - Settings → Actions → General
   - Enable "Read and write permissions"
   - Enable "Allow GitHub Actions to create and approve pull requests"

2. **Create Required Labels** (Step 4 in SETUP-GUIDE.md)
   - `bypass-resilience-check`
   - `architecture-board`
   - `resilience-bypass-request`
   - `governance`

3. **Configure Branch Protection** (Step 5 in SETUP-GUIDE.md)
   - Require status checks: "R1-R20 Guardrail Check"
   - Require branches to be up to date

4. **Test the Workflow** (Step 8 in SETUP-GUIDE.md)
   - Create a test PR
   - Verify workflow execution
   - Check PR comments and status

### Impact

- ✅ Improves code quality automatically
- ✅ Enforces resilience best practices
- ✅ Reduces manual review time
- ✅ Prevents production issues
- ✅ Aligns with IBM standards

### Rollback Plan

If issues arise, the workflow can be disabled by:
1. Renaming `.github/workflows/resiliencecheck.yml` to `.github/workflows/resiliencecheck.yml.disabled`
2. Or deleting the workflow file

### Questions?

- See [INDEX.md](.github/INDEX.md) for documentation navigation
- Contact @architecture-board for questions
- Create issue with label `resilience-checker` for bugs

---

**Ready for review!** 🚀
```

### Step 4: Request Reviews

Add reviewers:
- Architecture Board members
- Platform Engineering team
- Team leads

### Step 5: Add Labels

Add these labels to the PR:
- `enhancement`
- `documentation`
- `ci/cd`
- `governance`

---

## ✅ Post-Merge Setup

After the PR is merged, complete these steps:

### 1. Configure GitHub Actions Permissions

```
Repository → Settings → Actions → General
```

Enable:
- ✅ Read and write permissions
- ✅ Allow GitHub Actions to create and approve pull requests

### 2. Create Required Labels

Go to: `Issues → Labels → New Label`

Create these labels:

| Label | Color | Description |
|-------|-------|-------------|
| `bypass-resilience-check` | `#d73a4a` | Request to bypass resilience checks |
| `architecture-board` | `#0075ca` | Requires Architecture Board review |
| `resilience-bypass-request` | `#e99695` | Automated bypass request |
| `governance` | `#5319e7` | Governance and compliance |

### 3. Configure Branch Protection

```
Repository → Settings → Branches → Add rule
```

For your main branch:
- ✅ Require status checks to pass before merging
- ✅ Select: "R1-R20 Guardrail Check"
- ✅ Require branches to be up to date before merging

### 4. Test the Workflow

Create a test PR:

```bash
# Create test branch
git checkout -b test/resilience-checker

# Make a small change
echo "// Test" >> src/main/java/SomeFile.java

# Commit and push
git add .
git commit -m "test: trigger resilience checker"
git push origin test/resilience-checker
```

Then:
1. Create PR on IBM GHE
2. Wait for workflow to run
3. Check PR comment
4. Verify status check
5. Close test PR

### 5. Team Communication

Share with the team:
- Link to [QUICK-REFERENCE.md](.github/QUICK-REFERENCE.md)
- Schedule training session
- Document bypass process
- Set up Architecture Board workflow

---

## 🔧 Troubleshooting

### Issue: Authentication Failed

**Solution:**
```bash
# Configure IBM GHE credentials
git config --global credential.helper store

# Or use SSH instead of HTTPS
git remote set-url origin git@github.ibm.com:Amit-Kumar-Verma/vessel-operations-service.git
```

### Issue: Permission Denied

**Solution:**
- Verify you have write access to the repository
- Check with repository admin
- Ensure you're authenticated to IBM GHE

### Issue: Files Not Found

**Solution:**
```bash
# Verify you're in the correct directory
pwd

# List files
ls -la

# Check if files exist
ls -la workflows/
ls -la scripts/
```

### Issue: Workflow Not Running

**Solution:**
1. Check GitHub Actions is enabled
2. Verify workflow file syntax
3. Check branch protection rules
4. Review Actions logs

---

## 📞 Support

- **Deployment Issues**: Create issue with label `deployment`
- **Technical Questions**: Contact @architecture-board
- **IBM GHE Issues**: IBM GitHub Enterprise support

---

## 🎉 Success Checklist

After deployment, verify:

- [ ] Files copied to repository
- [ ] Branch created and pushed
- [ ] PR created with proper description
- [ ] Reviewers added
- [ ] Labels added
- [ ] PR merged
- [ ] GitHub Actions permissions configured
- [ ] Required labels created
- [ ] Branch protection configured
- [ ] Test PR created and verified
- [ ] Team notified
- [ ] Documentation shared

---

**Deployment Complete!** 🚀

Your resilience checker is now ready to help maintain high-quality, resilient code!

---

**Made with Bob** 🤖