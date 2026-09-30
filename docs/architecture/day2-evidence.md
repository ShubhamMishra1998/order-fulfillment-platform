# Day 2 Evidence

| Development-plan area | Evidence |
|---|---|
| Microservice Architecture Style | Service boundaries, communication strategy, service discovery |
| API Gateway / BFF concepts | API Gateway ADR |
| Synchronous vs asynchronous communication | Communication strategy + ADR |
| Service discovery | Service discovery document |
| Software Design | Trade-off analysis and ADRs |
| Engineering Practices | Architecture decision documentation |

## Key decisions
1. Services own their data.
2. Services communicate through contracts rather than direct database access.
3. REST is used where immediate responses are required.
4. Kafka is used for event-driven workflows.
5. API Gateway is the external entry point.
6. Service discovery is conceptually separate from the gateway.
7. Security is layered; services also validate JWTs and authorize requests.
