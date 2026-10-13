# Design Patterns – Template Method & Proxy

Both examples use an **order fulfillment** scenario and are plain Java, so you can run them without starting Spring Boot.

---

## 1. Template Method Pattern

📁 `src/main/java/com/levelup/designpatterns/templatemethod/`

### What is it?
A **behavioral** pattern where a base class defines the **steps of an algorithm** in a fixed order, and lets subclasses fill in **only the steps that differ**.

> 💡 Think of a recipe: the steps (prepare → cook → serve) are always the same, but *what* you cook changes.

### Problem it solves
Different order types (online, store pickup) follow the **same flow** but differ in a few steps. Without this pattern you would copy-paste the whole flow into every class, and someone could accidentally skip validation or change the order of steps.

### Key parts
| Part | In our code | Purpose |
|---|---|---|
| **Template method** | `processOrder()` (marked `final`) | Fixes the order of steps; subclasses can't change it |
| **Common step** | `validateOrder()` | Same for everyone; written once |
| **Abstract steps** | `makePayment()`, `deliver()` | Every subclass **must** implement these |
| **Hook** | `shouldSendNotification()` | Optional step with a default; subclasses **may** override |
| **Concrete classes** | `OnlineOrderProcessor`, `StorePickupOrderProcessor` | Supply their own versions of the varying steps |

### Flow
```
processOrder()                     (OrderProcessor - final)
 ├─ 1. validateOrder()             common
 ├─ 2. makePayment()               ← subclass decides
 ├─ 3. deliver()                   ← subclass decides
 └─ 4. sendNotification()          only if hook shouldSendNotification() == true
```

### Code snippet
```java
public final void processOrder(String orderId) {
    validateOrder(orderId);
    makePayment(orderId);      // abstract
    deliver(orderId);          // abstract
    if (shouldSendNotification()) {   // hook
        sendNotification(orderId);
    }
}
```

### Output
```
=== Processing order ORD-101 (OnlineOrderProcessor) ===
1. Validating order ORD-101
2. Charging credit card online for order ORD-101
3. Shipping order ORD-101 to customer's home address
4. Sending confirmation email for order ORD-101

=== Processing order ORD-202 (StorePickupOrderProcessor) ===
1. Validating order ORD-202
2. Customer pays cash at the store counter for order ORD-202
3. Customer picks up order ORD-202 from the store
```
Store pickup has no step 4 because it overrides the hook to return `false`.

### When to use
- Several classes share the same overall algorithm but differ in a few steps.
- You want to **enforce** the order of steps (e.g., validation must always run first).

### Real-world examples
- Spring `JdbcTemplate`, `RestTemplate`, and `AbstractController`
- JUnit lifecycle: `@BeforeEach → test → @AfterEach`
- `java.io.InputStream.read(byte[])` calls the abstract `read()`

---

## 2. Proxy Pattern

📁 `src/main/java/com/levelup/designpatterns/proxy/`

### What is it?
A **structural** pattern where a **stand-in object (proxy)** sits in front of the real object and **controls access** to it. The proxy implements the **same interface**, so the client never knows it's talking to a proxy.

> 💡 Think of a security guard at an office: visitors (clients) go through the guard (proxy), who checks ID before letting them reach the employee (real object).

### Problem it solves
We want to add things like **security checks, lazy loading, or logging** around `RealOrderService` **without modifying it** and without the client having to change.

### Key parts
| Part | In our code | Purpose |
|---|---|---|
| **Subject (interface)** | `OrderService` | Common contract for both real object and proxy |
| **Real subject** | `RealOrderService` | Does the actual work; treated as expensive to create |
| **Proxy** | `OrderServiceProxy` | Wraps the real object and adds extra behaviour |
| **Client** | `ProxyDemo` | Uses only `OrderService` |

### What our proxy adds
1. **Protection proxy:** only `ADMIN` can cancel orders.
2. **Virtual proxy (lazy creation):** `RealOrderService` is created **only on the first allowed call**, then reused.
3. **Logging proxy:** logs every request.

### Flow
```
Client ──► OrderServiceProxy ──(allowed?)──► RealOrderService
                │
                └── not ADMIN → "Access DENIED" (real object never touched)
```

### Code snippet
```java
public void cancelOrder(String orderId) {
    if (!"ADMIN".equals(userRole)) {
        System.out.println("Access DENIED");
        return;
    }
    if (realService == null) {
        realService = new RealOrderService();   // lazy creation
    }
    realService.cancelOrder(orderId);           // delegate
}
```

### Output
```
=== Customer tries to cancel ===
   [Proxy] CUSTOMER requested to cancel order ORD-101
   [Proxy] Access DENIED - only ADMIN can cancel orders

=== Admin cancels two orders ===
   [Proxy] ADMIN requested to cancel order ORD-202
   [RealOrderService] Created (expensive: connecting to DB...)
   [RealOrderService] Order ORD-202 cancelled successfully
   [Proxy] ADMIN requested to cancel order ORD-303
   [RealOrderService] Order ORD-303 cancelled successfully
```
"Created" prints **only once**. The second call reuses the same real object.

### Common types of proxy
| Type | Purpose |
|---|---|
| Protection proxy | Access control / permissions |
| Virtual proxy | Delay creating expensive objects until needed |
| Remote proxy | Represents an object running elsewhere (e.g., RMI, gRPC stubs) |
| Caching proxy | Returns cached results instead of calling the real object |
| Logging proxy | Logs or audits calls |

### Real-world examples
- **Spring AOP:** `@Transactional`, `@Cacheable`, and `@Async` all work through proxies
- **Hibernate** lazy loading of entities
- `java.lang.reflect.Proxy` (dynamic proxies)

---

## Template Method vs Proxy – Quick Comparison

| | Template Method | Proxy |
|---|---|---|
| Category | Behavioral | Structural |
| Core idea | Fixed steps in a base class; subclasses change some steps | Stand-in controls access to a real object |
| Relationship | **Inheritance** (`extends`) | **Composition** (proxy *has-a* real object) and the same interface |
| Who changes behaviour? | Subclasses override steps | The proxy adds behaviour before/after delegating |
| Typical use | Shared workflow with varying steps | Security, lazy loading, caching, logging |

---

## How to Run

**IntelliJ:** open `TemplateMethodDemo.java` or `ProxyDemo.java` and click ▶ next to `main`.

**Command line** (run from the `design-patterns` folder):
```powershell
.\mvnw.cmd -q compile
java -cp target\classes com.levelup.designpatterns.templatemethod.TemplateMethodDemo
java -cp target\classes com.levelup.designpatterns.proxy.ProxyDemo
```
