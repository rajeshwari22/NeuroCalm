# NeuroCalm Backend

Spring Boot (Java 21) backend for **NeuroCalm - Smart Stress-Aware Head Massager**.

> **Status:** the code was written without being compiled or tested (no Maven/JDK compiler was available when it was generated). Run `mvn test` first and fix any small compile error that shows up.

## 1. What it does
The ESP32-C3 talks to the Flutter app over BLE. **Flutter forwards telemetry to this backend** (the backend never talks to the ESP32). The backend:
- stores raw PPG samples and motor events (3 motors: LEFT, RIGHT, REAR) with timestamps,
- estimates heart rate (basic peak counting) and rates signal quality (GOOD / MODERATE / POOR),
- matches PPG timestamps with motor activity and reports *possible* motor interference,
- manages massage sessions and creates a summary when a session ends,
- serves history to Flutter and pushes live updates over a WebSocket.

**Important:** nothing here is clinically validated. Heart rate may be *estimated*, interference detection is a basic heuristic and does **not** remove motion artifacts. Raw data is always kept, and measured vs. derived values are marked (`heartRateEstimated`, `derivedPulse`).

## 2. Folder structure
```
neurocalm_backend/
  pom.xml  README.md  .gitignore
  src/main/java/com/neurocalm/
    NeuroCalmApplication.java
    config/       CORS, WebSocket, database setup
    controller/   REST endpoints (HTTP only)
    service/      business logic (telemetry, PPG, quality, motor correlation, sessions, history, users)
    model/        JPA entities (User, Session, PpgReading, MotorEvent, SessionSummary)
    repository/   Spring Data JPA repositories
    dto/          request/response objects
    websocket/    WebSocket handler + broadcaster
    enums/        MotorZone, SignalQuality, SessionStatus, ActivityType, MassageIntensity
    exception/    error handling
  src/main/resources/application.properties
  src/main/resources/db/migration/V1__initial_schema.sql
  src/test/java/com/neurocalm/   unit tests (JUnit 5 + Mockito)
```

## 3. Required software
Java 21 JDK, Maven 3.9+, PostgreSQL 14+, Postman (or curl), VS Code or IntelliJ IDEA.

## 4. Install Java 21
- Windows: `winget install EclipseAdoptium.Temurin.21.JDK`
- macOS: `brew install --cask temurin@21`
- Ubuntu: `sudo apt install openjdk-21-jdk`

Check: `java -version` and `mvn -version` (both must work; Maven must use Java 21).
Install Maven if needed: `brew install maven` / `sudo apt install maven` / `winget install Apache.Maven`.

## 5. Configure PostgreSQL and create the database
```
psql -U postgres
CREATE DATABASE neurocalm;
\q
```
Tables are created automatically by Flyway (`V1__initial_schema.sql`) when the app starts.

## 6. Database credentials (environment variables)
| Variable | Default | Meaning |
|---|---|---|
| `DB_HOST` | localhost | database host |
| `DB_PORT` | 5432 | database port |
| `DB_NAME` | neurocalm | database name |
| `DB_USERNAME` | postgres | user |
| `DB_PASSWORD` | *(none - required)* | password |

macOS / Linux: `export DB_PASSWORD='your_password'`
Windows PowerShell: `$env:DB_PASSWORD="your_password"`

## 7. Import into an IDE
- IntelliJ: *File > Open* and choose the `neurocalm_backend` folder (it detects `pom.xml`).
- VS Code: install "Extension Pack for Java" and "Spring Boot Extension Pack", then *File > Open Folder*.
Set the `DB_PASSWORD` environment variable in the run configuration.

## 8. Install dependencies and run
```
cd neurocalm_backend
mvn clean install -DskipTests     # downloads dependencies
mvn test                          # runs unit tests (no database needed)
mvn spring-boot:run               # starts on http://localhost:8080
```

