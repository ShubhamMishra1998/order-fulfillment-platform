# Microservices Architecture Design Patterns

A practical guide to the SAGA pattern and the other patterns you will meet most often when building and interviewing on microservices. Examples lean towards Java / Spring Boot / AWS.

---

## Table of Contents

1. [Pattern Map](#1-pattern-map)
2. [SAGA Pattern (deep dive)](#2-saga-pattern-deep-dive)
3. [Data Management Patterns](#3-data-management-patterns)
4. [Resilience Patterns](#4-resilience-patterns)
5. [Communication and Routing Patterns](#5-communication-and-routing-patterns)
6. [Decomposition and Migration Patterns](#6-decomposition-and-migration-patterns)
7. [Observability and Cross-Cutting Patterns](#7-observability-and-cross-cutting-patterns)
8. [Choosing the Right Pattern](#8-choosing-the-right-pattern)

---

## 1. Pattern Map

| Problem                                   | Pattern                                       |
| ----------------------------------------- | --------------------------------------------- |
| Transaction spanning several services     | **SAGA**                                      |
| Each service needs its own data           | **Database per Service**                      |
| Atomically update DB and publish an event | **Transactional Outbox**                      |
| Separate read and write workloads         | **CQRS**                                      |
| Need full audit trail / rebuild state     | **Event Sourcing**                            |
| Downstream service is failing             | **Circuit Breaker, Retry, Timeout, Bulkhead** |
| Single entry point for clients            | **API Gateway / BFF**                         |
| Services find each other dynamically      | **Service Discovery**                         |
| Duplicate messages from the broker        | **Idempotent Consumer**                       |
| Incrementally break up a monolith         | **Strangler Fig**                             |
| Cross-cutting network concerns            | **Sidecar / Service Mesh**                    |
| Trace a request across services           | **Distributed Tracing, Correlation ID**       |

---

## 2. SAGA Pattern (deep dive)

### 2.1 The problem

In a monolith, one ACID transaction can span orders, payments and inventory. In microservices, each service owns its database (see Database per Service), so a single local transaction cannot cover the whole business operation. Two-phase commit (2PC) is usually avoided because it is blocking, couples services tightly, and is poorly supported by message brokers and NoSQL stores.

### 2.2 The idea

A **saga** is a sequence of **local transactions**. Each local transaction updates its own service's database and then triggers the next step (via event or command). If a step fails, the saga runs **compensating transactions** to semantically undo the steps already completed.

```
Happy path:
  Create Order -> Reserve Inventory -> Charge Payment -> Confirm Order

Failure at payment:
  Create Order -> Reserve Inventory -> Charge Payment (FAILS)
                        |
        Release Inventory  <-  Cancel Order   (compensations)
```

Key vocabulary:

- **Local transaction**: an ACID transaction inside one service.
- **Compensating transaction**: a business-level undo (for example, `RefundPayment` is the compensation for `ChargePayment`). It is not a database rollback; the original change is already committed and visible.
- **Pivot transaction**: the point of no return. Steps before it are compensable; steps after it must be retriable until they succeed.
- **Retriable transaction**: steps after the pivot that must eventually succeed (retry with idempotency).

### 2.3 Two styles

#### A) Choreography (event-driven, no central coordinator)

Each service listens for events and publishes its own events.

```
Order Service --OrderCreated--> Inventory Service
Inventory Service --InventoryReserved--> Payment Service
Payment Service --PaymentCompleted--> Order Service (confirm)

On failure:
Payment Service --PaymentFailed--> Inventory Service (release stock)
Payment Service --PaymentFailed--> Order Service (cancel order)
```

**Pros**

- Simple for 2 to 4 participants
- No single point of failure or extra orchestrator service
- Loose coupling

**Cons**

- Flow is spread across services, so it is hard to see the whole picture
- Risk of cyclic dependencies between services
- Harder to test and debug as steps grow

#### B) Orchestration (central coordinator)

A saga orchestrator tells each participant what to do and tracks the saga state.

```
              +---------------------+
              |  Order Saga         |
              |  Orchestrator       |
              +---------------------+
               |        |         |
   ReserveStock|  Charge|  Confirm|
               v        v         v
          Inventory  Payment    Order
```

**Pros**

- Flow is explicit and in one place
- Easier to add steps, monitor, and reason about failures
- Avoids cyclic dependencies

**Cons**

- Orchestrator is additional logic to build and operate
- Risk of putting too much business logic in the orchestrator (keep it to flow control)

#### Which to choose?

| Factor          | Choreography   | Orchestration                                                 |
| --------------- | -------------- | ------------------------------------------------------------- |
| Number of steps | Few (2 to 4)   | Many or branching                                             |
| Visibility      | Low            | High                                                          |
| Coupling        | Lowest         | Orchestrator knows all participants                           |
| Debugging       | Harder         | Easier                                                        |
| Typical tooling | SNS/SQS, Kafka | Step Functions, Camunda, Temporal, Axon, Spring State Machine |

### 2.4 Example: Order saga (orchestration, Spring-flavoured pseudocode)

```java
public class OrderSagaOrchestrator {

    public void start(OrderCreated event) {
        saga.begin(event.orderId());
        send(new ReserveInventoryCommand(event.orderId(), event.items()));
    }

    @SagaEventHandler
    public void on(InventoryReserved e) {
        send(new ChargePaymentCommand(e.orderId(), e.amount()));
    }

    @SagaEventHandler
    public void on(PaymentCompleted e) {
        send(new ConfirmOrderCommand(e.orderId()));
        saga.end(e.orderId());
    }

    // ---- Compensations ----
    @SagaEventHandler
    public void on(PaymentFailed e) {
        send(new ReleaseInventoryCommand(e.orderId()));
    }

    @SagaEventHandler
    public void on(InventoryReleased e) {
        send(new CancelOrderCommand(e.orderId(), "Payment failed"));
        saga.end(e.orderId());
    }
}
```

### 2.5 Example: Choreography with AWS SNS + SQS

```
Order Service --> SNS topic "order-events"
                     |--> SQS "inventory-queue" --> Inventory Service
                     |--> SQS "payment-queue"   --> Payment Service
                     |--> SQS "order-queue"     --> Order Service
```

Each service:

1. Does its local DB transaction.
2. Writes the outgoing event using the **Transactional Outbox** (see section 3.2).
3. Consumes messages **idempotently** (see section 5.4), because SQS Standard queues are at-least-once and may reorder.

For orchestration on AWS, **AWS Step Functions** is a natural orchestrator, with `Catch` blocks that route to compensation states.

### 2.6 Design rules and pitfalls

1. **Compensations must be idempotent and retriable.** They may be called more than once.
2. **Make forward steps idempotent too.** Use an idempotency key (saga ID plus step).
3. **Saga lacks isolation (the "I" in ACID).** Other transactions can see intermediate state. Mitigate with _countermeasures_:
   - **Semantic locks**: mark records as `PENDING` / `APPROVAL_PENDING`.
   - **Commutative updates**: design operations so order does not matter.
   - **Pessimistic view**: reorder steps to reduce risk.
   - **Reread value**: verify data has not changed before overwriting.
   - **Version file / by-value**: pick strategy by business risk.
4. **Order the steps smartly.** Put the steps most likely to fail, or non-compensable steps, as late as possible (use the pivot idea).
5. **Persist saga state** so that crashes and restarts resume correctly.
6. **Handle timeouts.** Each step needs a deadline and a defined failure behaviour.
7. **Some actions cannot be undone** (an email sent, a physical shipment). Model them as post-pivot steps or use a compensating _business_ action (apology email, return label).
8. **Make the saga observable.** Log the saga ID in every message and expose saga status.

### 2.7 Saga states (example)

```
STARTED -> INVENTORY_RESERVED -> PAYMENT_COMPLETED -> COMPLETED
    |               |                    |
    |               +--> COMPENSATING <--+
    +-----------------> FAILED / CANCELLED
```

---

## 3. Data Management Patterns

### 3.1 Database per Service

Each service owns its schema and no other service reads or writes it directly. Other services use the owner's API or events.

- **Pros**: independent deployment and scaling, freedom to choose the right store (polyglot persistence), enforced boundaries.
- **Cons**: no joins or cross-service ACID transactions, so you need Saga, CQRS, or API composition.
- Variants: private tables, schema per service, database per service. The key is _ownership_, not necessarily separate servers.

### 3.2 Transactional Outbox

**Problem:** A service must update its database _and_ publish an event. Doing both without a shared transaction risks inconsistency (the DB commits but the publish fails, or the reverse). This is the "dual write" problem.

**Solution:** In the same local DB transaction, write the business change and an event row into an `outbox` table. A separate relay publishes outbox rows to the broker and marks them sent.

```
BEGIN
  INSERT INTO orders ...
  INSERT INTO outbox (id, aggregate_id, type, payload, created_at) ...
COMMIT

Relay (poller or CDC such as Debezium) --> Kafka / SNS / SQS
```

```java
@Transactional
public void placeOrder(OrderRequest req) {
    Order order = orderRepo.save(new Order(req));
    outboxRepo.save(OutboxEvent.of("OrderCreated", order.getId(), toJson(order)));
}
```

- Delivery is **at-least-once**, so pair it with an Idempotent Consumer.
- Relay options: polling publisher, or log-tailing with Change Data Capture (Debezium).

### 3.3 CQRS (Command Query Responsibility Segregation)

Split the model into a **write side (commands)** and a **read side (queries)**, each optimised separately.

```
Client --command--> Command Service --> Write DB
                                           |
                                       events
                                           v
Client --query----> Query Service  <--  Read DB (denormalised view)
```

- **Use when**: read and write loads or shapes differ greatly, you need fast denormalised views across services (replaces cross-service joins), or complex reporting.
- **Costs**: eventual consistency between write and read sides, more moving parts.
- Do not use for simple CRUD services.

### 3.4 Event Sourcing

Store **the sequence of events** that changed state rather than just the current state. Current state = replay of events (plus snapshots for speed).

```
OrderCreated -> ItemAdded -> ItemAdded -> PaymentReceived -> OrderShipped
```

- **Pros**: complete audit log, time travel and debugging, natural fit for publishing events, easy to build new projections.
- **Cons**: steeper learning curve, event schema versioning, querying needs projections (usually combined with CQRS), replay cost.
- Often paired: **Event Sourcing + CQRS + Saga** (Axon Framework is a common Java choice).

### 3.5 API Composition

For queries that need data from several services, a composer (gateway or dedicated service) calls each service and merges the results in memory.

- Simple, but latency is the slowest call, and large joins are inefficient. Use CQRS views when this hurts.

---

## 4. Resilience Patterns

In a distributed system, failures are normal. Combine these patterns; none is sufficient alone.

### 4.1 Timeout

Never wait forever. Set connect and read timeouts on every remote call, and keep them shorter than the caller's own timeout so failures do not cascade.

### 4.2 Retry (with backoff and jitter)

Retry transient failures only (network blips, 503s), not business errors (400s).

- Use **exponential backoff plus jitter** to avoid retry storms.
- Cap attempts.
- Only retry **idempotent** operations (or use idempotency keys).

### 4.3 Circuit Breaker

Stops calling a failing dependency so it can recover and so callers fail fast.

```
CLOSED --(failure rate > threshold)--> OPEN
OPEN   --(wait duration elapsed)-----> HALF_OPEN
HALF_OPEN --(trial calls succeed)----> CLOSED
HALF_OPEN --(trial calls fail)-------> OPEN
```

Resilience4j with Spring Boot:

```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
@Retry(name = "paymentService")
@TimeLimiter(name = "paymentService")
public CompletableFuture<PaymentResponse> charge(PaymentRequest req) {
    return CompletableFuture.supplyAsync(() -> paymentClient.charge(req));
}

private CompletableFuture<PaymentResponse> paymentFallback(PaymentRequest req, Throwable t) {
    return CompletableFuture.completedFuture(PaymentResponse.pending(req.orderId()));
}
```

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        sliding-window-size: 20
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 5
```

### 4.4 Bulkhead

Isolate resources (thread pools, connection pools, semaphores) per dependency so one slow service cannot exhaust everything. Named after ship compartments.

### 4.5 Fallback / Graceful Degradation

Return cached data, defaults, or a reduced feature when a dependency is down (for example, show recommendations from cache).

### 4.6 Rate Limiting / Throttling

Protect services from overload (token bucket, leaky bucket). Commonly applied at the API gateway.

### 4.7 Dead Letter Queue (DLQ)

Messages that fail repeatedly are moved to a DLQ for inspection and replay instead of blocking the main queue. In SQS, configure a redrive policy with `maxReceiveCount`.

### Typical call stack ordering

```
Rate Limiter -> Retry -> Circuit Breaker -> Bulkhead -> Timeout -> Remote call
```

(Exact ordering depends on the library; the point is that they compose.)

---

## 5. Communication and Routing Patterns

### 5.1 API Gateway

Single entry point for clients. Handles routing, authentication, rate limiting, TLS termination, request aggregation, and protocol translation.

- Examples: Spring Cloud Gateway, AWS API Gateway, Kong, NGINX.
- **Risk**: becoming a bottleneck or a "god" component. Keep business logic out of it.

### 5.2 Backend for Frontend (BFF)

A dedicated gateway per client type (web, mobile, partner) tailored to that client's needs, instead of one general-purpose gateway.

### 5.3 Service Discovery

Services register themselves and clients find them dynamically instead of hard-coding addresses.

- **Client-side discovery**: client queries a registry (Eureka, Consul) and load-balances.
- **Server-side discovery**: a load balancer or platform does it (Kubernetes Services, AWS ALB/ECS Service Connect).

### 5.4 Idempotent Consumer

Message brokers typically deliver **at least once**. The consumer must tolerate duplicates.

```java
@Transactional
public void handle(Message msg) {
    if (processedRepo.existsById(msg.getId())) return;   // already handled
    doBusinessLogic(msg);
    processedRepo.save(new ProcessedMessage(msg.getId())); // same transaction
}
```

For SQS FIFO queues, a deduplication ID and message group ID help, but application-level idempotency is still best practice.

### 5.5 Synchronous vs Asynchronous Communication

|                  | Synchronous (REST / gRPC)             | Asynchronous (events, queues)         |
| ---------------- | ------------------------------------- | ------------------------------------- |
| Coupling         | Temporal coupling (callee must be up) | Decoupled in time                     |
| Latency          | Immediate response                    | Eventual                              |
| Failure handling | Needs resilience patterns             | Retry, DLQ built in                   |
| Good for         | Queries, user-facing request/response | Workflows, notifications, integration |

Prefer asynchronous messaging for cross-service workflows (sagas) and synchronous calls for simple queries.

### 5.6 Publish/Subscribe and Event-Driven Architecture

Producers emit events without knowing consumers. Distinguish:

- **Event notification**: "OrderCreated" with minimal data; consumers call back for details.
- **Event-carried state transfer**: the event contains the data consumers need, so they keep a local copy.
- **Command vs event**: a command asks for something to happen (`ReserveInventory`); an event states that something already happened (`InventoryReserved`).

### 5.7 Claim Check

Large payloads go to object storage (S3) and only a reference travels in the message (SQS has a 256 KB message limit).

---

## 6. Decomposition and Migration Patterns

### 6.1 Decompose by Business Capability / Subdomain (DDD)

Split services around business capabilities (Orders, Payments, Catalog) or DDD **bounded contexts**, not around technical layers. Aim for high cohesion within a service and low coupling between services.

### 6.2 Strangler Fig

Migrate a monolith gradually. A routing layer (gateway or proxy) sends selected requests to new services while the rest still go to the monolith. Over time the monolith shrinks until it can be retired.

```
Client -> Gateway --/orders--> New Order Service
                  \--/*------> Legacy Monolith
```

### 6.3 Anti-Corruption Layer (ACL)

A translation layer between a new service and a legacy system or external model so the legacy design does not leak into the new domain model.

### 6.4 Branch by Abstraction

Introduce an abstraction in the monolith, implement a new service-backed version behind it, switch over, and delete the old implementation.

---

## 7. Observability and Cross-Cutting Patterns

### 7.1 Distributed Tracing and Correlation ID

Generate a trace/correlation ID at the edge and propagate it through every call and message so one request can be followed across services.

- Tools: OpenTelemetry, AWS X-Ray, Zipkin, Jaeger. Spring Boot 3 uses Micrometer Tracing.
- Put the trace ID in logs (MDC).

### 7.2 Centralised Logging

Ship structured (JSON) logs from all services to one place (CloudWatch Logs, ELK/OpenSearch, Loki).

### 7.3 Metrics and Health Checks

- Expose metrics (Micrometer, Prometheus): RED (Rate, Errors, Duration) for services and USE for resources.
- Health endpoints (Spring Boot Actuator): liveness vs readiness probes.

### 7.4 Externalised Configuration

Keep configuration outside the artifact: Spring Cloud Config, AWS Systems Manager Parameter Store, AWS Secrets Manager, Kubernetes ConfigMaps/Secrets. Same build runs in every environment.

### 7.5 Sidecar and Service Mesh

- **Sidecar**: a helper process deployed alongside a service (logging agent, proxy) to offload cross-cutting concerns.
- **Service mesh** (Istio, Linkerd, AWS App Mesh): sidecar proxies handle mTLS, retries, timeouts, traffic shifting and telemetry uniformly, without code changes.

### 7.6 Security Patterns

- **Access token at the gateway** (OAuth2 / OIDC, JWT) with services validating tokens.
- **Service-to-service auth** with mTLS or signed tokens.
- **Least privilege** IAM roles per service.

### 7.7 Deployment Patterns

- **Service per container**, orchestrated by Kubernetes or ECS.
- **Blue/Green**, **Canary**, and **Rolling** deployments for safe releases.
- **Consumer-driven contract testing** (Pact, Spring Cloud Contract) to catch breaking API changes between services.

---

## 8. Choosing the Right Pattern

```
Need a business transaction across services?
  -> SAGA (orchestration if many steps, choreography if few)
       -> publish events reliably?  Transactional Outbox
       -> consumers get duplicates? Idempotent Consumer

Need fast cross-service reads?
  -> CQRS read model (or API Composition if simple)

Need full history / audit?
  -> Event Sourcing

Calling an unreliable service?
  -> Timeout + Retry + Circuit Breaker + Bulkhead + Fallback

Breaking up a monolith?
  -> Decompose by subdomain + Strangler Fig + ACL

Many clients with different needs?
  -> API Gateway / BFF
```
