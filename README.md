# GPS Telemetry Backend

Backend module for the GPS Vehicle Tracking System.

The backend is responsible for receiving vehicle telemetry data, validating the incoming data, and storing the telemetry data in a time-series database using TimescaleDB.

---

## 1. Project Scope

This module currently focuses on:

- Receiving GPS telemetry data through REST API
- Validating incoming telemetry data
- Storing telemetry data in TimescaleDB
- Using Docker for database deployment
- Providing APIs for telemetry ingestion
- Preparing the backend for future GPS tracker integration

---

## 2. Technology Stack

- Java
- Spring Boot
- Maven
- PostgreSQL
- TimescaleDB
- Docker
- Docker Compose
- JDBC
- REST API
- Postman
- Git / GitHub

---

## 3. System Flow

GPS Tracker
    ↓
Telemetry Data
    ↓
Spring Boot REST API
    ↓
Data Validation
    ↓
Telemetry Service
    ↓
TimescaleDB
    ↓
Telemetry Storage

---

## 4. Telemetry Fields

The backend currently handles the following fields:

| Field | Description |
|---|---|
| vehicleId | Unique vehicle/tracker identifier |
| timestamp | Time at which telemetry was generated |
| latitude | Vehicle latitude |
| longitude | Vehicle longitude |
| speed | Vehicle speed |
| harshBrake | Indicates harsh braking |
| harshAcceleration | Indicates harsh acceleration |
| ignition | Vehicle ignition status |
| batteryVoltage | Vehicle battery voltage |
| crashEvent | Indicates a crash event |

---

## 5. Project Structure

