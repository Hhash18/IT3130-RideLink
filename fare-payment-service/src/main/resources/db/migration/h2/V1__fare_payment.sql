CREATE TABLE fares (
    id UUID PRIMARY KEY,
    ride_id VARCHAR(100) NOT NULL UNIQUE,
    passenger_id VARCHAR(100) NOT NULL,
    pickup VARCHAR(200) NOT NULL,
    destination VARCHAR(200) NOT NULL,
    distance_km NUMERIC(10,3) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    base_fare NUMERIC(12,2) NOT NULL,
    distance_charge NUMERIC(12,2) NOT NULL,
    time_charge NUMERIC(12,2) NOT NULL,
    total NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    rule_version VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    fare_id UUID NOT NULL REFERENCES fares(id),
    idempotency_key UUID NOT NULL,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(100),
    receipt_number VARCHAR(50) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT unique_payment_request UNIQUE (fare_id, idempotency_key)
);
CREATE INDEX payments_fare_created ON payments(fare_id, created_at);
