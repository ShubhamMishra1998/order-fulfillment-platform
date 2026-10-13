# Java Dependency Injection: Learning Artifact

**Author:** Shubham Mishra
**Skill:** Java Dependency Injection (Intermediate → Advanced)
**Purpose:** Close the gap from my assessment: I conflated the *Dependency Injection pattern* with the *Dependency Inversion Principle (DIP)*. This document records the corrected understanding, with runnable code.

---

## 1. Correcting the Misconception: Three Different Ideas

| Concept | What it is | Level |
|---|---|---|
| **Dependency Inversion Principle (DIP)** | SOLID principle: high-level modules must not depend on low-level modules; both depend on **abstractions**. Abstractions must not depend on details. | Design *principle* (about the direction of source-code dependencies) |
| **Inversion of Control (IoC)** | The framework/container, not your code, controls object creation and wiring. | Architectural *style* |
| **Dependency Injection (DI)** | A *technique/pattern* to implement IoC: a class receives its collaborators from outside (constructor, setter, field) instead of creating them with `new`. | Implementation *pattern* |

**Key insight:** DI does not guarantee DIP. You can inject a concrete class (DI, but DIP violated). You can also follow DIP and wire things manually with no container (DIP, but no DI framework).

```
DIP  -> "depend on interfaces"         (what you depend on)
DI   -> "get them handed to you"       (how you obtain them)
IoC  -> "someone else drives the wiring" (who is in control)
```

---

## 2. Code Demonstration (plain Java, no Spring)

### 2.1 DI used, DIP violated

```java
class MySqlOrderRepository {
    void save(String order) { System.out.println("MySQL saved: " + order); }
}

class OrderService {
    private final MySqlOrderRepository repo;           // concrete type!

    OrderService(MySqlOrderRepository repo) {          // injected -> DI is present
        this.repo = repo;
    }
    void place(String order) { repo.save(order); }
}
```

This is constructor injection, but `OrderService` (high-level) is still coupled to `MySqlOrderRepository` (low-level detail). Swapping to Mongo or a test double means editing `OrderService`.

### 2.2 DI + DIP together

```java
interface OrderRepository {                            // abstraction owned by the high-level module
    void save(String order);
}

class MySqlOrderRepository implements OrderRepository {
    public void save(String order) { System.out.println("MySQL saved: " + order); }
}

class InMemoryOrderRepository implements OrderRepository {
    final java.util.List<String> store = new java.util.ArrayList<>();
    public void save(String order) { store.add(order); }
}

class OrderService {
    private final OrderRepository repo;                // depends on abstraction (DIP)

    OrderService(OrderRepository repo) {               // injected (DI)
        this.repo = repo;
    }
    void place(String order) { repo.save(order); }
}

public class Main {
    public static void main(String[] args) {
        // Manual composition root: IoC without any container
        new OrderService(new MySqlOrderRepository()).place("Order-1");

        // Test double swapped in with zero change to OrderService
        InMemoryOrderRepository fake = new InMemoryOrderRepository();
        new OrderService(fake).place("Order-2");
        System.out.println("Fake store: " + fake.store);
    }
}
```

**What this proves:** `OrderService` never changed between production and test wiring. That is DIP delivering its benefit, with DI as the delivery mechanism.

---

## 3. Spring DI in Depth

### 3.1 Wiring with Spring

```java
@Service
public class OrderService {
    private final OrderRepository repo;

    // Single constructor: @Autowired is optional since Spring 4.3
    public OrderService(OrderRepository repo) { this.repo = repo; }
}

@Repository
@Primary                                   // default when multiple candidates exist
class MySqlOrderRepository implements OrderRepository { /* ... */ }

@Repository("inMemoryRepo")
class InMemoryOrderRepository implements OrderRepository { /* ... */ }

// Selecting a specific implementation:
public OrderService(@Qualifier("inMemoryRepo") OrderRepository repo) { ... }
```

### 3.2 Injection types compared

| Type | Pros | Cons |
|---|---|---|
| **Constructor** (preferred) | Immutable (`final`) fields, mandatory deps guaranteed, easy unit tests without Spring, fails fast on missing bean | Verbose with many params (a design smell hinting at too many responsibilities) |
| **Setter** | Good for optional/re-configurable deps | Object can be partially constructed; mutable |
| **Field** (`@Autowired` on field) | Short | Hides dependencies, cannot be `final`, needs reflection/Spring for tests, encourages hidden coupling |

