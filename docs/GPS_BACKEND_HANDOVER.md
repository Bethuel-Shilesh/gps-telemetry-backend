# GPS Vehicle Tracking Backend — Handover Documentation

## 1. Project Overview

This project implements the backend for the GPS Vehicle Tracking system.

### Current team responsibility

The current backend team's responsibility is limited to:

* Data acquisition
* Packet reception
* Packet validation
* Telemetry decoding
* Telemetry storage
* Basic REST access to stored telemetry

The following areas belong to other teams and are outside the current implementation scope:

* Driver behaviour analysis
* Analytics
* Dashboards/UI
* AI/ML
* Fleet management
* Maintenance
* Reporting

## 2. System Architecture

Current architecture:

```text
                 GPS Tracker
                     |
                     | GT06 / TCP
                     v
             Netty TCP Server
                Port 5000
                     |
                     v
             Packet Framer
                     |
                     v
             GT06 Decoder
                     |
             Header / Length
             Protocol / CRC
             Validation
                     |
                     v
              Telemetry Object
                     |
                     v
             Spring Boot Service
                     |
                     v
              JPA / Hibernate
                     |
                     v
          PostgreSQL / TimescaleDB
                     |
                     v
             Stored Telemetry
```

There is also a REST API for inserting telemetry directly:

```text
REST Client
    |
    | HTTP POST
    v
Spring Boot REST Controller
    |
    v
Telemetry Service
    |
    v
JPA / Hibernate
    |
    v
TimescaleDB
```

### Technology stack

* Java 17
* Spring Boot
* Spring Web
* JPA / Hibernate
* PostgreSQL
* TimescaleDB
* Docker
* Netty
* GT06 TCP protocol handling
* Maven
* Log4j2
* WSL2 Ubuntu
* Git / GitHub

## 3. Docker / TimescaleDB Setup

The database runs inside Docker.

### Container

```text
Container: gps-timescaledb
Database: gps_telemetry
Database user: gps_user
Database port: 5432
```

The Docker database uses:

```text
timescale/timescaledb:latest-pg17
```

### Start the database

From the project root:

```bash
docker compose up -d
```

Check that the container is running:

```bash
docker ps
```

The `gps-timescaledb` container should be running.

### Stop the database

```bash
docker compose down
```

Do not use this while Spring Boot is actively using the database unless you intentionally want to stop the database.

### Database connection

Current application configuration:

```properties
DB_URL=jdbc:postgresql://localhost:5432/gps_telemetry
DB_USERNAME=gps_user
DB_PASSWORD=gps_password
```

These are also the default values in `application.properties`.

### Important Windows/WSL note

The local Windows PostgreSQL service was stopped because Docker TimescaleDB uses:

```text
localhost:5432
```

Only one PostgreSQL service should occupy this port at a time.

## 4. Starting Spring Boot

Make sure Docker/TimescaleDB is running first.

From the project root:

```bash
./mvnw spring-boot:run
```

The application uses:

```text
HTTP port: 8080
TCP port: 5000
```

Successful startup should show the Spring Boot application and the GT06 TCP server starting on port `5000`.

### Stop Spring Boot

Press:

```text
Ctrl + C
```

## 5. TCP Port 5000

The GPS tracker communication server listens on:

```text
TCP 5000
```

Configuration:

```properties
gps.tcp.port=5000
```

Current expected communication flow:

```text
GPS Tracker
    |
    | TCP
    v
localhost:5000
```

For the actual vehicle tracker, the server must eventually be reachable from the network used by the tracker. The current local simulator uses `localhost`.

The actual tracker connection has not yet been performed.

## 6. GT06 Packet Processing

The current TCP implementation performs basic packet validation.

The processing sequence is:

```text
TCP data received
       |
       v
Check GT06 header
       |
       v
Check packet length
       |
       v
Check protocol number
       |
       v
Calculate CRC
       |
       v
Compare received CRC
       |
       v
Valid packet
       |
       v
Telemetry processing
       |
       v
Database storage
```

Invalid packets are rejected before normal telemetry storage.

### Current validation

Implemented validation includes:

* Packet length check
* GT06 header check
* Packet length-field validation
* Protocol validation
* CRC validation
* Stop-bit validation

## 7. REST API

The backend currently provides a REST endpoint for inserting telemetry.

### Endpoint

```text
POST /api/telemetry
```

Full local address:

```text
http://localhost:8080/api/telemetry
```

### Request header

```text
Content-Type: application/json
```

