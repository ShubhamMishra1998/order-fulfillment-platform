# Spring `@Transactional`: Mastery Guide

**Level Up Dev Plan \| Java 21 \| Spring Boot \| JPA/Hibernate**

## 1. What `@Transactional` actually does

`@Transactional` declares a method or class as a transactional boundary.
When Spring intercepts a call to that boundary, it coordinates
transaction creation/joining, commit, rollback, and transaction
attributes through a transaction manager.

A database transaction is commonly explained using **ACID**:

-   **Atomicity** --- the unit of work commits as a whole or rolls back.
-   **Consistency** --- constraints and application rules remain valid.
-   **Isolation** --- concurrent transactions interact according to the
    configured isolation level.
-   **Durability** --- committed changes persist according to the
    database's guarantees.

``` java
@Service
public class TransferService {
    private final AccountRepository accounts;

    public TransferService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional
    public void transfer(long fromId, long toId, BigDecimal amount) {
        Account from = accounts.findById(fromId).orElseThrow();
        Account to = accounts.findById(toId).orElseThrow();

        from.debit(amount);
        to.credit(amount);
        // Both updates commit together, or both roll back on a qualifying failure.
    }
}
```

`@Transactional` is **not** a distributed transaction across multiple
services or databases. It normally controls resources managed by one
configured transaction manager. For cross-service workflows, use
patterns such as Saga, outbox, idempotent consumers, and compensation.

## 2. How Spring applies it: proxy mental model

In the common Spring proxy-based configuration, Spring creates a proxy
around the bean. A call entering through that proxy can start or join a
transaction.

``` text
Controller
   |
   v
Spring proxy  -- begin/join transaction
   |
   v
Service method -- repository/database operations
   |
   +-- normal return --> commit (unless marked rollback-only)
   |
   +-- qualifying exception --> rollback
```

### The self-invocation trap

``` java
@Service
public class BillingService {
    public void process() {
        saveInvoice(); // direct call on this object; does not pass through the proxy
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveInvoice() {
        // The REQUIRES_NEW boundary may not be applied to this internal call.
    }
}
```

In proxy mode, a call from one method to another on the same instance
bypasses the proxy. Put the transactional method on another Spring bean
and call that bean through its injected proxy. AspectJ weaving is
another option, but is not the usual default.

Other proxy considerations: - The bean must be managed by Spring. - A
call must enter through the proxy for proxy-based advice to apply. - Do
not assume private methods or self-invocation create transaction
boundaries. - Prefer public service methods as clear transaction
boundaries. - Transactions are commonly thread-bound; starting a new
thread or using an async executor does not automatically carry the
caller's transaction.

## 3. Propagation levels --- the core interview topic

Propagation describes what a method does when a transaction already
exists.

  ----------------------------------------------------------------------------------
  Propagation       Existing          No existing       Typical use / caution
                    transaction?      transaction?      
  ----------------- ----------------- ----------------- ----------------------------
  `REQUIRED`        Join it           Start one         Default for most
  *(default)*                                           service-layer business
                                                        operations

  `REQUIRES_NEW`    Suspend outer     Start one         Independent audit/status
                    transaction and                     write; extra connection may
                    start an                            be needed
                    independent one                     

  `SUPPORTS`        Join it           Run without a     Read logic that can work
                                      transaction       either way

  `NOT_SUPPORTED`   Suspend it and    Run without one   Work that should not
                    run without one                     participate in a transaction

  `MANDATORY`       Join it           Throw an          Enforce that caller owns the
                                      exception         transaction

  `NEVER`           Throw an          Run without one   Assert that no transaction
                    exception                           is active

  `NESTED`          Use a nested      Behaves like a    Partial rollback within an
                    scope, commonly a new transaction   outer transaction; support
                    savepoint when                      is
                    supported                           manager/resource-dependent
  ----------------------------------------------------------------------------------

### 3.1 `REQUIRED` --- join or create

``` java
@Transactional // REQUIRED by default
public void placeOrder() {
    orderRepository.save(order);
    inventoryFacade.reserve(); // if transactional, usually joins this transaction
}
```

