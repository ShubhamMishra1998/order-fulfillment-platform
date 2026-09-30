# Communication Strategy

## Decision
Use both synchronous and asynchronous communication.

### Synchronous
Use REST when the caller requires an immediate response, such as client queries.

### Asynchronous
Use Kafka for business events and long-running workflows such as OrderCreated, InventoryReserved, PaymentCompleted, and compensation events.

## Benefits
- Reduced temporal coupling
- Event-driven workflows
- Independent consumer scaling
- Retry capability
- Multiple consumers can react to the same event

## Trade-offs
- Eventual consistency
- Duplicate event handling
- Ordering concerns
- More difficult debugging
- Additional infrastructure
- Need for idempotency and observability

## Principle
Choose communication style based on business requirements, not a blanket synchronous or asynchronous rule.