### Example request

```bash
curl -X POST http://localhost:8080/api/telemetry \
  -H "Content-Type: application/json" \
  -d '{"vehicleId":"WSL_TEST_001","timestamp":"2026-09-23T10:40:00Z","latitude":19.0330,"longitude":73.0297,"speed":40.0,"harshBrake":false,"harshAcceleration":false,"ignition":true,"batteryVoltage":12.4,"crashEvent":false}'
```

### Example telemetry fields

```json
{
  "vehicleId": "WSL_TEST_001",
  "timestamp": "2026-09-23T10:40:00Z",
  "latitude": 19.0330,
  "longitude": 73.0297,
  "speed": 40.0,
  "harshBrake": false,
  "harshAcceleration": false,
  "ignition": true,
  "batteryVoltage": 12.4,
  "crashEvent": false
}
```

The REST endpoint was successfully tested and stored telemetry in TimescaleDB.

## 8. Database Storage

The telemetry table currently contains:

```text
id
vehicle_id
timestamp
latitude
longitude
speed
harsh_brake
harsh_acceleration
ignition
battery_voltage
crash_event
```

The application uses:

```text
JPA / Hibernate
        |
        v
PostgreSQL JDBC
        |
        v
TimescaleDB
```

Hibernate is currently configured with:

```properties
spring.jpa.hibernate.ddl-auto=update
```

This allows Hibernate to update the database schema based on the entity during development.

## 9. Tracker Simulator

The project contains a local GT06 packet simulator.

Location:

```text
tracker-simulator/
```

Files:

```text
TrackerSimulator.java
InvalidCrcSimulator.java
InvalidHeaderSimulator.java
InvalidLengthSimulator.java
```

The simulator is only for local backend testing.

### Compile the simulators

From the project root:

```bash
cd tracker-simulator
javac *.java
```

### Run the valid packet simulator

Make sure Spring Boot is already running.

```bash
java TrackerSimulator
```

Expected behaviour:

```text
Connect to TCP port 5000
Send valid GT06 test packet
Backend receives packet
Backend validates packet
Backend sends ACK
Telemetry processing occurs
```

### Run invalid CRC test

```bash
java InvalidCrcSimulator
```

Expected result:

```text
CRC validation fails
Packet is rejected
No telemetry row is inserted
```

### Run invalid header test

```bash
java InvalidHeaderSimulator
```

Expected result:

```text
Invalid header is rejected
No normal decoder processing occurs
No telemetry row is inserted
```

### Run invalid length test

```bash
java InvalidLengthSimulator
```

Expected result:

```text
Length validation fails
Packet is rejected
No telemetry row is inserted
```

After testing, return to the project root:

```bash
cd ..
```

## 10. Validation Test Results

The following backend validation tests have been completed.

### Valid GT06 packet

Result:

```text
PASS
```

The backend accepted the packet and processed it.

### Invalid CRC

Test:

```text
Received CRC: 0x0000
Calculated CRC: 0x079E
```

Result:

```text
PASS
```

The packet was rejected because the CRC did not match.

The backend did not create a new telemetry database row.

### Invalid header

Test:

```text
Header changed from 0x7878 to 0x1234
```

Result:

```text
PASS
```

The packet was rejected during the framing/header stage.

No ACK was generated by the normal decoder path.

No telemetry database row was created.

### Invalid length

Test:

```text
Length field deliberately changed to an incorrect value
```

Result:

```text
PASS
```

The packet was rejected during packet framing/length validation.

No telemetry database row was created.

### Build validation

The following command completed successfully:

```bash
./mvnw clean test
```

Result:

```text
BUILD SUCCESS
```

## 11. Logging

The project uses Log4j2.

Configuration file:

```text
src/main/resources/log4j2-spring.xml
```

Logs are available in:

```text
Console
```

and:

```text
logs/gps-telemetry.log
```

The file logger uses rolling logs with:

```text
10 MB
```

size-based rotation and daily rotation.

Up to seven rollover files are retained.

Application code does not use:

```java
System.out
System.err
```

for normal application logging.

## 12. GitHub Repository

The source code is maintained in the GitHub repository:

```text
Bethuel-Shilesh/gps-telemetry-backend
```

Current branch:

```text
main
```

Latest verified checkpoint commit:

```text
e24d33e
```

Commit message:

```text
Add Log4j2 logging and packet validation tests
```

At the time of this documentation:

```text
Local branch = origin/main
Working tree = clean
```

