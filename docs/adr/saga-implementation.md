# Day 7 --- Saga Pattern & Compensation

## Objective

Implement a **choreography-based Saga** across Order, Inventory, and
Payment services using Kafka, Transactional Outbox, local transactions,
and compensating actions.

## 1. Why Saga?

The Order, Inventory, and Payment services each own a separate database.
A single `@Transactional` transaction cannot atomically update all three
databases.

Instead, the workflow uses a Saga:

``` text
OrderCreated
    ↓
InventoryReserved
    ↓
Payment
   ├── SUCCESS → PaymentCompleted → Order CONFIRMED
   └── FAILED  → PaymentFailed → Order PAYMENT_FAILED
                                      ↓
                           InventoryReleaseRequested
                                      ↓
                               Inventory RELEASED
```

This is a **choreography-based Saga**: services react to events rather
than using a central Saga orchestrator.

------------------------------------------------------------------------

## 2. Order State Machine

The Order domain was enhanced with:

``` java
public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    CONFIRMED,
    PAYMENT_FAILED,
    OUT_OF_STOCK
}
```

The important paths are:

``` text
PENDING → INVENTORY_RESERVED → CONFIRMED
```

and:

``` text
PENDING → INVENTORY_RESERVED → PAYMENT_FAILED
```

When payment fails, Inventory performs the compensation:

``` text
RESERVED → RELEASED
```

------------------------------------------------------------------------

## 3. Inventory Reservation

Order Service publishes `OrderCreated` to:

``` text
order-events
```

Inventory Service consumes it and, in one local transaction:

1.  Checks `processed_events` for idempotency.
2.  Creates an `InventoryReservation` with status `RESERVED`.
3.  Creates an `InventoryReservedEvent`.
4.  Stores the event in the Inventory Outbox.
5.  Records the original event ID in `processed_events`.

This guarantees that the reservation and event-processing record are
committed together.

------------------------------------------------------------------------

## 4. InventoryReserved Event

A typed event was introduced:

``` java
public record InventoryReservedEvent(
    UUID eventId,
    String orderId,
    String customerId,
    Instant occurredAt,
    int version
) {}
```

It is published to:

``` text
inventory-reserved-events
```

Payment Service consumes this event.

This ensures Payment is started only after Inventory reservation
succeeds.

------------------------------------------------------------------------

## 5. Payment Processing

Payment Service consumes `inventory-reserved-events`.

Payment states are:

``` java
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED
}
```

For testing, `CUST-FAIL` deterministically simulates a payment failure.
Other test customers follow the success path.

------------------------------------------------------------------------

## 6. Successful Payment Flow

When payment succeeds:

``` text
Payment → SUCCESS
```

Payment Service creates `PaymentCompletedEvent`, stores it in its
Outbox, and publishes it to:

``` text
payment-events
```

Order Service consumes it and changes the order to:

``` text
CONFIRMED
```

Final successful state:

``` text
Order      = CONFIRMED
Payment    = SUCCESS
Inventory  = RESERVED
```

------------------------------------------------------------------------

## 7. Payment Failure Flow

When payment fails:

``` text
Payment → FAILED
```

Payment Service creates `PaymentFailedEvent` and stores it in its
Outbox.

Order Service consumes `PaymentFailed` and, in one local transaction:

1.  Changes the order to `PAYMENT_FAILED`.
2.  Creates `InventoryReleaseRequestedEvent`.
3.  Stores the compensation event in the Order Outbox.

The compensation event is:

``` java
public record InventoryReleaseRequestedEvent(
    UUID eventId,
    String orderId,
    Instant occurredAt,
    String reason,
    int version
) {}
```

------------------------------------------------------------------------

## 8. Compensation Publishing

A dedicated `InventoryCompensationEventPublisher` was added to Order
Service.

The Order Outbox Publisher routes:

``` text
OrderCreated
    → order-events

InventoryReleaseRequested
    → inventory-compensation-events
```

This keeps the event routing explicit and avoids sending compensation
events through the normal Order event topic.

------------------------------------------------------------------------

## 9. Inventory Compensation

Inventory Service has a dedicated `InventoryCompensationConsumer`
listening to:

``` text
inventory-compensation-events
```

When `InventoryReleaseRequestedEvent` arrives, it:

1.  Deserializes the event.
2.  Checks `processed_events`.
3.  Finds the reservation by `orderId`.
4.  Changes status from `RESERVED` to `RELEASED`.
5.  Saves the reservation.
6.  Records the event ID in `processed_events`.

The reservation update and processed-event record are in one local
transaction.

------------------------------------------------------------------------

## 10. Transactional Outbox

The Saga uses the Transactional Outbox pattern.

Instead of:

