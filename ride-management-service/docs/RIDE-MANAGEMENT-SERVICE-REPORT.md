# Ride Management Service

## 1. Introduction

The Ride Management Service is one of the backend microservices developed for the RideLink system. Its responsibility is to manage ride requests and the ride lifecycle, from passenger request and driver assignment through completion or cancellation.

The service was developed with Java and Spring Boot as an independently deployable backend. It provides REST APIs, stores its ride data in its own database, and coordinates with the Driver & Vehicle and Fare & Payment services through HTTP APIs. Swagger UI and Postman can be used to demonstrate the service without a separate frontend.

## 2. Responsibilities

The Ride Management Service:

- Creates ride requests and stores pickup, destination, service area, estimated trip metrics, and fare estimate.
- Assigns an eligible available driver and vehicle to a requested ride.
- Manages ride acceptance, start, completion, and cancellation.
- Stores actual trip metrics supplied when an authorized driver completes a ride.
- Returns rides according to passenger ownership, driver assignment, or privileged access.
- Requests and stores a final-fare reference from the Fare & Payment Service after ride completion.
- Validates request and dependency data, enforces lifecycle rules, and returns consistent API errors.
- Maintains its own ride records and active driver/vehicle reservations.

Payment processing, receipts, driver and vehicle profile management, maps, and live driver tracking belong to other services or are outside the current implementation.

## 3. Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Backend programming language |
| Spring Boot 3.5.15 | Microservice framework and application runtime |
| Spring Web | REST API controllers and HTTP clients |
| Spring Data JPA / Hibernate | Entity mapping and database access |
| MySQL | Database for the integrated multi-service setup |
| H2 | Embedded database for the standalone demonstration and component tests |
| Flyway | Versioned database schema migrations |
| Bean Validation | Request validation |
| Spring Security OAuth2 Resource Server | JWT verification and role-based access |
| springdoc OpenAPI / Swagger UI | Interactive API documentation and testing |
| Maven | Dependency management and build |
| Postman | API workflow demonstration and manual testing |
| Git and GitHub | Version control and collaboration |

## 4. Microservice Architecture

The service follows a layered architecture:

**Controller → Service → Repository → Ride Management database**

### Controller layer

`RideController` exposes the HTTP endpoints under `/api/rides`. It validates and routes requests, applies role-level restrictions, and returns API responses.

### Service layer

`RideService` implements ride creation, authorization checks, driver selection and reservation, lifecycle transitions, and ride retrieval. `FareIntegrationService` coordinates final-fare requests.

### Repository layer

`RideRepository` handles ride persistence, pagination, and row-locked lifecycle updates. `DispatchLockRepository` serializes assignment decisions in the Ride database.

### Database layer

Ride Management owns its `rides` and `dispatch_lock` tables. Flyway creates the schema and Hibernate validates it at startup. Other microservices do not directly access this database; the service stores references and snapshots such as driver ID, vehicle ID, and final-fare ID rather than cross-service database relationships.

## 5. Ride Request and Fare Estimate

A passenger creates a ride with a pickup, destination, service area, estimated distance, and estimated duration. The service requests an estimate from Fare & Payment, validates the returned amount and currency, and then persists the ride in the `REQUESTED` state.

The passenger identity is taken from the authenticated token rather than a client-supplied passenger ID. If the estimate dependency fails or returns invalid data, no new ride is saved.

## 6. Driver Assignment

When assignment is requested, Ride Management asks Driver & Vehicle for available drivers. It filters for an `AVAILABLE` driver with a vehicle, an email, and a service area matching the ride's area. It chooses the lowest eligible driver ID that is not already reserved by an active ride in the Ride database.

A database dispatch lock serializes assignment choices. Unique active-driver and active-vehicle reservation columns protect against duplicate active reservations. On completion or cancellation, the active reservation is released while the historical driver and vehicle IDs remain on the ride.

