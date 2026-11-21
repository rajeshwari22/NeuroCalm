import 'dart:convert';
import 'dart:io';
import 'dart:math';
import '../models/massage_model.dart';
import '../models/session_model.dart';

/// PLACEHOLDER configuration - update to match YOUR backend. None of these
/// endpoints are assumed to exist.
class ApiConfig {
  static const baseUrl = 'http://10.0.2.2:8000'; // Android emulator -> host machine
  static const login = '/auth/login';
  static const profile = '/user/profile';
  static const liveData = '/sensors/live';
  static const history = '/sessions';
  static const massageCommand = '/massage/command';
}

class BackendService {
  BackendService._();
  static final instance = BackendService._();

  /// Sessions finished in this app run (kept locally in demo mode).
  final List<MassageSession> localSessions = [];

  /// Fetches live sensor data. Expected JSON (adjust to your backend):
  /// {"ppg":[..],"bvp":[..],"heartRate":72,"quality":"good"}
  /// Returns null when unavailable -> UI falls back to sample data.
  Future<SensorSnapshot?> fetchLive() async {
    try {
      final c = HttpClient()..connectionTimeout = const Duration(seconds: 2);
      final req = await c.getUrl(Uri.parse('${ApiConfig.baseUrl}${ApiConfig.liveData}'));
      final res = await req.close().timeout(const Duration(seconds: 3));
      if (res.statusCode != 200) return null;
      final j = jsonDecode(await res.transform(utf8.decoder).join()) as Map<String, dynamic>;
      List<double> l(dynamic v) => (v as List).map((e) => (e as num).toDouble()).toList();
      return SensorSnapshot(
        ppg: l(j['ppg']),
        bvp: l(j['bvp']),
        heartRate: (j['heartRate'] as num?)?.toInt(),
        quality: (j['quality'] as String?) ?? 'unknown',
        time: DateTime.now(),
        isLive: true,
      );
    } catch (_) {
      return null;
    }
  }

  /// Demo waveform shown before connection or when backend is unavailable.
  SensorSnapshot sample() {
    final r = Random(7);
    List<double> wave(double f, double phase) =>
        List.generate(40, (i) => 50 + 25 * sin(i / f + phase) + r.nextDouble() * 8);
    return SensorSnapshot(
      ppg: wave(3, 0),
      bvp: wave(2, 1),
      heartRate: null,
      quality: 'unknown',
      time: DateTime.now(),
      isLive: false,
    );
  }

  /// TODO: GET ApiConfig.history. Returns sample history for now.
  Future<List<MassageSession>> fetchHistory() async {
    final sample = [
      MassageSession(id: 'S-1004', time: DateTime(2025, 11, 20, 16, 15), intensity: Intensity.medium, pattern: MassagePattern.sequentialWave, zone: MotorZone.temple, minutes: 10, avgHr: 74, peakHr: 88),
      MassageSession(id: 'S-1003', time: DateTime(2025, 11, 19, 19, 30), intensity: Intensity.light, pattern: MassagePattern.gradualPulse, zone: MotorZone.both, minutes: 5),
      MassageSession(id: 'S-1002', time: DateTime(2025, 11, 18, 18, 20), intensity: Intensity.heavy, pattern: MassagePattern.alternating, zone: MotorZone.rear, minutes: 5, avgHr: 79, peakHr: 95),
      MassageSession(id: 'S-1001', time: DateTime(2025, 11, 17, 20, 15), intensity: Intensity.medium, pattern: MassagePattern.synchronousPulse, zone: MotorZone.both, minutes: 10, avgHr: 71, peakHr: 84),
    ];
    return [...localSessions.reversed, ...sample];
  }

  /// TODO: GET ApiConfig.profile.
  Future<Map<String, String>> fetchProfile(String? email) async =>
      {'Name': 'Demo User', 'Email': email ?? 'demo@neurocalm.app'};
}
