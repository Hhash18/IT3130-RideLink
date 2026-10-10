# RideLink Fare & Payment Service

Member 4's independently executable Java Spring Boot backend. Implements fare estimation, final fare calculation, simulated payment recording, payment status/history, and JSON receipts. No frontend, real payment gateway, Account implementation or Ride Management implementation is included.

## Project structure

The service follows the team's layered package convention used by Account and Driver & Vehicle:

```text
fare-payment-service/
├── pom.xml
├── mvnw / mvnw.cmd
├── src/main/java/com/ridelink/farepayment/
│   ├── FarePaymentApplication.java
│   ├── config/        # SecurityConfig and OpenApiConfig
│   ├── controller/    # FareController and PaymentController
│   ├── dto/           # Separate request and response records
│   ├── entity/        # Fare, Payment and status/method enums
│   ├── exception/     # ServiceException and GlobalExceptionHandler
│   ├── repository/    # JPA repositories
│   ├── service/       # FareService, PaymentService and FareCalculator
│   ├── security/      # Record ownership checks
│   └── client/        # Ride service interface, REST and demo adapters
├── src/main/resources/
│   ├── application.properties
│   ├── application-demo.properties
│   └── db/migration/
├── src/test/java/com/ridelink/farepayment/
│   ├── client/
│   ├── controller/
│   ├── security/
│   └── service/
├── docs/
└── postman/
```

API routes, database tables and fare/payment rules are unchanged by the package restructuring.

## Requirements and quick start

Java 17+ is required. Use Maven 3.6.3+ or the included Maven wrapper (`./mvnw` on macOS/Linux, `mvnw.cmd` on Windows). The project uses Spring Boot 3.5.15, Spring Security, Spring Data JPA, Flyway, MySQL (H2 for tests/demo) and springdoc OpenAPI 2.8.16. Build from this service directory:

```sh
cd fare-payment-service
mvn clean verify
```

For a standalone demonstration, supply a password interactively, then enable the explicit `demo` profile:

```sh
read -r -s DEMO_PASSWORD
export DEMO_PASSWORD
mvn spring-boot:run -Dspring-boot.run.profiles=demo,h2
```

Type a password after the `read` command and press Enter; input is hidden. Alternatively set `DEMO_PASSWORD` in your IDE run configuration and select profiles `demo,h2`. The service deliberately has no committed demo password.

- API: `http://localhost:8084/api`
- Swagger: `http://localhost:8084/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8084/v3/api-docs`
- Demo HTTP Basic users: `passenger-001`, `passenger-002`, `admin-demo`. All use your `DEMO_PASSWORD`.
- Swagger **Authorize** accepts those credentials. Use `admin-demo` to finalize a fare, then `passenger-001` for the payment workflow.

Demo fixtures are an in-process test adapter, not another service:

| Ride ID | Passenger | State | Distance / time |
|---|---|---|---|
| `ride-demo-001` | `passenger-001` | COMPLETED | 10 km / 20 min |
| `ride-demo-002` | `passenger-002` | COMPLETED | 10 km / 20 min |
| `ride-demo-active` | `passenger-001` | IN_PROGRESS | 10 km / 20 min |