The current service does not modify Driver & Vehicle availability. Its API does not provide an atomic reservation operation, so Ride Management owns reservation decisions within its own shared database.

## 7. Ride Lifecycle

The implemented ride states and transitions are:

```text
REQUESTED → ASSIGNED → ACCEPTED → IN_PROGRESS → COMPLETED
     └──────────────┴───────────┴──────────────→ CANCELLED
```c

Cancellation is allowed only before the ride starts. Invalid or repeated transitions return a conflict response. Completion records actual distance and duration, commits the completed ride, and releases the active driver and vehicle reservation. Completion metrics cannot be overwritten through a repeated completion request. Ride history is retained; there is no delete endpoint.

## 8. REST API Design

The REST base path is `/api/rides`.

| HTTP method | Endpoint | Description / access |
|---|---|---|
| `POST` | `/api/rides` | Passenger creates a ride and receives `201 Created` with a Location header. |
| `GET` | `/api/rides/{id}` | Retrieves a ride for its passenger, assigned driver, ADMIN, or SERVICE. |
| `GET` | `/api/rides?page=0&size=20` | Retrieves a paginated list scoped to the caller; ADMIN/SERVICE can view all rides. |
| `PUT` | `/api/rides/{id}/assignment` | Passenger owner, ADMIN, or SERVICE assigns an eligible driver. |
| `PUT` | `/api/rides/{id}/acceptance` | Assigned driver or ADMIN accepts the ride. |
| `PUT` | `/api/rides/{id}/start` | Assigned driver or ADMIN starts an accepted ride. |
| `PUT` | `/api/rides/{id}/completion` | Assigned driver or ADMIN completes an in-progress ride with actual trip metrics. |
| `PUT` | `/api/rides/{id}/cancellation` | Passenger owner or ADMIN cancels a ride before it starts. |
| `PUT` | `/api/rides/{id}/fare` | ADMIN or SERVICE requests and records the final fare after completion. |

The service therefore exposes **9 endpoint mappings**: eight ride and lifecycle operations plus paginated listing (the `GET /api/rides` collection route is counted separately from `GET /api/rides/{id}`). The APIs use `POST` for ride creation, `GET` for retrieval, and `PUT` for lifecycle actions and final-fare synchronization.

## 9. Data Validation and Error Handling

Bean Validation checks required fields and input bounds, including pickup and destination lengths, service-area length, distance and duration ranges, and cancellation reason length. Unknown JSON properties, fractional values for integer duration fields, and malformed UUIDs are rejected.

The global exception handler returns a consistent error shape containing `timestamp`, `status`, `code`, `message`, `path`, and `details`. Responses distinguish invalid input (`400`), missing or invalid authentication (`401`), access denial (`403`), missing rides (`404`), invalid transitions or assignment conflicts (`409`), invalid dependency responses (`502`), and unavailable dependencies (`503`).

## 10. DTOs, Enums, and Persistence Model

Separate request and response DTOs keep the external API contract distinct from persistence entities. DTOs cover ride creation, completion, cancellation, pagination, fare estimates, fare results, driver data, and API errors.

`RideStatus` defines the controlled values `REQUESTED`, `ASSIGNED`, `ACCEPTED`, `IN_PROGRESS`, `COMPLETED`, and `CANCELLED`. UUIDs identify rides, `BigDecimal` stores fare and distance values, and `Instant` stores timestamps. Flyway migrations define the schema; JPA maps the `Ride` and `DispatchLock` entities to the service-owned tables.

## 11. Security

The service verifies authenticated identities and enforces both role and record-level access. Passengers can access their own rides, drivers can access rides assigned to their verified email, and ADMIN/SERVICE roles have the documented privileged access. Driver assignment records the operational driver email and ID returned by Driver & Vehicle; the service does not assume that the operational driver ID equals the Account ID.

The application supports a standalone demo profile and integrated authentication. Demo mode uses local fixtures and HTTP Basic authentication. In the integrated setup, Account, Ride, Driver & Vehicle, and Fare & Payment use the shared HMAC JWT format with a singular `role` claim and shared `JWT_SECRET`. Ride Management does not issue tokens or create accounts.

