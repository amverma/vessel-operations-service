# Simple Deployment Script for R1-R20 Resilience Checker
# Target: https://github.ibm.com/Amit-Kumar-Verma/vessel-operations-service.git

$ErrorActionPreference = "Stop"

Write-Host "`n=== R1-R20 Resilience Checker Deployment ===" -ForegroundColor Cyan
Write-Host ""

# Configuration
$REPO_URL = "https://github.ibm.com/Amit-Kumar-Verma/vessel-operations-service.git"
$REPO_NAME = "vessel-operations-service"
$BRANCH_NAME = "feature/add-resilience-checker"
$SOURCE_DIR = Get-Location

Write-Host "Repository: $REPO_URL" -ForegroundColor Yellow
Write-Host "Branch: $BRANCH_NAME" -ForegroundColor Yellow
Write-Host ""

# Confirm
$confirm = Read-Host "Continue with deployment? (y/n)"
if ($confirm -ne 'y') {
    Write-Host "Cancelled." -ForegroundColor Red
    exit 0
}

# Check git
try {
    git --version | Out-Null
    Write-Host "[OK] Git is installed" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Git not found" -ForegroundColor Red
    exit 1
}

# Create temp directory
$TEMP_DIR = Join-Path $env:TEMP "resilience-deploy-$(Get-Date -Format 'yyyyMMddHHmmss')"
New-Item -ItemType Directory -Path $TEMP_DIR -Force | Out-Null
Write-Host "[OK] Temp directory: $TEMP_DIR" -ForegroundColor Green

# Clone repository
Write-Host "`nCloning repository..." -ForegroundColor Yellow
Set-Location $TEMP_DIR
try {
    git clone $REPO_URL $REPO_NAME 2>&1 | Out-Null
    Write-Host "[OK] Repository cloned" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to clone repository" -ForegroundColor Red
    Write-Host "Check: credentials, network, repository URL" -ForegroundColor Red
    exit 1
}

Set-Location $REPO_NAME

# Create branch
Write-Host "`nCreating branch..." -ForegroundColor Yellow
git checkout -b $BRANCH_NAME
Write-Host "[OK] Branch created: $BRANCH_NAME" -ForegroundColor Green

# Create directories
Write-Host "`nCreating directories..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path ".github\workflows" -Force | Out-Null
New-Item -ItemType Directory -Path ".github\scripts" -Force | Out-Null
Write-Host "[OK] Directories created" -ForegroundColor Green

# Copy files
Write-Host "`nCopying files..." -ForegroundColor Yellow

$files = @(
    @{Source="workflows\resiliencecheck.yml"; Dest=".github\workflows\resiliencecheck.yml"},
    @{Source="scripts\resilience_checker.py"; Dest=".github\scripts\resilience_checker.py"},
    @{Source="resilience-checker-config.yml"; Dest=".github\resilience-checker-config.yml"},
    @{Source="INDEX.md"; Dest=".github\INDEX.md"},
    @{Source="README-RESILIENCE-CHECKER.md"; Dest=".github\README-RESILIENCE-CHECKER.md"},
    @{Source="SETUP-GUIDE.md"; Dest=".github\SETUP-GUIDE.md"},
    @{Source="QUICK-REFERENCE.md"; Dest=".github\QUICK-REFERENCE.md"}
)

foreach ($file in $files) {
    $srcPath = Join-Path $SOURCE_DIR $file.Source
    if (Test-Path $srcPath) {
        Copy-Item $srcPath $file.Dest -Force
        Write-Host "  [OK] $($file.Source)" -ForegroundColor Green
    } else {
        Write-Host "  [ERROR] Not found: $($file.Source)" -ForegroundColor Red
        exit 1
    }
}

# Stage files
Write-Host "`nStaging files..." -ForegroundColor Yellow
git add .github/
Write-Host "[OK] Files staged" -ForegroundColor Green

# Show status
Write-Host "`nFiles to commit:" -ForegroundColor Yellow
git status --short

# Commit
Write-Host "`nCommitting..." -ForegroundColor Yellow
git commit -m "feat: add R1-R20 resilience checker workflow" `
           -m "Add GitHub Actions workflow for automated resilience checks" `
           -m "Add Python checker script validating 20 resilience patterns" `
           -m "Add configuration file and comprehensive documentation" `
           -m "Configure for IBM GitHub Enterprise"

Write-Host "[OK] Changes committed" -ForegroundColor Green

# Push
Write-Host "`nPushing to remote..." -ForegroundColor Yellow
try {
    git push -u origin $BRANCH_NAME
    Write-Host "[OK] Changes pushed successfully" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to push" -ForegroundColor Red
    Write-Host "Manual push: cd $TEMP_DIR\$REPO_NAME && git push -u origin $BRANCH_NAME" -ForegroundColor Yellow
    exit 1
}

# Success
Write-Host "`n=== Deployment Successful ===" -ForegroundColor Green
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Create PR at: https://github.ibm.com/Amit-Kumar-Verma/vessel-operations-service/pulls" -ForegroundColor White
Write-Host "2. Select branch: $BRANCH_NAME" -ForegroundColor White
Write-Host "3. Use title: 'feat: Add R1-R20 Resilience Checker'" -ForegroundColor White
Write-Host "4. Copy PR description from DEPLOYMENT-INSTRUCTIONS.md" -ForegroundColor White
Write-Host ""
Write-Host "Repository location: $TEMP_DIR\$REPO_NAME" -ForegroundColor Yellow
Write-Host ""

# Made with Bob
