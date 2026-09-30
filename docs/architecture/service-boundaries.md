# Service Boundaries

## 1. API Gateway

### Responsibilities

- External entry point.
- Routing.
- Cross-cutting request concerns.
- Initial authentication handling.
- Coarse-grained authorization where appropriate.
- Correlation/trace propagation.

### Does not own

- Order business rules.
- Inventory business rules.
- Payment business rules.

## 2. Order Service

### Owns

- Order aggregate.
- Order items.
- Order lifecycle/state.
- Order business rules.
- Saga participation for the order workflow.

### Example states

`PENDING -> INVENTORY_RESERVED -> PAYMENT_COMPLETED -> CONFIRMED`

Failure examples:

`PENDING -> CANCELLED`

`INVENTORY_RESERVED -> PAYMENT_FAILED -> INVENTORY_RELEASED -> CANCELLED`

## 3. Inventory Service

### Owns

- Product stock.
- Reservations.
- Stock availability rules.
- Inventory locking/concurrency behavior.

This service will be used to demonstrate optimistic and pessimistic locking.

## 4. Payment Service

### Owns

- Payment attempts.
- Payment status.
- Payment-provider abstraction.

This service will be used to demonstrate Dependency Inversion and Strategy Pattern.

## 5. Notification Service

### Owns

- Notification requests/history.
- Notification delivery abstraction.

It consumes business events rather than participating in the synchronous order transaction.

## Data ownership rule

Each service owns its persistence model. Other services must not directly read another service's database.

## Communication

Synchronous:
- Client -> Gateway
- Gateway -> service APIs where request/response behavior is required

Asynchronous:
- Business events through Kafka
- Saga coordination/compensation
- Notifications
