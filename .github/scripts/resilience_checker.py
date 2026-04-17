#!/usr/bin/env python3
"""
R1-R20 Resilience Guardrail Checker v1.0

This script audits code against the R1-R20 resilience checklist.
It analyzes Kafka consumers, Kubernetes manifests, AWS Lambda handlers,
and Avro schemas for disaster recovery and resiliency patterns.

Author: Architecture Team
Version: 1.0
"""

import argparse
import json
import os
import re
import sys
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Any, Optional
import yaml


class ResilienceCheck:
    """Base class for resilience checks"""
    
    def __init__(self, check_id: str, name: str, category: str, severity: str, description: str):
        self.check_id = check_id
        self.name = name
        self.category = category
        self.severity = severity
        self.description = description
        self.findings: List[Dict[str, Any]] = []
        self.passed = False
        self.recommendation = ""
    
    def check(self, file_path: str, content: str) -> bool:
        """Override this method in subclasses"""
        raise NotImplementedError
    
    def add_finding(self, file: str, message: str, line: Optional[int] = None):
        """Add a finding to this check"""
        self.findings.append({
            "file": file,
            "line": line,
            "message": message
        })
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert check to dictionary"""
        return {
            "id": self.check_id,
            "name": self.name,
            "category": self.category,
            "severity": self.severity,
            "description": self.description,
            "passed": self.passed,
            "findings": self.findings,
            "recommendation": self.recommendation
        }


class CircuitBreakerCheck(ResilienceCheck):
    """R1: Circuit Breaker Pattern"""
    
    def __init__(self):
        super().__init__(
            "R1",
            "Circuit Breaker Pattern",
            "Fault Tolerance",
            "CRITICAL",
            "Implement circuit breakers for all external service calls to prevent cascading failures."
        )
        self.recommendation = "Add @CircuitBreaker annotation from Resilience4j or configure circuit breaker in configuration files."
    
    def check(self, file_path: str, content: str) -> bool:
        # Check for circuit breaker annotations or configurations
        patterns = [
            r'@CircuitBreaker',
            r'CircuitBreakerConfig',
            r'circuitBreaker\s*\(',
            r'circuit-breaker:',
            r'circuitbreaker:'
        ]
        
        for pattern in patterns:
            if re.search(pattern, content, re.IGNORECASE):
                self.passed = True
                return True
        
        # Check if file contains external calls that need circuit breakers
        external_call_patterns = [
            r'RestTemplate',
            r'WebClient',
            r'@FeignClient',
            r'HttpClient',
            r'kafkaTemplate\.send',
            r'jdbcTemplate\.'
        ]
        
        has_external_calls = any(re.search(p, content) for p in external_call_patterns)
        
        if has_external_calls:
            self.add_finding(file_path, "External service calls detected without circuit breaker protection")
            return False
        
        # If no external calls, consider it passed
        self.passed = True
        return True


class RetryCheck(ResilienceCheck):
    """R2: Retry with Exponential Backoff"""
    
    def __init__(self):
        super().__init__(
            "R2",
            "Retry with Exponential Backoff",
            "Fault Tolerance",
            "HIGH",
            "Implement retry logic with exponential backoff for transient failures."
        )
        self.recommendation = "Add @Retry annotation with exponential backoff configuration."
    
    def check(self, file_path: str, content: str) -> bool:
        patterns = [
            r'@Retry',
            r'RetryConfig',
            r'retryTemplate',
            r'exponentialBackoff',
            r'backoff:',
            r'retry-policy:'
        ]
        
        for pattern in patterns:
            if re.search(pattern, content, re.IGNORECASE):
                self.passed = True
                return True
        
        # Check for operations that should have retry
        retry_needed_patterns = [
            r'RestTemplate',
            r'WebClient',
            r'@FeignClient',
            r'kafkaTemplate\.send'
        ]
        
        needs_retry = any(re.search(p, content) for p in retry_needed_patterns)
        
        if needs_retry:
            self.add_finding(file_path, "Operations that may fail transiently detected without retry mechanism")
            return False
        
        self.passed = True
        return True


class TimeoutCheck(ResilienceCheck):
    """R3: Timeout Configuration"""
    
    def __init__(self):
        super().__init__(
            "R3",
            "Timeout Configuration",
            "Fault Tolerance",
            "CRITICAL",
            "All external calls must have explicit timeout configurations."
        )
        self.recommendation = "Configure connection-timeout, read-timeout, and write-timeout for all external calls."
    
    def check(self, file_path: str, content: str) -> bool:
        timeout_patterns = [
            r'@TimeLimiter',
            r'timeout:',
            r'connection-timeout:',
            r'read-timeout:',
            r'connectTimeout',
            r'readTimeout',
            r'requestTimeout',
            r'\.timeout\('
        ]
        
        has_timeout = any(re.search(p, content, re.IGNORECASE) for p in timeout_patterns)
        
        # Check for external calls
        external_call_patterns = [
            r'RestTemplate',
            r'WebClient',
            r'HttpClient',
            r'kafkaTemplate',
            r'@FeignClient'
        ]
        
        has_external_calls = any(re.search(p, content) for p in external_call_patterns)
        
        if has_external_calls and not has_timeout:
            self.add_finding(file_path, "External calls detected without explicit timeout configuration")
            self.passed = False
            return False
        
        self.passed = True
        return True


class BulkheadCheck(ResilienceCheck):
    """R4: Bulkhead Pattern"""
    
    def __init__(self):
        super().__init__(
            "R4",
            "Bulkhead Pattern",
            "Resource Isolation",
            "HIGH",
            "Isolate resources to prevent resource exhaustion from affecting entire system."
        )
        self.recommendation = "Configure @Bulkhead annotation or separate thread pools for different operations."
    
    def check(self, file_path: str, content: str) -> bool:
        # Check if bulkhead pattern exists
        bulkhead_patterns = [
            r'@Bulkhead',
            r'BulkheadConfig',
            r'ThreadPoolTaskExecutor',
            r'thread-pool:',
            r'bulkhead:',
            r'io\.github\.resilience4j\.bulkhead'
        ]
        
        has_bulkhead = any(re.search(p, content, re.IGNORECASE) for p in bulkhead_patterns)
        
        if has_bulkhead:
            self.passed = True
            return True
        
        # Check if bulkhead is needed (concurrent/async operations)
        concurrent_operation_patterns = [
            r'@Async',
            r'CompletableFuture',
            r'ExecutorService',
            r'ThreadPoolExecutor',
            r'@Scheduled',
            r'parallel\(\)',
            r'parallelStream\(\)',
            r'kafkaTemplate\.send',  # Async Kafka operations
            r'@KafkaListener.*concurrency'  # Concurrent Kafka consumers
        ]
        
        needs_bulkhead = any(re.search(p, content) for p in concurrent_operation_patterns)
        
        # Only flag if needed but missing
        if needs_bulkhead:
            self.add_finding(
                file_path,
                "Concurrent/async operations detected without bulkhead protection for resource isolation"
            )
            self.passed = False
            return False
        
        # Pass by default if bulkhead not needed
        self.passed = True
        return True


class RateLimitingCheck(ResilienceCheck):
    """R5: Rate Limiting"""
    
    def __init__(self):
        super().__init__(
            "R5",
            "Rate Limiting",
            "Resource Protection",
            "HIGH",
            "Implement rate limiting to protect services from overload."
        )
        self.recommendation = "Add @RateLimiter annotation or configure rate limiting middleware."
    
    def check(self, file_path: str, content: str) -> bool:
        patterns = [
            r'@RateLimiter',
            r'RateLimiterConfig',
            r'rate-limit:',
            r'rateLimit',
            r'throttle'
        ]
        
        has_rate_limiting = any(re.search(p, content, re.IGNORECASE) for p in patterns)
        
        if has_rate_limiting:
            # Found rate limiting in this file - don't change overall pass status
            return True
        
        # Check if file contains endpoints/controllers that should have rate limiting
        endpoint_patterns = [
            r'@RestController',
            r'@Controller',
            r'@RequestMapping',
            r'@GetMapping',
            r'@PostMapping',
            r'@PutMapping',
            r'@DeleteMapping',
            r'@PatchMapping',
            r'app\.get\(',
            r'app\.post\(',
            r'router\.',
            r'@Path\(',
            r'@Route\('
        ]
        
        has_endpoints = any(re.search(p, content, re.IGNORECASE) for p in endpoint_patterns)
        
        if has_endpoints:
            self.add_finding(file_path, "API endpoints detected without rate limiting protection")
            self.passed = False  # Mark check as failed
            return False
        
        # If no endpoints in this file, it's not relevant for rate limiting check
        return True


class HealthCheckCheck(ResilienceCheck):
    """R6: Health Checks"""
    
    def __init__(self):
        super().__init__(
            "R6",
            "Health Checks",
            "Observability",
            "CRITICAL",
            "Comprehensive health checks for all dependencies."
        )
        self.recommendation = "Implement liveness, readiness, and startup probes with health indicators for all dependencies."
    
    def check(self, file_path: str, content: str) -> bool:
        health_patterns = [
            r'/actuator/health',
            r'@HealthIndicator',
            r'HealthIndicator',
            r'livenessProbe:',
            r'readinessProbe:',
            r'startupProbe:',
            r'health-check:'
        ]
        
        self.passed = any(re.search(p, content, re.IGNORECASE) for p in health_patterns)
        
        if not self.passed and (file_path.endswith(('.yml', '.yaml')) and 'k8s' in file_path):
            self.add_finding(file_path, "Kubernetes manifest missing health check probes")
            return False
        
        return self.passed


class GracefulShutdownCheck(ResilienceCheck):
    """R7: Graceful Shutdown"""
    
    def __init__(self):
        super().__init__(
            "R7",
            "Graceful Shutdown",
            "Availability",
            "CRITICAL",
            "Services must shutdown gracefully without losing in-flight requests."
        )
        self.recommendation = "Implement @PreDestroy hooks and configure graceful shutdown timeout."
    
    def check(self, file_path: str, content: str) -> bool:
        patterns = [
            r'@PreDestroy',
            r'shutdown\s*\(',
            r'close\s*\(',
            r'graceful-shutdown:',
            r'preStop:',
            r'terminationGracePeriodSeconds:'
        ]
        
        self.passed = any(re.search(p, content, re.IGNORECASE) for p in patterns)
        
        # Check for Kafka consumers that need graceful shutdown
        if 'KafkaListener' in content or 'KafkaConsumer' in content:
            if not any(re.search(p, content) for p in [r'@PreDestroy', r'\.close\(']):
                self.add_finding(file_path, "Kafka consumer detected without graceful shutdown logic")
                self.passed = False
                return False
        
        return self.passed


class IdempotencyCheck(ResilienceCheck):
    """R8: Idempotency"""
    
    def __init__(self):
        super().__init__(
            "R8",
            "Idempotency",
            "Data Integrity",
            "CRITICAL",
            "All state-changing operations must be idempotent."
        )
        self.recommendation = "Implement idempotency keys and duplicate detection mechanisms."
    
    def check(self, file_path: str, content: str) -> bool:
        patterns = [
            r'idempotency',
            r'idempotent',
            r'deduplication',
            r'unique.*constraint',
            r'UNIQUE',
            r'@Transactional.*readOnly\s*=\s*false'
        ]
        
        has_idempotency = any(re.search(p, content, re.IGNORECASE) for p in patterns)
        
        # Check for state-changing operations
        state_change_patterns = [
            r'@PostMapping',
            r'@PutMapping',
            r'@DeleteMapping',
            r'@KafkaListener',
            r'\.save\(',
            r'\.update\(',
            r'\.delete\('
        ]
        
        has_state_changes = any(re.search(p, content) for p in state_change_patterns)
        
        if has_state_changes and not has_idempotency:
            self.add_finding(file_path, "State-changing operations detected without idempotency guarantees")
            self.passed = False
            return False
        
        self.passed = True
        return True


class DeadLetterQueueCheck(ResilienceCheck):
    """R9: Dead Letter Queue (DLQ)"""
    
    def __init__(self):
        super().__init__(
            "R9",
            "Dead Letter Queue (DLQ)",
            "Error Handling",
            "HIGH",
            "Failed messages must be routed to DLQ for analysis and replay."
        )
        self.recommendation = "Configure DLQ for Kafka consumers and implement DLQ routing for failed messages."
    
    def check(self, file_path: str, content: str) -> bool:
        patterns = [
            r'dead.*letter',
            r'dlq',
            r'DLQ',
            r'error.*topic',
            r'failed.*topic',
            r'DeadLetterPublishingRecoverer'
        ]
        
        has_dlq = any(re.search(p, content, re.IGNORECASE) for p in patterns)
        
        # Check for Kafka consumers
        if '@KafkaListener' in content or 'KafkaConsumer' in content:
            if not has_dlq:
                self.add_finding(file_path, "Kafka consumer detected without DLQ configuration")
                self.passed = False
                return False
        
        self.passed = True
        return True


class StructuredLoggingCheck(ResilienceCheck):
    """R10: Structured Logging"""
    
    def __init__(self):
        super().__init__(
            "R10",
            "Structured Logging",
            "Observability",
            "HIGH",
            "All logs must be structured with correlation IDs for tracing."
        )
        self.recommendation = "Use JSON logging with MDC for correlation IDs."
    
    def check(self, file_path: str, content: str) -> bool:
        patterns = [
            r'MDC\.',
            r'correlation.*id',
            r'trace.*id',
            r'request.*id',
            r'LogstashEncoder',
            r'JsonLayout'
        ]
        
        self.passed = any(re.search(p, content, re.IGNORECASE) for p in patterns)
        return self.passed


class DistributedTracingCheck(ResilienceCheck):
    """R11: Distributed Tracing"""
    
    def __init__(self):
        super().__init__(
            "R11",
            "Distributed Tracing",
            "Observability",
            "HIGH",
            "Implement distributed tracing for request flow visibility."
        )
        self.recommendation = "Integrate OpenTelemetry or Spring Cloud Sleuth for distributed tracing."
    
    def check(self, file_path: str, content: str) -> bool:
        patterns = [
            r'spring-cloud-sleuth',
            r'opentelemetry',
            r'@NewSpan',
            r'Tracer',
            r'zipkin',
            r'jaeger'
        ]
        
        self.passed = any(re.search(p, content, re.IGNORECASE) for p in patterns)
        return self.passed


class MetricsCheck(ResilienceCheck):
    """R12: Metrics and Monitoring"""
    
    def __init__(self):
        super().__init__(
            "R12",
            "Metrics and Monitoring",
            "Observability",
            "CRITICAL",
            "Expose comprehensive metrics for monitoring."
        )
        self.recommendation = "Use Micrometer with Prometheus for metrics collection."
    
    def check(self, file_path: str, content: str) -> bool:
        patterns = [
            r'@Timed',
            r'@Counted',
            r'MeterRegistry',
            r'micrometer',
            r'prometheus',
            r'/actuator/metrics',
            r'metrics:'
        ]
        
        self.passed = any(re.search(p, content, re.IGNORECASE) for p in patterns)
        return self.passed


class SchemaEvolutionCheck(ResilienceCheck):
    """R13: Schema Evolution Strategy"""
    
    def __init__(self):
        super().__init__(
            "R13",
            "Schema Evolution Strategy",
            "Data Integrity",
            "CRITICAL",
            "Avro schemas must support backward/forward compatibility."
        )
        self.recommendation = "Configure schema registry with appropriate compatibility mode."
    
    def check(self, file_path: str, content: str) -> bool:
        if not file_path.endswith(('.avsc', '.avdl')):
            self.passed = True
            return True
        
        # Check for schema registry configuration
        patterns = [
            r'schema.*registry',
            r'compatibility',
            r'BACKWARD',
            r'FORWARD',
            r'FULL'
        ]
        
        self.passed = any(re.search(p, content, re.IGNORECASE) for p in patterns)
        
        if not self.passed:
            self.add_finding(file_path, "Avro schema without compatibility configuration")
        
        return self.passed


class KafkaConsumerBestPracticesCheck(ResilienceCheck):
    """R14: Kafka Consumer Best Practices"""
    
    def __init__(self):
        super().__init__(
            "R14",
            "Kafka Consumer Best Practices",
            "Message Processing",
            "CRITICAL",
            "Kafka consumers must follow best practices for reliability."
        )
        self.recommendation = "Use manual offset commit, proper exception handling, and rebalance listeners."
    
    def check(self, file_path: str, content: str) -> bool:
        if '@KafkaListener' not in content and 'KafkaConsumer' not in content:
            self.passed = True
            return True
        
        # Check for manual commit
        has_manual_commit = any(re.search(p, content) for p in [
            r'enable\.auto\.commit.*false',
            r'commitSync',
            r'commitAsync',
            r'AckMode\.MANUAL'
        ])
        
        # Check for error handling
        has_error_handling = any(re.search(p, content) for p in [
            r'@KafkaListener.*errorHandler',
            r'try.*catch',
            r'ErrorHandler',
            r'@RetryableTopic'
        ])
        
        if not has_manual_commit:
            self.add_finding(file_path, "Kafka consumer should use manual offset commit")
            self.passed = False
            return False
        
        if not has_error_handling:
            self.add_finding(file_path, "Kafka consumer missing proper error handling")
            self.passed = False
            return False
        
        self.passed = True
        return True


class DatabaseConnectionPoolingCheck(ResilienceCheck):
    """R15: Database Connection Pooling"""
    
    def __init__(self):
        super().__init__(
            "R15",
            "Database Connection Pooling",
            "Resource Management",
            "HIGH",
            "Proper database connection pool configuration."
        )
        self.recommendation = "Configure HikariCP with appropriate pool size and timeouts."
    
    def check(self, file_path: str, content: str) -> bool:
        patterns = [
            r'hikari',
            r'maximum-pool-size:',
            r'connection-timeout:',
            r'idle-timeout:',
            r'max-lifetime:'
        ]
        
        self.passed = any(re.search(p, content, re.IGNORECASE) for p in patterns)
        return self.passed


class KubernetesResourceLimitsCheck(ResilienceCheck):
    """R16: Kubernetes Resource Limits"""
    
    def __init__(self):
        super().__init__(
            "R16",
            "Kubernetes Resource Limits",
            "Resource Management",
            "CRITICAL",
            "All pods must have resource requests and limits."
        )
        self.recommendation = "Define CPU and memory requests/limits in Kubernetes manifests."
    
    def check(self, file_path: str, content: str) -> bool:
        if not (file_path.endswith(('.yml', '.yaml')) and ('k8s' in file_path or 'helm' in file_path)):
            self.passed = True
            return True
        
        has_requests = 'requests:' in content
        has_limits = 'limits:' in content
        has_cpu = 'cpu:' in content
        has_memory = 'memory:' in content
        
        if not (has_requests and has_limits and has_cpu and has_memory):
            self.add_finding(file_path, "Kubernetes manifest missing resource requests/limits")
            self.passed = False
            return False
        
        self.passed = True
        return True


class HPACheck(ResilienceCheck):
    """R17: Horizontal Pod Autoscaling (HPA)"""
    
    def __init__(self):
        super().__init__(
            "R17",
            "Horizontal Pod Autoscaling (HPA)",
            "Scalability",
            "HIGH",
            "Configure HPA for automatic scaling based on load."
        )
        self.recommendation = "Create HPA manifest with appropriate scaling metrics."
    
    def check(self, file_path: str, content: str) -> bool:
        if not (file_path.endswith(('.yml', '.yaml')) and ('k8s' in file_path or 'helm' in file_path)):
            self.passed = True
            return True
        
        patterns = [
            r'kind:\s*HorizontalPodAutoscaler',
            r'autoscaling/v2',
            r'minReplicas:',
            r'maxReplicas:',
            r'targetCPUUtilizationPercentage:'
        ]
        
        self.passed = any(re.search(p, content) for p in patterns)
        return self.passed


class PDBCheck(ResilienceCheck):
    """R18: Pod Disruption Budget (PDB)"""
    
    def __init__(self):
        super().__init__(
            "R18",
            "Pod Disruption Budget (PDB)",
            "Availability",
            "HIGH",
            "Define PDB to ensure availability during disruptions."
        )
        self.recommendation = "Create PDB manifest to maintain minimum available pods."
    
    def check(self, file_path: str, content: str) -> bool:
        if not (file_path.endswith(('.yml', '.yaml')) and ('k8s' in file_path or 'helm' in file_path)):
            self.passed = True
            return True
        
        patterns = [
            r'kind:\s*PodDisruptionBudget',
            r'minAvailable:',
            r'maxUnavailable:'
        ]
        
        self.passed = any(re.search(p, content) for p in patterns)
        return self.passed


class SecretsManagementCheck(ResilienceCheck):
    """R19: Secrets Management"""
    
    def __init__(self):
        super().__init__(
            "R19",
            "Secrets Management",
            "Security",
            "CRITICAL",
            "Secrets must never be hardcoded or committed to version control."
        )
        self.recommendation = "Use Kubernetes Secrets, environment variables, or external secret management."
    
    def check(self, file_path: str, content: str) -> bool:
        # Check for hardcoded secrets
        secret_patterns = [
            r'password\s*=\s*["\'][^"\']+["\']',
            r'api[_-]?key\s*=\s*["\'][^"\']+["\']',
            r'secret\s*=\s*["\'][^"\']+["\']',
            r'token\s*=\s*["\'][^"\']+["\']',
            r'jdbc:.*://.*:.*@'
        ]
        
        for pattern in secret_patterns:
            if re.search(pattern, content, re.IGNORECASE):
                self.add_finding(file_path, "Potential hardcoded secret detected")
                self.passed = False
                return False
        
        self.passed = True
        return True


class DisasterRecoveryTestingCheck(ResilienceCheck):
    """R20: Disaster Recovery Testing"""
    
    def __init__(self):
        super().__init__(
            "R20",
            "Disaster Recovery Testing",
            "Disaster Recovery",
            "CRITICAL",
            "Regular DR drills and backup verification."
        )
        self.recommendation = "Document backup strategy, RTO/RPO, and maintain DR runbook."
    
    def check(self, file_path: str, content: str) -> bool:
        # Check for DR documentation
        if file_path.endswith(('.md', '.txt', '.adoc')):
            patterns = [
                r'disaster.*recovery',
                r'backup',
                r'RTO',
                r'RPO',
                r'failover',
                r'DR.*drill'
            ]
            
            self.passed = any(re.search(p, content, re.IGNORECASE) for p in patterns)
            return self.passed
        
        # Check for backup configurations
        if file_path.endswith(('.yml', '.yaml')):
            patterns = [
                r'backup:',
                r'snapshot:',
                r'velero',
                r'backup-schedule:'
            ]
            
            self.passed = any(re.search(p, content, re.IGNORECASE) for p in patterns)
            return self.passed
        
        self.passed = True
        return True

class TerraformStateBackendCheck(ResilienceCheck):
    """R21: Terraform State Backend Configuration"""
    
    def __init__(self):
        super().__init__(
            "R21",
            "Terraform State Backend Configuration",
            "Infrastructure as Code",
            "HIGH",
            "Terraform state must be stored in a remote backend with locking enabled."
        )
        self.recommendation = "Configure S3/Azure Storage backend with state locking (DynamoDB/Azure Storage)."
    
    def check(self, file_path: str, content: str) -> bool:
        # Only check Terraform files
        if not file_path.endswith('.tf'):
            self.passed = True
            return True
        
        # Check for backend configuration
        backend_patterns = [
            r'backend\s+"s3"',
            r'backend\s+"azurerm"',
            r'backend\s+"gcs"',
            r'backend\s+"remote"'
        ]
        
        has_backend = any(re.search(p, content, re.IGNORECASE) for p in backend_patterns)
        
        if has_backend:
            # Check for state locking
            locking_patterns = [
                r'dynamodb_table',
                r'lock',
                r'state_locking'
            ]
            has_locking = any(re.search(p, content, re.IGNORECASE) for p in locking_patterns)
            
            if not has_locking:
                self.add_finding(file_path, "Terraform backend configured but state locking not detected")
                self.passed = False
                return False
            
            self.passed = True
            return True
        else:
            # Check if this is a main.tf or backend.tf file
            if 'main.tf' in file_path or 'backend.tf' in file_path:
                self.add_finding(file_path, "No remote backend configuration found in Terraform")
                self.passed = False
                return False
        
        self.passed = True
        return True


class OpenAPISpecificationCheck(ResilienceCheck):
    """R22: OpenAPI Specification Validation"""
    
    def __init__(self):
        super().__init__(
            "R22",
            "OpenAPI Specification Validation",
            "API Design",
            "MEDIUM",
            "API specifications must include error responses, rate limiting, and versioning."
        )
        self.recommendation = "Include 4xx/5xx responses, rate limit headers, and API versioning in OpenAPI spec."
    
    def check(self, file_path: str, content: str) -> bool:
        # Only check OpenAPI/Swagger files
        if not (file_path.endswith(('.yaml', '.yml', '.json')) and 
                ('openapi' in file_path.lower() or 'swagger' in file_path.lower() or 
                 'openapi:' in content.lower() or 'swagger:' in content.lower())):
            self.passed = True
            return True
        
        issues = []
        
        # Check for error responses
        error_response_patterns = [
            r'["\']4\d{2}["\']',  # 4xx responses
            r'["\']5\d{2}["\']'   # 5xx responses
        ]
        has_error_responses = any(re.search(p, content) for p in error_response_patterns)
        if not has_error_responses:
            issues.append("Missing error response definitions (4xx/5xx)")
        
        # Check for rate limiting headers
        rate_limit_patterns = [
            r'X-RateLimit',
            r'RateLimit',
            r'rate.*limit',
            r'throttle'
        ]
        has_rate_limiting = any(re.search(p, content, re.IGNORECASE) for p in rate_limit_patterns)
        if not has_rate_limiting:
            issues.append("No rate limiting headers defined")
        
        # Check for versioning
        version_patterns = [
            r'version:\s*["\']?\d+',
            r'/v\d+/',
            r'api.*version'
        ]
        has_versioning = any(re.search(p, content, re.IGNORECASE) for p in version_patterns)
        if not has_versioning:
            issues.append("API versioning not detected")
        
        if issues:
            for issue in issues:
                self.add_finding(file_path, issue)
            self.passed = False
            return False
        
        self.passed = True
        return True


class HelmChartBestPracticesCheck(ResilienceCheck):
    """R23: Helm Chart Best Practices"""
    
    def __init__(self):
        super().__init__(
            "R23",
            "Helm Chart Best Practices",
            "Deployment",
            "MEDIUM",
            "Helm charts must follow best practices for production deployments."
        )
        self.recommendation = "Include resource limits, health checks, and proper labels in Helm charts."
    
    def check(self, file_path: str, content: str) -> bool:
        # Only check Helm chart files
        if not (file_path.startswith('helm') and file_path.endswith(('.yaml', '.yml'))):
            self.passed = True
            return True
        
        # Check Chart.yaml for required fields
        if 'Chart.yaml' in file_path:
            required_fields = ['name', 'version', 'apiVersion']
            missing_fields = [f for f in required_fields if not re.search(f'{f}:', content)]
            
            if missing_fields:
                self.add_finding(file_path, f"Missing required fields in Chart.yaml: {', '.join(missing_fields)}")
                self.passed = False
                return False
        
        # Check values.yaml for best practices
        if 'values.yaml' in file_path:
            issues = []
            
            # Check for resource limits
            if not re.search(r'resources:', content):
                issues.append("No resource limits defined in values.yaml")
            
            # Check for health checks
            health_check_patterns = [
                r'livenessProbe:',
                r'readinessProbe:',
                r'startupProbe:'
            ]
            has_health_checks = any(re.search(p, content) for p in health_check_patterns)
            if not has_health_checks:
                issues.append("No health check probes defined in values.yaml")
            
            # Check for replica count
            if not re.search(r'replicaCount:', content):
                issues.append("No replicaCount defined in values.yaml")
            
            if issues:
                for issue in issues:
                    self.add_finding(file_path, issue)
                self.passed = False
                return False
        
        self.passed = True
        return True


class ResilienceChecker:
    """Main resilience checker class"""
    
    def __init__(self, repo_path: str, severity_threshold: str = "CRITICAL"):
        self.repo_path = Path(repo_path)
        self.severity_threshold = severity_threshold
        self.checks: List[ResilienceCheck] = self._initialize_checks()
        self.severity_order = ["LOW", "MEDIUM", "HIGH", "CRITICAL"]
    
    def _initialize_checks(self) -> List[ResilienceCheck]:
        """Initialize all resilience checks"""
        return [
            CircuitBreakerCheck(),
            RetryCheck(),
            TimeoutCheck(),
            BulkheadCheck(),
            RateLimitingCheck(),
            HealthCheckCheck(),
            GracefulShutdownCheck(),
            IdempotencyCheck(),
            DeadLetterQueueCheck(),
            StructuredLoggingCheck(),
            DistributedTracingCheck(),
            MetricsCheck(),
            SchemaEvolutionCheck(),
            KafkaConsumerBestPracticesCheck(),
            DatabaseConnectionPoolingCheck(),
            KubernetesResourceLimitsCheck(),
            HPACheck(),
            PDBCheck(),
            SecretsManagementCheck(),
            DisasterRecoveryTestingCheck(),
            TerraformStateBackendCheck(),
            OpenAPISpecificationCheck(),
            HelmChartBestPracticesCheck()
        ]
    
    def _should_check_file(self, file_path: Path) -> bool:
        """Determine if file should be checked"""
        # Check if file is in monitored paths
        monitored_paths = ['src', 'k8s', 'helm', 'schemas', 'infrastructure', 'specs', 'config']
        
        path_str = str(file_path)
        if not any(path_str.startswith(p) or f'/{p}/' in path_str or f'\\{p}\\' in path_str for p in monitored_paths):
            return False
        
        # Check file extensions
        relevant_extensions = [
            '.java', '.kt', '.py', '.js', '.ts', '.go',
            '.yml', '.yaml', '.json',
            '.avsc', '.avdl',
            '.md', '.txt', '.adoc',
            '.tf', '.tfvars'
        ]
        
        return any(str(file_path).endswith(ext) for ext in relevant_extensions)
    
    def _get_files_to_check(self) -> List[Path]:
        """Get list of files to check"""
        files = []
        
        for pattern in ['src/**/*', 'k8s/**/*', 'helm/**/*', 'schemas/**/*', 'infrastructure/**/*', 'specs/**/*', 'config/**/*']:
            for file_path in self.repo_path.glob(pattern):
                if file_path.is_file() and self._should_check_file(file_path):
                    files.append(file_path)
        
        return files
    
    def run_checks(self) -> Dict[str, Any]:
        """Run all resilience checks"""
        files = self._get_files_to_check()
        
        print(f"Checking {len(files)} files against R1-R20 resilience checklist...")
        
        for file_path in files:
            try:
                with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()
                
                relative_path = file_path.relative_to(self.repo_path)
                
                for check in self.checks:
                    check.check(str(relative_path), content)
            
            except Exception as e:
                print(f"Error checking file {file_path}: {e}")
        
        return self._generate_report()
    
    def _generate_report(self) -> Dict[str, Any]:
        """Generate final report"""
        passed_checks = [c for c in self.checks if c.passed]
        failed_checks = [c for c in self.checks if not c.passed]
        
        # Calculate summary by severity
        summary = {
            "critical": {"total": 0, "passed": 0, "failed": 0},
            "high": {"total": 0, "passed": 0, "failed": 0},
            "medium": {"total": 0, "passed": 0, "failed": 0},
            "low": {"total": 0, "passed": 0, "failed": 0}
        }
        
        for check in self.checks:
            severity_key = check.severity.lower()
            summary[severity_key]["total"] += 1
            if check.passed:
                summary[severity_key]["passed"] += 1
            else:
                summary[severity_key]["failed"] += 1
        
        # Determine overall status based on threshold
        threshold_index = self.severity_order.index(self.severity_threshold)
        overall_status = "PASS"
        
        for check in failed_checks:
            check_severity_index = self.severity_order.index(check.severity)
            if check_severity_index >= threshold_index:
                overall_status = "FAIL"
                break
        
        report = {
            "version": "1.0",
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "overall_status": overall_status,
            "severity_threshold": self.severity_threshold,
            "summary": summary,
            "total_checks": len(self.checks),
            "passed_checks": [c.to_dict() for c in passed_checks],
            "failed_checks": [c.to_dict() for c in failed_checks]
        }
        
        return report


def main():
    parser = argparse.ArgumentParser(
        description="R1-R20 Resilience Guardrail Checker v1.0"
    )
    parser.add_argument(
        "--repo-path",
        required=True,
        help="Path to the repository to check"
    )
    parser.add_argument(
        "--output-format",
        choices=["json", "text"],
        default="json",
        help="Output format (default: json)"
    )
    parser.add_argument(
        "--output-file",
        help="Output file path (default: stdout)"
    )
    parser.add_argument(
        "--severity-threshold",
        choices=["CRITICAL", "HIGH", "MEDIUM", "LOW"],
        default="CRITICAL",
        help="Minimum severity to fail the check (default: CRITICAL)"
    )
    
    args = parser.parse_args()
    
    checker = ResilienceChecker(args.repo_path, args.severity_threshold)
    report = checker.run_checks()
    
    if args.output_format == "json":
        output = json.dumps(report, indent=2)
    else:
        output = f"R1-R20 Resilience Check Report\n"
        output += f"Status: {report['overall_status']}\n"
        output += f"Passed: {len(report['passed_checks'])}/{report['total_checks']}\n"
        output += f"Failed: {len(report['failed_checks'])}/{report['total_checks']}\n"
    
    if args.output_file:
        with open(args.output_file, 'w') as f:
            f.write(output)
        print(f"Report written to {args.output_file}")
    else:
        print(output)
    
    # Exit with error code if checks failed
    sys.exit(0 if report['overall_status'] == "PASS" else 1)


if __name__ == "__main__":
    main()

# Made with Bob
