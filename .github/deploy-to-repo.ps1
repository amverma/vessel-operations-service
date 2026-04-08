# Deployment Script for R1-R20 Resilience Checker (PowerShell)
# Target Repository: https://github.ibm.com/Amit-Kumar-Verma/vessel-operations-service.git

$ErrorActionPreference = "Stop"

Write-Host "🚀 R1-R20 Resilience Checker Deployment Script" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$REPO_URL = "https://github.ibm.com/Amit-Kumar-Verma/vessel-operations-service.git"
$REPO_NAME = "vessel-operations-service"
$BRANCH_NAME = "feature/add-resilience-checker"
$SOURCE_DIR = Get-Location

# Function to print colored output
function Print-Success {
    param($Message)
    Write-Host "✅ $Message" -ForegroundColor Green
}

function Print-Error {
    param($Message)
    Write-Host "❌ $Message" -ForegroundColor Red
}

function Print-Info {
    param($Message)
    Write-Host "ℹ️  $Message" -ForegroundColor Yellow
}

# Check if git is installed
try {
    $null = git --version
    Print-Success "Git is installed"
} catch {
    Print-Error "Git is not installed. Please install git first."
    exit 1
}

# Ask for confirmation
Write-Host ""
Print-Info "This script will:"
Write-Host "  1. Clone the vessel-operations-service repository"
Write-Host "  2. Create a new branch: $BRANCH_NAME"
Write-Host "  3. Copy all resilience checker files"
Write-Host "  4. Commit and push changes"
Write-Host "  5. Provide instructions to create a PR"
Write-Host ""
$confirmation = Read-Host "Do you want to continue? (y/n)"

if ($confirmation -ne 'y' -and $confirmation -ne 'Y') {
    Print-Info "Deployment cancelled"
    exit 0
}

# Create temporary directory
$TEMP_DIR = Join-Path $env:TEMP "resilience-checker-deploy-$(Get-Date -Format 'yyyyMMddHHmmss')"
New-Item -ItemType Directory -Path $TEMP_DIR -Force | Out-Null
Print-Info "Created temporary directory: $TEMP_DIR"

# Clone repository
Write-Host ""
Print-Info "Cloning repository..."
Set-Location $TEMP_DIR

try {
    git clone $REPO_URL $REPO_NAME
    Print-Success "Repository cloned successfully"
} catch {
    Print-Error "Failed to clone repository. Please check:"
    Write-Host "  - Your IBM GHE credentials"
    Write-Host "  - Repository URL: $REPO_URL"
    Write-Host "  - Network connectivity"
    exit 1
}

Set-Location $REPO_NAME

# Create new branch
Print-Info "Creating branch: $BRANCH_NAME"
git checkout -b $BRANCH_NAME
Print-Success "Branch created"

# Create directory structure
Print-Info "Creating directory structure..."
New-Item -ItemType Directory -Path ".github\workflows" -Force | Out-Null
New-Item -ItemType Directory -Path ".github\scripts" -Force | Out-Null
Print-Success "Directories created"

# Copy files
Write-Host ""
Print-Info "Copying resilience checker files..."

# Copy workflow
$workflowSource = Join-Path $SOURCE_DIR "workflows\resiliencecheck.yml"
if (Test-Path $workflowSource) {
    Copy-Item $workflowSource ".github\workflows\"
    Print-Success "Copied: workflows/resiliencecheck.yml"
} else {
    Print-Error "Source file not found: workflows\resiliencecheck.yml"
    exit 1
}

# Copy script
$scriptSource = Join-Path $SOURCE_DIR "scripts\resilience_checker.py"
if (Test-Path $scriptSource) {
    Copy-Item $scriptSource ".github\scripts\"
    Print-Success "Copied: scripts/resilience_checker.py"
} else {
    Print-Error "Source file not found: scripts\resilience_checker.py"
    exit 1
}

# Copy configuration
$configSource = Join-Path $SOURCE_DIR "resilience-checker-config.yml"
if (Test-Path $configSource) {
    Copy-Item $configSource ".github\"
    Print-Success "Copied: resilience-checker-config.yml"
} else {
    Print-Error "Source file not found: resilience-checker-config.yml"
    exit 1
}

