# R1-R20 Resilience Checker - Documentation Index

**Repository:** vessel-operations-service  
**IBM GitHub Enterprise:** https://github.ibm.com/Amit-Kumar-Verma/vessel-operations-service.git  
**Version:** 1.0

## 📋 Documentation Overview

This directory contains the complete R1-R20 Resilience Checker implementation for the vessel-operations-service repository. The checker automatically validates code changes against 20 critical resilience patterns.

## 🗂️ File Structure

```
.github/
├── workflows/
│   └── resiliencecheck.yml              # GitHub Actions workflow
├── scripts/
│   └── resilience_checker.py            # Python checker implementation
├── resilience-checker-config.yml        # Configuration file
├── README-RESILIENCE-CHECKER.md         # Complete documentation
├── SETUP-GUIDE.md                       # Setup instructions
├── QUICK-REFERENCE.md                   # Developer quick reference
└── INDEX.md                             # This file
```

## 📖 Documentation Guide

### For Developers

**Start here:** [QUICK-REFERENCE.md](QUICK-REFERENCE.md)
- Quick fixes for common issues
- Code examples and templates
- Local testing instructions
- 5-minute read

**Then read:** [README-RESILIENCE-CHECKER.md](README-RESILIENCE-CHECKER.md)
- Complete R1-R20 checklist explanation
- How the checker works
- Detailed troubleshooting
- 15-minute read

### For DevOps/Platform Engineers

**Start here:** [SETUP-GUIDE.md](SETUP-GUIDE.md)
- Step-by-step setup instructions
- IBM GHE specific configuration
- Troubleshooting guide
- Maintenance procedures
- 20-minute read

**Then configure:** [resilience-checker-config.yml](resilience-checker-config.yml)
- Adjust severity thresholds
- Enable/disable checks
- Configure exclusions
- Set up notifications

### For Architecture Board

**Review:** [README-RESILIENCE-CHECKER.md](README-RESILIENCE-CHECKER.md)
- Bypass process documentation
- ADR requirements
- Governance integration
- Compliance settings

## 🚀 Quick Start Paths

### Path 1: I'm a Developer with a Failing PR
1. Check the PR comment for specific failures
2. Open [QUICK-REFERENCE.md](QUICK-REFERENCE.md)
3. Find your issue and apply the fix
4. Push changes and wait for re-check

### Path 2: I'm Setting Up the Checker
1. Read [SETUP-GUIDE.md](SETUP-GUIDE.md)
2. Follow steps 1-10
3. Test with a sample PR
4. Configure [resilience-checker-config.yml](resilience-checker-config.yml)

### Path 3: I Need to Bypass a Check
1. Read bypass section in [README-RESILIENCE-CHECKER.md](README-RESILIENCE-CHECKER.md)
2. Create ADR documenting the exception
3. Add `bypass-resilience-check` label to PR
4. Wait for Architecture Board review

### Path 4: I Want to Understand R1-R20
1. Read [README-RESILIENCE-CHECKER.md](README-RESILIENCE-CHECKER.md) - "What It Checks" section
2. Review [QUICK-REFERENCE.md](QUICK-REFERENCE.md) for code examples
3. Check `docs/R1-R20-RESILIENCE-CHECKLIST.md` for detailed patterns

## 🔧 Key Components

### 1. GitHub Actions Workflow
**File:** [`workflows/resiliencecheck.yml`](workflows/resiliencecheck.yml)

**Triggers:**
- Pull request events (opened, synchronized, reopened)
- Manual workflow dispatch

**Features:**
- Automatic PR comments with results
- Status checks that block merge on critical failures
- Artifact uploads (reports, badges)
- Architecture Board notifications on bypass attempts

### 2. Python Checker Script
**File:** [`scripts/resilience_checker.py`](scripts/resilience_checker.py)

**Capabilities:**
- Scans 20 resilience patterns (R1-R20)
- Supports multiple languages (Java, Kotlin, Python, Go, JS/TS)
- Analyzes Kubernetes manifests, Kafka configs, Avro schemas
- Generates JSON reports with detailed findings

### 3. Configuration File
**File:** [`resilience-checker-config.yml`](resilience-checker-config.yml)

**Configures:**
- Severity thresholds
- Monitored paths and file types
- Check enable/disable settings
- Exclusions and bypass rules
- IBM GHE specific settings

## 📊 The R1-R20 Checklist

