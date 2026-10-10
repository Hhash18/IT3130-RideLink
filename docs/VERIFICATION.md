# Local verification — 2026-10-10

Verified the merged main project with Java 21, real MySQL 8.4 in Docker, four independently running Spring Boot services, Account-issued JWTs and Newman 6.2.3. No demo authentication or fake Ride/Driver/Fare adapters were enabled in this integration run.

## Maven checks

| Service | Tests | Failures / errors / skipped |
|---|---:|---|
| Driver & Vehicle | 28 | 0 / 0 / 0 |
| Account | 15 | 0 / 0 / 0 |
| Ride Management | 24 | 0 / 0 / 0 |
| Fare & Payment | 24 | 0 / 0 / 0 |
| Total | 91 | 0 / 0 / 0 |

All four Maven builds succeeded. Driver now includes JWT regression checks for HS256/384/512, wrong signatures, expired tokens and missing identity/expiry. Isolated unit/API tests use H2 where applicable; the live checks below use MySQL.

## Live Postman / Newman checks

- `RideLink-Full-Flow`: **68 requests, 83 assertions, zero failures**.
- `RideLink-Verify-Saved-Data` after restarting MySQL and all four Java services: **11 requests, 18 assertions, zero failures**.
- Compared collection method/path pairs with the running OpenAPI documents: **39 of 39 business endpoints covered** — Account 7, Driver/Vehicle 16, Ride 9, Fare/Payment 7.
- Both before and after restart, direct MySQL queries passed **9 persistence checks**: account with hashed password, driver assignment, vehicle, completed ride with final-fare reference, cancelled ride, fare amount, successful payment/receipt, retained failed payment, and exactly two payment attempts.

The flow used fictional local accounts and a unique service area. It verified registration/login, Account JWT rejection for invalid tokens in all four services, driver/vehicle CRUD, ride creation/assignment/acceptance/start/completion, cancellation, the Fare-to-Ride callback, ownership/role rejection, declined and successful simulated payments, idempotent retries and duplicate-payment prevention. For 12 km and 25 minutes, the stored fare and receipt total were LKR 945.

MySQL uses four separate databases and database users. Ride and Fare applied their MySQL Flyway migrations and passed Hibernate schema validation. The same records were retrieved after restart; no database reset or reseeding occurred between the full-flow run and saved-data verification.

## Reproduce

See [Local MySQL and Postman](LOCAL-MYSQL-POSTMAN.md). The workflow `.github/workflows/all-services-mysql.yml` contains the same build, integration and restart checks for GitHub Actions. These results are from local execution, not a claim that the remote workflow has completed.

Existing H2 and SQL Server records are not copied into MySQL automatically. Existing H2 files were preserved. The shared local environment, tokens and passwords are ignored by Git. These checks cover coursework backend behavior and simulated payments; they are not a production security/load audit or a real payment gateway test.