If both calls use the same transaction manager and the inner method
joins the outer transaction, they share one transaction. A rollback-only
marker set by the inner work can cause the outer commit to fail with
`UnexpectedRollbackException`, even if the outer method caught the
original exception.

**Memory aid:** *REQUIRED = reuse the current transaction, otherwise
create one.*

### 3.2 `REQUIRES_NEW` --- independent transaction

``` java
@Transactional
public void processPayment() {
    paymentRepository.save(payment);
    auditService.recordAttempt(); // separate bean; REQUIRES_NEW on recordAttempt
    throw new IllegalStateException("simulate failure");
}

@Service
class AuditService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttempt() {
        auditRepository.save(new AuditEntry("payment attempted"));
    }
}
```

The outer transaction is suspended while the inner transaction runs. If
the inner transaction commits successfully, the outer rollback does not
roll back the inner commit.

Use carefully: - The inner commit is independent; it can remain even if
the outer operation fails. - Suspending the outer transaction while
opening another can require another database connection; high
concurrency can exhaust the pool. - Do not use it just to "make rollback
work." Choose it only when independent commit semantics are intended.

**Memory aid:** *REQUIRES_NEW = pause the old one; start and finish a
separate one.*

### 3.3 `SUPPORTS` --- transaction optional

``` java
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public OrderSummary getSummary(long id) {
    return querySummary(id);
}
```

If a transaction exists, participate in it; otherwise run without one.
Actual behavior of transaction synchronization and resource access
depends on the transaction manager. `readOnly = true` is generally a
hint, not a universal write-prevention mechanism.

### 3.4 `NOT_SUPPORTED` --- explicitly non-transactional

``` java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void generateLargeExport() {
    // Runs without the caller's transaction; an existing one is suspended.
}
```

Useful when a lengthy operation should not hold a database transaction
open. It does not make every operation magically independent of all
transactional resources; it means this method does not participate in
the current transaction.

### 3.5 `MANDATORY` --- caller must provide a transaction

``` java
@Transactional(propagation = Propagation.MANDATORY)
public void updateOrderLines(List<OrderLine> lines) {
    // Reuse caller's transaction; fail if none exists.
}
```

Good for internal operations that must only run as part of a larger
atomic unit.

### 3.6 `NEVER` --- reject an active transaction

``` java
@Transactional(propagation = Propagation.NEVER)
public void callNonTransactionalIntegration() {
    // Throws if invoked while a transaction is active.
}
```

This is an assertion of non-transactional execution, not merely a
preference.

### 3.7 `NESTED` --- savepoint-style nested scope

``` java
@Transactional
public void importBatch(List<Row> rows) {
    for (Row row : rows) {
        try {
            importer.importOneNested(row);
        } catch (RuntimeException ex) {
            // Continue with other rows if the nested rollback was isolated.
        }
    }
}

@Service
class Importer {
    @Transactional(propagation = Propagation.NESTED)
    public void importOneNested(Row row) {
        // A supported manager may create a savepoint here.
    }
}
```

`NESTED` is **not the same as `REQUIRES_NEW`**. It usually means a
savepoint inside the existing physical transaction: rolling back to the
savepoint can undo nested changes while allowing the outer transaction
to continue. It depends on the transaction manager and underlying
resource. It is not uniformly supported by every JPA/JTA setup. Verify
behavior with the actual manager/database before relying on it.

**Memory aid:** *NESTED = a savepoint inside the parent transaction,
when supported.*

## 4. Propagation decision guide

-   **Normal business operation that should be atomic with its caller:**
    `REQUIRED`.
-   **Work that must commit even if the caller rolls back:** consider
    `REQUIRES_NEW` (for example, an audit record), understanding the
    independent commit.
-   **Can participate if a transaction exists but can run without one:**
    `SUPPORTS`.
-   **Must run without the caller's transaction:** `NOT_SUPPORTED`.
-   **Must only be called inside a transaction:** `MANDATORY`.
-   **Must never be called inside a transaction:** `NEVER`.
-   **Need savepoint-based partial rollback within a transaction:**
    `NESTED`, only after verifying support.

