# Local MySQL and Postman

## Start the complete backend (macOS/Linux)

Install JDK 21, Maven, Python 3 and Docker Desktop. Open Docker Desktop. From the repository root:

```sh
python3 scripts/local.py start
python3 scripts/prepare-postman.py
```

`start` runs all Maven tests, starts MySQL 8.4 in Docker, and starts the four Java services. After a successful build, use `python3 scripts/local.py start --no-build` for faster startup. Existing services on the same ports must be stopped first.

The setup generates a shared JWT secret and separate database passwords in ignored `.local/settings.json` and `.local/mysql.env`. Keep these files with the Docker volume; regenerating passwords does not change existing MySQL users. Do not commit or share these files. Logs are in `.local/*.log`. The public configuration has no real passwords.

| Service | Port | Database | MySQL user |
|---|---:|---|---|
| Driver & Vehicle (root project) | 8081 | ridelink_driver_db | ridelink_driver |
| Account | 8082 | ridelink_account_db | ridelink_account |
| Ride Management | 8083 | ridelink_ride_db | ridelink_ride |
| Fare & Payment | 8084 | ridelink_fare_payment_db | ridelink_fare_payment |

The provided MySQL container listens on **localhost:3308**, avoiding the existing MySQL container on 3307. The startup script sets each service's `DB_URL`. Each database user has access only to its own service database. In a database GUI use port 3308 and credentials from `.local/settings.json`.

## Postman

1. Import `postman/RideLink-Full-Flow.postman_collection.json`.
2. Import `.local/RideLink-Local.postman_environment.json` (show hidden files to find `.local`).
3. Select **RideLink Local (ready to test)** in Postman.
4. Open the collection Runner and run requests in their saved order, once per iteration.
5. Keep/persist environment variable changes in the Runner. The collection saves tokens, account IDs, driver/vehicle IDs, ride ID, payment ID and receipt number in this environment.

The preparation script provisions `admin.local@example.test` only inside the isolated local Docker database. It first registers a normal account with a BCrypt password, then the local database owner assigns ADMIN. Public registration still rejects ADMIN. Use this setup only for local development.

The first collection request logs in as that admin and generates unique fictional passenger/driver emails for the run. Passwords are generated locally. Later requests automatically attach the right bearer token. For manual login, use the email variables and `test_password` in the imported local environment; no token should be handwritten.

The full collection contains 68 requests and covers all 39 documented business endpoints: registration/login, driver/vehicle CRUD, estimates, the ride lifecycle, cancellation, final fare callbacks, failed and successful simulated payments, receipt retrieval, duplicate-payment protection and ownership restrictions. Expected 400/401/403/409 responses are successful negative tests; check the **Test Results** tab. Payments do not charge a real card.

For a 10 km / 20 minute estimate the amount is LKR 800. Completing the ride at 12 km / 25 minutes produces LKR 945. The payment history should contain one failed and one successful attempt, even after retrying the same successful payment key.

## Prove data survives restart

After a successful full-flow run, import `postman/RideLink-Verify-Saved-Data.postman_collection.json` using the same environment. Then:

```sh
python3 scripts/local.py stop
docker compose restart mysql
python3 scripts/local.py start --no-build
```

Run **RideLink-Verify-Saved-Data**. It logs in again and reads the same saved account, driver, vehicle, completed/cancelled rides, fare, payment history and receipt. Do not rerun the preparation script between these two collections because it recreates the environment without the saved IDs.

MySQL data lives in the `ridelink-local_mysql-data` Docker volume. Stopping Java or running `docker compose stop` retains it. `docker compose down -v` deletes it and should not be used if you want to keep your records.

## Inspect database records

```sh
docker compose exec mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot'
```

Run these SQL statements:

```sql
SHOW DATABASES;
SELECT id, name, email, role FROM ridelink_account_db.accounts;
SELECT id, name, email, vehicle_id FROM ridelink_driver_db.drivers;
SELECT BIN_TO_UUID(id), status, final_fare FROM ridelink_ride_db.rides;
SELECT BIN_TO_UUID(id), ride_id, total FROM ridelink_fare_payment_db.fares;
SELECT BIN_TO_UUID(id), status, receipt_number FROM ridelink_fare_payment_db.payments;
```

UUIDs are stored as 16-byte binary values; `BIN_TO_UUID` makes them readable. Ride and Fare use MySQL Flyway migrations and Hibernate schema validation. Account and Driver currently use Hibernate `ddl-auto=update` for their coursework schema.

## Start a service manually / IntelliJ

Set `JWT_SECRET`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and optionally `PORT` in each run configuration. Use the generated values from `.local/settings.json`, the correct database above, and a JDBC URL such as `jdbc:mysql://localhost:3308/ridelink_ride_db?connectionTimeZone=UTC`. Run the relevant Spring Boot main class or `mvn spring-boot:run` in its service folder. Do not enable `demo` for real cross-service integration.

All JWT verifiers now follow Account's raw UTF-8 HMAC secret and singular `role` claim. HS256/384/512 is selected by the same key-length rules. Account is the only token issuer. Driver authorization by Ride matches the signed account email to the operational driver's email.

For an isolated old Ride/Fare demo only, enable `demo,h2` and provide `DEMO_PASSWORD`. Their existing `data/*.mv.db` files are preserved. Existing H2 or SQL Server records are **not automatically migrated to MySQL**; this setup creates separate MySQL databases.

## Run the same Postman tests from the terminal

Node.js/npm are required only for this optional command:

```sh
npx --yes --registry=https://registry.npmjs.org newman@6.2.3 run postman/RideLink-Full-Flow.postman_collection.json -e .local/RideLink-Local.postman_environment.json --export-environment .local/RideLink-Local.postman_environment.json --bail
python3 scripts/verify-persistence.py
```

After restarting, run the saved-data collection with the same `-e` environment. See [verified results](VERIFICATION.md).

## Stop / inspect status

```sh
python3 scripts/local.py status
python3 scripts/local.py stop
docker compose stop mysql
```