### 3.3 Practical gotchas I studied

1. **Circular dependencies:** A needs B, B needs A. Constructor injection fails at startup (`BeanCurrentlyInCreationException`), which is a *useful* signal of bad design. Spring Boot 2.6+ prohibits circular references by default. Fix by refactoring (extract a third component or use events), not by switching to field injection.
2. **Prototype into singleton:** Injecting a `prototype` bean into a `singleton` happens once, so you always get the same instance. Fix with `ObjectProvider<T>`, `@Lookup`, or a scoped proxy:
   ```java
   @Service
   class ReportService {
       private final ObjectProvider<ReportBuilder> builderProvider;
       ReportService(ObjectProvider<ReportBuilder> p) { this.builderProvider = p; }
       void run() { ReportBuilder b = builderProvider.getObject(); /* new each call */ }
   }
   ```
3. **Multiple candidates:** `NoUniqueBeanDefinitionException` is resolved via `@Primary`, `@Qualifier`, or injecting `List<OrderRepository>` / `Map<String, OrderRepository>` for strategy-style dispatch.
4. **Optional dependencies:** `Optional<T>`, `@Autowired(required = false)`, or `ObjectProvider<T>`.
5. **Proxies and DI:** Injected beans may be CGLIB/JDK proxies (e.g. `@Transactional`). Self-invocation inside the same class bypasses the proxy, so the advice does not run.

### 3.4 How the container does it (conceptual flow)

1. Scan/register `BeanDefinition`s (component scan, `@Configuration`/`@Bean`)
2. `BeanFactoryPostProcessor`s adjust definitions
3. Instantiate beans, resolving constructor args by type (then qualifier/name)
4. `AutowiredAnnotationBeanPostProcessor` injects field/setter dependencies
5. Initialization callbacks (`@PostConstruct`, `InitializingBean`), then `BeanPostProcessor`s (this is where AOP proxies get created)

---

## 4. Unit Testing Without Spring (proof DI works)

```java
class OrderServiceTest {
    @Test
    void placesOrderIntoRepository() {
        InMemoryOrderRepository fake = new InMemoryOrderRepository();
        OrderService service = new OrderService(fake);   // no Spring context needed

        service.place("Order-42");

        assertEquals(List.of("Order-42"), fake.store);
    }
}
```

Constructor injection plus an interface means the test runs in milliseconds with no container.

---

## 5. Self-Check Questions (answered)

**Q1. Is Spring DI the same as the Dependency Inversion Principle?**
No. DI is a technique for supplying dependencies; DIP is a principle about depending on abstractions. Spring facilitates DI; whether you honor DIP depends on whether your injected types are interfaces/abstractions.

**Q2. Can you have DI without DIP?**
Yes: injecting a concrete class through the constructor (Section 2.1).

**Q3. Can you have DIP without a DI framework?**
Yes: manual wiring in a composition root (Section 2.2).

**Q4. Why prefer constructor injection?**
Immutability, explicit mandatory dependencies, fail-fast startup, and framework-free testing.

**Q5. Why is the abstraction "inverted"?**
Normally high-level code depends on low-level code. With DIP, the interface is defined by the high-level module's needs and the low-level module implements it, so the source-code dependency points *toward* the abstraction from both sides.

**Q6. How does Spring resolve ambiguity between multiple beans of the same type?**
`@Primary`, `@Qualifier`, bean name matching against the parameter name, or collection injection.

---

## 6. Reflection

- **Before:** I treated "DI" and "DIP" as interchangeable because Spring's `@Autowired` made both feel like "interfaces + injection".
- **Now:** I can separate principle (DIP), style (IoC), and technique (DI), and I can show each with and without the others.
- **Applied next:** In my Spring services, I will review injected types to ensure they are interfaces owned by the consuming layer, prefer constructor injection, and treat constructors with many parameters as a refactoring signal.

---

## 7. References Studied
- Spring Framework Reference: Core Technologies, *Dependency Injection*
- Robert C. Martin, *Agile Software Development: Principles, Patterns, and Practices* (Dependency Inversion Principle)
- Martin Fowler, *Inversion of Control Containers and the Dependency Injection pattern*