## 12. Inter-Service Communication and Final Fare

Ride Management uses REST clients behind `DriverClient` and `FareClient` interfaces. Demo adapters allow standalone demonstration; REST adapters call configured Driver and Fare service URLs. The clients use bounded connection and read timeouts and do not silently switch to demo data when a live dependency fails.

Final fare is a separate operation after completion. Ride Management first commits the completed ride, then calls Fare & Payment. Fare can call back to retrieve the committed ride. On success, Ride Management stores the final-fare ID and amount. If the fare service is unavailable, the completed ride remains completed and the fare request can be retried. Fare & Payment owns real fare calculation, payment, and receipt records; Ride Management stores only the returned fare reference and snapshot.

The merged team project aligns the HMAC JWT contract across all four services. The shared local verification record reports a successful MySQL-backed, four-service integration run using Account-issued tokens and Newman. The exact checks and their scope are documented in the repository's `docs/VERIFICATION.md`; standalone demo and component-test results are reported separately.

## 13. API Documentation and Testing

Swagger UI is available at `http://localhost:8083/swagger-ui.html` when the service is running, and OpenAPI JSON is available at `/v3/api-docs`. The supplied Postman Demo collection exercises the standalone ride workflow; the Integration collection is intended for team bearer tokens after integration contracts are aligned.

The component test report records **24 tests, 0 failures, 0 errors, and 0 skipped**. It also records **22 standalone demo HTTP scenarios** exercised with a local HTTP runner; those scenarios were not run with Postman/Newman. Separately, the repository's `docs/VERIFICATION.md` records the merged integration check: all four services on MySQL, 91 Maven tests passing, 68 full-flow Postman/Newman requests with 83 assertions, and 11 saved-data checks after restart. These are documented local results; they do not claim that the remote GitHub workflow completed. See both reports for their scope and evidence.

## 14. Git and Version Control

The service is in the `IT3130-RideLink` repository under `ride-management-service/`. The checked-out development branch is:

**`feature/ride-management`**

The branch tracks `origin/feature/ride-management`. The service is a dedicated member implementation intended for integration with the other RideLink microservices.

## 15. Challenges and Solutions

The service must coordinate state across a ride database and independent Driver and Fare services. The implementation addresses this by using explicit lifecycle transitions, row locks for ride updates, a dispatch lock and unique active reservations for assignment, and a separately retryable final-fare operation.

Network calls can fail independently of local persistence. Ride creation therefore fails without saving when fare estimation fails, assignment remains unassigned when driver lookup fails, and fare synchronization can be retried after ride completion. The current JWT claim and signing-format differences between the Account and Fare branches remain a team integration issue described in the integration notes.

## 16. Compliance with Assignment Requirements

The Ride Management Service:

- Uses Java and Spring Boot for backend implementation.
- Runs as an independently structured microservice with a service-owned persistence boundary.
- Exposes RESTful APIs for ride requests and lifecycle management.
- Uses a layered controller, service, repository, and database architecture.
- Provides OpenAPI/Swagger documentation and Postman collections for demonstration.
- Can be demonstrated without a separate frontend through its standalone demo profile.

AI assistance was used in preparing the original implementation and documentation. Review and disclose that assistance according to the institute's academic-integrity policy.

## 17. Conclusion

The Ride Management Service owns RideLink's ride requests, driver assignment decisions, lifecycle transitions, trip metrics, authorized ride retrieval, and final-fare synchronization. It uses Java and Spring Boot, keeps its ride data within its own persistence boundary, and coordinates with Driver & Vehicle and Fare & Payment through REST APIs.

Its documented build and standalone demo results show the component workflow and test coverage. The merged team project also has a documented local end-to-end verification using aligned JWT contracts, MySQL, and Postman/Newman; the repository's verification report records the exact scope and results.