```text
gps-telemetry-backend/
│
├── .mvn/
│   └── wrapper/
│
├── .postman/
│
├── database/
│   └── init.sql
│
├── postman/
│   └── globals/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/gps/telemetry/
│   │   │       │
│   │   │       ├── controller/
│   │   │       │   └── TelemetryController.java
│   │   │       │
│   │   │       ├── model/
│   │   │       │   └── Telemetry.java
│   │   │       │
│   │   │       ├── service/
│   │   │       │   ├── DatabaseTestService.java
│   │   │       │   └── TelemetryService.java
│   │   │       │
│   │   │       └── GpsTelemetryBackendApplication.java
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│
├── docker-compose.yml
├── mvnw
├── mvnw.cmd
├── pom.xml
├── .gitignore
└── README.md
6. Backend Components
GpsTelemetryBackendApplication.java

Main Spring Boot application class used to start the backend server.

TelemetryController.java

Handles HTTP requests related to telemetry.

Current endpoints:

GET  /api/telemetry/db-test
POST /api/telemetry
Telemetry.java

Model class representing the telemetry data received by the backend.

It also contains validation rules for incoming data.

TelemetryService.java

Responsible for storing validated telemetry data in the TimescaleDB database.

DatabaseTestService.java

Used to verify that the backend can successfully connect to the database.

7. Database

The project uses TimescaleDB on top of PostgreSQL.

Database configuration:

Database: gps_telemetry
Username: gps_user
Port: 5432

The application configuration supports environment variables:

spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/gps_telemetry}
spring.datasource.username=${DB_USERNAME:gps_user}
spring.datasource.password=${DB_PASSWORD:gps_password}

The default values are intended for local development.

8. Database Table

The main database table is:

telemetry

Columns:

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

The timestamp column is used as the primary time dimension.

The telemetry table is configured as a TimescaleDB hypertable for time-series data.

9. Database Initialization

The database initialization script is located at:

database/init.sql

The script is responsible for:

Enabling the TimescaleDB extension
Creating the telemetry table
Creating the required sequence
Creating the timestamp index
Converting the telemetry table into a TimescaleDB hypertable
10. Docker Setup

TimescaleDB is run using Docker Compose.

Docker configuration file:

docker-compose.yml

Docker image:

timescale/timescaledb:latest-pg17

Container name:

gps-timescaledb

Database port:

5432

Database data is stored in a Docker volume:

timescale_data

This allows database data to persist when the container is stopped or recreated.

11. Requirements

Install the following:

Java JDK
Docker Desktop
Git

Maven does not need to be installed separately because the project includes the Maven Wrapper.

12. Clone the Repository
git clone https://github.com/Bethuel-Shilesh/gps-telemetry-backend.git
cd gps-telemetry-backend
13. Start TimescaleDB

Start the database using:

docker compose up -d

Check whether the container is running:

docker ps

Expected container:

gps-timescaledb
14. Start Spring Boot Backend
Windows

Run:

mvnw.cmd spring-boot:run

The backend runs on:

http://localhost:8080
15. Test Database Connection

Endpoint:

GET /api/telemetry/db-test

Using Windows Command Prompt:

curl http://localhost:8080/api/telemetry/db-test

Expected response:

Database connected. Result = 1
16. Send Telemetry Data

Endpoint:

POST /api/telemetry

Content-Type:

application/json

Example JSON:

{
  "vehicleId": "GPS001",
  "timestamp": "2026-08-26T12:00:00Z",
  "latitude": 19.0760,
  "longitude": 72.8777,
  "speed": 60.5,
  "harshBrake": false,
  "harshAcceleration": true,
  "ignition": true,
  "batteryVoltage": 12.6,
  "crashEvent": false
}

Windows Command Prompt:

curl -X POST http://localhost:8080/api/telemetry ^
-H "Content-Type: application/json" ^
-d "{\"vehicleId\":\"GPS001\",\"timestamp\":\"2026-08-26T12:00:00Z\",\"latitude\":19.0760,\"longitude\":72.8777,\"speed\":60.5,\"harshBrake\":false,\"harshAcceleration\":true,\"ignition\":true,\"batteryVoltage\":12.6,\"crashEvent\":false}"
17. Validation

The backend validates incoming telemetry data.

Current validation includes:

vehicleId
    → Required

timestamp
    → Required

latitude
    → Must be between -90 and 90

longitude
    → Must be between -180 and 180

speed
    → Cannot be negative

batteryVoltage
    → Cannot be negative

Invalid telemetry requests return:

HTTP 400 Bad Request
18. Check Stored Telemetry

Telemetry records can be checked directly from TimescaleDB.

Run:

docker exec -it gps-timescaledb psql -U gps_user -d gps_telemetry -c "SELECT * FROM telemetry ORDER BY timestamp DESC LIMIT 10;"

Example stored record:

vehicle_id: GPS001
timestamp: 2026-08-26 12:00:00+00
latitude: 19.076
longitude: 72.8777
speed: 60.5
harsh_brake: false
harsh_acceleration: true
ignition: true
battery_voltage: 12.6
crash_event: false
19. Postman

Postman files are included in the repository for API testing.

The main API can be tested using:

POST /api/telemetry
GET  /api/telemetry/db-test
20. Current Implementation Status
Completed
Spring Boot backend setup
Telemetry data model
Telemetry validation
REST API
Database connection
PostgreSQL setup
TimescaleDB setup
Docker Compose configuration
Docker volume configuration
Database initialization script
Telemetry table
TimescaleDB hypertable
Timestamp indexing
Telemetry data insertion
Database connection testing
Postman setup
GitHub repository setup
Not Yet Implemented
Direct GPS tracker hardware communication
Tracker-specific communication protocol
GPS packet decoding/parsing
TCP/UDP/MQTT communication with actual tracker
Automatic telemetry acquisition from the physical tracker
Additional data-consumption APIs
Production deployment
21. Future Integration

The current backend is designed so that actual GPS tracker communication can be integrated later.

The expected future flow is:

GPS Tracker
    ↓
Communication Protocol
    ↓
Packet Reception
    ↓
Packet Parsing
    ↓
Telemetry Validation
    ↓
TimescaleDB

The exact communication protocol and packet format will depend on the GPS tracker hardware provided for the project.

22. Important Docker Note

The database data is stored in a Docker volume and is not committed to GitHub.

The repository contains:

docker-compose.yml
database/init.sql

These files are sufficient to create a fresh database environment.

If another PostgreSQL service is already using port 5432, Docker may fail to start the TimescaleDB container.

Check port usage with:

netstat -ano | findstr :5432
23. GitHub Repository

Repository:

https://github.com/Bethuel-Shilesh/gps-telemetry-backend