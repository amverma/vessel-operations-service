# Vessel Operations Service - Architecture Documentation

## Overview

The Vessel Operations Service is a **mission-critical (P1)** microservice responsible for managing vessel loading operations and departure coordination in a freight forwarding system. The service follows **Clean Architecture** principles with **Domain-Driven Design (DDD)** patterns.

## Service Classification

- **Criticality**: P1 (Highest - Mission Critical)
- **RTO**: 2 minutes
- **RPO**: 2 minutes
- **Impact**: Vessel unable to depart halts all operations

## Architecture Patterns

### 1. Clean Architecture

The service is organized into distinct layers with clear dependency rules:

```
┌─────────────────────────────────────────────────────────┐
│                    API Layer (Presentation)              │
│  - REST Controllers                                      │
│  - DTOs (Request/Response)                               │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                  Application Layer                       │
│  - Use Cases (Business Logic)                            │
│  - Ports (Interfaces)                                    │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    Domain Layer                          │
│  - Entities & Aggregates (Vessel, LoadedContainer)      │
│  - Value Objects (VesselId, ContainerId)                │
│  - Domain Events                                         │
│  - Repository Interfaces                                 │
└─────────────────────────────────────────────────────────┘
                          ↑
┌─────────────────────────────────────────────────────────┐
│                Infrastructure Layer                      │
│  - Kafka Integration                                     │
│  - Database (JPA/PostgreSQL)                             │
│  - AWS Glue Schema Registry                              │
│  - Repository Implementations                            │
└─────────────────────────────────────────────────────────┘
```

### 2. Domain-Driven Design (DDD)

#### Bounded Context
**Vessel Operations** - Manages vessel loading and departure

#### Aggregates
- **Vessel** (Aggregate Root)
  - Manages vessel lifecycle
  - Enforces business rules for departure
  - Emits domain events

- **LoadedContainer** (Entity)
  - Tracks containers loaded on vessels
  - Part of vessel loading process

#### Value Objects
- `VesselId` - Unique vessel identifier
- `ContainerId` - ISO 6346 compliant container number

#### Domain Events
- `ContainerLoadedOnVessel` - Critical event for tracking
- `VesselDeparted` - Triggers downstream processes
- `VesselReconciled` - Confirms vessel ready to depart
- `ETAUpdated` - Notifies of schedule changes
- `LoadListPrepared` - One of departure conditions
- `StowagePlanCompleted` - One of departure conditions

### 3. Event-Driven Architecture

#### Process Choreography

```
Port Operations → ContainerGatedIn
                       ↓
              Vessel Operations → ContainerLoadedOnVessel
                       ↓
              Vessel Operations → VesselReconciled
                       ↓
              Vessel Operations → VesselDeparted
                       ↓
         ┌──────────────┴──────────────┐
         ↓                             ↓
    Customs Service            Tracking Service
    Billing Service            Notification Service
```

## Technology Stack

### Core Framework
- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **Spring Kafka**

### Database
- **PostgreSQL** (AWS Aurora in production)
- **Flyway** for migrations

### Messaging
- **Apache Kafka** for event streaming
- **AWS Glue Schema Registry** for schema validation
- **Avro** for serialization

### Resilience
- **Resilience4j** for circuit breaker and retry patterns
- **Spring Retry** for transient failure handling

### Monitoring
- **Spring Actuator** for health checks
- **Micrometer** with Prometheus for metrics

## Key Business Rules

### Vessel Departure Conditions

A vessel can **ONLY** depart when ALL of the following conditions are met:

1. ✅ Load list prepared and shared with terminal
2. ✅ Stowage plan and load instructions complete
3. ✅ All planned containers loaded (count matches)
4. ✅ Vessel reconciled and confirmed
5. ✅ Vessel status is not BLOCKED

**Failure to meet any condition results in P1 incident**

### Container Loading Rules

- Container ID must be ISO 6346 compliant (4 letters + 7 digits)
- Container cannot be loaded twice
- Stowage plan must be complete before loading
- Each load emits `ContainerLoadedOnVessel` event (CRITICAL)

## Critical Operations

### 1. Load Container on Vessel

**Endpoint**: `POST /api/v1/vessel-operations/containers/load`

**Flow**:
1. Validate vessel exists
2. Check container not already loaded
3. Execute domain logic (Vessel.loadContainer)
4. Persist to database
5. **Publish ContainerLoadedOnVessel event** (CRITICAL)

**Resilience**:
- Circuit breaker pattern
- Retry mechanism (3 attempts)
- Fallback handling

### 2. Depart Vessel

