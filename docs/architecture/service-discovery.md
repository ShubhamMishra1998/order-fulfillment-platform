# Service Discovery

Services register their available instances with a service registry.

## Client-side discovery
The calling service queries the registry and selects an instance, usually with client-side load balancing.

`Caller -> Registry -> Caller selects instance -> Target`

## Server-side discovery
An intermediary such as an API Gateway or load balancer performs discovery and routing.

`Caller -> Gateway -> Discovery -> Target`

## Project decision
External clients use the API Gateway as the single entry point. Internal service locations are hidden from clients.
