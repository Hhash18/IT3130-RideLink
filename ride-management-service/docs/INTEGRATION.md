# Team integration contracts and known gaps

## Authentication in the merged project

Account issues HMAC tokens with `sub` (account ID), `email`, singular `role`, `iat` and `exp`. Fare & Payment now accepts this format. Configure Ride with `JWT_MODE=account` and supply the same externally managed `JWT_SECRET` to all four services. Use a randomly generated secret of 32–47 UTF-8 bytes because the current Driver decoder supports HS256; Account, Ride and Fare also support longer HMAC keys but Driver does not.

ADMIN can finalize fares; the Account enum currently does not issue SERVICE roles. Ride forwards the caller's bearer token when communicating with Driver and Fare. The alternative Ride JWKS mode is not used by the current Account service.

Default ports are Driver 8081, Account 8082, Ride 8083 and Fare 8084. Each service owns its database. Component tests do not establish that the complete system has passed a live integration run.

Driver operational email must match the Account driver's signed email. Do not assume numeric operational driver ID equals Account ID. Driver's current response has no accountId. Do not allow users to alter their operational email to impersonate another account; profile creation/update protection belongs to the Driver/Account owners.

## Ride → Driver Service

```http
GET /api/drivers/available
Authorization: Bearer <caller-token>
```

Example response:

```json
[{"id":1,"email":"driver@example.test","status":"AVAILABLE","serviceArea":"Colombo","vehicleId":101}]
```

Extra fields are ignored. Candidates without a matching area, vehicle or email are excluded. Lowest operational ID wins among locally unreserved candidates. Service URLs/ports are configurable; the default Driver port is 8081. Ride stores reservations locally; it does not mutate the external availability API. The current Driver branch's GET/PATCH APIs offer no atomic lease. If the team later adds reservations, agree an atomic reserve/release contract and recovery semantics before changing this adapter.

## Ride → Fare estimate

```http
POST /api/fares/estimate
Authorization: Bearer <passenger-token>
Content-Type: application/json
```

```json
{"pickup":"A","destination":"B","distanceKm":10,"durationMinutes":20}
```

Expected response contains `{"fare":{"total":800.00,"currency":"LKR"}}`; other fields are ignored. The service validates the amount/currency before saving the request. Failure creates no ride.

## Completion → final fare → callback

1. Assigned driver invokes `PUT /api/rides/{id}/completion` with actual metrics. Ride commits `COMPLETED` first.
2. ADMIN (or a future trusted SERVICE caller) invokes `PUT /api/rides/{id}/fare`.
3. Ride forwards its bearer token to `PUT /api/fares/rides/{id}`.
4. Fare forwards that same token to `GET /api/rides/{id}`. Ride authorizes ADMIN/SERVICE and returns:

```json
{"id":"<ride-uuid>","passengerId":"<account-subject>","status":"COMPLETED","pickup":"A","destination":"B","actualDistanceKm":12.000,"actualDurationMinutes":25}
```

This endpoint returns additional ride fields, which the existing Fare client ignores. The shared token must be accepted by BOTH services for the callback to succeed. There is no open unauthenticated internal endpoint and no committed service credential.

5. Fare returns `id`, matching `rideId`, and `fare.total` / `fare.currency`. Ride stores this final-fare snapshot.
6. Passenger records simulated payment and gets receipts directly through Member 4's APIs; Ride Management does not own payment state.

Fare synchronization is a separate, explicit operation so it can be retried without changing ride completion. Failed synchronization leaves `finalFareId` null. Repeated successful synchronization returns the stored result. There is no distributed transaction; transient remote-success/local-save-failure is recovered by retrying Member 4's idempotent PUT.

## Acceptance checklist for live integration

- Agree JWT format, ports, allowed roles and driver identity mapping.
- Start Account, Driver, Fare and Ride services using their own persistence boundaries.
- Register matching fictional passenger/driver accounts and a vehicle-enabled AVAILABLE driver in the intended area.
- Use the bearer Integration Postman collection to request, assign, accept, start, complete and finalize a fare.
- Confirm a second passenger/driver is denied, a no-driver area gives 409, and Fare downtime preserves COMPLETED until retry.
- Continue payment/receipt using Member 4's collection with the created ride ID.

The provided demo adapters and HTTP test servers are component-test fixtures, not proof of these live steps.
