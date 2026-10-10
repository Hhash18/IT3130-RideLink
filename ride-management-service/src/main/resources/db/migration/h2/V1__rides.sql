CREATE TABLE rides (
 id UUID PRIMARY KEY,
 passenger_id VARCHAR(100) NOT NULL,
 pickup VARCHAR(200) NOT NULL,
 destination VARCHAR(200) NOT NULL,
 service_area VARCHAR(100) NOT NULL,
 estimated_distance_km NUMERIC(10,3) NOT NULL,
 estimated_duration_minutes INTEGER NOT NULL,
 estimated_fare NUMERIC(12,2) NOT NULL,
 currency VARCHAR(3) NOT NULL,
 status VARCHAR(20) NOT NULL,
 driver_id BIGINT,
 driver_email VARCHAR(254),
 vehicle_id BIGINT,
 active_driver_id BIGINT UNIQUE,
 active_vehicle_id BIGINT UNIQUE,
 actual_distance_km NUMERIC(10,3),
 actual_duration_minutes INTEGER,
 cancellation_reason VARCHAR(300),
 final_fare_id UUID,
 final_fare NUMERIC(12,2),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 assigned_at TIMESTAMP WITH TIME ZONE,
 accepted_at TIMESTAMP WITH TIME ZONE,
 started_at TIMESTAMP WITH TIME ZONE,
 completed_at TIMESTAMP WITH TIME ZONE,
 cancelled_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX rides_passenger ON rides(passenger_id, created_at);
CREATE INDEX rides_driver_email ON rides(driver_email, created_at);
CREATE TABLE dispatch_lock (id INTEGER PRIMARY KEY);
INSERT INTO dispatch_lock(id) VALUES (1);
