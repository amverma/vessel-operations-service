#!/bin/bash

# Deployment Script for R1-R20 Resilience Checker
# Target Repository: https://github.ibm.com/Amit-Kumar-Verma/vessel-operations-service.git

set -e  # Exit on error

echo "🚀 R1-R20 Resilience Checker Deployment Script"
echo "================================================"
echo ""

# Configuration
REPO_URL="https://github.ibm.com/Amit-Kumar-Verma/vessel-operations-service.git"
REPO_NAME="vessel-operations-service"
BRANCH_NAME="feature/add-resilience-checker"
SOURCE_DIR="$(pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

# Check if git is installed
if ! command -v git &> /dev/null; then
    print_error "Git is not installed. Please install git first."
    exit 1
fi

print_success "Git is installed"

# Ask for confirmation
echo ""
print_info "This script will:"
echo "  1. Clone the vessel-operations-service repository"
echo "  2. Create a new branch: $BRANCH_NAME"
echo "  3. Copy all resilience checker files"
echo "  4. Commit and push changes"
echo "  5. Provide instructions to create a PR"
echo ""
read -p "Do you want to continue? (y/n) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_info "Deployment cancelled"
    exit 0
fi

# Create temporary directory
TEMP_DIR=$(mktemp -d)
print_info "Created temporary directory: $TEMP_DIR"

# Clone repository
echo ""
print_info "Cloning repository..."
cd "$TEMP_DIR"

if git clone "$REPO_URL" "$REPO_NAME"; then
    print_success "Repository cloned successfully"
else
    print_error "Failed to clone repository. Please check:"
    echo "  - Your IBM GHE credentials"
    echo "  - Repository URL: $REPO_URL"
    echo "  - Network connectivity"
    exit 1
fi

cd "$REPO_NAME"

# Create new branch
print_info "Creating branch: $BRANCH_NAME"
git checkout -b "$BRANCH_NAME"
print_success "Branch created"

# Create directory structure
print_info "Creating directory structure..."
mkdir -p .github/workflows
mkdir -p .github/scripts
print_success "Directories created"

# Copy files
echo ""
print_info "Copying resilience checker files..."

# Copy workflow
if [ -f "$SOURCE_DIR/workflows/resiliencecheck.yml" ]; then
    cp "$SOURCE_DIR/workflows/resiliencecheck.yml" .github/workflows/
    print_success "Copied: workflows/resiliencecheck.yml"
else
    print_error "Source file not found: workflows/resiliencecheck.yml"
    exit 1
fi

# Copy script
if [ -f "$SOURCE_DIR/scripts/resilience_checker.py" ]; then
    cp "$SOURCE_DIR/scripts/resilience_checker.py" .github/scripts/
    chmod +x .github/scripts/resilience_checker.py
    print_success "Copied: scripts/resilience_checker.py"
else
    print_error "Source file not found: scripts/resilience_checker.py"
    exit 1
fi

# Copy configuration
if [ -f "$SOURCE_DIR/resilience-checker-config.yml" ]; then
    cp "$SOURCE_DIR/resilience-checker-config.yml" .github/
    print_success "Copied: resilience-checker-config.yml"
else
    print_error "Source file not found: resilience-checker-config.yml"
    exit 1
fi

# Copy documentation
for doc in INDEX.md README-RESILIENCE-CHECKER.md SETUP-GUIDE.md QUICK-REFERENCE.md; do
    if [ -f "$SOURCE_DIR/$doc" ]; then
        cp "$SOURCE_DIR/$doc" .github/
        print_success "Copied: $doc"
    else
        print_error "Source file not found: $doc"
        exit 1
    fi
done

# Add all files
print_info "Staging files..."
git add .github/

# Show what will be committed
echo ""
print_info "Files to be committed:"
git status --short

# Commit changes
echo ""
print_info "Committing changes..."
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
- Disaster recovery

Closes: [Add issue number if applicable]"

print_success "Changes committed"

# Push to remote
echo ""
print_info "Pushing to remote repository..."
if git push -u origin "$BRANCH_NAME"; then
    print_success "Changes pushed successfully"
else
    print_error "Failed to push changes. Please check your credentials and try manually."
    echo ""
    print_info "Manual push command:"
    echo "  cd $TEMP_DIR/$REPO_NAME"
    echo "  git push -u origin $BRANCH_NAME"
    exit 1
fi

# Success message
echo ""
echo "================================================"
print_success "Deployment completed successfully!"
echo "================================================"
echo ""
print_info "Next steps:"
echo ""
echo "1. Create Pull Request:"
echo "   - Go to: https://github.ibm.com/Amit-Kumar-Verma/vessel-operations-service/pulls"
echo "   - Click 'New Pull Request'"
echo "   - Select branch: $BRANCH_NAME"
echo "   - Title: 'feat: Add R1-R20 Resilience Checker'"
echo ""
echo "2. PR Description (copy this):"
echo "   ---"
echo "   ## 🛡️ R1-R20 Resilience Checker"
echo "   "
echo "   This PR adds automated resilience validation for all code changes."
echo "   "
echo "   ### What's Included"
echo "   - ✅ GitHub Actions workflow"
echo "   - ✅ Python checker for 20 resilience patterns"
echo "   - ✅ Configuration file"
echo "   - ✅ Complete documentation"
echo "   "
echo "   ### Documentation"
echo "   - 📋 [INDEX.md](.github/INDEX.md) - Start here"
echo "   - 📖 [README](.github/README-RESILIENCE-CHECKER.md) - Complete guide"
echo "   - 🚀 [Setup Guide](.github/SETUP-GUIDE.md) - Deployment steps"
echo "   - ⚡ [Quick Reference](.github/QUICK-REFERENCE.md) - Developer guide"
echo "   "
echo "   ### After Merge"
echo "   1. Follow [SETUP-GUIDE.md](.github/SETUP-GUIDE.md) steps 3-10"
echo "   2. Configure GitHub Actions permissions"
echo "   3. Create required labels"
echo "   4. Test with a sample PR"
echo "   ---"
echo ""
echo "3. After PR is merged:"
echo "   - Follow steps 3-10 in .github/SETUP-GUIDE.md"
echo "   - Configure GitHub Actions permissions"
echo "   - Create required labels"
echo "   - Set up branch protection"
echo ""
echo "4. Repository location:"
echo "   $TEMP_DIR/$REPO_NAME"
echo ""
print_info "You can now create the PR on IBM GitHub Enterprise!"
echo ""

# Made with Bob
