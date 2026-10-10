# Member 4 integration contract

Contracts were checked against Account `aa16bc0`, Ride Management `33a8d60`, and Driver & Vehicle `61e3be3` on 2026-10-03. This change updates Member 4 only. Other services and their databases are not modified.

## Account → Fare & Payment

Account currently signs tokens using `Keys.hmacShaKeyFor(secret.getBytes(UTF_8))` and JJWT `signWith(secretKey)`. Fare uses the same raw secret from the `JWT_SECRET` environment variable and matches that algorithm selection: HS256 for 32–47 bytes, HS384 for 48–63, HS512 for 64+. Missing/blank/short secrets stop normal-mode startup. There is no committed default secret.

The Driver branch currently verifies HS256 only. To use all four current services together without changing Driver, provision the SAME 32–47-byte secret across Account, Driver, Ride and Fare. Ride must use `JWT_MODE=account`. Do not change an already shared secret independently. Alternatively, the Driver owner must align its algorithm verification with Account before the team uses a longer secret.

Example claim shape (not a usable token):

```json
{"sub":"42","email":"passenger@example.test","role":"PASSENGER","iat":1791000000,"exp":1791003600}
```

Fare validates signature, expiry/standard timestamp rules and the subject, then maps singular `role` to Spring's `ROLE_` authorities. Account does not issue issuer/audience claims, so Fare no longer requires them or a JWKS endpoint. There is no local account registration or token-issuance API. Passenger IDs returned by Ride must be the same stable Account subject string, not email or an unrelated numeric ID.

Current Account roles are PASSENGER, DRIVER and ADMIN. Use an Account-issued ADMIN token to finalize fares; the Integration Postman environment calls it `adminToken`. SERVICE remains a reserved trusted backend role in Fare/Ride but is not issued by current Account. Passengers cannot create final fares or read/pay another passenger's records.

## Ports and clone configuration

Account is configured on 8082. Fare defaults to 8084; Ride should run on 8083. Driver's current port is not present in tracked configuration—confirm it with Member 2 and configure Ride's DRIVER_SERVICE_URL explicitly. Do not send Driver API calls to Account's 8082 port.

The Ride branch lacks `ride-management-service/src/main/resources/application.properties` in Git because the repository-root `.gitignore` ignores every file called application.properties. Member 3 must restore a secret-free properties template/configuration with environment placeholders. Driver also lacks tracked application configuration. Fare's own application.properties is already tracked and contains placeholders, so it remains available when merging into main. No real secrets should be added to resolve this issue.

## Fare & Payment → Ride Management

`GET {RIDE_SERVICE_URL}/api/rides/{rideId}` with `Authorization: Bearer <caller-token>`:

```json
{
  "id":"ride-123",
  "passengerId":"passenger-001",
  "status":"COMPLETED",
  "pickup":"Colombo Fort",
  "destination":"Bambalapitiya",
  "actualDistanceKm":10.000,
  "actualDurationMinutes":20
}
```

Required fields: IDs and labels must be nonblank (IDs ≤100 characters, labels ≤200); distance 0.001–1000 with ≤3 fractional digits; integer duration 0–1440. Ride Management must preserve completed ride ownership/metrics. Fare finalization requires exactly `COMPLETED`. The Fare service validates the response ID against the requested ID. Unknown extra Ride fields are ignored by the client.

Connection timeout is 2 seconds; read timeout is 3 seconds. Missing ride maps to 404; upstream auth denial to 403; invalid trip data to 502; HTTP server/connection failures and malformed JSON to 503. There is no automatic retry or database fallback. The caller can safely retry the PUT. Final fares already stored do not require a network call to retrieve or finalize again.

## Ride Management → Fare & Payment

1. During booking, request `POST /api/fares/estimate` with simulated route metrics. Persist the Ride record in the Ride service only.
2. After completing a ride and persisting actual metrics, call `PUT /api/fares/rides/{rideId}` using an Account-issued ADMIN bearer token. Fare & Payment calls back to read the completed trip. Complete the Ride transaction before calling Fare & Payment so the callback sees committed data.
3. Passenger (or trusted backend) calls `POST /api/payments/rides/{rideId}` with a UUID `Idempotency-Key` and `{"method":"MOCK_CARD","simulation":"SUCCESS"}`.
4. Query `GET /api/payments/rides/{rideId}` for payment state. Retrieve `/api/payments/{paymentId}/receipt` after success.

A failed simulated payment never reopens or cancels a completed ride. Ride lifecycle status and payment status are separate. Final fares and receipts are immutable snapshots. No refund, fare correction or cancellation fee feature is included in this minimum scope.

## Group-level acceptance still needed

Restore missing Ride/Driver configuration, agree ports and the shared secret/algorithm, then test live Account → Fare authentication and Ride → Fare → Ride finalization using the Integration Postman collection. Driver role/ownership protections must be addressed by its owner; authentication alone currently protects its mutation endpoints. The demo adapter, locally generated Account-format tokens and HTTP test stub verify this component but are not evidence of the group's full live integration.
