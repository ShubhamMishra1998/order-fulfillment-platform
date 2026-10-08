                         ┌─────────────────┐
                         │   API Gateway   │
                         │     :8080       │
                         └────────┬────────┘
                                  │
                  ┌───────────────┼───────────────┐
                  ↓               ↓               ↓
           Order Service   Inventory Service  Payment Service
              :8000             :8001             :8002
                  │               │               │
                  ↓               ↓               ↓
              Order DB       Inventory DB      Payment DB
                  │               │               │
                  └──────────── Kafka ────────────┘
                                  │
                              Saga Flow



“I introduced an API Gateway as the single client-facing entry point. I configured static routing to the Order Service and kept business
logic and Saga orchestration inside the services. Internal service-to-service communication continues to use Kafka for asynchronous workflows.”