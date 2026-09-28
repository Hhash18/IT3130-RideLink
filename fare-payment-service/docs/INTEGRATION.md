# Member 4 integration contract

This is the proposed interface for coordinating with Members 1 and 3. Their implementations were not available in this workspace. Adapt contracts by agreement; do not implement or directly access their databases here.

## Account → Fare & Payment

Account issues RS256 JWTs and serves its public JWKS. Fare & Payment verifies the signature, issuer, expiry and audience; no local account registration or login is added. Example claims (not a usable token):

```json
{"sub":"passenger-001","iss":"http://localhost:8081","aud":["ridelink"],"roles":["PASSENGER"],"exp":1900000000}
```

Use stable account IDs as subjects, and role names without `ROLE_`. Only Account administrators may issue ADMIN/SERVICE claims. A service token represents a trusted backend caller. Fare & Payment forwards the finalization caller's token to Ride Management; coordinate this audience and permission on both services.

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
2. After completing a ride and persisting actual metrics, call `PUT /api/fares/rides/{rideId}` using a SERVICE or ADMIN bearer token. Fare & Payment calls back to read the completed trip. Complete the Ride transaction before calling Fare & Payment so the callback sees committed data.
3. Passenger (or trusted backend) calls `POST /api/payments/rides/{rideId}` with a UUID `Idempotency-Key` and `{"method":"MOCK_CARD","simulation":"SUCCESS"}`.
4. Query `GET /api/payments/rides/{rideId}` for payment state. Retrieve `/api/payments/{paymentId}/receipt` after success.

A failed simulated payment never reopens or cancels a completed ride. Ride lifecycle status and payment status are separate. Final fares and receipts are immutable snapshots. No refund, fare correction or cancellation fee feature is included in this minimum scope.

## Group-level acceptance still needed

Confirm route/claim names, implement the Ride endpoint, and test real Account → Fare authentication and Ride → Fare → Ride finalization using the integration Postman collection. The demo adapter and HTTP test stub verify this component but are not evidence of the group's full live integration.
