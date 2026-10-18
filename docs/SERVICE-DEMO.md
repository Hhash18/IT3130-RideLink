# Service-by-service lecturer demonstration

Import `postman/RideLink-Service-Folders.postman_collection.json` into Postman and select the existing **RideLink Local (ready to test)** environment. Start all four services using the [local setup guide](LOCAL-MYSQL-POSTMAN.md). The collection contains 68 requests grouped into four service folders; tokens and IDs are saved by its scripts.

## Shared setup

Run these requests once, in order, before the two presentations:

1. Account: **Login admin**, **Register passenger**, **Login passenger**, **Register driver**, **Login driver**.
2. For ownership tests, also run Account: **Register other**, **Login other**.
3. Driver and Vehicle: **Create vehicle**, **Create driver profile**, **Assign vehicle**, **Driver availability**.
4. Check **List available drivers**: the driver must be AVAILABLE, have a vehicle, and match the ride's service area. Its email must match the signed driver account email.

Do not rerun **Login admin** midway through this setup: its prerequest script creates fresh email and area variables. To start a new demonstration, repeat the setup from the beginning. The complete collection can also be run from folders 01 through 04 in order.

## Ride Management presentation — folder 03

Send **Request ride**, **List own rides**, **Get ride**, **Assign driver to ride**, **Driver accepts ride**, **Driver starts ride**, **Driver completes ride**, then **Finalize fare through Ride and callback**.

The lifecycle progresses REQUESTED → ASSIGNED → ACCEPTED → IN_PROGRESS → COMPLETED. Completion uses 12 km and 25 minutes; final fare is LKR 945. The final-fare request uses the admin token and calls the Fare service, which calls Ride back to read the completed trip.

Use **Request another ride** followed by **Cancel second ride** to demonstrate cancellation. These use a separate `cancel_ride_id`; the completed `ride_id` remains available for the payment demonstration.

Show the folder's invalid-token and ownership checks as well. Expected 401/403 responses should pass their assertions.

## Fare and Payment presentation — folder 04

Send the following in order:

1. **Fare estimate** — LKR 800 for 10 km and 20 minutes.
2. **Finalize fare - returns existing fare if Ride already finalized it** — the same stored LKR 945 final fare.
3. **Read fare**.
4. **Payment initially unpaid** — UNPAID.
5. **Record declined payment** — HTTP 200 with FAILED payment status.
6. **Failed payment has no receipt** — expected HTTP 409.
7. **Record successful payment** — HTTP 200 with SUCCEEDED status and receipt number.
8. **Retry same payment safely** — returns the same payment ID.
9. **Prevent duplicate payment** — expected HTTP 409 for a different key on an already paid ride.
10. **Read payment**, **Read receipt**, then **Payment history has two attempts** — PAID with exactly one failed and one successful attempt.

Also demonstrate **Invalid fare estimate** (400) and ownership restrictions (403). Payments are simulated and do not charge a real card. Use a fresh completed ride for a new unpaid-to-paid demonstration; a previously paid ride will not pass the UNPAID assertion.

## Show each database in a separate terminal

From the repository root, Terminal 1:

```sh
docker compose exec mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot ridelink_ride_db'
```

```sql
SELECT BIN_TO_UUID(id) AS ride_id, status, actual_distance_km,
       actual_duration_minutes, final_fare
FROM rides ORDER BY created_at DESC LIMIT 10;
```

Terminal 2:

```sh
docker compose exec mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot ridelink_fare_payment_db'
```

```sql
SELECT BIN_TO_UUID(id) AS fare_id, ride_id, total, currency
FROM fares ORDER BY created_at DESC LIMIT 10;
SELECT BIN_TO_UUID(id) AS payment_id, BIN_TO_UUID(fare_id) AS fare_id,
       method, status, receipt_number
FROM payments ORDER BY created_at DESC LIMIT 10;
```

Match the Postman IDs with database IDs. The two service databases share a local MySQL container but have separate database users. Services communicate through REST rather than querying each other's tables. Type `exit;` to leave each MySQL session.
