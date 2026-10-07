                  OrderCreated
                       ↓
                 processPayment()
                       │
              ┌────────┴────────┐
              ↓                 ↓
        CUST-FAIL           Other customer
              ↓                 ↓
        Payment FAILED     Payment SUCCESS
              ↓                 ↓
        PaymentFailed      PaymentCompleted
              ↓                 ↓
              └───────┬─────────┘
                      ↓
                  OutboxEvent
                      ↓
                    COMMIT
                      ↓
             PaymentOutboxPublisher
                      ↓
               payment-events