## 5. Rollback rules

By default, Spring rolls back on `RuntimeException` and `Error`. Checked
exceptions do **not** trigger rollback by default.

``` java
@Transactional
public void createOrder() throws IOException {
    repository.save(order);
    throw new IOException("checked exception");
    // By default, a checked IOException does not itself trigger rollback.
}
```

To request rollback for a checked exception:

``` java
@Transactional(rollbackFor = IOException.class)
public void createOrder() throws IOException {
    repository.save(order);
    throw new IOException("rollback this transaction");
}
```

You can also use `noRollbackFor`, but use rollback rules deliberately
and consistently. Prefer rethrowing failures when the transaction should
roll back. Catching an exception and returning normally can lead to
commit unless the transaction has already been marked rollback-only.

### Rollback-only and `UnexpectedRollbackException`

With `REQUIRED`, an inner method can mark the shared transaction
rollback-only. If the outer method catches the inner exception and then
returns normally, Spring cannot safely commit that transaction. At outer
completion it may throw `UnexpectedRollbackException`. Catching an
exception does not automatically reset the rollback-only flag.

## 6. Other important `@Transactional` attributes

``` java
@Transactional(
    propagation = Propagation.REQUIRED,
    isolation = Isolation.READ_COMMITTED,
    timeout = 5,
    readOnly = true,
    rollbackFor = BusinessException.class
)
public OrderResponse findOrProcess(...) {
    // Keep the transaction focused and short.
}
```

-   **`propagation`** --- interaction with an existing transaction.
-   **`isolation`** --- visibility/concurrency rules for this
    transaction; supported levels depend on the database. An isolation
    setting generally matters when a new transaction is started, not
    when joining an existing one.
-   **`timeout`** --- transaction timeout, subject to
    transaction-manager/resource support.
-   **`readOnly`** --- optimization hint for the transaction
    manager/provider; do not treat it as a security boundary.
-   **`rollbackFor` / `noRollbackFor`** --- exception classes that
    should or should not trigger rollback.
-   **`transactionManager`** (also known as `value` in annotation usage)
    --- selects the transaction manager when multiple are configured.

Class-level `@Transactional` supplies defaults for methods; method-level
settings can override them. Use the Spring version's API documentation
for exact supported options and any newer attributes.

## 7. Isolation is different from propagation

Do not confuse these concepts:

-   **Propagation:** "What happens if a transaction already exists?"
-   **Isolation:** "How do concurrent transactions see one another's
    changes?"

Common isolation levels (database support/behavior varies):

  -----------------------------------------------------------------------
  Isolation                           General idea
  ----------------------------------- -----------------------------------
  `READ_UNCOMMITTED`                  May allow dirty reads where
                                      supported

  `READ_COMMITTED`                    Reads only committed data;
                                      non-repeatable reads may occur

  `REPEATABLE_READ`                   Re-reading a row is generally
                                      stable; phantom behavior depends on
                                      database

  `SERIALIZABLE`                      Strongest isolation; may reduce
                                      concurrency or cause retries
  -----------------------------------------------------------------------

Never assume every database implements these levels identically.

## 8. Applying this to the Level Up microservices project

Your Order Fulfillment Platform uses separate service databases, Kafka,
an outbox, and Saga compensation. Keep these boundaries clear:

1.  **Within Order Service:** one local `@Transactional` method can save
    an order and its outbox event atomically in the same database
    transaction.
2.  **Outbox publisher:** publish only events whose outbox records are
    committed; handle retries and duplicate delivery.
3.  **Inventory Service:** its own local transaction updates reservation
    state and records processed-event/idempotency information atomically
    where possible.
4.  **Across Order, Inventory, and Payment:** `@Transactional` alone
    cannot create one atomic transaction across services. Use Saga state
    transitions and compensating actions.
5.  **Do not hold a database transaction open while waiting on Kafka or
    a remote HTTP call** unless there is a carefully justified design.
    Prefer short local transactions and asynchronous messaging.