Normal execution uses MySQL database `ridelink_fare_payment_db`. Supply `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and the shared `JWT_SECRET`, or use the [root local setup](../docs/LOCAL-MYSQL-POSTMAN.md). Flyway selects `db/migration/mysql`; Hibernate validates the schema. The explicit `h2` profile keeps existing files under `./data` and selects the unchanged H2 migrations. Existing H2 data is preserved but is not automatically copied into MySQL. Unit/API tests use isolated in-memory databases.

## Fare rule

`LKR-STANDARD-v1`: **total = LKR 100.00 + (distanceKm × LKR 60.00) + (durationMinutes × LKR 5.00)**.

Each charge is rounded to 2 decimals using `HALF_UP`; totals use `BigDecimal`. Distance must be 0.001–1000 km with at most 3 fractional digits; duration must be an integer from 0–1440 minutes. No tax, surge, discounts or extra fees are applied. For 10 km and 20 minutes the fare is **LKR 800.00**. Rates are explicit fictional assignment assumptions, not real taxi prices.

Estimates accept simulated distance and time along with pickup/destination labels. Finalization accepts only the ride ID: the service fetches actual measurements and the passenger ID from Ride Management, requires `COMPLETED`, and stores an immutable snapshot of the trip, rule and price. An estimate is not a locked price. Existing finalized fares are never silently repriced.

## API

All business endpoints require authentication. Passengers can access only their own final fares, payment attempts and receipts. `ADMIN` and trusted `SERVICE` roles can access any ride. `DRIVER` has no access to this service.

| Method | Endpoint | Purpose | Role |
|---|---|---|---|
| POST | `/api/fares/estimate` | Return a fare estimate | PASSENGER / ADMIN / SERVICE |
| PUT | `/api/fares/rides/{rideId}` | Finalize using a completed Ride service record | ADMIN / SERVICE |
| GET | `/api/fares/rides/{rideId}` | Retrieve immutable final fare | Owner / ADMIN / SERVICE |
| POST | `/api/payments/rides/{rideId}` | Record simulated payment | Owner / ADMIN / SERVICE |
| GET | `/api/payments/rides/{rideId}` | UNPAID / FAILED / PAID and all attempts | Owner / ADMIN / SERVICE |
| GET | `/api/payments/{paymentId}` | Retrieve one payment attempt | Owner / ADMIN / SERVICE |
| GET | `/api/payments/{paymentId}/receipt` | Retrieve successful payment's JSON receipt | Owner / ADMIN / SERVICE |

Estimate body:

```json
{"pickup":"Colombo Fort","destination":"Bambalapitiya","distanceKm":10,"durationMinutes":20}
```

Payment body:

```json
{"method":"MOCK_CARD","simulation":"SUCCESS"}
```

Payment requires an `Idempotency-Key` header containing a UUID. Methods are `CASH` and `MOCK_CARD`; outcomes are `SUCCESS` or `DECLINED`. Both methods are simulations; no card numbers or bank information are accepted. There is deliberately no client-supplied amount or passenger ID.

Successful requests return HTTP 200. A simulated decline is a successfully recorded attempt with `status: FAILED` and `failureReason: SIMULATED_DECLINE` (also HTTP 200). Replaying a key with the same body returns the same attempt, including a failed attempt. Reusing a key with a changed body returns 409. To retry a decline, use a new key. Once one attempt succeeds, any new payment attempt returns 409. Keys are scoped to a ride. A database row lock serializes payments for that ride, and a unique constraint protects request identity. A receipt number is assigned and stored in the same transaction as a successful payment. No receipt is issued for a decline.

Validation returns 400; missing/invalid credentials 401; denied access 403; missing records 404; lifecycle/payment conflicts 409; invalid Ride data 502; Ride connection/timeout/server failures 503. Concurrent first-time fare finalizations may return 409; retry the same PUT. Errors have a consistent structure:

```json
{"timestamp":"2026-09-28T12:00:00Z","status":409,"code":"RIDE_NOT_COMPLETED","message":"Only a completed ride can have a final fare","path":"/api/fares/rides/ride-demo-active","details":[]}
```

## Postman demonstration

Import `postman/Fare-Payment-Demo.postman_collection.json` and `postman/Fare-Payment-Demo.postman_environment.json`. Set the environment's `demoPassword` locally to your chosen password. Select that environment, start with a fresh demo database, and run the collection in order. It generates request keys and stores payment IDs automatically. It demonstrates estimates, final fares, unpaid status, a decline, successful retry, idempotent replay, receipts, access denial, invalid input, incomplete rides and duplicate-payment rejection. Do not export populated passwords or tokens.

The matching **Integration** collection/environment uses bearer tokens. Set actual completed/active ride IDs, `passengerToken`, `otherPassengerToken` and `adminToken`, and use a fresh completed unpaid ride. Its three roles and ride ownership must match the contracts below. This collection is ready for integration; no claim is made that your teammates' services have been tested.

## Connecting the team services

Normal mode now verifies the **current Account Service's HMAC JWT format** (singular `role`, account ID in `sub`, `email`, `iat`, `exp`). No JWKS endpoint is required. Supply exactly the same raw UTF-8 `JWT_SECRET` used by Account and Ride; do not generate a different secret just for Fare.

```sh
read -r -s JWT_SECRET
export JWT_SECRET
export RIDE_SERVICE_URL='http://localhost:8083'
mvn spring-boot:run
```

After `read`, enter the existing team secret and press Enter. Normal mode fails fast if it is blank or shorter than 32 UTF-8 bytes. Never put its real value in properties, Postman exports, command examples or Git. Demo mode still uses only `DEMO_PASSWORD` and requires no JWT secret.

All services now match Account: 32–47 UTF-8 bytes → HS256; 48–63 → HS384; 64+ → HS512. Use the same raw `JWT_SECRET` in each service. Tokens contain singular `role`, subject and expiry. Do not rotate the secret for one service independently.

Fare checks the signature, expiry/standard timestamp rules, nonblank subject (max 100 characters), and operation/ownership permissions. Account currently does not issue `iss` or `aud`, so those claims are not required. The subject must match the passenger ID stored by Ride. `role` is a string such as `PASSENGER`, `DRIVER` or `ADMIN`; the current Account service does not issue SERVICE roles. Use an Account-issued **ADMIN token** for final fare creation. SERVICE support remains available for a future explicitly trusted service identity; it is not required for the current demo or group integration.

Finalization forwards that ADMIN token to Ride Management's `GET /api/rides/{id}`. Ride must use the same secret and Account mode. See [contracts and team checks](docs/INTEGRATION.md).

| Configuration | Default / use |
|---|---|
| `PORT` | Fare runs on 8084 |
| DB_URL | `jdbc:mysql://localhost:3306/ridelink_fare_payment_db?connectionTimeZone=UTC` |
| DB_USERNAME / DB_PASSWORD | `ridelink_fare_payment` / required externally supplied password |
| `JWT_SECRET` | Required only in normal mode; must match Account/Ride |
| `RIDE_SERVICE_URL` | `http://localhost:8083` |
| `DEMO_PASSWORD` | Required only for demo; no default |