``` text
Business DB update
       ↓
Kafka publish
```

the service performs:

``` text
Local DB transaction
       ├── Business data
       └── Outbox event
```

A scheduled Outbox Publisher later publishes pending events to Kafka.

Each Outbox record contains:

-   `event_id`
-   `event_type`
-   `aggregate_id`
-   `payload`
-   `published`

This avoids the database/Kafka dual-write problem.

------------------------------------------------------------------------

## 11. Consumer Idempotency

Kafka delivery can be duplicated, so consumers use `eventId` for
idempotency.

Before processing:

``` java
if (processedEventRepository.existsById(event.eventId())) {
    return;
}
```

After successful processing:

``` java
processedEventRepository.save(
    new ProcessedEvent(event.eventId())
);
```

Therefore, a duplicate Kafka delivery does not execute the business
operation twice.

Business identity is also protected using `orderId`, and Payment uses a
unique constraint on `order_id`.

------------------------------------------------------------------------

## 12. Kafka Topics

The final topic design is:

  ---------------------------------------------------------------------------------------
  Topic                             Producer          Consumer          Purpose
  --------------------------------- ----------------- ----------------- -----------------
  `order-events`                    Order             Inventory         Order events

  `inventory-reserved-events`       Inventory         Payment           Successful
                                                                        inventory
                                                                        reservation

  `payment-events`                  Payment           Order             Payment result

  `inventory-compensation-events`   Order             Inventory         Inventory
                                                                        compensation
  ---------------------------------------------------------------------------------------

------------------------------------------------------------------------

## 13. Architectural Issue Found During Testing

During end-to-end testing, Inventory reservations were initially
becoming `RELEASED` even for successful payments.

The cause was that both:

``` text
InventoryReserved
InventoryReleaseRequested
```

were being sent through the same `inventory-events` topic.

The compensation consumer could therefore receive an `InventoryReserved`
message and interpret it as a release request.

### Initial design

``` text
inventory-events
    ├── InventoryReserved
    └── InventoryReleaseRequested
```

### Final design

``` text
inventory-reserved-events
        ↓
      Payment

inventory-compensation-events
        ↓
      Inventory
```

This separated the event contracts and fixed the issue.

Both success and failure scenarios were then re-tested successfully.

------------------------------------------------------------------------

## 14. End-to-End Success Verification

Test input:

``` json
{
  "customerId": "CUST-2000"
}
```

Observed:

``` text
OrderCreated
    ↓
InventoryReserved
    ↓
Payment SUCCESS
    ↓
PaymentCompleted
    ↓
Order CONFIRMED
```

Final state:

``` text
Order      = CONFIRMED
Payment    = SUCCESS
Inventory  = RESERVED
```

------------------------------------------------------------------------

## 15. End-to-End Failure Verification

Test input:

``` json
{
  "customerId": "CUST-FAIL"
}
```

Observed:

``` text
OrderCreated
    ↓
InventoryReserved
    ↓
Payment FAILED
    ↓
PaymentFailed
    ↓
Order PAYMENT_FAILED
    ↓
InventoryReleaseRequested
    ↓
Inventory RELEASED
```

Final state:

``` text
Order      = PAYMENT_FAILED
Payment    = FAILED
Inventory  = RELEASED
```

Both scenarios were successfully verified end-to-end.

------------------------------------------------------------------------

## 16. Why Saga Instead of 2PC?

A distributed transaction using 2PC/XA would require coordination across
multiple independent databases.

The Saga approach instead:

-   Keeps database ownership within each microservice.
-   Uses local ACID transactions.
-   Uses Kafka for asynchronous communication.
-   Handles business failures with compensation.
-   Accepts eventual consistency between services.
-   Avoids a global transaction coordinator.

------------------------------------------------------------------------

## 17. Day 7 Definition of Done

-   [x] Implement Order state machine
-   [x] Implement Inventory reservation
-   [x] Implement `InventoryReservedEvent`
-   [x] Make Payment depend on successful inventory reservation
-   [x] Implement Payment success event
-   [x] Implement Payment failure event
-   [x] Implement Order payment-event consumer
-   [x] Implement `InventoryReleaseRequestedEvent`
-   [x] Implement Inventory compensation consumer
-   [x] Implement Transactional Outbox for Saga events
-   [x] Implement consumer idempotency
-   [x] Separate Kafka topics by event responsibility
-   [x] Verify successful Saga end-to-end
-   [x] Verify failed Saga and compensation end-to-end
-   [x] Identify and fix the initial Kafka topic-routing issue

## Status

**DAY 7 --- COMPLETE**

The platform now demonstrates a working choreography-based Saga with
reliable event publication, idempotent consumers, and compensating
transactions.
