# ADR-002 — Communication Strategy

## Status
Accepted

## Context
Order fulfillment spans independently owned services. Some interactions need immediate responses while the main workflow benefits from temporal decoupling.

## Options
1. REST for everything
2. Kafka for everything
3. Hybrid synchronous/asynchronous model

## Decision
Use a hybrid model. REST is used for request/response APIs; Kafka is used for business events and Saga workflow transitions.

## Decision drivers
Immediate response, temporal coupling, eventual consistency, failure handling, retries, operational complexity, observability.

## Consequences
Positive: appropriate communication per use case, Saga support, simple query APIs, independent scaling.

Negative: two communication models, duplicate delivery/order concerns, eventual consistency, distributed tracing requirements.