# Copy documentation
$docs = @("INDEX.md", "README-RESILIENCE-CHECKER.md", "SETUP-GUIDE.md", "QUICK-REFERENCE.md")
foreach ($doc in $docs) {
    $docSource = Join-Path $SOURCE_DIR $doc
    if (Test-Path $docSource) {
        Copy-Item $docSource ".github\"
        Print-Success "Copied: $doc"
    } else {
        Print-Error "Source file not found: $doc"
        exit 1
    }
}

# Add all files
Print-Info "Staging files..."
git add .github/

# Show what will be committed
Write-Host ""
Print-Info "Files to be committed:"
git status --short

# Commit changes
Write-Host ""
Print-Info "Committing changes..."

git commit -m "feat: add R1-R20 resilience checker workflow" -m "Add GitHub Actions workflow for automated resilience checks" -m "Add Python checker script validating 20 resilience patterns" -m "Add configuration file for customization" -m "Add comprehensive documentation" -m "Configure for IBM GitHub Enterprise" -m "Support for Java, Kotlin, Python, Go, JS/TS, YAML, Avro" -m "This implements automated validation of resilience patterns" -m "Closes: [Add issue number if applicable]"

Print-Success "Changes committed"

# Push to remote
Write-Host ""
Print-Info "Pushing to remote repository..."
try {
    git push -u origin $BRANCH_NAME
    Print-Success "Changes pushed successfully"
} catch {
    Print-Error "Failed to push changes. Please check your credentials and try manually."
    Write-Host ""
    Print-Info "Manual push command:"
    Write-Host "  cd $TEMP_DIR\$REPO_NAME"
    Write-Host "  git push -u origin $BRANCH_NAME"
    exit 1
}

# Success message
Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Print-Success "Deployment completed successfully!"
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Print-Info "Next steps:"
Write-Host ""
Write-Host "1. Create Pull Request:"
Write-Host "   - Go to: https://github.ibm.com/Amit-Kumar-Verma/vessel-operations-service/pulls"
Write-Host "   - Click 'New Pull Request'"
Write-Host "   - Select branch: $BRANCH_NAME"
Write-Host "   - Title: 'feat: Add R1-R20 Resilience Checker'"
Write-Host ""
Write-Host "2. PR Description (copy this):"
Write-Host "   ---"
Write-Host "   ## 🛡️ R1-R20 Resilience Checker"
Write-Host "   "
Write-Host "   This PR adds automated resilience validation for all code changes."
Write-Host "   "
Write-Host "   ### What's Included"
Write-Host "   - ✅ GitHub Actions workflow"
Write-Host "   - ✅ Python checker for 20 resilience patterns"
Write-Host "   - ✅ Configuration file"
Write-Host "   - ✅ Complete documentation"
Write-Host "   "
Write-Host "   ### Documentation"
Write-Host "   - 📋 [INDEX.md](.github/INDEX.md) - Start here"
Write-Host "   - 📖 [README](.github/README-RESILIENCE-CHECKER.md) - Complete guide"
Write-Host "   - 🚀 [Setup Guide](.github/SETUP-GUIDE.md) - Deployment steps"
Write-Host "   - ⚡ [Quick Reference](.github/QUICK-REFERENCE.md) - Developer guide"
Write-Host "   "
Write-Host "   ### After Merge"
Write-Host "   1. Follow [SETUP-GUIDE.md](.github/SETUP-GUIDE.md) steps 3-10"
Write-Host "   2. Configure GitHub Actions permissions"
Write-Host "   3. Create required labels"
Write-Host "   4. Test with a sample PR"
Write-Host "   ---"
Write-Host ""
Write-Host "3. After PR is merged:"
Write-Host "   - Follow steps 3-10 in .github/SETUP-GUIDE.md"
Write-Host "   - Configure GitHub Actions permissions"
Write-Host "   - Create required labels"
Write-Host "   - Set up branch protection"
Write-Host ""
Write-Host "4. Repository location:"
Write-Host "   $TEMP_DIR\$REPO_NAME"
Write-Host ""
Print-Info "You can now create the PR on IBM GitHub Enterprise!"
Write-Host ""

# Made with Bob
