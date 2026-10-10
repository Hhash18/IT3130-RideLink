# Member 4 integration contract

The merged project uses a shared Account JWT contract and four separate MySQL databases. See [local setup](../../docs/LOCAL-MYSQL-POSTMAN.md).

## Account → Fare & Payment

Account currently signs tokens using `Keys.hmacShaKeyFor(secret.getBytes(UTF_8))` and JJWT `signWith(secretKey)`. Fare uses the same raw secret from the `JWT_SECRET` environment variable and matches that algorithm selection: HS256 for 32–47 bytes, HS384 for 48–63, HS512 for 64+. Missing/blank/short secrets stop normal-mode startup. There is no committed default secret.

Driver, Ride and Fare now all match Account’s HMAC algorithm selection. Supply the same raw `JWT_SECRET` to all four services.

Example claim shape (not a usable token):

```json
{"sub":"00000000-0000-4000-8000-000000000001","email":"passenger@example.test","role":"PASSENGER","iat":1791000000,"exp":1791003600}
```

Fare validates signature, expiry/standard timestamp rules and the subject, then maps singular `role` to Spring's `ROLE_` authorities. Account does not issue issuer/audience claims, so Fare no longer requires them or a JWKS endpoint. There is no local account registration or token-issuance API. Passenger IDs returned by Ride must be the same stable Account subject string, not email or an unrelated numeric ID.

Current Account roles are PASSENGER, DRIVER and ADMIN. Use an Account-issued ADMIN token to finalize fares; the Integration Postman environment calls it `adminToken`. SERVICE remains a reserved trusted backend role in Fare/Ride but is not issued by current Account. Passengers cannot create final fares or read/pay another passenger's records.

## Ports and clone configuration

Driver runs on 8081, Account 8082, Ride 8083 and Fare 8084. All services include tracked configuration with environment placeholders; passwords and signing secrets are external.

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