The inspected Account branch runs on **8082**, not 8081. Fare does not call Account for token verification; it verifies signatures locally. Driver's port must be confirmed by its owner because its application properties are not tracked. Set Ride's `DRIVER_SERVICE_URL` to the actual Driver address, not Account's 8082 port. Each service must use a distinct port when running on one machine.

Start Account and Ride Management for the live workflow (Ride also needs Driver/Fare for its own workflows). All services can start independently with their required configuration; calls still require the corresponding dependency to be running. When running on different machines, `localhost` refers to each machine itself—use the correct reachable service address.

This service owns its MySQL database. Other services communicate through REST and never read its tables.

## Tests and CI

`mvn verify` runs calculation/service unit tests, real REST-client tests against a local HTTP server, database/API tests, concurrent-payment tests and Account-compatible signed-JWT security tests. Tests use isolated in-memory databases and generated test signing secrets. Test results are under `target/surefire-reports/`. Tests generate tokens with the same JJWT 0.12.6 library as Account, including HMAC algorithm boundaries, ADMIN token forwarding to a local Ride HTTP stub, and a passenger payment/receipt workflow. The build produces `target/fare-payment-service-1.0.0.jar`.

The root `.github/workflows/fare-payment-ci.yml` builds and tests this service on push/PR and uploads test reports. It is scoped to this service; the other members need their own jobs or an agreed build matrix. No remote CI run, Git contribution history or group-wide integration is fabricated here.

See [design and diagrams](docs/DESIGN.md) and [integration contract](docs/INTEGRATION.md). Assign your real name/student ID as owner in the group report and use your own branch, commits and peer-reviewed PR. This implementation was created with Codex assistance; review and understand it and record permitted tool use according to your module policy. The team's full report and other services remain outside this component's scope.
