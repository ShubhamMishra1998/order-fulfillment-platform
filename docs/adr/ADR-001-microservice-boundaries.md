# ADR-001 — Microservice Boundaries

## Status

Accepted for the Level-Up development project.

## Context

The development plan requires practical evidence of microservice architecture, distributed transactions, Saga patterns, security, persistence, and software design.

A single monolithic application would make it difficult to demonstrate service ownership and distributed failure handling.

## Options considered

1. Modular monolith
2. Microservices
3. Large number of fine-grained microservices

## Decision

Use five logical components:

- API Gateway
- Order Service
- Inventory Service
- Payment Service
- Notification Service

Each business service owns its data.

## Rationale

The boundaries correspond to distinct business responsibilities and allow the project to demonstrate:

- independent service ownership
- asynchronous communication
- distributed transactions
- Saga compensation
- service-level security
- independent persistence behavior

Five components are intentionally kept small enough for a four-week development cycle.

## Consequences

### Positive

- Clear ownership boundaries.
- Realistic distributed workflow.
- Practical Saga and failure scenarios.
- Allows persistence and security concerns to be demonstrated independently.

### Negative

- More operational complexity than a monolith.
- Distributed debugging is harder.
- Eventual consistency must be handled.
- Local development requires multiple processes/containers.

## Revisit when

The service boundaries should be revisited if implementation shows that a service has no meaningful independent responsibility or creates excessive coupling.
