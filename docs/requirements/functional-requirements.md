# Functional Requirements

## FR-01 — Customer authentication

The system shall require an authenticated customer to create and manage orders.

## FR-02 — Create order

A customer shall be able to create an order containing one or more products and quantities.

## FR-03 — Validate inventory

The system shall verify that sufficient inventory is available for the requested products.

## FR-04 — Reserve inventory

The system shall reserve inventory as part of order processing.

## FR-05 — Process payment

The system shall initiate payment for an order after inventory reservation succeeds.

## FR-06 — Confirm order

An order shall become `CONFIRMED` only after the required downstream steps succeed.

## FR-07 — Handle failure

If a downstream Saga step fails, the system shall execute the appropriate compensating action and move the order to a valid terminal state such as `CANCELLED`.

## FR-08 — Notifications

The system shall publish/send an order status notification after important state transitions.

## FR-09 — Query order

A customer shall be able to retrieve an order and its current status.

## FR-10 — Cancel order

An authorized user shall be able to cancel an order when its current state permits cancellation.

## FR-11 — Idempotent processing

Duplicate delivery of an event shall not cause the same business operation to be executed multiple times.

## FR-12 — Auditability

Important order state transitions shall be traceable through logs/events.
