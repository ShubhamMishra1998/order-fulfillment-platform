# Non-Functional Requirements

These targets are development-project goals, not production SLAs.

## NFR-01 — Security

- JWT-based authentication.
- JWT signature and standard claim validation at protected services.
- Authorization based on roles/scopes.
- Sensitive operations protected at service level, not only at the gateway.

## NFR-02 — Availability and resilience

- A temporary downstream failure must not corrupt the order state.
- Saga compensation shall be used where applicable.
- Services should fail gracefully and expose meaningful errors.

## NFR-03 — Performance

- Target p95 latency below 500 ms for simple synchronous APIs under the local test workload.
- Avoid unnecessary database queries.
- Demonstrate and measure N+1 behavior and persistence optimizations.

## NFR-04 — Consistency

- Each service owns its data.
- Cross-service workflows use eventual consistency through a Saga.
- Business invariants must be enforced within the owning service.

## NFR-05 — Observability

- Structured logs.
- Correlation/trace IDs.
- Metrics for important operations.
- Distributed tracing across the order workflow.

## NFR-06 — Testability

- Unit tests for business logic.
- Integration tests for service/database boundaries.
- Testcontainers for infrastructure-dependent tests.
- Failure and concurrency scenarios must be covered.

## NFR-07 — Maintainability

- Clear service boundaries.
- SOLID principles where appropriate.
- ADRs for important architectural decisions.
- Consistent API and error-handling conventions.

## NFR-08 — Deployment

The application should be containerized and deployable through a repeatable CI/CD pipeline.
