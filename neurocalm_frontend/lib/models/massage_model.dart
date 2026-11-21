enum Intensity {
  light('Light', 5, 'Gentle vibration'),
  medium('Medium', 10, 'Moderate vibration'),
  heavy('Heavy', 5, 'Stronger vibration (within validated hardware limits)');

  final String label;
  final int defaultMinutes;
  final String description;
  const Intensity(this.label, this.defaultMinutes, this.description);
}

enum MassagePattern {
  sequentialWave('Sequential Wave'),
  synchronousPulse('Synchronous Pulse'),
  alternating('Alternating Mode'),
  gradualPulse('Gradual Pulse'),
  custom('Custom Mode');

  final String label;
  const MassagePattern(this.label);
}

enum MotorZone {
  both('Both Zones'),
  temple('Temple Zone'),
  rear('Rear Zone');

  final String label;
  const MotorZone(this.label);
}
