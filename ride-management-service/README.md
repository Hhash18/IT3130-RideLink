# RideLink Ride Management Service

Member 3 backend, Java Spring Boot, port **8083**. Includes ride requests, pickup/destination and simulated metrics, deterministic driver assignment, acceptance/start/completion/cancellation, authorized ride retrieval, persistence and REST integrations. No frontend, real maps or real payments.

**Start with the standalone demo. Live group integration needs JWT alignment: the current Account branch issues HMAC tokens with `role`, while the existing Fare service expects RSA/JWKS tokens with `roles`, issuer and audience. See [integration notes](docs/INTEGRATION.md).**

## Prerequisites and run

Java 21+ and Maven 3.6.3+ (or use the included `./mvnw` / `mvnw.cmd`). Spring Boot 3.5.15 and springdoc 2.8.16 are pinned in `pom.xml`.

From this folder:

```sh
mvn clean verify
read -r -s DEMO_PASSWORD
export DEMO_PASSWORD
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

After `read`, type your chosen password and press Enter; it is hidden. On Windows or in IntelliJ, set `DEMO_PASSWORD` in the run configuration and select active profile `demo` instead. Do not commit the chosen password.

- Swagger UI: `http://localhost:8083/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8083/v3/api-docs`
- REST base: `http://localhost:8083/api/rides`
- Executable artifact after build: `target/ride-management-service-1.0.0.jar`

Demo users all use the externally supplied password:

| Username | Role |
|---|---|
| `passenger-001` | PASSENGER |
| `passenger-002` | PASSENGER (ownership failure scenario) |
| `driver@example.test` | DRIVER (assigned demo driver) |
| `other-driver@example.test` | DRIVER (permission failure scenario) |
| `admin-demo` | ADMIN |

Demo mode provides one available driver: ID `1`, vehicle `101`, service area `Colombo`, email `driver@example.test`. An unmatched area produces `NO_AVAILABLE_DRIVER`. The demo fare adapter uses `100 + distanceKm*60 + minutes*5`, in LKR, only to exercise the ride workflow. It does not create real records in the Fare service. Real payment and receipt APIs remain entirely in Member 4's service.

The default embedded H2 database persists in `./data/ride-management.mv.db`. Keep the working directory at the service root. Flyway creates schema tables and Hibernate validates them. To use a disposable database, set `DB_URL='jdbc:h2:mem:ride-demo;DB_CLOSE_DELAY=-1'` before startup. Test databases are isolated in memory; tests do not touch demo data.

## Team-style structure

```text
ride-management-service/
  pom.xml
  mvnw / mvnw.cmd
  src/main/java/com/ridelink/ride/
    RideManagementApplication.java
    config/        SecurityConfig, OpenApiConfig
    controller/    RideController
    dto/           Separate request/response records
    entity/        Ride, RideStatus, DispatchLock
    exception/     ServiceException, GlobalExceptionHandler
    repository/    RideRepository, DispatchLockRepository
    service/       RideService, RideStatePolicy, DriverSelector,
                   FareValidator, FareIntegrationService
    security/      AccessControl
    client/        Driver/Fare interfaces, REST and demo adapters
  src/main/resources/
    application.properties
    application-demo.properties
    db/migration/V1__rides.sql
  src/test/
  postman/
  docs/
```

## Lifecycle and API

```text
REQUESTED -> ASSIGNED -> ACCEPTED -> IN_PROGRESS -> COMPLETED
    |           |           |
    +-----------+-----------+----> CANCELLED
```

Only the displayed transitions are allowed. Completion metrics are immutable after completion. A repeated state transition returns 409; retrieve the ride to confirm its current state. Final-fare requests are safely repeatable. There is no delete endpoint that would erase trip history.

| Method | Endpoint | Who / result |
|---|---|---|
| POST | `/api/rides` | PASSENGER; 201 + Location; creates REQUESTED with Fare estimate |
| GET | `/api/rides/{id}` | Owner, assigned driver, ADMIN or SERVICE; 200 |
| GET | `/api/rides?page=0&size=20` | Paginated authorized rides; size 1–100 |
| PUT | `/api/rides/{id}/assignment` | Owner passenger, ADMIN or SERVICE; REQUESTED → ASSIGNED |
| PUT | `/api/rides/{id}/acceptance` | Assigned DRIVER or ADMIN; ASSIGNED → ACCEPTED |
| PUT | `/api/rides/{id}/start` | Assigned DRIVER or ADMIN; ACCEPTED → IN_PROGRESS |
| PUT | `/api/rides/{id}/completion` | Assigned DRIVER or ADMIN; IN_PROGRESS → COMPLETED |
| PUT | `/api/rides/{id}/cancellation` | Owner passenger or ADMIN; only before start |
| PUT | `/api/rides/{id}/fare` | ADMIN or SERVICE; ask Fare service for final fare after completion |

ADMIN/SERVICE can read all rides; passengers see their own; drivers see assigned rides. Passenger ID is taken from the authenticated subject, never trusted from the request body. Account driver IDs and operational driver IDs belong to different services; the current Driver API lacks `accountId`, so driver authorization matches the signed JWT `email` claim against the assigned operational profile's email. The team must keep these emails verified, unique and consistent. A stable `accountId` field in Driver Service is the recommended future contract improvement.

Create body:

```json
{"pickup":"Colombo Fort","destination":"Bambalapitiya","serviceArea":"Colombo","estimatedDistanceKm":10,"estimatedDurationMinutes":20}
```

Completion body:

```json
{"actualDistanceKm":12,"actualDurationMinutes":25}
```

Cancellation body:

```json
{"reason":"Plans changed"}
```

