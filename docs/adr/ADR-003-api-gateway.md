# ADR-003 — API Gateway

## Status
Accepted

## Context
External clients should not know internal service locations or topology.

## Decision
Use an API Gateway as the external entry point for routing, correlation/trace propagation, rate limiting where appropriate, gateway-level security concerns, service-discovery integration, and routing/load balancing.

## Security
The gateway is not the only security boundary. Protected services will also validate JWTs and enforce service-level authorization.

## Consequences
Positive: single entry point, hidden topology, centralized routing and cross-cutting concerns.

Negative: additional infrastructure, possible bottleneck, gateway configuration becomes operationally important, security must not rely only on the gateway.
