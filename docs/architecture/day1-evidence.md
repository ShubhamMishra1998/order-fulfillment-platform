# Day 1 Evidence

## Development-plan mapping

| Development-plan area | Day 1 evidence |
|---|---|
| Microservice Architecture Style | Service boundary document + system context + ADR-001 |
| Software Design | Explicit responsibilities and ownership boundaries |
| Software Engineering Knowledge & Experience | Functional/non-functional requirements and architectural trade-offs |
| Software Engineering Practices | Documented architecture decision |
| Software Engineering Processes | Requirements captured before implementation |

## Day 1 learning outcome

By the end of Day 1, I can explain:

1. Why the project is split into these services.
2. What each service owns.
3. Why databases are owned by individual services.
4. Which communication is synchronous and which is asynchronous.
5. Why distributed transactions will require a Saga.
6. What trade-offs the chosen architecture introduces.

## Evidence to show the Tech Lead

- `docs/requirements/functional-requirements.md`
- `docs/requirements/non-functional-requirements.md`
- `docs/architecture/service-boundaries.md`
- `docs/architecture/system-context.md`
- `docs/adr/ADR-001-microservice-boundaries.md`
