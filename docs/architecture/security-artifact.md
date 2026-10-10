# Level Up Project --- Security Implementation Artifact

## Day 9--10: Keycloak, JWT Authentication, and Downstream Service Protection

**Project:** Distributed Order Fulfillment Platform\
**Status:** Completed for application-level JWT security\
**Technology:** Java 21, Spring Boot 4, Spring Security, Spring Cloud
Gateway Server MVC, Keycloak, OAuth 2.0 / OpenID Connect, JWT, Kafka

------------------------------------------------------------------------

## 1. Objective

Secure the client-facing API Gateway and downstream HTTP services so
requests require a valid Keycloak-issued JWT. Apply role-based
authorization to order operations and prevent the Gateway from being the
only application-level security boundary.

## 2. Architecture

``` text
                         Keycloak
                  levelup-realm (OIDC)
                          |
                     Issues JWT
                          |
                          v
Client / Postman ---> API Gateway :8080
                       - Validates JWT
                       - Enforces order roles
                          |
             +------------+-------------+
             |                          |
             v                          v
      Order Service :8000       Internal Kafka workflows
      - Validates JWT             Order -> Inventory
      - Enforces roles             Inventory -> Payment
             |                     Payment -> Order
             v                     Compensation events
          orderdb

Inventory Service :8001 --> inventorydb
- Validates JWT for HTTP requests

Payment Service :8002 --> paymentdb
- Validates JWT for HTTP requests
```

Keycloak is the identity provider/authorization server. The Gateway and
downstream services act as OAuth 2.0 Resource Servers for HTTP requests.

## 3. Keycloak configuration

Created the following in Keycloak:

-   Realm: `levelup-realm`
-   Client: `levelup-gateway`
-   Test user: `levelup-user`
-   Reader test user: `levelup-reader`
-   Realm roles: `ORDER_WRITE` and `ORDER_READ`

Postman obtained an access token using the Authorization Code with PKCE
flow. The client redirect URI was configured for Postman's OAuth
callback:

``` text
https://oauth.pstmn.io/v1/callback
```

Issuer configured for the services:

``` properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/levelup-realm
```

Do not store real passwords, access tokens, client secrets, or other
credentials in this artifact or source control.

## 4. Gateway security

The `api-gateway` module includes Spring Boot's OAuth2 Resource Server
starter and a stateless Spring Security filter chain.

Security behavior:

-   Requests without a token are rejected with `401 Unauthorized`.
-   Invalid or expired tokens are rejected.
-   `POST /api/v1/orders` requires `ORDER_WRITE`.
-   `GET /api/v1/orders/**` requires `ORDER_READ`.
-   Other requests require authentication unless explicitly configured
    otherwise.

Keycloak's realm roles are exposed in the JWT under
`realm_access.roles`. A custom `KeycloakRoleConverter` maps each realm
role to a Spring Security authority with the `ROLE_` prefix. For
example, `ORDER_WRITE` becomes `ROLE_ORDER_WRITE`.

The role rules use `hasRole("ORDER_WRITE")` and `hasRole("ORDER_READ")`;
Spring Security adds the `ROLE_` prefix for these checks.

## 5. Downstream service security

The following services were configured as JWT Resource Servers:

-   Order Service --- port `8000`
-   Inventory Service --- port `8001`
-   Payment Service --- port `8002`

Each service uses the Keycloak issuer URI. Order Service additionally
maps Keycloak realm roles and enforces the order read/write permissions.

This means a client cannot bypass Order Service's authentication and
authorization simply by calling port `8000` directly without a token.

The Inventory and Payment configurations protect their HTTP requests.
Kafka consumers continue to process events without requiring HTTP Bearer
tokens; Kafka message security is a separate concern.

