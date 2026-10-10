# Handoff verification - 2026-09-30

Historical standalone handoff results. For the merged MySQL system, see [current verification](../../docs/VERIFICATION.md).

Build command: `mvn -B -ntp -f ride-management-service/pom.xml verify`

Result: **BUILD SUCCESS - 24 tests, 0 failures, 0 errors, 0 skipped.**

| Test suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| com.ridelink.ride.client.RestClientsTest | 3 | 0 | 0 | 0 |
| com.ridelink.ride.config.AccountJwtTest | 3 | 0 | 0 | 0 |
| com.ridelink.ride.controller.DependencyFailureTest | 4 | 0 | 0 | 0 |
| com.ridelink.ride.controller.RideApiTest | 8 | 0 | 0 | 0 |
| com.ridelink.ride.service.DriverSelectorTest | 2 | 0 | 0 | 0 |
| com.ridelink.ride.service.FareValidatorTest | 2 | 0 | 0 | 0 |
| com.ridelink.ride.service.RideStatePolicyTest | 2 | 0 | 0 | 0 |

The executable JAR was also started with the explicit demo profile, a generated temporary password and an isolated in-memory H2 database. All **22 demo HTTP scenarios** corresponding to the supplied Postman collection were exercised successfully with a local HTTP runner. This was not a Postman/Newman execution. It verified Basic authentication, creation, assignment, driver authorization, all lifecycle stages, cancellation, no-driver/invalid-state errors, a final fare of LKR 945 for 12 km / 25 minutes and stable repeated fare identity. The temporary process was stopped after verification.

POST /api/rides returns 201; the successful read/transition operations return 200; negative scenarios returned their expected 400, 401, 403 or 409 status.

These are component and standalone-demo results. Live integration with the team's Account, Driver and Fare processes and a remote GitHub Actions run have NOT been performed. Resolve the authentication contract in INTEGRATION.md before claiming group-level integration.

Reproduce unit/API/HTTP-client tests with `mvn clean verify`. Use the supplied Postman collection for the interactive demonstration. Build outputs and databases are intentionally excluded from the source ZIP.