## 9. Testing with Postman (suggested order)
1. `POST /api/users` -> note the user `id`.
2. `POST /api/sessions/start` -> note the session `id`.
3. Connect a WebSocket client to `ws://localhost:8080/ws/monitoring` (Postman > New > WebSocket).
4. `POST /api/telemetry` several times (needs about 3+ seconds of samples, e.g. 75+ samples at 25 Hz, for a heart-rate estimate).
5. `POST /api/sessions/{id}/end`, then `GET /api/history/{userId}`.

## 10. REST endpoints
| Method | Path | Purpose | Success |
|---|---|---|---|
| POST | `/api/users` | create user | 201 |
| GET | `/api/users/{userId}` | get user | 200 |
| POST | `/api/sessions/start` | start session | 201 |
| POST | `/api/sessions/{sessionId}/end` | end session + summary (body optional) | 200 |
| GET | `/api/sessions/{sessionId}` | session info | 200 |
| POST | `/api/telemetry` | send PPG + motor data (session must be ACTIVE) | 201 |
| GET | `/api/telemetry/{sessionId}` | all stored PPG readings and motor events | 200 |
| GET | `/api/history/{userId}` | all sessions of a user (newest first) | 200 |
| GET | `/api/history/{userId}/{sessionId}` | one session with PPG + motor events | 200 |
| WS | `/ws/monitoring` | live updates | - |

Errors return JSON: `{"timestamp","status","error","message"}` with 400 (invalid data), 404 (not found), 409 (conflict, e.g. session already ended / email already used), 500.

## 11. Example JSON
**POST /api/users**
```json
{ "name": "Asha", "email": "asha@example.com" }
```
Response 201:
```json
{ "id": 1, "name": "Asha", "email": "asha@example.com", "createdAt": "2025-11-21T10:00:00Z" }
```

**POST /api/sessions/start** (`intensity`: LIGHT | MEDIUM | HEAVY)
```json
{ "userId": 1, "intensity": "LIGHT", "rhythm": "sequential-wave" }
```
Response 201:
```json
{ "id": 1, "userId": 1, "startTime": "2025-11-21T10:00:00Z", "endTime": null, "status": "ACTIVE",
  "intensity": "LIGHT", "rhythm": "sequential-wave", "durationSeconds": 0, "summary": null }
```

**POST /api/telemetry** (`motors`, `heartRate`, `timestamp` and `bvp` are optional; `zone`: LEFT | RIGHT | REAR; `pwmDuty` is 0-100 %)
```json
{
  "sessionId": 1,
  "heartRate": null,
  "ppg": [
    { "timestamp": "2025-11-21T10:00:01.000Z", "value": 100, "bvp": 0.12 },
    { "timestamp": "2025-11-21T10:00:01.040Z", "value": 105, "bvp": 0.15 }
  ],
  "motors": [
    { "zone": "LEFT",  "active": true,  "pwmDuty": 60, "rhythm": "sequential-wave", "timestamp": "2025-11-21T10:00:01.000Z" },
    { "zone": "REAR",  "active": false, "pwmDuty": 0 }
  ]
}
```
Motor events are stored only when a motor changes state (on / off / intensity change), so Flutter may repeat the current state in every request. Invalid samples (`"value": null`) are skipped safely.

Response 201 (same JSON is broadcast on the WebSocket):
```json
{
  "sessionId": 1,
  "ppg": [100.0, 105.0],
  "bvp": [0.12, 0.15],
  "heartRate": 72,
  "heartRateEstimated": true,
  "quality": "good",
  "motors": [
    { "zone": "LEFT",  "active": true,  "pwmDuty": 60, "rhythm": "sequential-wave", "timestamp": "2025-11-21T10:00:01Z" },
    { "zone": "RIGHT", "active": false, "pwmDuty": 0,  "rhythm": null, "timestamp": "2025-11-21T10:00:01.040Z" },
    { "zone": "REAR",  "active": false, "pwmDuty": 0,  "rhythm": null, "timestamp": "2025-11-21T10:00:01.040Z" }
  ],
  "motorInterferenceSuspected": false,
  "motorInterferenceNote": "Motor active; no baseline yet, so interference cannot be assessed.",
  "derivedPulse": [-2.5, 2.5],
  "sessionDurationSeconds": 1,
  "timestamp": "2025-11-21T10:00:01.040Z",
  "dataNote": "ppg/bvp are measured. heartRate is estimated when heartRateEstimated=true. derivedPulse is calculated. Not clinically validated."
}
```
`heartRate` is `null` when the signal is too short or `quality` is `poor`. If Flutter sends a plausible `heartRate` (30-220) it is used as measured (`heartRateEstimated: false`). The Flutter-compatible fields are `ppg`, `bvp`, `heartRate`, `quality`.