Distance: 0.001–1000 km, max 3 decimals. Duration: integer 0–1440 minutes. Pickup/destination: nonblank, max 200 characters. Service area: nonblank, max 100. Cancellation reason: nonblank, max 300. Unknown fields, fractional minutes and malformed UUIDs are rejected. Creation is a POST that creates a new ride on every successful call; avoid blind retries after an ambiguous network failure—check the passenger's list first.

## Assignment rule and database protection

Retrieve `GET /api/drivers/available`, keep profiles with `AVAILABLE` status, a vehicle ID, an email and a matching service area (case-insensitive, trimmed), then choose the **lowest driver ID** not already reserved by an active ride in this service. Selection is deterministic and uses no maps.

The singleton `dispatch_lock` table serializes assignment decisions within this database. `active_driver_id` and `active_vehicle_id` are unique nullable columns on rides, preventing two active rides from reserving the same driver or vehicle. Completion/cancellation clears those reservation columns but preserves historical `driver_id` and `vehicle_id`. Row locks protect lifecycle updates against concurrent transitions.

Reservations are owned by Ride Management. This implementation does **not** PATCH Driver Service's availability: its current API has no atomic reservation/release contract, so a GET/PATCH pair would not safely coordinate concurrent dispatchers. Driver Service supplies declared availability; Ride Management excludes its own active reservations. This is safe for a single shared Ride database, not a distributed reservation system spanning unrelated dispatchers. Service/vehicle availability changes after assignment require team-level operational handling; assigned trip snapshots stay stable.

## Final fare and recovery

Completion commits actual metrics and the COMPLETED state first. An ADMIN/SERVICE caller then invokes `/api/rides/{id}/fare`. This calls Member 4's `PUT /api/fares/rides/{id}` outside the Ride database transaction; Fare can call `GET /api/rides/{id}` back and see the committed trip. A successful result stores `finalFareId` and `finalFare` on the ride. If Fare is unavailable, the ride remains completed with no final fare; retry the fare endpoint later. If the remote fare was created but recording the result locally failed, Member 4's idempotent PUT returns the same fare on retry.

Only Fare & Payment calculates the real final fare and records payment. Ride Management stores a reference/snapshot; it does not access Member 4's database. A completed ride is not reopened if payment is declined.

## Errors

All error responses use `timestamp`, `status`, `code`, `message`, `path`, `details`.

| Status | Example |
|---|---|
| 400 | Invalid body, UUID or pagination |
| 401 | Missing/invalid authentication |
| 403 | Wrong owner, assigned driver or role |
| 404 | Ride does not exist |
| 409 | INVALID_TRANSITION, NO_AVAILABLE_DRIVER, database conflict |
| 502 | Invalid dependency data or DEPENDENCY_AUTH_FAILED |
| 503 | Driver/Fare connection, timeout or HTTP server failure |

Dependencies use 2-second connect and 3-second read timeouts, with no automatic retries or silent demo fallback. An estimate dependency failure leaves no new ride; assignment failure leaves the ride REQUESTED. No upstream response body or credentials are exposed in errors.

## Postman

Import the Demo collection and Demo environment under `postman/`, set `demoPassword` locally and run in order. IDs are captured automatically. It covers the full lifecycle, final fare, cancellation, invalid transitions, missing auth, wrong passenger/driver and no available driver. Completed/cancelled trips release the demo driver, so a completed run can be repeated without deleting history. If you interrupt a run while a driver is reserved, finish or cancel that ride first, or use a fresh disposable database.

The Integration collection uses bearer tokens and the same endpoints. Configure real passenger/driver/admin tokens, a service area with an eligible driver and an area with no eligible drivers. It requires the identity and Fare contracts in [INTEGRATION.md](docs/INTEGRATION.md) to be aligned first.

## Configuration and live mode

| Variable | Default / purpose |
|---|---|
| PORT | 8083 |
| DB_URL | Embedded H2 file |
| DB_USERNAME / DB_PASSWORD | Embedded H2 `sa` / empty; override externally |
| DRIVER_SERVICE_URL | `http://localhost:8082` |
| FARE_SERVICE_URL | `http://localhost:8084` |
| DEMO_PASSWORD | Required for demo only |
| JWT_MODE | `account` or `jwks`; default `account` |
| JWT_SECRET | Account mode: same raw UTF-8 secret as Account, minimum 32 bytes |
| JWT_JWK_SET_URI | JWKS mode: public signing key endpoint |
| JWT_ISSUER | JWKS mode: required issuer |
| JWT_AUDIENCE | JWKS mode: default `ridelink` |

Without demo, `account` mode verifies the Account branch's HMAC JWT format and singular `role` claim. JJWT chooses HS256/384/512 based on secret length; this decoder follows the same 32/48/64-byte thresholds. `jwks` mode verifies RS256 public keys, issuer, expiry, audience and plural `roles` for the contract used by Fare. Neither mode creates accounts or issues tokens. All modes enforce role and record-level permissions.

## Build, tests, CI and handoff

Run `mvn clean verify` (or `./mvnw clean verify`). Tests cover state policy, driver selection, validation, endpoint security/ownership, assignment concurrency, REST contracts/failures, signed Account JWTs, completion and fare retries. See `docs/TEST-RESULTS.md` for the verified handoff build. Test reports are generated under `target/surefire-reports`.

The handoff root contains a GitHub Actions workflow. Copy it to the team's repository root `.github/workflows/` along with this service. It builds this service on Java 21; it does not deploy it or build teammates' services. No Git branch or remote CI run has been created as part of this handoff.

See `../START-HERE.md` for the recipient's own branch/push steps. AI assistance was used to generate this implementation and documentation. The recipient should review, understand, test and disclose permitted assistance under the institute's policy. Group integration, contribution history and peer review must be completed honestly in the shared repository.