| ID | Pattern | Severity | Category |
|----|---------|----------|----------|
| R1 | Circuit Breaker | CRITICAL | Fault Tolerance |
| R2 | Retry with Backoff | HIGH | Fault Tolerance |
| R3 | Timeout Configuration | CRITICAL | Fault Tolerance |
| R4 | Bulkhead Pattern | HIGH | Resource Isolation |
| R5 | Rate Limiting | HIGH | Resource Protection |
| R6 | Health Checks | CRITICAL | Observability |
| R7 | Graceful Shutdown | CRITICAL | Availability |
| R8 | Idempotency | CRITICAL | Data Integrity |
| R9 | Dead Letter Queue | HIGH | Error Handling |
| R10 | Structured Logging | HIGH | Observability |
| R11 | Distributed Tracing | HIGH | Observability |
| R12 | Metrics & Monitoring | CRITICAL | Observability |
| R13 | Schema Evolution | CRITICAL | Data Integrity |
| R14 | Kafka Best Practices | CRITICAL | Message Processing |
| R15 | Connection Pooling | HIGH | Resource Management |
| R16 | K8s Resource Limits | CRITICAL | Resource Management |
| R17 | HPA | HIGH | Scalability |
| R18 | PDB | HIGH | Availability |
| R19 | Secrets Management | CRITICAL | Security |
| R20 | DR Testing | CRITICAL | Disaster Recovery |

## 🎯 Common Use Cases

### Use Case 1: New Feature Development
1. Develop feature in feature branch
2. Create PR to main branch
3. Checker runs automatically
4. Review PR comment for any issues
5. Fix issues using [QUICK-REFERENCE.md](QUICK-REFERENCE.md)
6. Merge when all checks pass

### Use Case 2: Hotfix Deployment
1. Create hotfix branch
2. Make critical fix
3. Create PR
4. If checker blocks merge and fix is urgent:
   - Add `bypass-resilience-check` label
   - Create ADR documenting the exception
   - Get Architecture Board approval
   - Merge with bypass
   - Create follow-up ticket to fix properly

### Use Case 3: Refactoring Existing Code
1. Run checker locally first: `python .github/scripts/resilience_checker.py --repo-path .`
2. Identify all issues in current code
3. Create plan to fix issues incrementally
4. Submit PRs with fixes
5. Monitor checker results

### Use Case 4: Onboarding New Team Member
1. Share [QUICK-REFERENCE.md](QUICK-REFERENCE.md)
2. Explain R1-R20 patterns
3. Show example PR with checker results
4. Have them fix a sample issue
5. Review [README-RESILIENCE-CHECKER.md](README-RESILIENCE-CHECKER.md) together

## 🔐 IBM GHE Integration

This checker is specifically configured for IBM GitHub Enterprise:

- **Repository URL:** https://github.ibm.com/Amit-Kumar-Verma/vessel-operations-service.git
- **Compliance:** Aligned with IBM security and governance standards
- **Network:** Configured for IBM internal network access
- **Teams:** Integrated with IBM GHE team structure

## 📞 Support & Contact

| Need | Contact | Method |
|------|---------|--------|
| Technical Issues | Create Issue | Label: `resilience-checker` |
| Architecture Questions | Architecture Board | @architecture-board |
| Bypass Approval | Architecture Board | PR label + ADR |
| IBM GHE Issues | IBM Support | IBM GitHub Enterprise support |
| General Questions | Vessel Ops Team | @vessel-ops-team |

## 🔄 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-04-06 | Initial release for vessel-operations-service |

## 📝 Contributing

To improve the checker:

1. Create issue describing the improvement
2. Discuss with Architecture Board
3. Submit PR with changes
4. Update documentation
5. Test thoroughly before merging

## 🎓 Learning Resources

### Internal Resources
- R1-R20 Resilience Checklist: `docs/R1-R20-RESILIENCE-CHECKLIST.md`
- Architecture Decision Records: `docs/adr/`
- Team Wiki: [Link to internal wiki]

### External Resources
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring Cloud Circuit Breaker](https://spring.io/projects/spring-cloud-circuitbreaker)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
- [Kafka Consumer Best Practices](https://kafka.apache.org/documentation/#consumerconfigs)

## ✅ Checklist for Success

- [ ] Read appropriate documentation for your role
- [ ] Understand R1-R20 patterns
- [ ] Know how to fix common issues
- [ ] Understand bypass process
- [ ] Know where to get help
- [ ] Configured local testing
- [ ] Bookmarked this documentation

## 🎉 Benefits

By using this resilience checker, you get:

- ✅ **Automated Quality Gates** - Catch issues before production
- ✅ **Consistent Standards** - Everyone follows R1-R20 patterns
- ✅ **Faster Reviews** - Automated checks reduce manual review time
- ✅ **Better Reliability** - Proven resilience patterns enforced
- ✅ **Knowledge Sharing** - Built-in documentation and examples
- ✅ **Compliance** - IBM standards automatically enforced

---

**Ready to get started?** Choose your path above and dive in! 🚀

**Made with Bob** 🤖