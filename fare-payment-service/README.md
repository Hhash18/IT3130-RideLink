# RideLink Fare & Payment Service

Member 4's independently executable Java Spring Boot backend. Implements fare estimation, final fare calculation, simulated payment recording, payment status/history, and JSON receipts. No frontend, real payment gateway, Account implementation or Ride Management implementation is included.

## Requirements and quick start

Java 17+ is required. Use Maven 3.6.3+ or the included Maven wrapper (`./mvnw` on macOS/Linux, `mvnw.cmd` on Windows). The project uses Spring Boot 3.5.15, Spring Security, Spring Data JPA, Flyway, H2 and springdoc OpenAPI 2.8.16. Build from this service directory:

```sh
cd fare-payment-service
mvn clean verify
```

For a standalone demonstration, supply a password interactively, then enable the explicit `demo` profile:

```sh
read -r -s DEMO_PASSWORD
export DEMO_PASSWORD
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Type a password after the `read` command and press Enter; input is hidden. Alternatively set `DEMO_PASSWORD` in your IDE run configuration and select profile `demo`. The service deliberately has no committed demo password.

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

By default the service persists data in `./data/fare-payment.mv.db` relative to its working directory; restarting preserves payments and receipts. Flyway applies schema migrations, and Hibernate validates the schema. To run an isolated, repeatable demo without clearing a persistent database, set `DB_URL='jdbc:h2:mem:fare-demo;DB_CLOSE_DELAY=-1'` before starting. Restart that process for a fresh Postman run. The H2 console is disabled.

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

The matching **Integration** collection/environment uses bearer tokens. Set actual completed/active ride IDs, `passengerToken`, `otherPassengerToken` and `serviceToken`, and use a fresh completed unpaid ride. Its three roles and ride ownership must match the contracts below. This collection is ready for integration; no claim is made that your teammates' services have been tested.

## Connecting the team services

Run without `demo` to enable RSA-signed Account JWT verification and the real REST Ride client:

```sh
export JWT_ISSUER='http://localhost:8081'
export JWT_JWK_SET_URI='http://localhost:8081/.well-known/jwks.json'
export JWT_AUDIENCE='ridelink'
export RIDE_SERVICE_URL='http://localhost:8083'
mvn spring-boot:run
```

Start Account and Ride Management first for the integrated workflow. Fare & Payment can start independently; finalization requires Ride availability, and token verification requires accessible Account public keys on first use. The service owns only its own H2 database and never reads another service's tables.

JWTs must have a valid RS256 signature, matching `iss`, an `aud` containing `ridelink`, a nonblank `sub` (the stable account ID), an unexpired `exp`, and `roles` such as `["PASSENGER"]`, `["ADMIN"]` or `["SERVICE"]`. Standard timestamp validation includes Spring Security's clock-skew tolerance. Fare finalization forwards the caller's JWT to Ride Management; that service must accept the shared audience and allow the trusted SERVICE/ADMIN role to read the trip. Coordinate token claims and route names with Members 1 and 3; see [contracts](docs/INTEGRATION.md).

| Variable | Default / use |
|---|---|
| `PORT` | 8084 |
| `DB_URL` | Local H2 file under `./data` |
| `DB_USERNAME` / `DB_PASSWORD` | H2 embedded `sa` / empty; override for your environment |
| `JWT_ISSUER` | `http://localhost:8081` |
| `JWT_JWK_SET_URI` | `http://localhost:8081/.well-known/jwks.json` |
| `JWT_AUDIENCE` | `ridelink` |
| `RIDE_SERVICE_URL` | `http://localhost:8083` |
| `DEMO_PASSWORD` | Required only for demo; no default |

This project ships an H2 driver/schema. A different database requires adding its driver/Flyway module and verifying migrations. Do not point it at another service's database. The demo profile is for local demonstrations only; use JWT authentication for integration.

## Tests and CI

`mvn verify` runs calculation/service unit tests, real REST-client tests against a local HTTP server, database/API tests, concurrent-payment tests and signed-JWT security tests. Tests use isolated in-memory databases and generated test signing keys. Test results are under `target/surefire-reports/`. The build produces `target/fare-payment-service-1.0.0.jar`.

The root `.github/workflows/fare-payment-ci.yml` builds and tests this service on push/PR and uploads test reports. It is scoped to this service; the other members need their own jobs or an agreed build matrix. No remote CI run, Git contribution history or group-wide integration is fabricated here.

See [design and diagrams](docs/DESIGN.md) and [integration contract](docs/INTEGRATION.md). Assign your real name/student ID as owner in the group report and use your own branch, commits and peer-reviewed PR. This implementation was created with Codex assistance; review and understand it and record permitted tool use according to your module policy. The team's full report and other services remain outside this component's scope.