**POST /api/sessions/{id}/end** (optional body `{ "note": "felt calm" }`) returns the session with:
```json
"summary": { "durationSeconds": 600, "totalReadings": 15000, "goodReadings": 12000, "moderateReadings": 2500,
  "poorReadings": 500, "readingsDuringMotorActivity": 9000, "poorReadingsDuringMotor": 400,
  "averageHeartRate": 71.8, "peakHeartRate": 84, "note": "Heart rate values may be estimated and are not clinically validated." }
```
History/telemetry endpoints use enum-style quality (`"GOOD"`); the live message uses lowercase (`"good"`).

## 12. WebSocket
- URL: `ws://<host>:8080/ws/monitoring` (Android emulator: `ws://10.0.2.2:8080/ws/monitoring`).
- Receive-only: every client gets each new telemetry result as a JSON text message (all sessions; filter by `sessionId` in the app if needed).
- Flutter example (needs the `web_socket_channel` package):
```dart
final channel = WebSocketChannel.connect(Uri.parse('ws://10.0.2.2:8080/ws/monitoring'));
channel.stream.listen((message) {
  final data = jsonDecode(message as String);   // data['ppg'], data['bvp'], data['heartRate'], data['quality']
});
```

## 13. Connect the Flutter app
In the Flutter project open `lib/services/backend_service.dart` and set `ApiConfig.baseUrl`:
- Android emulator: `http://10.0.2.2:8080`
- Real phone: `http://<your-computer-LAN-IP>:8080` (same Wi-Fi; allow port 8080 in your firewall)

Notes for the Flutter developer:
- The placeholder paths in that file (`/auth/login`, `/sensors/live`, `/sessions`) do **not** exist here. Use the endpoints in section 10.
- This backend has no GET "live" endpoint. Live data arrives through the WebSocket (section 12), so replace the polling in `home_screen.dart` with a WebSocket listener.
- There is **no authentication** yet (users are created with `POST /api/users`). Add authentication before any real use.
- Plain `http://` needs `android:usesCleartextTraffic="true"` in `AndroidManifest.xml` during development.

## 14. Common errors
| Problem | Solution |
|---|---|
| `Could not resolve placeholder 'DB_PASSWORD'` | Set the `DB_PASSWORD` environment variable (section 6). |
| `password authentication failed` | Wrong `DB_USERNAME` / `DB_PASSWORD`. |
| `database "neurocalm" does not exist` | Run `CREATE DATABASE neurocalm;` |
| `Connection refused` to port 5432 | PostgreSQL is not running; start the service. |
| `release version 21 not supported` | Maven is using an older JDK; set `JAVA_HOME` to JDK 21. |
| `Port 8080 already in use` | `export SERVER_PORT=8081` and restart. |
| Flyway checksum error | You edited an applied migration. Drop and recreate the dev database or add a `V2__...sql` file instead. |
| `404 Session not found` / `400 Session ... is not active` | Start a session first; telemetry is rejected after the session has ended. |
| Phone cannot reach the backend | Use your PC's LAN IP, same Wi-Fi, open port 8080. |

## 15. Limitations
- Heart-rate estimation and quality rules are simple heuristics for a first version.
- Interference assessment compares signal variability with earlier no-motor readings; it only reports a *possibility*.
- No authentication/authorization, no rate limiting, CORS is open to all origins (development setting).
