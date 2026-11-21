import 'massage_model.dart';

class MassageSession {
  final String id;
  final DateTime time;
  final Intensity intensity;
  final MassagePattern pattern;
  final MotorZone zone;
  final int minutes;
  final int? avgHr;
  final int? peakHr;
  const MassageSession({
    required this.id,
    required this.time,
    required this.intensity,
    required this.pattern,
    required this.zone,
    required this.minutes,
    this.avgHr,
    this.peakHr,
  });
}

/// One batch of physiological data. [isLive] false = sample data.
class SensorSnapshot {
  final List<double> ppg;
  final List<double> bvp;
  final int? heartRate;
  final String quality; // 'good' | 'poor' | 'unknown'
  final DateTime time;
  final bool isLive;
  const SensorSnapshot({
    required this.ppg,
    required this.bvp,
    required this.heartRate,
    required this.quality,
    required this.time,
    required this.isLive,
  });
}