6.  **`REQUIRES_NEW` does not make Kafka and the database one atomic
    resource.** It also does not replace the transactional outbox.

Example of the local outbox boundary:

``` java
@Service
public class OrderService {
    private final OrderRepository orders;
    private final OutboxEventRepository outbox;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orders,
            OutboxEventRepository outbox,
            ObjectMapper objectMapper) {
        this.orders = orders;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = orders.save(Order.from(request));
        String payload = serialize(new OrderCreatedEvent(order.getId()));
        outbox.save(OutboxEvent.pending("OrderCreated", payload));
        return OrderResponse.from(order);
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize event", ex);
        }
    }
}
```

This is illustrative: adapt entity factories, event schema, and
exception policy to your code. Because serialization failure is wrapped
in a runtime exception, Spring's default rollback rules apply. The order
and outbox row must use the same database and transaction manager for
this atomicity.

## 9. Common mistakes to explain in an interview

1.  **Self-invocation:** internal `this.someTransactionalMethod()` calls
    bypass the usual proxy.
2.  **Checked exception:** does not cause rollback by default.
3.  **Catching exceptions:** a caught exception does not guarantee
    commit; a shared transaction may already be rollback-only.
4.  **Misusing `REQUIRES_NEW`:** creates an independent commit and can
    consume an extra connection.
5.  **Assuming `NESTED` always works:** savepoint and manager support
    must be verified.
6.  **Long transactions:** increase lock duration, connection occupancy,
    and contention.
7.  **Remote calls inside transactions:** can hold locks/connections
    while waiting on network latency.
8.  **Assuming cross-service atomicity:** local Spring transactions do
    not automatically span independent service databases.
9.  **Assuming `readOnly = true` prevents writes:** it is generally a
    hint, not an authorization mechanism.
10. **Assuming every method is intercepted:** transaction management
    depends on bean/proxy configuration and call path.

## 10. Practice lab: prove the behavior

Create a small Spring Boot integration-test lab with a real database
(for example, Testcontainers PostgreSQL) and assert persisted rows after
each scenario.

### Lab A --- `REQUIRED`

-   Outer service writes row A.
-   Inner `REQUIRED` service writes row B and throws a runtime
    exception.
-   Let the exception escape.
-   Expected: neither row commits.

### Lab B --- caught inner exception

-   Outer writes row A.
-   Inner `REQUIRED` writes row B and throws.
-   Outer catches the exception and returns normally.
-   Expected: shared transaction may be rollback-only; assert final
    result and whether `UnexpectedRollbackException` is raised.

### Lab C --- `REQUIRES_NEW`

-   Outer writes row A.
-   Inner `REQUIRES_NEW` writes row B and commits.
-   Outer then throws.
-   Expected: A rolls back; B remains committed.

### Lab D --- checked exception

-   Save a row, throw a checked exception.
-   Test default behavior, then add `rollbackFor`.
-   Expected: by default the checked exception alone does not trigger
    rollback; with `rollbackFor`, it does.

### Lab E --- proxy/self-invocation

-   Compare an externally invoked transactional method with the same
    method called from another method on the same bean.
-   Confirm that the internal call does not create the expected proxy
    boundary.

### Lab F --- `NESTED`

-   Use a transaction manager/resource that supports savepoints.
-   Insert in outer scope, insert in nested scope, roll back nested
    scope, then commit outer scope.
-   Verify outer row remains and nested row does not. Document if your
    JPA/transaction-manager combination does not support the behavior.

**Evidence to retain:** test source, test output, a short explanation of
each observed result, and the transaction manager/database versions
used.

## 11. One-minute memory map

-   `REQUIRED` --- join or create.
-   `REQUIRES_NEW` --- suspend parent; independent transaction.
-   `SUPPORTS` --- join if present; otherwise no transaction.
-   `NOT_SUPPORTED` --- suspend parent; run without transaction.
-   `MANDATORY` --- transaction required; fail if absent.
-   `NEVER` --- fail if a transaction exists.
-   `NESTED` --- savepoint inside parent, if supported.