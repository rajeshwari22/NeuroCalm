import 'dart:async';
import 'package:flutter/foundation.dart';
import '../models/massage_model.dart';
import '../models/session_model.dart';
import 'backend_service.dart';
import 'bluetooth_service.dart';

enum RunState { idle, running, paused }

/// Massage session logic. APP-SIDE safety only (connection required, timer,
/// stop on disconnect). The ESP32 firmware MUST independently enforce motor
/// runtime limits, safety timeouts and BLE-disconnect shutdown.
class MassageService extends ChangeNotifier {
  MassageService._() {
    _bt.addListener(_onBluetooth);
  }
  static final instance = MassageService._();

  static const maxMinutes = 30; // app-side cap; firmware must enforce its own
  final _bt = BluetoothService.instance;

  Intensity intensity = Intensity.medium;
  MassagePattern pattern = MassagePattern.sequentialWave;
  MotorZone zone = MotorZone.both;
  int minutes = Intensity.medium.defaultMinutes;

  RunState state = RunState.idle;
  int remaining = 0;
  int total = 0;
  String? message;
  Timer? _timer;

  bool get active => state != RunState.idle;
  double get progress => total == 0 ? 0 : 1 - remaining / total;

  void setIntensity(Intensity i) {
    intensity = i;
    minutes = i.defaultMinutes;
    notifyListeners();
  }

  void setPattern(MassagePattern p) { pattern = p; notifyListeners(); }
  void setZone(MotorZone z) { zone = z; notifyListeners(); }
  void setMinutes(int m) { minutes = m.clamp(1, maxMinutes); notifyListeners(); }

  /// Loads a past session's settings. Does NOT start the motors.
  void applySession(MassageSession s) {
    if (active) return;
    intensity = s.intensity;
    pattern = s.pattern;
    zone = s.zone;
    minutes = s.minutes;
    notifyListeners();
  }

  Future<void> start() async {
    if (active) return;
    if (!_bt.isConnected) {
      message = 'Connect a device before starting a massage.';
      notifyListeners();
      return;
    }
    message = null;
    total = remaining = minutes * 60;
    state = RunState.running;
    await _send('START;${intensity.name};${pattern.name};${zone.name};$total');
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (state != RunState.running) return;
      remaining--;
      if (remaining <= 0) {
        stop(reason: 'Session complete.');
      } else {
        notifyListeners();
      }
    });
    notifyListeners();
  }

  Future<void> pause() async {
    if (state != RunState.running) return;
    state = RunState.paused;
    await _send('PAUSE');
    notifyListeners();
  }

  Future<void> resume() async {
    if (state != RunState.paused) return;
    state = RunState.running;
    await _send('RESUME');
    notifyListeners();
  }

  Future<void> stop({String? reason}) async {
    if (!active) return;
    _timer?.cancel();
    final elapsedMin = ((total - remaining) / 60).round();
    state = RunState.idle;
    message = reason ?? 'Session stopped.';
    await _send('STOP');
    if (elapsedMin > 0) {
      BackendService.instance.localSessions.add(MassageSession(
        id: 'L-${DateTime.now().millisecondsSinceEpoch % 10000}',
        time: DateTime.now(),
        intensity: intensity,
        pattern: pattern,
        zone: zone,
        minutes: elapsedMin,
      ));
    }
    notifyListeners();
  }

  void _onBluetooth() {
    if (active && !_bt.isConnected) stop(reason: 'Device disconnected - session stopped.');
  }

  // Returns silently in demo mode; real transmission is in BluetoothService.
  Future<void> _send(String cmd) => _bt.sendCommand(cmd);

  @override
  void dispose() {
    _timer?.cancel();
    _bt.removeListener(_onBluetooth);
    super.dispose();
  }
}