## 13. Current Simulation vs Real Tracker

### Currently simulated

The following are currently tested locally:

```text
GT06 TCP connection
GT06 test packet
Packet framing
Header validation
Length validation
Protocol validation
CRC validation
ACK handling
Telemetry processing
Database storage
```

The local simulator generates a test GT06 packet.

The current decoder intentionally uses simulated telemetry values for the valid test packet.

Therefore, the current simulator proves the backend communication and validation flow, but it does NOT prove that the packet fields match the real AW01 tracker.

### Requires the real tracker

The following still require the actual R12/AW01 tracker:

```text
Actual tracker TCP connection
Actual tracker IP/domain configuration
Actual tracker packet capture
Actual AW01 packet structure verification
Actual field positions
Actual timestamp format
Actual latitude/longitude encoding
Actual speed encoding
Actual heading/course encoding
Actual ignition/ACC encoding
Actual battery information, if provided
Actual reporting interval
Actual tracker ACK requirements
```

These should not be assumed from the simulator.

## 14. Current Limitations and Dependencies

### 1. Real tracker packet format

The exact AW01 packet structure has not yet been verified using a real tracker packet.

The current decoder therefore should be treated as a GT06 test implementation, not as a confirmed AW01 production decoder.

### 2. Tracker documentation

The final implementation depends on receiving the actual tracker specification/documentation containing:

* Protocol information
* Packet format
* Field definitions
* Byte positions
* Units
* Timestamp format
* Coordinate encoding
* Device identifiers
* ACK requirements
* Reporting configuration

### 3. Network connectivity

The local simulator connects to:

```text
localhost:5000
```

A real tracker will require a reachable server address and appropriate network configuration.

### 4. Company/cloud integration

Company-provided cloud credentials/API/protocol information has not yet been integrated.

This should be handled separately once the required information is provided.

### 5. Telemetry field confirmation

The current telemetry entity contains fields such as:

```text
vehicleId
timestamp
latitude
longitude
speed
ignition
batteryVoltage
```

Some fields currently used for testing may eventually need to be mapped differently depending on the actual tracker packet.

### 6. Analytics

The backend does not currently implement:

* Driver behaviour analysis
* Harsh braking analysis
* Harsh acceleration analysis
* Dashboards
* AI/ML
* Fleet analytics

Those functions are outside the current data acquisition and storage responsibility.

## 15. Recommended Procedure When the Real Tracker Becomes Available

Do not immediately rewrite the decoder.

Follow this sequence:

```text
1. Obtain tracker documentation
          |
          v
2. Capture one real packet
          |
          v
3. Save the raw hexadecimal packet
          |
          v
4. Compare it with current GT06 assumptions
          |
          v
5. Identify actual field positions
          |
          v
6. Modify only the protocol-specific decoder
          |
          v
7. Test with real tracker
          |
          v
8. Verify stored database values
          |
          v
9. Repeat with multiple real packets
          |
          v
10. Re-run invalid packet tests
```

The database and storage architecture should only be changed if the actual tracker data requires additional or different fields.

## 16. Quick Start — New Developer

For a developer returning to the project:

### Terminal 1 — Start database

```bash
cd ~/gps-telemetry-backend
docker compose up -d
docker ps
```

### Terminal 2 — Start Spring Boot

```bash
cd ~/gps-telemetry-backend
./mvnw spring-boot:run
```

### Terminal 3 — Run simulator

```bash
cd ~/gps-telemetry-backend/tracker-simulator
javac *.java
java TrackerSimulator
```

### Run tests

From the project root:

```bash
./mvnw clean test
```

### Check Git status

```bash
git status
```

The working tree should normally be clean after committing changes.

## 17. Current Project Status

```text
Backend implementation              COMPLETE
Database integration                COMPLETE
Docker TimescaleDB                  COMPLETE
REST telemetry insertion            TESTED
GT06 TCP server                     COMPLETE
Packet framing                      COMPLETE
Header validation                   TESTED
Length validation                   TESTED
CRC validation                      TESTED
ACK handling                        TESTED
Log4j2 logging                      COMPLETE
Simulator                           COMPLETE
Invalid packet simulators           COMPLETE
Maven tests                         PASS
GitHub checkpoint                   COMPLETE

Real tracker integration            PENDING
Actual AW01 packet verification     PENDING
Company cloud integration           PENDING
```

The backend is currently prepared for the next stage: connecting the real tracker and replacing the simulated telemetry mapping with verified tracker data if required.
