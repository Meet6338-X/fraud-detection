# Fraud Detection System Architecture

## Overview

A high-performance Java-based fraud detection system designed to process up to 10,000 transactions per second using a hybrid rules-based approach with extensibility for ML integration.

## System Architecture

```mermaid
flowDiagram
    REST_API --> TransactionController
    TransactionController --> TransactionValidator
    TransactionValidator --> RuleEngine
    RuleEngine --> Redis_Cache
    RuleEngine --> PostgreSQL
    RuleEngine --> FraudAlertService
    FraudAlertService --> NotificationService
    Redis_Cache --> UserProfile
    PostgreSQL --> TransactionHistory
```

## Core Components

### 1. REST API Layer
- **Endpoint**: `/api/v1/transactions/analyze`
- **Input**: JSON payload with transaction details
- **Output**: Fraud analysis result (APPROVED, REVIEW, BLOCKED)

### 2. Transaction Processing Pipeline
1. **Validation** - Validate incoming transaction data
2. **Enrichment** - Load user profile and historical data from Redis
3. **Rule Evaluation** - Apply configurable fraud detection rules
4. **Decision** - Generate fraud verdict with risk score
5. **Storage** - Persist transaction and result to PostgreSQL
6. **Alert** - Generate alerts for high-risk transactions

### 3. Rule Engine
- Configurable rule definitions (JSON/YAML)
- Rule types:
  - Amount-based rules
  - Velocity rules (transactions per time window)
  - Location-based rules
  - Behavioral pattern rules
  - Device/IP reputation rules

### 4. Caching Strategy (Redis)
- User profiles (TTL: 24 hours)
- Recent transaction history (TTL: 1 hour)
- IP reputation cache (TTL: 1 hour)
- Rule configuration cache (TTL: 5 minutes)

### 5. Data Storage (PostgreSQL)
- `transactions` - Transaction records
- `fraud_alerts` - Generated alerts
- `user_profiles` - User information
- `rule_executions` - Audit trail of rule evaluations

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17+ |
| Build | Maven 3.8+ |
| REST Framework | Spring Boot 3.x |
| Database | PostgreSQL 15+ |
| Cache | Redis 7.x |
| Connection Pool | HikariCP |
| JSON Processing | Jackson |
| Validation | Jakarta Validation |
| Logging | SLF4J + Logback |

## Project Structure

```
fraud-detection/
├── src/
│   ├── main/
│   │   ├── java/com/fraud/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── model/           # Domain models
│   │   │   ├── repository/      # Data access layer
│   │   │   ├── service/         # Business logic
│   │   │   ├── engine/          # Rule engine
│   │   │   ├── cache/           # Redis caching
│   │   │   └── util/            # Utilities
│   │   └── resources/
│   │       ├── application.yml
│   │       └── rules.json
│   └── test/
│       └── java/com/fraud/
├── docs/
├── docker/
└── pom.xml
```

## Transaction Data Model

```json
{
  "transactionId": "string",
  "userId": "string",
  "amount": "decimal",
  "currency": "string",
  "timestamp": "datetime",
  "merchantId": "string",
  "merchantCategory": "string",
  "cardLast4": "string",
  "ipAddress": "string",
  "deviceId": "string",
  "location": {
    "country": "string",
    "city": "string",
    "coordinates": {"lat": "double", "lng": "double"}
  },
  "metadata": {}
}
```

## Fraud Detection Result

```json
{
  "transactionId": "string",
  "decision": "APPROVED | REVIEW | BLOCKED",
  "riskScore": "double (0-100)",
  "reasons": ["string"],
  "triggeredRules": ["string"],
  "processingTimeMs": "long",
  "timestamp": "datetime"
}
```

## Performance Targets

| Metric | Target |
|--------|--------|
| Throughput | 10,000 TPS |
| P99 Latency | < 100ms |
| Availability | 99.9% |
| Data Retention | 90 days |

## Scalability

- Horizontal scaling via stateless application instances
- Read replicas for PostgreSQL
- Redis cluster for high availability
- Connection pooling with optimized pool sizes

## Next Steps (ML Integration)

When ready for ML integration:
1. Create separate ML microservice (Python)
2. Export models as PMML or ONNX
3. Add gRPC/REST interface for model serving
4. Implement ensemble scoring (rules + ML)
5. Add model versioning and A/B testing
