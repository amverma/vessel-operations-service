# Disaster Recovery Plan - Vessel Operations Service

## Service Classification
- **Criticality**: P1 (Mission Critical)
- **RTO (Recovery Time Objective)**: 2 minutes
- **RPO (Recovery Point Objective)**: 2 minutes
- **Impact**: Vessel unable to depart halts all operations

## Critical Failure Scenarios

### Scenario 1: ContainerLoadedOnVessel Event Not Published

**Problem**: Vessel Operations Service fails to publish `ContainerLoadedOnVessel` event after loading a container.

**Impact**: 
- Container cannot be traced on vessel
- Vessel cannot depart (P1 incident)
- Operations halted

**Detection**:
- Monitor event publishing metrics
- Alert if event publish fails
- Check Kafka topic lag
- Verify event count matches container load count

**Recovery Steps**:
1. **Immediate (0-2 minutes)**:
   - Check circuit breaker status
   - Verify Kafka connectivity
   - Check AWS Glue Schema Registry availability
   - Review application logs for errors

2. **Short-term (2-5 minutes)**:
   - Query database for loaded containers without published events
   - Manually republish missing events from database state
   - Verify event delivery to downstream services

3. **Verification**:
   ```sql
   -- Find containers loaded but events not published
   SELECT lc.* FROM loaded_containers lc
   WHERE lc.loaded_at > NOW() - INTERVAL '1 hour'
   AND NOT EXISTS (
       SELECT 1 FROM event_outbox eo 
       WHERE eo.aggregate_id = lc.container_id 
       AND eo.event_type = 'ContainerLoadedOnVessel'
   );
   ```

### Scenario 2: Service Complete Failure

**Problem**: Vessel Operations Service crashes or becomes unresponsive.

**Impact**:
- Cannot load containers
- Cannot depart vessels
- P1 incident

**Detection**:
- Health check endpoint fails
- No response from service
- Circuit breakers open
- High error rates in logs

**Recovery Steps**:
1. **Immediate (0-1 minute)**:
   - Check service health: `curl http://service-url/actuator/health`
   - Review pod/container status
   - Check resource utilization (CPU, memory)

2. **Auto-Recovery (1-2 minutes)**:
   - Kubernetes auto-restart unhealthy pods
   - Load balancer redirects to healthy instances
   - Circuit breakers prevent cascading failures

3. **Manual Intervention (if auto-recovery fails)**:
   - Scale up replicas: `kubectl scale deployment vessel-operations-service --replicas=5`
   - Force pod restart: `kubectl rollout restart deployment vessel-operations-service`
   - Check database connectivity
   - Verify Kafka connectivity

### Scenario 3: Database Failure

**Problem**: AWS Aurora database becomes unavailable.

**Impact**:
- Cannot read/write vessel data
- Cannot track container loading
- P1 incident

**Detection**:
- Database connection errors
- High database latency
- Failed health checks

**Recovery Steps**:
1. **Immediate (0-1 minute)**:
   - AWS Aurora automatic failover to standby replica
   - Connection pool retries with exponential backoff

2. **Verification (1-2 minutes)**:
   - Verify database connectivity
   - Check replication lag
   - Validate data consistency

3. **Fallback**:
   - Use read replicas for read operations
   - Queue write operations for retry
   - Enable circuit breaker to prevent overload

### Scenario 4: Kafka Failure

**Problem**: Kafka cluster or specific topics unavailable.

**Impact**:
- Cannot publish events
- Cannot consume events from other services
- Vessel operations blocked

**Detection**:
- Kafka producer errors
- Consumer lag increases
- Event publishing failures

**Recovery Steps**:
1. **Immediate (0-2 minutes)**:
   - Retry mechanism activates (3 retries with backoff)
   - Circuit breaker prevents cascading failures
   - Events stored in database outbox table

2. **Short-term (2-5 minutes)**:
   - Verify Kafka cluster health
   - Check topic availability
   - Review broker logs

3. **Recovery**:
   - Republish events from outbox table once Kafka recovers
   - Verify event ordering maintained
   - Check consumer group offsets

## Monitoring & Alerting

### Critical Metrics
1. **Event Publishing Success Rate**: Must be > 99.9%
2. **API Response Time**: Must be < 500ms (p95)
3. **Database Connection Pool**: Monitor active connections
4. **Kafka Consumer Lag**: Must be < 100 messages
5. **Circuit Breaker Status**: Alert on OPEN state

### Alert Thresholds
- **P1 Alert**: Event publish failure, service down, vessel cannot depart
- **P2 Alert**: High latency (> 1s), circuit breaker half-open
- **P3 Alert**: Increased error rate (> 1%), high resource usage

## Backup & Recovery

### Database Backups
- **Frequency**: Continuous (AWS Aurora automated backups)
- **Retention**: 35 days
- **Point-in-Time Recovery**: Available for last 35 days

### Event Replay
- **Kafka Retention**: 7 days
- **Event Sourcing**: All events stored in database for audit
- **Replay Capability**: Can replay events from specific offset

## Testing

### Disaster Recovery Drills
- **Frequency**: Monthly
- **Scenarios**:
  1. Simulate service failure
  2. Simulate database failover
  3. Simulate Kafka unavailability
  4. Test event replay mechanism

### Chaos Engineering
- Kill random pods
- Introduce network latency
- Simulate database connection failures
- Test circuit breaker behavior

## Runbook

### Quick Reference Commands

```bash
# Check service health
curl http://vessel-operations-service:8080/actuator/health

# Check Kafka consumer lag
kafka-consumer-groups --bootstrap-server kafka:9092 \
  --group vessel-operations-service --describe

# Scale service
kubectl scale deployment vessel-operations-service --replicas=5

# View recent logs
kubectl logs -l app=vessel-operations-service --tail=100

# Check database connections
kubectl exec -it vessel-operations-service-pod -- \
  psql -h aurora-endpoint -U username -d vessel_operations \
  -c "SELECT count(*) FROM pg_stat_activity;"

# Republish events from outbox
kubectl exec -it vessel-operations-service-pod -- \
  java -jar app.jar --republish-events --from-timestamp="2024-01-01T00:00:00Z"
```

## Escalation

### P1 Incident Response
1. **0-2 minutes**: On-call engineer notified
2. **2-5 minutes**: Team lead engaged
3. **5-10 minutes**: Engineering manager notified
4. **10+ minutes**: VP Engineering and CTO notified

### Contact Information
- On-call Engineer: [PagerDuty rotation]
- Team Lead: [Contact details]
- Engineering Manager: [Contact details]
- Database Team: [Contact details]
- Infrastructure Team: [Contact details]

## Post-Incident

### Required Actions
1. Root cause analysis within 24 hours
2. Incident report within 48 hours
3. Action items to prevent recurrence
4. Update runbook with lessons learned
5. Review and update monitoring/alerting