**Endpoint**: `POST /api/v1/vessel-operations/vessels/{vesselId}/depart`

**Flow**:
1. Validate all departure conditions
2. Execute domain logic (Vessel.depart)
3. Persist to database
4. **Publish VesselDeparted event** (CRITICAL)

**Resilience**:
- Circuit breaker pattern
- Retry mechanism
- Comprehensive validation

## Event Publishing Strategy

### Reliability Measures

1. **Synchronous Publishing with Retry**
   - 3 retry attempts with exponential backoff
   - Circuit breaker to prevent cascading failures

2. **Idempotent Events**
   - Each event has unique ID
   - Consumers handle duplicates

3. **Schema Validation**
   - AWS Glue Schema Registry validates all events
   - Backward compatibility enforced

4. **Monitoring**
   - Track publish success rate (must be > 99.9%)
   - Alert on failures

## Database Schema

### Tables

#### vessels
- Stores vessel information and status
- Tracks departure conditions
- Indexed for performance

#### loaded_containers
- Tracks containers loaded on vessels
- Foreign key to vessels
- Indexed on vessel_id and container_id

### Indexes
- Performance-optimized for common queries
- Supports vessel departure validation

## Disaster Recovery

### Failure Scenarios

1. **Event Publishing Failure**
   - Retry mechanism activates
   - Circuit breaker prevents overload
   - Manual recovery from database state

2. **Service Failure**
   - Kubernetes auto-restart
   - Load balancer redirects traffic
   - RTO: < 2 minutes

3. **Database Failure**
   - AWS Aurora automatic failover
   - Read replicas for read operations
   - RPO: < 2 minutes

4. **Kafka Failure**
   - Retry with exponential backoff
   - Event outbox pattern for recovery
   - Manual replay if needed

See [DISASTER_RECOVERY.md](DISASTER_RECOVERY.md) for detailed procedures.

## API Endpoints

### Container Operations

```
POST /api/v1/vessel-operations/containers/load
- Load a container onto a vessel
- Returns: LoadContainerResponse
```

### Vessel Operations

```
POST /api/v1/vessel-operations/vessels/{vesselId}/depart
- Depart a vessel (validates all conditions)
- Returns: VesselDepartureResponse
```

### Health & Monitoring

```
GET /actuator/health
- Service health status

GET /actuator/metrics
- Service metrics

GET /actuator/prometheus
- Prometheus metrics endpoint
```

## Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=vessel_operations
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# AWS
AWS_REGION=us-east-1
SCHEMA_REGISTRY_NAME=freight-forwarder-schema-registry

# Server
SERVER_PORT=8080
```

## Running the Service

### Local Development

```bash
# Start dependencies
docker-compose up -d postgres kafka zookeeper

# Run service
./mvnw spring-boot:run
```

### Docker

```bash
# Build and run all services
docker-compose up --build
```

### Production Deployment

```bash
# Build Docker image
docker build -t vessel-operations-service:latest .

# Deploy to Kubernetes
kubectl apply -f k8s/deployment.yaml
```

## Monitoring & Alerting

### Key Metrics

1. **Event Publishing Success Rate**: > 99.9%
2. **API Response Time (p95)**: < 500ms
3. **Database Connection Pool**: Monitor active connections
4. **Kafka Consumer Lag**: < 100 messages
5. **Circuit Breaker Status**: Alert on OPEN

### Alerts

- **P1**: Event publish failure, service down, vessel cannot depart
- **P2**: High latency, circuit breaker half-open
- **P3**: Increased error rate, high resource usage

## Testing Strategy

### Unit Tests
- Domain logic (Vessel aggregate)
- Use cases
- Event handlers

### Integration Tests
- Database operations
- Kafka integration
- API endpoints

### Disaster Recovery Tests
- Monthly DR drills
- Chaos engineering
- Failover testing

## Security Considerations

1. **Authentication**: OAuth2/JWT (to be implemented)
2. **Authorization**: Role-based access control
3. **Data Encryption**: TLS for data in transit
4. **Secrets Management**: AWS Secrets Manager
5. **Audit Logging**: All critical operations logged

## Future Enhancements

1. **Event Sourcing**: Complete event store implementation
2. **CQRS**: Separate read/write models
3. **Saga Pattern**: For complex multi-service transactions
4. **GraphQL API**: For flexible querying
5. **Real-time Dashboard**: WebSocket-based updates

## References

- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Event-Driven Architecture](https://martinfowler.com/articles/201701-event-driven.html)
- [AWS Glue Schema Registry](https://docs.aws.amazon.com/glue/latest/dg/schema-registry.html)