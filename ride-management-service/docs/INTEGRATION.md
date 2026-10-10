# Team integration contracts and known gaps

Inspected on 2026-09-30:
- Account branch: `1fffcb8` (HMAC token, `sub` account ID, `email`, singular `role`, `iat`, `exp`).
- Driver & Vehicle branch: `origin/feature/driver-vehicle-service`; available endpoint returns operational numeric IDs, email, serviceArea, status and vehicleId.
- Fare & Payment branch: `a279254` (RSA/JWKS, plural roles, issuer and audience).

These branches were inspected read-only. This handoff does not modify them or claim that the complete team system has passed live integration.

## Authentication mismatch to resolve before group demonstration

Account currently signs HMAC JWTs and does not issue the JWKS/issuer/audience contract expected by Fare. Ride supports either configured format; it does not translate or forge tokens. Because it forwards the caller's token to Fare, choosing `JWT_MODE=account` alone does NOT make the current Fare branch accept Account tokens.

Agree one group contract:

1. Keep the current Account format. Configure Ride with `JWT_MODE=account` and the externally supplied shared `JWT_SECRET`. Member 4 must update Fare verification to the same algorithm/claims. ADMIN is sufficient to finalize fares; the current Account enum does not issue SERVICE roles.
2. Adopt the existing Fare RSA contract. Account must issue RS256 JWTs with `roles`, `iss`, `aud`, `sub`, `email` and `exp`, and expose JWKS. Configure Ride with `JWT_MODE=jwks` and matching issuer/JWKS/audience, and align Fare with it.

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

Extra fields are ignored. Candidates without a matching area, vehicle or email are excluded. Lowest operational ID wins among locally unreserved candidates. Service URLs/ports are configurable; the actual Driver port must be confirmed by its owner. Ride stores reservations locally; it does not mutate the external availability API. The current Driver branch's GET/PATCH APIs offer no atomic lease. If the team later adds reservations, agree an atomic reserve/release contract and recovery semantics before changing this adapter.

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
