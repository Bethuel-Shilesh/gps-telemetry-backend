CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS telemetry (
    id BIGSERIAL NOT NULL,
    vehicle_id VARCHAR(50) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    speed DOUBLE PRECISION,
    harsh_brake BOOLEAN DEFAULT FALSE,
    harsh_acceleration BOOLEAN DEFAULT FALSE,
    ignition BOOLEAN DEFAULT FALSE,
    battery_voltage DOUBLE PRECISION,
    crash_event BOOLEAN DEFAULT FALSE
);

SELECT create_hypertable(
    'telemetry',
    'timestamp',
    if_not_exists => TRUE
);

CREATE INDEX IF NOT EXISTS telemetry_timestamp_idx
ON telemetry (timestamp DESC);