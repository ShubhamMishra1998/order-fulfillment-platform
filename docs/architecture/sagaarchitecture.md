                    OrderCreated
                         │
                         ▼
                  ┌─────────────┐
                  │  Inventory  │
                  │   RESERVED  │
                  └──────┬──────┘
                         │
                 InventoryReserved
                         │
                         ▼
                  ┌─────────────┐
                  │   Payment   │
                  └──────┬──────┘
                         │
                ┌────────┴────────┐
                │                 │
             SUCCESS            FAILED
                │                 │
                ▼                 ▼
        PaymentCompleted     PaymentFailed
                │                 │
                ▼                 ▼
          Order CONFIRMED   Order PAYMENT_FAILED
                                  │
                         InventoryReleaseRequested
                                  │
                                  ▼
                           Inventory RELEASED