## 6. Authorization rules

  --------------------------------------------------------------------------------
  HTTP request                     Required permission     Expected behavior
  -------------------------------- ----------------------- -----------------------
  Any protected request without a  Authentication          `401 Unauthorized`
  token                                                    

  `POST /api/v1/orders` with       `ORDER_WRITE`           Allowed; verified as
  `ORDER_WRITE`                                            `201 Created`

  `POST /api/v1/orders` without    `ORDER_WRITE`           `403 Forbidden`
  `ORDER_WRITE`                                            

  `GET /api/v1/orders/{orderId}`   `ORDER_READ`            Allowed; verified
  with `ORDER_READ`                                        successfully

  Direct POST to Order Service     Authentication          `401 Unauthorized`
  port `8000` without a token                              

  Direct POST to Order Service     `ORDER_WRITE`           Allowed; verified as
  port `8000` with `ORDER_WRITE`                           `201 Created`

  Direct POST to Order Service     `ORDER_WRITE`           `403 Forbidden`
  port `8000` with a valid token                           
  but without `ORDER_WRITE`                                
  --------------------------------------------------------------------------------

## 7. End-to-end regression verification

Security changes were followed by end-to-end checks to ensure
asynchronous Saga processing still worked.

### Payment success

  Component   Expected/observed final state
  ----------- -------------------------------
  Order       `CONFIRMED`
  Inventory   `RESERVED`
  Payment     `SUCCESS`

### Payment failure and compensation

  Component   Expected/observed final state
  ----------- -------------------------------
  Order       `PAYMENT_FAILED`
  Inventory   `RELEASED`
  Payment     `FAILED`

Both scenarios were confirmed working after security configuration was
added to the services.

## 8. Security concepts demonstrated

-   **Authentication:** verifies the request's identity using a valid
    JWT.
-   **Authorization:** checks whether the authenticated principal has
    the required role.
-   **401 Unauthorized:** credentials are missing or invalid.
-   **403 Forbidden:** authentication succeeded, but permission is
    insufficient.
-   **OAuth 2.0 Resource Server:** validates bearer access tokens.
-   **JWT issuer validation:** trusts tokens from the configured
    Keycloak realm.
-   **Stateless security:** services do not rely on an HTTP server-side
    session.
-   **Defense in depth:** the Order Service independently validates
    tokens and roles instead of relying exclusively on the Gateway.
-   **Separation of concerns:** HTTP authentication and Kafka
    message/broker security are distinct.

## 9. Known limitation and next hardening step

Application-level JWT validation is implemented, but network-level
isolation is **not yet complete**. The local development services are
still configured on host ports, so they may be reachable directly from
the host; requests are protected by their application security
configuration.

For a production deployment, restrict direct external access to
downstream service ports using private networking,
firewall/security-group rules, or container orchestration network
policies. Also define service-to-service identity and Kafka
broker/client security separately. Do not assume that securing HTTP
endpoints automatically secures Kafka.

Automated security tests were intentionally skipped for this milestone,
as agreed. Manual Postman checks and end-to-end Saga regression
scenarios were used for verification.

## 10. Definition of Done

-   [x] Keycloak realm and gateway client configured.
-   [x] JWT access token obtained through Postman.
-   [x] API Gateway validates JWTs.
-   [x] Gateway enforces `ORDER_WRITE` for order creation.
-   [x] Gateway enforces `ORDER_READ` for order retrieval.
-   [x] Order Service independently validates JWTs and enforces order
    roles.
-   [x] Inventory Service configured for JWT validation on HTTP
    requests.
-   [x] Payment Service configured for JWT validation on HTTP requests.
-   [x] Direct Order Service requests verified with `401`, `201`, and
    `403` outcomes.
-   [x] Successful-payment Saga scenario verified.
-   [x] Failed-payment Saga and inventory-compensation scenario
    verified.
-   [ ] Network-level restrictions on downstream ports implemented.
-   [ ] Automated security tests added later if scope permits.

## 11. Suggested Git commit

``` bash
git add .
git commit -m "feat: secure gateway and downstream services with jwt"
git push
```

## 12. Interview / promotion summary

> I integrated Keycloak as the identity provider for the Distributed
> Order Fulfillment Platform. The API Gateway and downstream HTTP
> services validate Keycloak-issued JWT access tokens. I mapped Keycloak
> realm roles into Spring Security authorities and enforced
> `ORDER_WRITE` and `ORDER_READ` permissions on order APIs. I verified
> the `401` and `403` behavior, including direct requests to Order
> Service, and reran both Saga outcomes to confirm that security changes
> did not disrupt asynchronous payment processing or inventory
> compensation. Network-level isolation and automated security tests
> remain follow-up hardening items.
