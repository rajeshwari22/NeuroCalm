# NeuroCalm (Flutter)

Login → Home Dashboard → Bottom sheets (Connection, Massage, History, Settings).
No third-party packages; charts are custom-painted.

## Setup
1. Install Flutter (3.27+).
2. In this folder run `flutter create . --platforms=android` (generates the `android/` platform files without touching `lib/`).
3. Merge `android/app/src/main/AndroidManifest.permissions.xml` into the generated `AndroidManifest.xml`.
4. `flutter pub get` then `flutter run`.

Demo login: any valid email + password of 6+ characters.

## Demo mode
- Backend unavailable → graphs show **Sample data** (labelled).
- Device list = **sample entries, not real Bluetooth results**. "Connecting" is a demo state; no commands are transmitted (`BluetoothService.sendCommand` returns false).

## Backend integration
Edit `ApiConfig` in `lib/services/backend_service.dart` (placeholders, not real endpoints). Live data is expected as
`{"ppg":[...],"bvp":[...],"heartRate":72,"quality":"good"}`; change the parser to match yours. Auth/profile/history/command TODOs are marked in `auth_service.dart` and `backend_service.dart`.

## BLE (ESP32) integration
Add `flutter_blue_plus`, then implement the TODOs in `lib/services/bluetooth_service.dart` (scan, connect, disconnect, sensor stream, command write).

## Safety
App-side: connected device required, Stop always available, timer stop, stop on disconnect/logout, 30-min cap, no automatic start (recommendations only pre-select a mode, and only from live, good-quality data; the rule is a placeholder, not clinical).
**The ESP32 firmware must independently enforce motor runtime limits, safety timeouts and shutdown on BLE disconnect.**
